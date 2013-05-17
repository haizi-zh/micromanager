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
	sprintf(sZName, "%s%s", XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZDevNameLabel).c_str(), MM::g_Keyword_Name);
	int ret = CreateProperty(sZName, XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZStageDevName).c_str(), MM::String, true);

	m_nAnswerTimeoutMs = XMTE517::Instance()->GetTimeoutInterval();
	m_nAnswerTimeoutTrys = XMTE517::Instance()->GetTimeoutTrys();

	std::ostringstream osMessage;

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<ZStage::class-constructor> CreateProperty(" << sZName << "=" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZStageDevName).c_str() << "), ReturnCode=" << ret << endl;
		this->LogMessage(osMessage.str().c_str());
	}

	// Description
	char sZDesc[120];
	sprintf(sZDesc, "%s%s", XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZDevDescLabel).c_str(), MM::g_Keyword_Description);
	ret = CreateProperty(sZDesc, "MP-285 Z Stage Driver", MM::String, true);

	// osMessage.clear();
	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<ZStage::class-constructor> CreateProperty(" << sZDesc << " = MP-285 Z Stage Driver), ReturnCode=" << ret << endl;
		this->LogMessage(osMessage.str().c_str());
	}
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
	//if (XMTE517::Instance()->GetNumberOfAxes() < 3) return DEVICE_NOT_CONNECTED;

	//int ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_GetPositionZ).c_str(), "undefined", MM::String, true);  // get position Z
	CPropertyAction* pActOnGetPosZ = new CPropertyAction(this, &ZStage::OnGetPositionZ);
	char sPosZ[20];
	double dPosZ = XMTE517::Instance()->GetPositionZ();
	sprintf(sPosZ, "%ld", (long)(dPosZ * (double)XMTE517::Instance()->GetUm2UStep()));
	int ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_GetPositionZ).c_str(), sPosZ, MM::Integer, false, pActOnGetPosZ);  // get position Z 

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<ZStage::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_GetPositionZ).c_str() << " = " << sPosZ << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK)  return ret;

	ret = GetPositionUm(dPosZ);
	sprintf(sPosZ, "%ld", (long)(dPosZ * (double)XMTE517::Instance()->GetPositionZ()));

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<ZStage::Initialize> GetPosSteps(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_GetPositionZ).c_str() << " = " << sPosZ << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK)  return ret;

	CPropertyAction* pActOnSetPosZ = new CPropertyAction(this, &ZStage::OnSetPositionZ);
	sprintf(sPosZ, "%.2f", dPosZ);
	ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_SetPositionZ).c_str(), sPosZ, MM::Float, false, pActOnSetPosZ);  // Absolute  vs Relative
	// ret = CreateProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_SetPositionZ).c_str(), "Undefined", MM::Integer, true);  // Absolute  vs Relative

	if (XMTE517::Instance()->GetDebugLogFlag() > 0)
	{
		osMessage.str("");
		osMessage << "<ZStage::Initialize> CreateProperty(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_SetPositionZ).c_str() << " = " << sPosZ << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK)  return ret;
	SetMotionMode(0);
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
	CDeviceUtils::CopyLimitedString(Name, XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZStageDevName).c_str());
}

