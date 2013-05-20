//////////////////////////////////////////////////////////////////////////////
// FILE:          XMTE517Ctrl.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   XMTE517 Controller Driver
//
// COPYRIGHT:     Sutter Instrument,
//				  Mission Bay Imaging, San Francisco, 2011
//                All rights reserved
//
// LICENSE:       This library is free software; you can redistribute it and/or
//                modify it under the terms of the GNU Lesser General Public
//                License as published by the Free Software Foundation.
//                
//                You should have received a copy of the GNU Lesser General Public
//                License along with the source distribution; if not, write to
//                the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
//                Boston, MA  02111-1307  USA
//
//                This file is distributed in the hope that it will be useful,
//                but WITHOUT ANY WARRANTY; without even the implied warranty
//                of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//                IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//                CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
//
// AUTHOR:        Lon Chu (lonchu@yahoo.com), created on June 2011
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
//#include <strsafe.h>
#include "../../MMCore/MMCore.h"
#include "../../MMDevice/ModuleInterface.h"
#include "../../MMDevice/DeviceUtils.h"
#include "XMTE517Error.h"
#include "XMTE517.h"
#include "XMTE517Ctrl.h"

using namespace std;


//////////////////////////////////////////////////////
// XMTE517 Controller
//////////////////////////////////////////////////////
//
// Controller - Controller for XYZ Stage.
// Note that this adapter uses two coordinate systems.  There is the adapters own coordinate
// system with the X and Y axis going the 'Micro-Manager standard' direction
// Then, there is the XMTE517s native system.  All functions using 'steps' use the XMTE517 system
// All functions using Um use the Micro-Manager coordinate system
//


//
// XMTE517 Controller Constructor
//
XMTE517Ctrl::XMTE517Ctrl() :
    										//m_nAnswerTimeoutMs(1000),   // wait time out set 1000 ms
    										m_yInitialized(false)       // initialized flag set to false
{
	// call initialization of error messages
	InitializeDefaultErrorMessages();

	m_nAnswerTimeoutMs = XMTE517::Instance()->GetTimeoutInterval();
	m_nAnswerTimeoutTrys = XMTE517::Instance()->GetTimeoutTrys();

	// Port:
	CPropertyAction* pAct = new CPropertyAction(this, &XMTE517Ctrl::OnPort);
	int ret = CreateProperty(MM::g_Keyword_Port, "Undefined", MM::String, false, pAct, true);

	std::ostringstream osMessage;

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::class-constructor> CreateProperty(" << MM::g_Keyword_Port << " = Undfined), ReturnCode=" << ret;
		this->LogMessage(osMessage.str().c_str());
	}
}

//
// XMTE517 Controller Destructor
//
XMTE517Ctrl::~XMTE517Ctrl()
{
	Shutdown();
}

//
// return device name of the XMTE517 controller
//
void XMTE517Ctrl::GetName(char* sName) const
{
	CDeviceUtils::CopyLimitedString(sName, XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevName).c_str());
}

