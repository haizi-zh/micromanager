//////////////////////////////////////////////////////////////////////////////
// FILE:          StepMotorZStage.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   StepMotors Controller Driver
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
#include "../../MMCore/MMCore.h"
#include "../../MMDevice/ModuleInterface.h"
#include "../../MMDevice/DeviceUtils.h"
#include "StepMotorError.h"
#include "StepMotorZStage.h"

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

///////////////////////////////////////////////////////////////////////////////
// Z - Stage
///////////////////////////////////////////////////////////////////////////////
//
// Z Stage - single axis stage device.
// Note that this adapter uses two coordinate systems.  There is the adapters own coordinate
// system with the X and Y axis going the 'Micro-Manager standard' direction
// Then, there is the StepMotors native system.  All functions using 'steps' use the StepMotor system
// All functions using Um use the Micro-Manager coordinate system
//

//
// Single axis stage constructor
//
ZStage::ZStage() :
    																								m_yInitialized(false)
//m_nAnswerTimeoutMs(1000)
//, stepSizeUm_(1)
{
	InitializeDefaultErrorMessages();

	// Name
	char sZName[120];
	sprintf(sZName, "%s%s", StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_ZDevNameLabel).c_str(), MM::g_Keyword_Name);
	int ret = CreateProperty(sZName, StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_ZStageDevName).c_str(), MM::String, true);

	m_nAnswerTimeoutMs = StepMotor::Instance()->GetTimeoutInterval();
	m_nAnswerTimeoutTrys = StepMotor::Instance()->GetTimeoutTrys();

	std::ostringstream osMessage;

	// Description
	char sZDesc[120];
	sprintf(sZDesc, "%s%s", StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_ZDevDescLabel).c_str(), MM::g_Keyword_Description);
	ret = CreateProperty(sZDesc, "MP-285 Z Stage Driver", MM::String, true);
}


//
// Z stage destructor
//
ZStage::~ZStage()
{
	Shutdown();
}

///////////////////////////////////////////////////////////////////////////////
// Stage methods required by the API
///////////////////////////////////////////////////////////////////////////////

//
// Z stage initialization
//
int ZStage::Initialize()
{
	std::ostringstream osMessage;

	if (!StepMotor::Instance()->GetDeviceAvailability()) return DEVICE_NOT_CONNECTED;
	CPropertyAction* pActOnGetPosZ = new CPropertyAction(this, &ZStage::OnGetPositionZ);
	char sPosZ[20];
	double dPosZ = StepMotor::Instance()->GetPositionZ();
	sprintf(sPosZ, "%ld", (long)(dPosZ));
	int ret = CreateProperty(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_GetPositionZ).c_str(), sPosZ, MM::Integer, false, pActOnGetPosZ);  // get position Z 
	if (ret != DEVICE_OK)  return ret;

	ret = GetPositionUm(dPosZ);
	sprintf(sPosZ, "%ld", (long)(dPosZ * (double)StepMotor::Instance()->GetPositionZ()));
	if (ret != DEVICE_OK)  return ret;

	CPropertyAction* pActOnSetPosZ = new CPropertyAction(this, &ZStage::OnSetPositionZ);
	sprintf(sPosZ, "%.2f", dPosZ);
	ret = CreateProperty(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_SetPositionZ).c_str(), sPosZ, MM::Float, false, pActOnSetPosZ);  // Absolute  vs Relative
	if (ret != DEVICE_OK)  return ret;

	StepMotor::Instance()->SetMotionMode(0);//Fast
	CPropertyAction* pActOnMotionMode = new CPropertyAction(this, &ZStage::OnMotionMode);
	ret = CreateProperty(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_SetOrigin).c_str(), "Undefined", MM::Integer, false, pActOnMotionMode);  // Absolute  vs Relative
	ret = UpdateStatus();
	if (ret != DEVICE_OK) return ret;

	m_yInitialized = true;
	return DEVICE_OK;
}

