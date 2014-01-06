//////////////////////////////////////////////////////////////////////////////
// FILE:          StepMotorCtrl.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   StepMotor Controller Driver
//


#ifdef WIN32
#include <windows.h>
#define snprintf _snprintf
#endif

#include <stdio.h>
#include <string>
#include <fstream>
#include <iostream>
#include <sstream>
#include <math.h>
#include "../../MMCore/MMCore.h"
#include "../../MMDevice/ModuleInterface.h"
#include "../../MMDevice/DeviceUtils.h"
#include "StepMotorError.h"
#include "StepMotor.h"
#include "StepMotorCtrl.h"

using namespace std;


//error code

#define C51DEVICE_OK 0x01 +'I'-1
#define C51DEVICE_BUSY 0x02 +'J'-2
#define C51OUT_OF_LOW_LIMIT 0x03 +'K'-3
#define C51OUT_OF_HIGH_LIMIT 0x04 +'L'-4
#define C51CHECK_SUM_ERROR 0x05  +'M'-5
#define C51BAD_COMMAND	    0x06 +'N'-6

//command string
#define SetZeroPosition 0x07 +'Z'-7
#define MoveUp	        0x08 +'U'-8
#define MoveDown	    0x09 +'D'-9
#define SetRunningDelay 0x0A +'R'-10
#define SetStartDelay 	0x0B +'S'-11
#define FindLimit		0x0C +'L'-12
#define ReleasePower	0x0D +'P'-13
#define QueryPosition   0x0E +'Q'-14
#define QueryStage   	0x0F +'E'-15
#define SetPosition	    0x10 + 'T' - 16
#define SetUM2Step	    0x11 + 'M'-17

//////////////////////////////////////////////////////
// StepMotor Controller
//////////////////////////////////////////////////////
//
// Controller - Controller for XYZ Stage.
// Note that this adapter uses two coordinate systems.  There is the adapters own coordinate
// system with the X and Y axis going the 'Micro-Manager standard' direction
// Then, there is the StepMotors native system.  All functions using 'steps' use the StepMotor system
// All functions using Um use the Micro-Manager coordinate system
//


//
// StepMotor Controller Constructor
//
StepMotorCtrl::StepMotorCtrl() :
    										m_yInitialized(false)       // initialized flag set to false
{
	// call initialization of error messages
	InitializeDefaultErrorMessages();

	m_nAnswerTimeoutMs = StepMotor::Instance()->GetTimeoutInterval();
	m_nAnswerTimeoutTrys = StepMotor::Instance()->GetTimeoutTrys();

	// Port:
	CPropertyAction* pAct = new CPropertyAction(this, &StepMotorCtrl::OnPort);
	int ret = CreateProperty(MM::g_Keyword_Port, "Undefined", MM::String, false, pAct, true);

}

//
// StepMotor Controller Destructor
//
StepMotorCtrl::~StepMotorCtrl()
{
	Shutdown();
}

//
// return device name of the StepMotor controller
//
void StepMotorCtrl::GetName(char* sName) const
{
	CDeviceUtils::CopyLimitedString(sName, StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_CtrlDevName).c_str());
}