//
// Initialize the XMTE517 controller
//
int XMTE517Ctrl::Initialize()
{
	std::ostringstream osMessage;

	// empty the Rx serial buffer before sending command
	int ret = ClearPort(*this, *GetCoreCallback(), XMTE517::Instance()->GetSerialPort().c_str());

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> ClearPort(Port = " << XMTE517::Instance()->GetSerialPort().c_str() << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	// Name
	char sCtrlName[120];
	sprintf(sCtrlName, "%s%s", XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevNameLabel).c_str(), MM::g_Keyword_Name);
	ret = CreateProperty(sCtrlName, XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevName).c_str(), MM::String, true);

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> CreateProperty(" << sCtrlName << " = " << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevName).c_str() << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	// Description
	char sCtrlDesc[120];
	sprintf(sCtrlDesc, "%s%s", XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevDescLabel).c_str(), MM::g_Keyword_Description);
	ret = CreateProperty(sCtrlDesc, "Sutter MP-285 Controller", MM::String, true);

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> CreateProperty(" << sCtrlDesc << " = " << "Sutter MP-285 Controller" << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK)  return ret;

	// Create read-only property for version info
	// XMTE517 Adpater Version Property
	// const char* sVersionLabel = XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_XMTE517VerLabel).c_str();
	// const char* sVersion = XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_XMTE517Version).c_str();
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_XMTE517VerLabel).c_str(), XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_XMTE517Version).c_str(), MM::String, true);

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_XMTE517VerLabel).c_str() << " = " << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_XMTE517Version).c_str() << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	char sTimeoutInterval[20];
	memset(sTimeoutInterval, 0, 20);
	sprintf(sTimeoutInterval, "%d", XMTE517::Instance()->GetTimeoutInterval());

	CPropertyAction* pActOnTimeoutInterval = new CPropertyAction(this, &XMTE517Ctrl::OnTimeoutInterval);
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutInterval).c_str(), sTimeoutInterval, MM::Integer,  false, pActOnTimeoutInterval);

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutInterval).c_str() << " = " << sTimeoutInterval << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	char sTimeoutTrys[20];
	memset(sTimeoutTrys, 0, 20);
	sprintf(sTimeoutTrys, "%d", XMTE517::Instance()->GetTimeoutTrys());

	CPropertyAction* pActOnTimeoutTrys = new CPropertyAction(this, &XMTE517Ctrl::OnTimeoutTrys);
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutTrys).c_str(), sTimeoutTrys, MM::Integer,  false, pActOnTimeoutTrys);

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutTrys).c_str() << " = " << sTimeoutTrys << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;


	// Read status data
	unsigned int nLength = 256;
	unsigned char sResponse[256];
	ret = CheckStatus(sResponse, nLength);

	if (ret != DEVICE_OK) return ret;

	bool yCommError = CheckError(sResponse[0]);

	char sCommStat[30];
	if (yCommError)
		sprintf((char*)sCommStat, "Error Code ==> <%2x>", sResponse[0]);
	else
		strcpy(sCommStat, "Success");
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CommStateLabel).c_str(), sCommStat, MM::String, true);

	XMTE517::Instance()->SetUm2UStep(1);
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_Um2UStepUnit).c_str(), "1", MM::Integer, true);

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_Um2UStepUnit).c_str() << " = s[" << 1 << "] ), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;


	XMTE517::Instance()->SetMotionMode(0);//Fast
	char sMotionMode[20];
	memset(sMotionMode, 0, 20);
	sprintf(sMotionMode, "0");
	XMTE517::Instance()->SetDebugLogFlag(2);
	CPropertyAction* pActOnMotionMode = new CPropertyAction(this, &XMTE517Ctrl::OnMotionMode);
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str(), "Undefined", MM::Integer, false, pActOnMotionMode);  // Absolute  vs Relative

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str() << " = Undefined), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK)  return ret;

	ret = UpdateStatus();
	if (ret != DEVICE_OK) return ret;

	// Create  property for debug log flag
	int nDebugLogFlag = XMTE517::Instance()->GetDebugLogFlag();
	CPropertyAction* pActDebugLogFlag = new CPropertyAction (this, &XMTE517Ctrl::OnDebugLogFlag);
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_DebugLogFlagLabel).c_str(), CDeviceUtils::ConvertToString(nDebugLogFlag), MM::Integer, true, pActDebugLogFlag);

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "XMTE517Ctrl::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_DebugLogFlagLabel).c_str() << " = " << nDebugLogFlag << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	m_yInitialized = true;
	XMTE517::Instance()->SetDeviceAvailable(true);

	return DEVICE_OK;
}

//
// check controller's status bytes
//
int XMTE517Ctrl::CheckStatus(unsigned char* sResponse, unsigned int nLength)
{
	std::ostringstream osMessage;
	unsigned char buf[9];
	XMTE517::Instance()->PackageCommand("ABC",NULL,buf);
	int ret = WriteCommand(buf, 9);
	if (ret != DEVICE_OK) return ret;

	XMTE517::Instance()->PackageCommand("WEA",NULL,buf);
	ret = WriteCommand(buf, 9);
	if (ret != DEVICE_OK) return ret;

	XMTE517::Instance()->PackageCommand("TQL",NULL,buf);
	ret = WriteCommand(buf, 9);
	if (ret != DEVICE_OK) return ret;

	XMTE517::Instance()->PackageCommand("RAA",NULL,buf);
	ret = WriteCommand(buf, 9);
	if (ret != DEVICE_OK) return ret;



	memset(sResponse, 0, nLength);
	ret = ReadMessage(sResponse, 7);

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::CheckStatus::ReadMessage> (ReturnCode = " << ret << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

//
// shutdown the controller
//
int XMTE517Ctrl::Shutdown()
{ 
	m_yInitialized = false;
	XMTE517::Instance()->SetDeviceAvailable(false);
	return DEVICE_OK;
}

//////////////// Action Handlers (Hub) /////////////////

//
// check for valid communication port
//
int XMTE517Ctrl::OnPort(MM::PropertyBase* pProp, MM::ActionType pAct)
{
	std::ostringstream osMessage;

	osMessage.str("");

	if (pAct == MM::BeforeGet)
	{
		pProp->Set(XMTE517::Instance()->GetSerialPort().c_str());
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnPort> (BeforeGet::PORT=<" << XMTE517::Instance()->GetSerialPort().c_str() << ">";
			osMessage << " PROPSET=<" << XMTE517::Instance()->GetSerialPort().c_str() << ">)";
		}
	}
	else if (pAct == MM::AfterSet)
	{
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnPort> (AfterSet::PORT=<" << XMTE517::Instance()->GetSerialPort().c_str() << ">";
		}
		if (m_yInitialized)
		{
			pProp->Set(XMTE517::Instance()->GetSerialPort().c_str());
			if (XMTE517::Instance()->GetDebugLogFlag() > 1)
			{
				osMessage << "Initialized::SET=<" << XMTE517::Instance()->GetSerialPort().c_str() << ">";
				this->LogMessage(osMessage.str().c_str());
			}
			return DEVICE_INVALID_INPUT_PARAM;
		}
		pProp->Get(XMTE517::Instance()->GetSerialPort());
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << " SPROPGET=<" << XMTE517::Instance()->GetSerialPort().c_str() << ">)";
		}
	}
	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
	}
	return DEVICE_OK;
}

