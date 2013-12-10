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
{
	InitializeDefaultErrorMessages();

	std::ostringstream osMessage;
	// Name
	char sZName[120];
	sprintf(sZName, "%s%s", StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_ZDevNameLabel).c_str(), MM::g_Keyword_Name);
	int ret = CreateProperty(sZName, StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_ZStageDevName).c_str(), MM::String, true);

	// Description
	char sZDesc[120];
	sprintf(sZDesc, "%s%s", StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_ZDevDescLabel).c_str(), MM::g_Keyword_Description);
	ret = CreateProperty(sZDesc, "MP-285 Z Stage Driver", MM::String, true);

	// Port:
	//	CPropertyAction* pAct = new CPropertyAction(this, &ZStage::OnPort);
	//	ret = CreateProperty(MM::g_Keyword_Port, "Undefined", MM::String, false, pAct, true);
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

	//act getPositionz
	CPropertyAction* pActOnGetPosZ = new CPropertyAction(this, &ZStage::OnGetPositionZ);
	char sPosZ[20];
	double dPosZ = StepMotor::Instance()->GetPositionZ();
	sprintf(sPosZ, "%ld", (long)(dPosZ * (double)StepMotor::Instance()->GetUm2UStep()));
	int ret = CreateProperty(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_GetPositionZ).c_str(), sPosZ, MM::Integer, false, pActOnGetPosZ);  // get position Z 
	if (ret != DEVICE_OK)  return ret;

	ret = GetPositionUm(dPosZ);
	sprintf(sPosZ, "%ld", (long)(dPosZ * (double)StepMotor::Instance()->GetPositionZ()));
	if (ret != DEVICE_OK)  return ret;

	//act setPositionz
	CPropertyAction* pActOnSetPosZ = new CPropertyAction(this, &ZStage::OnSetPositionZ);
	sprintf(sPosZ, "%.2f", dPosZ);
	ret = CreateProperty(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_SetPositionZ).c_str(), sPosZ, MM::Float, false, pActOnSetPosZ);  // Absolute  vs Relative
	if (ret != DEVICE_OK)  return ret;

	// Speed  pluse/s
	// -----------------
	// Get current speed from the controller
	// Speed information started at the 27th byte and 2 bytes long
	char sVelocity[20];
	memset(sVelocity, 0, 20);
	long lVelocity =StepMotor::Instance()->GetVelocity();
	sprintf(sVelocity, "%ld", lVelocity);

	CPropertyAction* pActOnSpeed = new CPropertyAction(this, &ZStage::OnSpeed);
	ret = CreateProperty(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_VelocityLabel).c_str(), sVelocity, MM::Integer,  false, pActOnSpeed); // usteps/step

	// umTostep
	// -----------------
	// Get current speed from the controller
	// Speed information started at the 27th byte and 2 bytes long
	char sUm2Step[20];
	memset(sUm2Step, 0, 20);
	double lUm2Step =StepMotor::Instance()->GetUm2UStep();
	sprintf(sUm2Step, "%f", lUm2Step);

	CPropertyAction* pActOnUmToStep = new CPropertyAction(this, &ZStage::OnUmToStep);
	ret = CreateProperty(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_Um2UStepLabel).c_str(), sUm2Step, MM::Float,  false, pActOnUmToStep); // usteps/step
	// SetOrigin
	// -----------------
	// Get current speed from the controller
	// Speed information started at the 27th byte and 2 bytes long

	CPropertyAction* pActOnSetOrigin = new CPropertyAction(this, &ZStage::OnSetOrigin);
	ret = CreateProperty(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_SetOrigin).c_str(),"0", MM::Integer,  false, pActOnSetOrigin); // usteps/step
	//Stage mirrorz
	CPropertyAction* pActOnStageMirror = new CPropertyAction(this, &ZStage::OnStageMirrorZ);
	ret = CreateProperty(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_StageMirror).c_str(),"0", MM::Integer,  false, pActOnStageMirror); // usteps/step

	//Stage mirrorz
	CPropertyAction* pActOnPort = new CPropertyAction(this, &ZStage::OnPort);
	ret = CreateProperty(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_CommLabel).c_str(),"COM1", MM::String,  false, pActOnPort); // usteps/step


	HANDLE oldComm = StepMotor::Instance()->getCommHandle();
	if(oldComm != INVALID_HANDLE_VALUE)
		CloseHandle(oldComm);

	HANDLE hComm = CreateFile(StepMotor::Instance()->GetSerialPort().c_str(),
			GENERIC_READ | GENERIC_WRITE,
			0,
			0,
			OPEN_EXISTING,
			FILE_FLAG_OVERLAPPED,
			0);
	if(hComm == INVALID_HANDLE_VALUE)
	{
		osMessage.str("");
		osMessage << "<initrail 3eorror port" << StepMotor::Instance()->GetSerialPort().c_str() ;
		this->LogMessage(osMessage.str().c_str());
		return GetLastError();
	}

	ret = UpdateStatus();
	if (ret != DEVICE_OK) return ret;
	StepMotor::Instance()->setCommHandle(hComm);
	m_yInitialized = true;
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
	CDeviceUtils::CopyLimitedString(Name, StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_ZStageDevName).c_str());
}