//
// Set Motion Mode (1: relatice, 0: absolute)
//
int ZStage::SetMotionMode(long lMotionMode)
{
	std::ostringstream osMessage;
	 
	unsigned char sResponse[64];
	int ret = DEVICE_OK;
	unsigned char buf[9];
	
	if (lMotionMode == 0)
		XMTE517::Instance()->PackageCommand("TQH",NULL,buf);
	else
		XMTE517::Instance()->PackageCommand("TQL",NULL,buf);

	ret = WriteCommand(buf, 9);

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<ZStage::SetMotionMode> = [" << lMotionMode << "," << lMotionMode << "], Returncode =" << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	XMTE517::Instance()->SetMotionMode(lMotionMode);

	return DEVICE_OK;
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

	bool yCommError = false;
	int nTrys = 0;

	while (!yCommError && nTrys < XMTE517::Instance()->GetTimeoutTrys())
	{
		ret = ReadMessage(sResponse, 7);

		ostringstream osMessage;
		char sCommStat[30];
		int nError = CheckError(sResponse[0]);
		yCommError = (sResponse[0] == 0) ? false : nError != 0;
		if (yCommError)
		{
			if (nError == MPError::MPERR_SerialZeroReturn && nTrys < XMTE517::Instance()->GetTimeoutTrys()) { nTrys++; yCommError = false; }

			if (XMTE517::Instance()->GetDebugLogFlag() > 1)
			{
				osMessage.str("");
				osMessage << "<XYStage::GetPositionSteps> Response = (" << nError << "," << nTrys << ")" ;
			}

			sprintf(sCommStat, "Error Code ==> <%2x>", sResponse[0]);
		}
		else
		{

			lZPosSteps  =  XMTE517::Instance()->RawToFloat((byte *)sResponse,2);
			strcpy(sCommStat, "Success");

			if (XMTE517::Instance()->GetDebugLogFlag() > 1)
			{
				osMessage.str("");
				osMessage << "<ZStage::GetPositionSteps> Response( Z = <"<< lZPosSteps << ">), ReturnCode=" << ret;
			}

			nTrys = XMTE517::Instance()->GetTimeoutTrys();

		}

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			this->LogMessage(osMessage.str().c_str());
		}

		//ret = SetProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CommStateLabel).c_str(), sCommStat);

	}

	if (ret != DEVICE_OK) return ret;


	dZPosUm = (double)lZPosSteps / (double)XMTE517::Instance()->GetUm2UStep();

	ostringstream osMessage;

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<ZStage::GetPositionUm> (z=" << dZPosUm << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	XMTE517::Instance()->SetPositionZ(dZPosUm);

	//char sPosition[20];
	//sprintf(sPosition, "%.2f", dZPosUm);
	//ret = SetProperty(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_GetPositionZ).c_str(), sPosition);

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<ZStage::GetPositionUm> Z=[" << dZPosUm << /*"," << sPosition <<*/ "], Returncode=" << ret ;
		this->LogMessage(osMessage.str().c_str());
	}

	//f (ret != DEVICE_OK) return ret;

	//ret = UpdateStatus();
	//if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

//
// Move to Z stage to relative distance from current position in um
//
int ZStage::SetRelativePositionUm(double dZPosUm)
{
	int ret = DEVICE_OK;
	ostringstream osMessage;

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage << "<ZStage::SetRelativePositionUm> (z=" << dZPosUm << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	// set relative motion mode
	if (XMTE517::Instance()->GetMotionMode() != 0)
	{
		ret = SetMotionMode(0);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage.str("");
			osMessage << "<ZStage::SetRelativePositionUm> (" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str() << " = <RELATIVE>), ReturnCode = " << ret;
			this->LogMessage(osMessage.str().c_str());
		}

		if (ret != DEVICE_OK) return ret;
	}

	// convert um to steps
	double currPos =0;
	GetPositionUm(currPos);
	float lZPosSteps = (float)(dZPosUm + currPos);

	// send move command to controller
	ret = SetPositionUm(lZPosSteps);

	if (ret != DEVICE_OK) return ret;

	XMTE517::Instance()->SetPositionZ(dZPosUm);

	double dPosZ = 0.;

	ret = GetPositionUm(dPosZ);

	if (ret != DEVICE_OK) return ret;

	return ret;
}


//
// Move to Z stage position in um
//
int ZStage::SetPositionUm(double dZPosUm)
{
	int ret = DEVICE_OK;
	ostringstream osMessage;

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage << "<ZStage::SetPositionUm> (z=" << dZPosUm << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	// set absolute motion mode
	if (XMTE517::Instance()->GetMotionMode() != 0)
	{
		ret = SetMotionMode(0);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage.str("");
			osMessage << "<ZStage::SetPositionUm> (" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str() << " = <ABSOLUTE>), ReturnCode = " << ret;
			this->LogMessage(osMessage.str().c_str());
		}

		if (ret != DEVICE_OK) return ret;
	}

	// convert um to steps
	float lZPosSteps = (float)(dZPosUm * (double)XMTE517::Instance()->GetUm2UStep());


	// send move command to controller
	  ret = DEVICE_OK;
		byte rawData[4];
		byte buf[10];
		XMTE517::Instance()->FloatToRaw(lZPosSteps,rawData);
		byte cmd[3];
		cmd[0] = 'T';
		cmd[1] = (char)0;
		cmd[2] = 'S';
		XMTE517::Instance()->PackageCommand((const char*)cmd,rawData,buf);
		ret = WriteCommand(buf, 9);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			//osMessage.str("");
			osMessage << "<ZStage::_SetPositionSteps> Command(<0x6D>,<" <<lZPosSteps << ">), ReturnCode=" << ret;
			LogMessage(osMessage.str().c_str());
		}

		if (ret != DEVICE_OK)  return ret;

		 

		CDeviceUtils::SleepMs(10);
	XMTE517::Instance()->SetPositionZ(dZPosUm);

	double dPosZ = 0.;

	ret = GetPositionUm(dPosZ);

	if (ret != DEVICE_OK) return ret;

	return ret;
}