//
// Initialize the StepMotor controller
//
int StepMotorCtrl::Initialize()
{
	std::ostringstream osMessage;

	// empty the Rx serial buffer before sending command
	int ret = DEVICE_OK ;
	ret = ClearPort(*this, *GetCoreCallback(), StepMotor::Instance()->GetSerialPort().c_str());
	if (ret != DEVICE_OK) return ret;

	// Name
	char sCtrlName[120];
	sprintf(sCtrlName, "%s%s", StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_CtrlDevNameLabel).c_str(), MM::g_Keyword_Name);
	ret = CreateProperty(sCtrlName, StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_CtrlDevName).c_str(), MM::String, true);
 	if (ret != DEVICE_OK) return ret;

	// Description
	char sCtrlDesc[120];
	sprintf(sCtrlDesc, "%s%s", StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_CtrlDevDescLabel).c_str(), MM::g_Keyword_Description);
	ret = CreateProperty(sCtrlDesc, "Sutter MP-285 Controller", MM::String, true);
	if (ret != DEVICE_OK)  return ret;

	ret = CreateProperty(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_StepMotorVerLabel).c_str(), StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_StepMotorVersion).c_str(), MM::String, true);
	if (ret != DEVICE_OK) return ret;

	char sTimeoutInterval[20];
	memset(sTimeoutInterval, 0, 20);
	sprintf(sTimeoutInterval, "%d", StepMotor::Instance()->GetTimeoutInterval());
	CPropertyAction* pActOnTimeoutInterval = new CPropertyAction(this, &StepMotorCtrl::OnTimeoutInterval);
	ret = CreateProperty(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_TimeoutInterval).c_str(), sTimeoutInterval, MM::Integer,  false, pActOnTimeoutInterval);
	if (ret != DEVICE_OK) return ret;

	char sTimeoutTrys[20];
	memset(sTimeoutTrys, 0, 20);
	sprintf(sTimeoutTrys, "%d", StepMotor::Instance()->GetTimeoutTrys());
	CPropertyAction* pActOnTimeoutTrys = new CPropertyAction(this, &StepMotorCtrl::OnTimeoutTrys);
	ret = CreateProperty(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_TimeoutTrys).c_str(), sTimeoutTrys, MM::Integer,  false, pActOnTimeoutTrys);
	if (ret != DEVICE_OK) return ret;

	// Read status data
	unsigned int nLength = 256;
	unsigned char sResponse[256];
	ret = CheckStatus(sResponse, nLength);
	if (ret != DEVICE_OK) return ret;

	ret = UpdateStatus();
	if (ret != DEVICE_OK) return ret;

	// Create  property for debug log flag
	int nDebugLogFlag = StepMotor::Instance()->GetDebugLogFlag();
	CPropertyAction* pActDebugLogFlag = new CPropertyAction (this, &StepMotorCtrl::OnDebugLogFlag);
	ret = CreateProperty(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_DebugLogFlagLabel).c_str(), CDeviceUtils::ConvertToString(nDebugLogFlag), MM::Integer, true, pActDebugLogFlag);
	m_yInitialized = true;
	StepMotor::Instance()->SetDeviceAvailable(true);
	return DEVICE_OK;
}

//
// check controller's status bytes
//
int StepMotorCtrl::CheckStatus(unsigned char* sResponse, unsigned int nLength)
{
	std::ostringstream osMessage;
	unsigned char buf[8];
	int ret = DEVICE_OK ;
	StepMotor::Instance()->PackageCommand(QueryStage,NULL,buf);
	ret = WriteCommand(buf, 8);
	if (ret != DEVICE_OK) return ret;
	Sleep(800);
	memset(sResponse, 0, nLength);
	ret = ReadMessage(sResponse, 10);
	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

//
// shutdown the controller
//
int StepMotorCtrl::Shutdown()
{ 
	m_yInitialized = false;
	StepMotor::Instance()->SetDeviceAvailable(false);
	return DEVICE_OK;
}

//////////////// Action Handlers (Hub) /////////////////

//
// check for valid communication port
//
int StepMotorCtrl::OnPort(MM::PropertyBase* pProp, MM::ActionType pAct)
{
	std::ostringstream osMessage;

	osMessage.str("");

	if (pAct == MM::BeforeGet)
	{
		pProp->Set(StepMotor::Instance()->GetSerialPort().c_str());
	}
	else if (pAct == MM::AfterSet)
	{
		if (m_yInitialized)
		{
			pProp->Set(StepMotor::Instance()->GetSerialPort().c_str());
			return DEVICE_INVALID_INPUT_PARAM;
		}
		pProp->Get(StepMotor::Instance()->GetSerialPort());
	}
	return DEVICE_OK;
}

//
// get/set debug log flag
//
int StepMotorCtrl::OnDebugLogFlag(MM::PropertyBase* pProp, MM::ActionType pAct)
{
	long lDebugLogFlag = (long)StepMotor::Instance()->GetDebugLogFlag();
	std::ostringstream osMessage;

	osMessage.str("");

	if (pAct == MM::BeforeGet)
	{
		pProp->Set(lDebugLogFlag);
	}
	else if (pAct == MM::AfterSet)
	{
		pProp->Get(lDebugLogFlag);
		StepMotor::Instance()->SetDebugLogFlag((int)lDebugLogFlag);
	}

	return DEVICE_OK;
}


/*
 * Set/Get Timeout Interval
 */
int StepMotorCtrl::OnTimeoutInterval(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	long lTimeoutInterval = (long)StepMotor::Instance()->GetTimeoutInterval();
	int ret = DEVICE_OK;

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(lTimeoutInterval);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(lTimeoutInterval);
		StepMotor::Instance()->SetTimeoutInterval((int)lTimeoutInterval);
	}

	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}