//
// Set Motion Mode (1: relatice, 0: absolute)
//
int ZStage::SetMotionMode(long lMotionMode)
{
	std::ostringstream osMessage;
	unsigned char sCommand[6] = { 0x00, StepMotor::StepMotor_TxTerm, 0x0A, 0x00, 0x00, 0x00 };
	unsigned char sResponse[64];
	int ret = DEVICE_OK;

	if (lMotionMode == 0)
		sCommand[0] = 'a';
	else
		sCommand[0] = 'b';

	ret = WriteCommand(sCommand, 3);

	if (StepMotor::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<ZStage::SetMotionMode> = [" << lMotionMode << "," << sCommand[0] << "], Returncode =" << ret;
		this->LogMessage(osMessage.str().c_str());
	}

	if (ret != DEVICE_OK) return ret;

	ret = ReadMessage(sResponse, 2);

	if (ret != DEVICE_OK) return ret;

	StepMotor::Instance()->SetMotionMode(lMotionMode);

	return DEVICE_OK;
}

//
// Get Z stage position in um
//
int ZStage::GetPositionUm(double& dZPosUm)
{
	dZPosUm = StepMotor::Instance()->GetPositionZ();
	return DEVICE_OK;
}

//
// Move to Z stage to relative distance from current position in um
//
int ZStage::SetRelativePositionUm(double dZPosUm)
{

	double currentPos = StepMotor::Instance()->GetPositionZ();

	if(StepMotor::Instance()->GetIsSetOrigin()){
		if( dZPosUm + currentPos<0){
			dZPosUm = -1*currentPos;
			StepMotor::Instance()->SetPositionZ(currentPos + dZPosUm);
		}
	}
	// convert um to steps
	long lZPosSteps = (long)(dZPosUm * (double)StepMotor::Instance()->GetUm2UStep());
	// send move command to controller
	int ret = SetPositionSteps(lZPosSteps);

	if(StepMotor::Instance()->GetIsSetOrigin()){
		StepMotor::Instance()->SetPositionZ(currentPos + dZPosUm);
	}
	if (ret != DEVICE_OK) return ret;
	return DEVICE_OK;
}


//
// Move to Z stage position in um
//
int ZStage::SetPositionUm(double dZPosUm)
{
	if( !StepMotor::Instance()->GetIsSetOrigin())
		return DEVICE_OK;

	double currentPos = StepMotor::Instance()->GetPositionZ();

	dZPosUm = dZPosUm - currentPos;

	SetRelativePositionUm(dZPosUm);
	return DEVICE_OK;
}

//
// Get Z stage position in steps
//
int ZStage::GetPositionSteps(long& lZPosSteps)
{
	// get current position
	return DEVICE_OK;
}



//
// Move x-y stage to a relative distance from current position in uSteps
//
int ZStage::SetRelativePositionSteps(long lZPosSteps)
{
	return DEVICE_OK;
}

//
// move z stage to absolute position in uSsteps
//
int ZStage::SetPositionSteps(long lZPosSteps)
{
	if(lZPosSteps >0)//up
	{
		if(StepMotor::Instance()->GetStageMirrorZ())
			SetRTS();
		else
			ClrRTS();

	}else{
		if(StepMotor::Instance()->GetStageMirrorZ())
			ClrRTS();
		else
			SetRTS();
	}
	lZPosSteps = abs(lZPosSteps);
	for(long i=0;i<lZPosSteps;i++){
		SetDTR();
		ClrDTR();
	}
	return DEVICE_OK;
}
void ZStage::SetDTR()
{
	EscapeCommFunction(StepMotor::Instance()->getCommHandle(),SETDTR);
	Sleep(StepMotor::Instance()->GetPluseInterval());
}