/*
 * Speed as returned by device is in um/s
 */
int ZStage::OnMotionMode(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::string sMotionMode;
	std::ostringstream osMessage;
	long lMotionMode = (long)StepMotor::Instance()->GetMotionMode();
	int ret = DEVICE_OK;

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(lMotionMode);

	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(lMotionMode);
		ret = SetMotionMode(lMotionMode);
	}
	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}
//
// Set Motion Mode
//
int ZStage::SetMotionMode(long lMotionMode)//1 high else low
{

	std::ostringstream osMessage;

	unsigned char sResponse[64];
	int ret = DEVICE_OK;

	char sCommStat[30];
	bool yCommError = false;
	byte RawData[4];
	unsigned char buf[9];
	StepMotor::Instance()->LongToRaw(lMotionMode,RawData);
	StepMotor::Instance()->PackageCommand(SetZeroPosition,RawData,buf);

	ret = WriteCommand(buf, 7);
	if (ret != DEVICE_OK) return ret;

	StepMotor::Instance()->SetMotionMode(lMotionMode);
	return DEVICE_OK;
}
//
//  Shutdown Z stage
//
int ZStage::Shutdown()
{
	m_yInitialized = false;
	StepMotor::Instance()->SetDeviceAvailable(false);
	return DEVICE_OK;
}

//
// Get the device name of the Z stage
//
void ZStage::GetName(char* Name) const
{
	CDeviceUtils::CopyLimitedString(Name, StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_ZStageDevName).c_str());
}

//
// Get Z stage position in um
//
int ZStage::GetPositionUm(double& dZPosUm)
{
	float lZPosSteps = 0;

	// get current position
	unsigned char buf[9];
	StepMotor::Instance()->PackageCommand(QueryPosition,NULL,buf);
	int ret = WriteCommand(buf, 7);
	if (ret != DEVICE_OK) return ret;
	CDeviceUtils::SleepMs(200);
	unsigned char sResponse[64];

	bool yCommError = true;
	while (yCommError )
	{

		memset(sResponse, 0, 64);
		ret = ReadMessage(sResponse, 7);
		yCommError = (ret != DEVICE_OK);
		CDeviceUtils::SleepMs(10);
	}

	dZPosUm  =  StepMotor::Instance()->RawToLong((byte *)sResponse,1);

	StepMotor::Instance()->SetPositionZ(dZPosUm);
	return DEVICE_OK;
}

//
// Move to Z stage to relative distance from current position in um
//
int ZStage::SetRelativePositionUm(double dZPosUm)
{
	int ret = DEVICE_OK;
	// convert um to steps
	double currPos =0;
	GetPositionUm(currPos);
	float target = (float)(dZPosUm + currPos);

	// send move command to controller
	ret = SetPositionUm(target);

	if (ret != DEVICE_OK) return ret;
	ret = UpdateStatus();
	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}


//
// Move to Z stage position in um
//
int ZStage::SetPositionUm(double dZPosUm)
{
	int ret = DEVICE_OK;

	ret = DEVICE_OK;
	byte rawData[4];
	byte buf[10];
	double currPos =0;
	double step2Um = 0.09969;
	GetPositionUm(currPos);
	StepMotor::Instance()->LongToRaw((unsigned long)dZPosUm,rawData);
	StepMotor::Instance()->PackageCommand(SetPosition,rawData,buf);
	long sleept =  0;
	double delta = currPos - dZPosUm;

	if(delta<0)
		delta *= -1;

	sleept = 0.2*delta/step2Um;

	ret = WriteCommand(buf, 7);

	if (ret != DEVICE_OK)  return ret;
	CDeviceUtils::SleepMs(sleept);

	unsigned char sResponse[64];

	bool yCommError = true;
	while (yCommError)
	{

		memset(sResponse, 0, 64);
		ret = ReadMessage(sResponse, 7);
		yCommError = (ret != DEVICE_OK);
		CDeviceUtils::SleepMs(10);
	}

	if (ret != DEVICE_OK) return ret;
	StepMotor::Instance()->SetPositionZ(dZPosUm);

	double dPosZ = 0;

	ret = GetPositionUm(dPosZ);

	if (ret != DEVICE_OK) return ret;

	return ret;
}