int ZStage::SetPositionSteps(long steps)
{
	double pos = steps * XMTE517::Instance()->GetUm2UStep();
	return SetPositionUm(pos);
}

int ZStage::GetPositionSteps(long& steps)
{
	return DEVICE_OK;
	double pos;
	int ret = GetPositionUm(pos);
	if (ret != DEVICE_OK)
		return ret;
	steps = (long) ((pos / XMTE517::Instance()->GetUm2UStep()));
	return DEVICE_OK;
}

//
// Get Z stage position in steps
//
//
//int ZStage::GetPositionSteps(float& lZPosSteps)
//{
//	// get current position
//	unsigned char buf[9];
//	XMTE517::Instance()->PackageCommand("RB1",NULL,buf);
//	int ret = WriteCommand(buf, 9);
//	if (ret != DEVICE_OK) return ret;
//
//	unsigned char sResponse[64];
//	memset(sResponse, 0, 64);
//
//	bool yCommError = false;
//	int nTrys = 0;
//
//	while (!yCommError && nTrys < XMTE517::Instance()->GetTimeoutTrys())
//	{
//		ret = ReadMessage(sResponse, 7);
//
//		ostringstream osMessage;
//		char sCommStat[30];
//		int nError = CheckError(sResponse[0]);
//		yCommError = (sResponse[0] == 0) ? false : nError != 0;
//		if (yCommError)
//		{
//			if (nError == MPError::MPERR_SerialZeroReturn && nTrys < XMTE517::Instance()->GetTimeoutTrys()) { nTrys++; yCommError = false; }
//
//			if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//			{
//				osMessage.str("");
//				osMessage << "<XYStage::GetPositionSteps> Response = (" << nError << "," << nTrys << ")" ;
//			}
//
//			sprintf(sCommStat, "Error Code ==> <%2x>", sResponse[0]);
//		}
//		else
//		{
//
//			lZPosSteps  =  XMTE517::Instance()->RawToFloat((byte *)sResponse,2);
//			strcpy(sCommStat, "Success");
//
//			if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//			{
//				osMessage.str("");
//				osMessage << "<ZStage::GetPositionSteps> Response( Z = <"<< lZPosSteps << ">), ReturnCode=" << ret;
//			}
//
//			nTrys = XMTE517::Instance()->GetTimeoutTrys();
//
//		}
//
//		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//		{
//			this->LogMessage(osMessage.str().c_str());
//		}
//
//
//	}
//
//	if (ret != DEVICE_OK) return ret;
//
//	return DEVICE_OK;
//}



//
// Move x-y stage to a relative distance from current position in uSteps
//
//
//int ZStage::SetRelativePositionSteps(float lZPosSteps)
//{
//	int ret = DEVICE_OK;
//	ostringstream osMessage;
//
//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//	{
//		osMessage << "<ZStage::SetRelativePositionSteps> (z=" << lZPosSteps << ")";
//		this->LogMessage(osMessage.str().c_str());
//	}
//
//	if (XMTE517::Instance()->GetMotionMode() == 0)
//	{
//		ret = SetMotionMode(1);
//
//		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//		{
//			osMessage.str("");
//			osMessage << "<ZStage::SetRelativePositionSteps> (" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str() << " = <RELATIVE>), ReturnCode = " << ret;
//			this->LogMessage(osMessage.str().c_str());
//		}
//
//		if (ret != DEVICE_OK) return ret;
//	}
//
//	ret = _SetPositionSteps(lZPosSteps);
//
//	if (ret != DEVICE_OK) return ret;
//
//	return DEVICE_OK;
//}