void ZStage::SetRTS()
{
	EscapeCommFunction(StepMotor::Instance()->getCommHandle(),SETRTS);
	Sleep(StepMotor::Instance()->GetPluseInterval());
}

void ZStage::ClrDTR()
{
	EscapeCommFunction(StepMotor::Instance()->getCommHandle(),CLRDTR);
	Sleep(StepMotor::Instance()->GetPluseInterval());
}

void ZStage::ClrRTS()
{
	EscapeCommFunction(StepMotor::Instance()->getCommHandle(),CLRRTS);
	Sleep(StepMotor::Instance()->GetPluseInterval());
}

//
// Set current position as origin
//
int ZStage::SetOrigin()
{
	return DEVICE_OK;
}

//
// stop and interrupt Z stage motion
//
int ZStage::Stop()
{
	return DEVICE_OK;
}

///////////////////////////////////////////////////////////////////////////////
// Action handlers
///////////////////////////////////////////////////////////////////////////////

//
// Unsupported command from StepMotor
//
int ZStage::OnStepSize (MM::PropertyBase* /*pProp*/, MM::ActionType /*eAct*/) 
{
	return DEVICE_OK;
}

int ZStage::OnSpeed(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	long lVelocity = StepMotor::Instance()->GetVelocity();

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(lVelocity);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(lVelocity);
		StepMotor::Instance()->SetVelocity(lVelocity);
		StepMotor::Instance()->SetPluseInterval(1000/lVelocity);
	}

	return DEVICE_OK;
}

int ZStage::OnGetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	return DEVICE_OK;
}

