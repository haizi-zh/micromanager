//////////////////////////////////////////////////////////////////////////////
// FILE:          XMTE517ZStage.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   XMTE517s Controller Driver
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
#include "XMTE517Error.h"
#include "XMTE517ZStage.h"

using namespace std;


///////////////////////////////////////////////////////////////////////////////
// Z - Stage
///////////////////////////////////////////////////////////////////////////////
//
// Z Stage - single axis stage device.
// Note that this adapter uses two coordinate systems.  There is the adapters own coordinate
// system with the X and Y axis going the 'Micro-Manager standard' direction
// Then, there is the XMTE517s native system.  All functions using 'steps' use the XMTE517 system
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
	sprintf(sZName, "%s%s", XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_ZDevNameLabel).c_str(), MM::g_Keyword_Name);
	int ret = CreateProperty(sZName, XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_ZStageDevName).c_str(), MM::String, true);

	m_nAnswerTimeoutMs = XMTE517::Instance()->GetTimeoutInterval();
	m_nAnswerTimeoutTrys = XMTE517::Instance()->GetTimeoutTrys();

	std::ostringstream osMessage;

	// Description
	char sZDesc[120];
	sprintf(sZDesc, "%s%s", XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_ZDevDescLabel).c_str(), MM::g_Keyword_Description);
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

	if (!XMTE517::Instance()->GetDeviceAvailability()) return DEVICE_NOT_CONNECTED;
	CPropertyAction* pActOnGetPosZ = new CPropertyAction(this, &ZStage::OnGetPositionZ);
	char sPosZ[20];
	double dPosZ = XMTE517::Instance()->GetPositionZ();
	sprintf(sPosZ, "%ld", (long)(dPosZ));
	int ret = CreateProperty(XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_GetPositionZ).c_str(), sPosZ, MM::Integer, false, pActOnGetPosZ);  // get position Z 
	if (ret != DEVICE_OK)  return ret;

	ret = GetPositionUm(dPosZ);
	sprintf(sPosZ, "%ld", (long)(dPosZ * (double)XMTE517::Instance()->GetPositionZ()));
	if (ret != DEVICE_OK)  return ret;

	CPropertyAction* pActOnSetPosZ = new CPropertyAction(this, &ZStage::OnSetPositionZ);
	sprintf(sPosZ, "%.2f", dPosZ);
	ret = CreateProperty(XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_SetPositionZ).c_str(), sPosZ, MM::Float, false, pActOnSetPosZ);  // Absolute  vs Relative
	if (ret != DEVICE_OK)  return ret;

	ret = UpdateStatus();
	if (ret != DEVICE_OK) return ret;

	m_yInitialized = true;
	return DEVICE_OK;
}

//
//  Shutdown Z stage
//
int ZStage::Shutdown()
{
	m_yInitialized = false;
	XMTE517::Instance()->SetDeviceAvailable(false);
	return DEVICE_OK;
}

//
// Get the device name of the Z stage
//
void ZStage::GetName(char* Name) const
{
	CDeviceUtils::CopyLimitedString(Name, XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_ZStageDevName).c_str());
}

//
// Get Z stage position in um
//
int ZStage::GetPositionUm(double& dZPosUm)
{
	float lZPosSteps = 0;

	// get current position
	unsigned char buf[9];
	XMTE517::Instance()->PackageCommand("RB1",NULL,buf);
	int ret = WriteCommand(buf, 9);
	if (ret != DEVICE_OK) return ret;

	unsigned char sResponse[64];
	memset(sResponse, 0, 64);

	ret = ReadMessage(sResponse, 7);
	if (ret != DEVICE_OK) return ret;

	ostringstream osMessage;
	char sCommStat[30];
	dZPosUm  =  XMTE517::Instance()->RawToFloat((byte *)sResponse,2);

	XMTE517::Instance()->SetPositionZ(dZPosUm);
	return DEVICE_OK;
}

//
// Move to Z stage to relative distance from current position in um
//
int ZStage::SetRelativePositionUm(double dZPosUm)
{
	int ret = DEVICE_OK;
	ostringstream osMessage;

	// convert um to steps
	double currPos =0;
	GetPositionUm(currPos);
	float target = (float)(dZPosUm + currPos);

	// send move command to controller
	ret = SetPositionUm(target);

	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}


//
// Move to Z stage position in um
//
int ZStage::SetPositionUm(double dZPosUm)
{
	int ret = DEVICE_OK;
	ostringstream osMessage;

	// send move command to controller
	ret = DEVICE_OK;
	byte rawData[4];
	byte buf[10];
	XMTE517::Instance()->FloatToRaw((float)dZPosUm,rawData);
	byte cmd[3];
	cmd[0] = 'T';
	cmd[1] = (char)0;
	cmd[2] = 'S';
	XMTE517::Instance()->PackageCommand((const char*)cmd,rawData,buf);
	ret = WriteCommand(buf, 9);
	if (ret != DEVICE_OK)  return ret;

	XMTE517::Instance()->SetPositionZ(dZPosUm);

	double dPosZ = 0.;
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
	double dPos = XMTE517::Instance()->GetPositionZ();

	osMessage.str("");

	ret = GetPositionUm(dPos);
	char sPos[20];
	sprintf(sPos, "%ld", (long)dPos);

	pProp->Set(dPos);
	XMTE517::Instance()->SetPositionZ(dPos);
	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}

int ZStage::OnSetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	int ret = DEVICE_OK;
	double dPos = XMTE517::Instance()->GetPositionZ();;

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

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<ZStage::WriteCommand> (Command=";
		char sHex[4] = { NULL, NULL, NULL, NULL };
		for (int n = 0; n < nLength && ret == DEVICE_OK; n++)
		{
			XMTE517::Instance()->Byte2Hex((const unsigned char)sCommand[n], sHex);
			char c = (const unsigned char)sCommand[n];
			if (c == '\0')
				c = 'Z';
			osMessage << "<" << sHex << "|"<< c << "|"<< (int)c <<">";
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
int ZStage::ReadMessage(unsigned char* sResponse, int nBytesRead)
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
			osMessage << "<ZStage::ReadMessage> (ReadFromSerial = (" << nBytesRead << "," << lRead << "," << lByteRead << ")::<";

			for (unsigned long lIndx=0; lIndx < lByteRead; lIndx++)
			{
				// convert to hext format
				XMTE517::Instance()->Byte2Hex(sAnswer[lRead+lIndx], sHex);
				osMessage << "[" << sAnswer[lRead+lIndx]  << "]";
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
		yTimeout = ((double)(GetClockTicksUs() - lStartTime)) > (double) m_nAnswerTimeoutMs;
		if (!yTimeout) CDeviceUtils::SleepMs(3);

	}

	for (unsigned long lIndx=0; lIndx < (unsigned long)nBytesRead; lIndx++)
	{
		sResponse[lIndx] = sAnswer[lIndx];
	}

	return DEVICE_OK;
}