/*
 * Set/Get Timeout Trys
 */
int StepMotorCtrl::OnTimeoutTrys(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	long lTimeoutTrys = (long)StepMotor::Instance()->GetTimeoutTrys();
	int ret = DEVICE_OK;

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(lTimeoutTrys);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(lTimeoutTrys);
		StepMotor::Instance()->SetTimeoutTrys((int)lTimeoutTrys);
	}

	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}

///////////////////////////////////////////////////////////////////////////////
// Internal, helper methods
///////////////////////////////////////////////////////////////////////////////

//
// Write a coomand to serial port
//
int StepMotorCtrl::WriteCommand(unsigned char* sCommand, int nLength)
{
	int ret = DEVICE_OK;
	ostringstream osMessage;

	if (StepMotor::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<StepMotorCtrl::WriteCommand> (Command=";
		for (int n=0; n < nLength; n++)
		{
			osMessage << "[" << (char)sCommand[n] << "|" << (byte)sCommand[n] << "]";
		}
		osMessage << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	for (int nBytes = 0; nBytes < nLength && ret == DEVICE_OK; nBytes++)
	{
		ret = WriteToComPort(StepMotor::Instance()->GetSerialPort().c_str(), (const unsigned char*)&sCommand[nBytes], 1);
		CDeviceUtils::SleepMs(1);
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

//
// Read a message from serial port
//
int StepMotorCtrl::ReadMessage(unsigned char* sResponse, int nBytesRead)
{
	// block/wait for acknowledge, or until we time out;
	unsigned int nLength = 256;
	unsigned char sAnswer[256];
	memset(sAnswer, 0, nLength);
	unsigned long lRead = 0;
	unsigned long lStartTime = GetClockTicksUs();

	ostringstream osMessage;
	char sHex[4] = { NULL, NULL, NULL, NULL };
	int ret = DEVICE_OK;
	bool yRead = false;
	bool yTimeout = false;
	while (!yRead && !yTimeout && ret == DEVICE_OK )
	{
		unsigned long lByteRead;

		const MM::Device* pDevice = this;
		ret = (GetCoreCallback())->ReadFromSerial(pDevice, StepMotor::Instance()->GetSerialPort().c_str(), (unsigned char *)&sAnswer[lRead], (unsigned long)nLength-lRead, lByteRead);

		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage.str("");
			osMessage << "<StepMotorCtrl::ReadMessage> (ReadFromSerial = (" << nBytesRead << "," << lRead << "," << lByteRead << ")::<";

			for (unsigned long lIndx=0; lIndx < lByteRead; lIndx++)
			{
				osMessage << "[" <<(char)sAnswer[lRead+lIndx]<<"|"<< (int)sAnswer[lRead+lIndx] << "]";
			}
			osMessage << ">";
			this->LogMessage(osMessage.str().c_str());
		}

		// concade new string
		lRead += lByteRead;

		if (lRead >= 2)
		{
			yRead = (sAnswer[0] == '@') ;
		}

		yRead = yRead || (lRead >= (unsigned long)nBytesRead);

		if (yRead) break;

		// check for timeout
		yTimeout = ((double)(GetClockTicksUs() - lStartTime)) > (double) m_nAnswerTimeoutMs;
		if (!yTimeout) CDeviceUtils::SleepMs(3);

	}

	return DEVICE_OK;
}