int ZStage::OnSetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	return DEVICE_OK;
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

	if (StepMotor::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<ZStage::WriteCommand> (Command=";
		char sHex[4] = { NULL, NULL, NULL, NULL };
		for (int n = 0; n < nLength && ret == DEVICE_OK; n++)
		{
			StepMotor::Instance()->Byte2Hex((const unsigned char)sCommand[n], sHex);
			osMessage << "[" << n << "]=<" << sHex << ">";
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
		ret = (GetCoreCallback())->ReadFromSerial(pDevice, StepMotor::Instance()->GetSerialPort().c_str(), (unsigned char *)&sAnswer[lRead], (unsigned long)nLength-lRead, lByteRead);

		if (StepMotor::Instance()->GetDebugLogFlag() > 2)
		{
			osMessage.str("");
			osMessage << "<StepMotorCtrl::ReadMessage> (ReadFromSerial = (" << lByteRead << ")::<";
			for (unsigned long lIndx=0; lIndx < lByteRead; lIndx++)
			{
				// convert to hext format
				StepMotor::Instance()->Byte2Hex(sAnswer[lRead+lIndx], sHex);
				osMessage << "[" << sHex  << "]";
			}
			osMessage << ">";
			this->LogMessage(osMessage.str().c_str());
		}

		// concade new string
		lRead += lByteRead;

		if (lRead > 2)
		{
			yRead = (sAnswer[0] == 0x30 || sAnswer[0] == 0x31 || sAnswer[0] == 0x32 || sAnswer[0] == 0x34 || sAnswer[0] == 0x38) &&
					(sAnswer[1] == 0x0D) &&
					(sAnswer[2] == 0x0D);
		}

		yRead = yRead || (lRead >= (unsigned long)nBytesRead);

		if (yRead) break;

		// check for timeout
		yTimeout = ((double)(GetClockTicksUs() - lStartTime) / 10000.) > (double) m_nAnswerTimeoutMs;
		if (!yTimeout) CDeviceUtils::SleepMs(3);
	}

	// block/wait for acknowledge, or until we time out
	// if (!yRead || yTimeout) return DEVICE_SERIAL_TIMEOUT;
	// StepMotor::Instance()->ByteCopy(sResponse, sAnswer, nBytesRead);
	// if (checkError(sAnswer[0]) != 0) ret = DEVICE_SERIAL_COMMAND_FAILED;

	if (StepMotor::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<StepMotorCtrl::ReadMessage> (ReadFromSerial = <";
	}

	for (unsigned long lIndx=0; lIndx < (unsigned long)nBytesRead; lIndx++)
	{
		sResponse[lIndx] = sAnswer[lIndx];
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			StepMotor::Instance()->Byte2Hex(sResponse[lIndx], sHex);
			osMessage << "[" << sHex  << ",";
			StepMotor::Instance()->Byte2Hex(sAnswer[lIndx], sHex);
			osMessage << sHex  << "]";
		}
	}

	if (StepMotor::Instance()->GetDebugLogFlag() > 1)
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
	if (bErrorCode == StepMotor::StepMotor_SP_OVER_RUN)
	{
		// Serial command buffer over run
		nErrorCode = MPError::MPERR_SerialOverRun;
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == StepMotor::StepMotor_FRAME_ERROR)
	{
		// Receiving serial command time out
		nErrorCode = MPError::MPERR_SerialTimeout;
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == StepMotor::StepMotor_BUFFER_OVER_RUN)
	{
		// Serial command buffer full
		nErrorCode = MPError::MPERR_SerialBufferFull;
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == StepMotor::StepMotor_BAD_COMMAND)
	{
		// Invalid serial command
		nErrorCode = MPError::MPERR_SerialInpInvalid;
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == StepMotor::StepMotor_MOVE_INTERRUPTED)
	{
		// Serial command interrupt motion
		nErrorCode = MPError::MPERR_SerialIntrupMove;
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == 0x0D)
	{
		// read carriage return
		nErrorCode = MPError::MPERR_OK;
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<XYStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}
	else if (bErrorCode == 0x00)
	{
		// No response from serial port
		nErrorCode = MPError::MPERR_SerialZeroReturn;
		if (StepMotor::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<ZStage::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
	}

	if (StepMotor::Instance()->GetDebugLogFlag() > 1)
	{
		this->LogMessage(osMessage.str().c_str());
	}

	return (nErrorCode);
}


//
// check for valid communication port
//
int ZStage::OnPort(MM::PropertyBase* pProp, MM::ActionType pAct)
{
	std::ostringstream osMessage;
	string port = "";
	osMessage.str("");

	if (pAct == MM::BeforeGet)
	{
		pProp->Set(StepMotor::Instance()->GetSerialPort().c_str());
	}
	else if (pAct == MM::AfterSet)
	{
		if (m_yInitialized)
		{
			pProp->Get(port);
			StepMotor::Instance()->SetSerialPort(port);
			HANDLE oldComm = StepMotor::Instance()->getCommHandle();
			if(oldComm != INVALID_HANDLE_VALUE)
				CloseHandle(oldComm);

			HANDLE hComm = CreateFile(StepMotor::Instance()->GetSerialPort().c_str(),
					GENERIC_READ | GENERIC_WRITE,
					0,
					0,
					OPEN_EXISTING,
					FILE_FLAG_OVERLAPPED,
					0);
			if(hComm == INVALID_HANDLE_VALUE)
			{
				return GetLastError();
			}
			StepMotor::Instance()->setCommHandle(hComm);
		}
		pProp->Get(StepMotor::Instance()->GetSerialPort());
	}
	return DEVICE_OK;
}

int ZStage::OnUmToStep(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	int ret = DEVICE_OK;
	double Um2UStep = StepMotor::Instance()->GetUm2UStep();

	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(Um2UStep);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(Um2UStep);
		StepMotor::Instance()->SetUm2UStep(Um2UStep);
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

int ZStage::OnSetOrigin(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	int ret = DEVICE_OK;
	bool bisset = StepMotor::Instance()->GetIsSetOrigin();
	long iIsset =bisset?1:0;
	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(iIsset);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(iIsset);
		bisset = (iIsset == 1)?true:false;
		if(bisset)
			StepMotor::Instance()->SetPositionZ(0);
		StepMotor::Instance()->SetIsSetOrigin(bisset);
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}

int ZStage::OnStageMirrorZ(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	std::ostringstream osMessage;
	int ret = DEVICE_OK;
	bool bisset = StepMotor::Instance()->GetStageMirrorZ();
	long iIsset =bisset?1:0;
	osMessage.str("");

	if (eAct == MM::BeforeGet)
	{
		pProp->Set(iIsset);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(iIsset);
		bisset = (iIsset == 1)?true:false;
		StepMotor::Instance()->SetStageMirrorZ(bisset);
	}

	if (ret != DEVICE_OK) return ret;

	return DEVICE_OK;
}