///////////////////////////////////////////////////////////////////////////////
// Action handlers
///////////////////////////////////////////////////////////////////////////////

int ZStage::OnGetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	int ret = DEVICE_OK;
	double dPos = StepMotor::Instance()->GetPositionZ();

	osMessage.str("");

	ret = GetPositionUm(dPos);
	char sPos[20];
	sprintf(sPos, "%ld", (long)dPos);

	pProp->Set(dPos);
	StepMotor::Instance()->SetPositionZ(dPos);
	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}

int ZStage::OnSetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	int ret = DEVICE_OK;
	double dPos = StepMotor::Instance()->GetPositionZ();;

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(dPos);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(dPos);
		ret = SetPositionUm(dPos);
	}
	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}


///////////////////////////////////////////////////////////////////////////////
// Helper, internal methods
///////////////////////////////////////////////////////////////////////////////

//
// Write a coomand to serial port
//
int ZStage::WriteCommand(unsigned char* sCommand, int nLength)
{
	int ret = DEVICE_OK;
	ostringstream osMessage;
	const unsigned char em = 'X';
	for (int nBytes = 0; nBytes < 10 && ret == DEVICE_OK; nBytes++)
	{
		ret = WriteToComPort(StepMotor::Instance()->GetSerialPort().c_str(), &em, 1);
		CDeviceUtils::SleepMs(1);
	}
	ret = ClearPort(*this, *GetCoreCallback(), StepMotor::Instance()->GetSerialPort().c_str());
	if (ret != DEVICE_OK) return ret;
	for (int nBytes = 0; nBytes < nLength && ret == DEVICE_OK; nBytes++)
	{
		ret = WriteToComPort(StepMotor::Instance()->GetSerialPort().c_str(), (const unsigned char*)&sCommand[nBytes], 1);
		CDeviceUtils::SleepMs(1);
	}
	if (StepMotor::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<ZStage::WriteCommand> (Command=";
		char sHex[4] = { NULL, NULL, NULL, NULL };
		for (int n = 0; n < nLength && ret == DEVICE_OK; n++)
		{
			if(sCommand[n] == 0)
				sCommand[n] = '#';
			osMessage << "[" << (char)sCommand[n] << "|"<< (int)sCommand[n] <<"]";
		}
		osMessage << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

//
// Read a message from serial port
//
int ZStage::ReadMessage(unsigned char* sResponse, int nBytesRead)
{

	// block/wait for acknowledge, or until we time out;
	unsigned int nLength = 256;
	unsigned char sAnswer[256];
	memset(sAnswer, 0, nLength);
	unsigned long lRead = 0;
	unsigned long lStartTime = GetClockTicksUs();

	ostringstream osMessage;
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
			osMessage << "<StepMotorZstage::ReadMessage> (ReadFromSerial = (" << nBytesRead << "," << lRead << "," << lByteRead << ")::<";

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
			yRead = (sAnswer[0] == '@');// && sAnswer[lRead-1]=='\r';
		}

		yRead = yRead || (lRead >= (unsigned long)nBytesRead);

		if (yRead) break;

		// check for timeout
		 yTimeout = ((double)(GetClockTicksUs() - lStartTime) / 1000) > (double) m_nAnswerTimeoutMs;
		if (!yTimeout) CDeviceUtils::SleepMs(3);

	}
	if (yTimeout) return DEVICE_SERIAL_TIMEOUT;
	for (unsigned long lIndx=0; lIndx < (unsigned long)nBytesRead; lIndx++)
	{
		sResponse[lIndx] = sAnswer[lIndx];
	}


	return DEVICE_OK;
}