//
// move z stage to absolute position in uSsteps
//
//
//int ZStage::SetPositionSteps(float lZPosSteps)
//{
//	int ret = DEVICE_OK;
//	ostringstream osMessage;
//
//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//	{
//		osMessage << "<ZStage::SetPositionSteps> (z=" << lZPosSteps << ")";
//		this->LogMessage(osMessage.str().c_str());
//	}
//
//	if (XMTE517::Instance()->GetMotionMode() != 0)
//	{
//		ret = SetMotionMode(0);
//
//		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//		{
//			osMessage.str("");
//			osMessage << "<ZStage::SetPositionSteps> (" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_MotionMode).c_str() << " = <ABSOLUTE>), ReturnCode = " << ret;
//			this->LogMessage(osMessage.str().c_str());
//		}
//
//		if (ret != DEVICE_OK) return ret;
//	}
//
//	ret = _SetPositionSteps( lZPosSteps);
//
//	if (ret != DEVICE_OK) return ret;
//
//	return DEVICE_OK;
//}

//
// move x-y-z stage in uSsteps
//
//int ZStage::_SetPositionSteps(float lZPosSteps)
//{
//	int ret = DEVICE_OK;
//	ostringstream osMessage;
//
//	byte rawData[4];
//	byte buf[10];
//	XMTE517::Instance()->FloatToRaw(lZPosSteps,rawData);
//	byte cmd[3];
//	cmd[0] = 'T';
//	cmd[1] = (char)0;
//	cmd[2] = 'S';
//	XMTE517::Instance()->PackageCommand((const char*)cmd,rawData,buf);
//	ret = WriteCommand(buf, 9);
//
//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//	{
//		osMessage << "<ZStage::_SetPositionSteps> Command(<0x6D>,<" <<lZPosSteps << ">), ReturnCode=" << ret;
//		LogMessage(osMessage.str().c_str());
//	}
//
//	if (ret != DEVICE_OK)  return ret;
//
//	double dVelocity = (double)XMTE517::Instance()->GetVelocity() * (double)XMTE517::Instance()->GetUm2UStep();
//	double dSec = 0.;
//	if (XMTE517::GetMotionMode == 0)
//	{
//		long lOldZPosSteps = (long)XMTE517::Instance()->GetPositionZ();
//		dSec = (double)labs(lZPosSteps-lOldZPosSteps) / dVelocity;
//	}
//	else
//	{
//		dSec = (double)labs(lZPosSteps) / dVelocity;
//	}
//	long lSleep = (long)(dSec * 120.);
//
//	CDeviceUtils::SleepMs(lSleep);
//
//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//	{
//		osMessage.str("");
//		osMessage << "<ZStage::_SetPositionSteps> Sleep..." << lSleep << " millisec...";
//		LogMessage(osMessage.str().c_str());
//	}
//
//	bool yCommError = true;
//
//	while (yCommError)
//	{
//		unsigned char sResponse[64];
//		memset(sResponse, 0, 64);
//
//		ret = ReadMessage(sResponse, 2);
//
//		yCommError = CheckError(sResponse[0]) != MPError::MPERR_OK;
//	}
//
//	if (ret != DEVICE_OK) return ret;
//
//	return DEVICE_OK;
//}

//
// Set current position as origin
//
//int ZStage::SetOrigin()
//{
//	unsigned char sCommand[6] = { 0x6F, XMTE517::XMTE517_TxTerm, 0x0A, 0x00, 0x00, 0x00 };
//	int ret = WriteCommand(sCommand, 3);
//
//	std::ostringstream osMessage;
//
//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//	{
//		osMessage.str("");
//		osMessage << "<ZStage::SetOrigin> (ReturnCode=" << ret << ")";
//		this->LogMessage(osMessage.str().c_str());
//	}
//
//	if (ret!=DEVICE_OK) return ret;
//
//	unsigned char sResponse[64];
//
//	memset(sResponse, 0, 64);
//	ret = ReadMessage(sResponse, 2);
//
//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//	{
//		osMessage.str("");
//		osMessage << "<ZStage::CheckStatus::SetOrigin> (ReturnCode = " << ret << ")";
//		this->LogMessage(osMessage.str().c_str());
//	}
//
//	if (ret != DEVICE_OK) return ret;
//
//	bool yCommError = CheckError(sResponse[0]) != 0;
//
//	char sCommStat[30];
//	if (yCommError)
//		sprintf(sCommStat, "Error Code ==> <%2x>", sResponse[0]);
//	else
//		strcpy(sCommStat, "Success");
//
//
//	if (ret != DEVICE_OK) return ret;
//
//	return DEVICE_OK;
//}