//
// get/set debug log flag
//
int XMTE517Ctrl::OnDebugLogFlag(MM::PropertyBase* pProp, MM::ActionType pAct)
{
	long lDebugLogFlag = (long)XMTE517::Instance()->GetDebugLogFlag();
	std::ostringstream osMessage;

	osMessage.str("");

	if (pAct == MM::BeforeGet)
	{
		pProp->Set(lDebugLogFlag);
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnDebugLogFalg> (BeforeGet::<" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_DebugLogFlagLabel).c_str() << "> PROPSET=<" << lDebugLogFlag << ">)";
		}
	}
	else if (pAct == MM::AfterSet)
	{
		pProp->Get(lDebugLogFlag);
		XMTE517::Instance()->SetDebugLogFlag((int)lDebugLogFlag);
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnDebugLogFalg> (AfterSet::<" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_DebugLogFlagLabel).c_str() << "> PROPSET=<" << lDebugLogFlag << ">)";
		}
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
	}

	return DEVICE_OK;
}


//
// Set Motion Mode
//
int XMTE517Ctrl::SetMotionMode(long lMotionMode)//0 high else low
{
	 
	std::ostringstream osMessage;
	 
	unsigned char sResponse[64];
	int ret = DEVICE_OK;
	return ret;
	char sCommStat[30];
	bool yCommError = false;

	unsigned char buf[9];
	if (lMotionMode == 1)
		XMTE517::Instance()->PackageCommand("QHH",NULL,buf);
	else
		XMTE517::Instance()->PackageCommand("QHL",NULL,buf);

	ret = WriteCommand(buf, 9);

	if (ret != DEVICE_OK) return ret;

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::SetMotionMode> = [" << lMotionMode << "," << lMotionMode << "], Returncode =" << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;


	XMTE517::Instance()->SetMotionMode(lMotionMode);

	strcpy((char*)sCommStat, "Success");

	ret = SetProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CommStateLabel).c_str(), (const char*)sCommStat);

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

/*
 * Speed as returned by device is in um/s
 */
int XMTE517Ctrl::OnMotionMode(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::string sMotionMode;
	std::ostringstream osMessage;
	long lMotionMode = (long)XMTE517::Instance()->GetMotionMode();
	int ret = DEVICE_OK;

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(lMotionMode);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnMotionMode> BeforeGet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str() << " = " << lMotionMode << "), ReturnCode = " << ret;
		}
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(lMotionMode);

		ret = SetMotionMode(lMotionMode);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnSpeed> AfterSet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str() << " = " << lMotionMode <<  "), ReturnCode = " << ret;
		}
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

/*
 * Set/Get Timeout Interval
 */
int XMTE517Ctrl::OnTimeoutInterval(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	long lTimeoutInterval = (long)XMTE517::Instance()->GetTimeoutInterval();
	int ret = DEVICE_OK;

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(lTimeoutInterval);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnTimeoutInterval> BeforeGet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutInterval).c_str() << " = " << lTimeoutInterval << "), ReturnCode = " << ret;
		}
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(lTimeoutInterval);
		XMTE517::Instance()->SetTimeoutInterval((int)lTimeoutInterval);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnTimeoutInterval> AfterSet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutInterval).c_str() << " = " << XMTE517::Instance()->GetTimeoutInterval() <<  "), ReturnCode = " << ret;
		}
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

/*
 * Set/Get Timeout Trys
 */
int XMTE517Ctrl::OnTimeoutTrys(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	long lTimeoutTrys = (long)XMTE517::Instance()->GetTimeoutTrys();
	int ret = DEVICE_OK;

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(lTimeoutTrys);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnTimeoutTrys> BeforeGet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutTrys).c_str() << " = " << lTimeoutTrys << "), ReturnCode = " << ret;
		}
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(lTimeoutTrys);
		XMTE517::Instance()->SetTimeoutTrys((int)lTimeoutTrys);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnTimeoutTrys> AfterSet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_TimeoutTrys).c_str() << " = " << XMTE517::Instance()->GetTimeoutTrys() <<  "), ReturnCode = " << ret;
		}
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
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
int XMTE517Ctrl::WriteCommand(unsigned char* sCommand, int nLength)
{
	int ret = DEVICE_OK;
	ostringstream osMessage;

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::WriteCommand> (Command=";
		char sHex[4] = { NULL, NULL, NULL, NULL };
		for (int n=0; n < nLength; n++)
		{
			XMTE517::Instance()->Byte2Hex((const unsigned char)sCommand[n], sHex);
			osMessage << "[" << n << "]=<" << sHex << ">";
		}
		osMessage << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	for (int nBytes = 0; nBytes < nLength && ret == DEVICE_OK; nBytes++)
	{
		ret = WriteToComPort(XMTE517::Instance()->GetSerialPort().c_str(), (const unsigned char*)&sCommand[nBytes], 1);
		CDeviceUtils::SleepMs(1);
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

//
// Read a message from serial port
//
int XMTE517Ctrl::ReadMessage(unsigned char* sResponse, int nBytesRead)
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
		ret = (GetCoreCallback())->ReadFromSerial(pDevice, XMTE517::Instance()->GetSerialPort().c_str(), (unsigned char *)&sAnswer[lRead], (unsigned long)nLength-lRead, lByteRead);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage.str("");
			osMessage << "<XMTE517Ctrl::ReadMessage> (ReadFromSerial = (" << nBytesRead << "," << lRead << "," << lByteRead << ")::<";

			for (unsigned long lIndx=0; lIndx < lByteRead; lIndx++)
			{
				// convert to hext format
				XMTE517::Instance()->Byte2Hex(sAnswer[lRead+lIndx], sHex);
				osMessage << "[" << sHex  << "]";
			}
			osMessage << ">";
			this->LogMessage(osMessage.str().c_str());
		}

		// concade new string
		lRead += lByteRead;

		if (lRead >= 1)
		{
			yRead = (sAnswer[0] != '@') ;
		}

		yRead = yRead || (lRead >= (unsigned long)nBytesRead);

		if (yRead) break;

		// check for timeout
		yTimeout = ((double)(GetClockTicksUs() - lStartTime) / 10000. ) > (double) m_nAnswerTimeoutMs;
		if (!yTimeout) CDeviceUtils::SleepMs(3);

	}

	// block/wait for acknowledge, or until we time out
	// if (!yRead || yTimeout) return DEVICE_SERIAL_TIMEOUT;
	// XMTE517::Instance()->ByteCopy(sResponse, sAnswer, nBytesRead);
	// if (checkError(sAnswer[0])) ret = DEVICE_SERIAL_COMMAND_FAILED;

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<XMTE517Ctrl::ReadMessage> (ReadFromSerial = <";
	}

	for (unsigned long lIndx=0; lIndx < (unsigned long)nBytesRead; lIndx++)
	{
		sResponse[lIndx] = sAnswer[lIndx];
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			XMTE517::Instance()->Byte2Hex(sResponse[lIndx], sHex);
			osMessage << "[" << sHex  << ",";
			XMTE517::Instance()->Byte2Hex(sAnswer[lIndx], sHex);
			osMessage << sHex  << "]";
		}
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage << ">";
		this->LogMessage(osMessage.str().c_str());
	}

	return DEVICE_OK;
}

//
// check the error code for the message returned from serial communivation
//
bool XMTE517Ctrl::CheckError(unsigned char bErrorCode)
{
	// if the return message is 2 bytes message including CR
	unsigned int nErrorCode = 0;
	ostringstream osMessage;

	osMessage.str("");

	// check 4 error code
	if (bErrorCode == XMTE517::XMTE517_SP_OVER_RUN)
	{
		// Serial command buffer over run
		nErrorCode = MPError::MPERR_SerialOverRun;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_FRAME_ERROR)
	{
		// Receiving serial command time out
		nErrorCode = MPError::MPERR_SerialTimeout;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_BUFFER_OVER_RUN)
	{
		// Serial command buffer full
		nErrorCode = MPError::MPERR_SerialBufferFull;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_BAD_COMMAND)
	{
		// Invalid serial command
		nErrorCode = MPError::MPERR_SerialInpInvalid;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_MOVE_INTERRUPTED)
	{
		// Serial command interrupt motion
		nErrorCode = MPError::MPERR_SerialIntrupMove;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == 0x00)
	{
		// No response from serial port
		nErrorCode = MPError::MPERR_SerialZeroReturn;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
	}

	return (nErrorCode!=0);
}