//
// stop and interrupt Z stage motion
//
//int ZStage::Stop()
//{
//	unsigned char sCommand[6] = { 0x03, XMTE517::XMTE517_TxTerm, 0x00, 0x00, 0x00, 0x00 };
//
//	int ret = WriteCommand(sCommand, 2);
//
//	ostringstream osMessage;
//
//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
//	{
//		osMessage.str("");
//		osMessage << "<ZStage::Stop> (ReturnCode = " << ret << ")";
//		this->LogMessage(osMessage.str().c_str());
//	}
//
//	return ret;
//}

///////////////////////////////////////////////////////////////////////////////
// Action handlers
///////////////////////////////////////////////////////////////////////////////

//
// Unsupported command from XMTE517
//
int ZStage::OnStepSize (MM::PropertyBase* /*pProp*/, MM::ActionType /*eAct*/) 
{
	return DEVICE_OK;
}

int ZStage::OnSpeed(MM::PropertyBase* /*pProp*/, MM::ActionType /*eAct*/)
{
	return DEVICE_OK;
}

int ZStage::OnGetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	int ret = DEVICE_OK;
	double dPos = XMTE517::Instance()->GetPositionZ();

	osMessage.str("");

	//if (eAct == MM::BeforeGet)
	//{
	//    pProp->Set(dPos);
	//
	//	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	//	{
	//		osMessage << "<XMTE517Ctrl::OnGetPositionZ> BeforeGet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_SetPositionX).c_str() << " = [" << dPos << "], ReturnCode = " << ret;
	//		//this->LogMessage(osMessage.str().c_str());
	//	}
	//}
	//if (eAct == MM::AfterSet)
	//{
	// pProp->Get(dPos);  // not used

	ret = GetPositionUm(dPos);
	dPos *= (double)XMTE517::Instance()->GetUm2UStep();
	char sPos[20];
	sprintf(sPos, "%ld", (long)dPos);

	pProp->Set(dPos);

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage << "<XMTE517Ctrl::OnGetPositionZ> AfterSet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_SetPositionX).c_str() << " = [" << dPos << "," << sPos << "], ReturnCode = " << ret;
		//this->LogMessage(osMessage.str().c_str());
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage << ")";
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;
	//}

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

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnSetPositionZ> BeforeGet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_SetPositionZ).c_str() << " = [" << dPos << "], ReturnCode = " << ret;
			//this->LogMessage(osMessage.str().c_str());
		}
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(dPos);

		if (XMTE517::Instance()->GetMotionMode() == 0)
			ret = SetPositionUm(dPos);
		else
			ret = SetRelativePositionUm(dPos);

		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XMTE517Ctrl::OnSetPositionZ> AfterSet(" << XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_SetPositionZ).c_str() << " = [" << dPos << "], ReturnCode = " << ret;
			//this->LogMessage(osMessage.str().c_str());
		}

	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage << ")";
		this->LogMessage(osMessage.str().c_str());
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
			osMessage << "<" << c << ">";
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
			osMessage << "<XMTE517Ctrl::ReadMessage> (ReadFromSerial = (" << nBytesRead << "," << lRead << "," << lByteRead << ")::<";

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
int ZStage::CheckError(unsigned char bErrorCode)
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
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_FRAME_ERROR)
	{
		// Receiving serial command time out
		nErrorCode = MPError::MPERR_SerialTimeout;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_BUFFER_OVER_RUN)
	{
		// Serial command buffer full
		nErrorCode = MPError::MPERR_SerialBufferFull;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_BAD_COMMAND)
	{
		// Invalid serial command
		nErrorCode = MPError::MPERR_SerialInpInvalid;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == XMTE517::XMTE517_MOVE_INTERRUPTED)
	{
		// Serial command interrupt motion
		nErrorCode = MPError::MPERR_SerialIntrupMove;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == 0x0D)
	{
		// read carriage return
		nErrorCode = MPError::MPERR_OK;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XYStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == 0x00)
	{
		// No response from serial port
		nErrorCode = MPError::MPERR_SerialZeroReturn;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}

	else if (bErrorCode != '@')
	{
		// No response from serial port
		nErrorCode = MPError::MPERR_SerialInpInvalid;
		if (XMTE517::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}

	if (XMTE517::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
	}

	return (nErrorCode);
}

