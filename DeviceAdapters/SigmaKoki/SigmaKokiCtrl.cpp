//////////////////////////////////////////////////////////////////////////////
// FILE:          SigmaKokiCtrl.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   SigmaKoki Controller Driver
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
#include "SigmaKokiError.h"
#include "SigmaKoki.h"
#include "SigmaKokiCtrl.h"

using namespace std;


//////////////////////////////////////////////////////
// SigmaKoki Controller
//////////////////////////////////////////////////////
//
// Controller - Controller for XYZ Stage.
// Note that this adapter uses two coordinate systems.  There is the adapters own coordinate
// system with the X and Y axis going the 'Micro-Manager standard' direction
// Then, there is the SigmaKokis native system.  All functions using 'steps' use the SigmaKoki system
// All functions using Um use the Micro-Manager coordinate system
//


//
// SigmaKoki Controller Constructor
//
SigmaKokiCtrl::SigmaKokiCtrl() :
    //m_nAnswerTimeoutMs(1000),   // wait time out set 1000 ms
    m_yInitialized(false)       // initialized flag set to false
{
    // call initialization of error messages
    InitializeDefaultErrorMessages();

    m_nAnswerTimeoutMs = SigmaKoki::Instance()->GetTimeoutInterval();
    m_nAnswerTimeoutTrys = SigmaKoki::Instance()->GetTimeoutTrys();

    // Port:
    CPropertyAction* pAct = new CPropertyAction(this, &SigmaKokiCtrl::OnPort);
    int ret = CreateProperty(MM::g_Keyword_Port, "Undefined", MM::String, false, pAct, true);
    
    std::ostringstream osMessage;

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::class-constructor> CreateProperty(" << MM::g_Keyword_Port << " = Undfined), ReturnCode=" << ret;
		this->LogMessage(osMessage.str().c_str());
	}
}

//
// SigmaKoki Controller Destructor
//
SigmaKokiCtrl::~SigmaKokiCtrl()
{
    Shutdown();
}

//
// return device name of the SigmaKoki controller
//
void SigmaKokiCtrl::GetName(char* sName) const
{
    CDeviceUtils::CopyLimitedString(sName, SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevName).c_str());
}

//
// Initialize the SigmaKoki controller
//
int SigmaKokiCtrl::Initialize()
{
    std::ostringstream osMessage;

    // empty the Rx serial buffer before sending command
    int ret = ClearPort(*this, *GetCoreCallback(), SigmaKoki::Instance()->GetSerialPort().c_str());

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::Initialize> ClearPort(Port = " << SigmaKoki::Instance()->GetSerialPort().c_str() << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    // Name
    char sCtrlName[120];
	sprintf(sCtrlName, "%s%s", SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevNameLabel).c_str(), MM::g_Keyword_Name);
    ret = CreateProperty(sCtrlName, SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevName).c_str(), MM::String, true);

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::Initialize> CreateProperty(" << sCtrlName << " = " << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevName).c_str() << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    // Description
    char sCtrlDesc[120];
	sprintf(sCtrlDesc, "%s%s", SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevDescLabel).c_str(), MM::g_Keyword_Description);
    ret = CreateProperty(sCtrlDesc, "Sutter MP-285 Controller", MM::String, true);

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::Initialize> CreateProperty(" << sCtrlDesc << " = " << "Sutter MP-285 Controller" << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK)  return ret;

    // Create read-only property for version info
    // SigmaKoki Adpater Version Property
    // const char* sVersionLabel = SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_SigmaKokiVerLabel).c_str();
    // const char* sVersion = SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_SigmaKokiVersion).c_str();
    ret = CreateProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_SigmaKokiVerLabel).c_str(), SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_SigmaKokiVersion).c_str(), MM::String, true);

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::Initialize> CreateProperty(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_SigmaKokiVerLabel).c_str() << " = " << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_SigmaKokiVersion).c_str() << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    char sTimeoutInterval[20];
    memset(sTimeoutInterval, 0, 20);
    sprintf(sTimeoutInterval, "%d", SigmaKoki::Instance()->GetTimeoutInterval());

    CPropertyAction* pActOnTimeoutInterval = new CPropertyAction(this, &SigmaKokiCtrl::OnTimeoutInterval);
    ret = CreateProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutInterval).c_str(), sTimeoutInterval, MM::Integer,  false, pActOnTimeoutInterval); 

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::Initialize> CreateProperty(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutInterval).c_str() << " = " << sTimeoutInterval << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    char sTimeoutTrys[20];
    memset(sTimeoutTrys, 0, 20);
    sprintf(sTimeoutTrys, "%d", SigmaKoki::Instance()->GetTimeoutTrys());

    CPropertyAction* pActOnTimeoutTrys = new CPropertyAction(this, &SigmaKokiCtrl::OnTimeoutTrys);
    ret = CreateProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutTrys).c_str(), sTimeoutTrys, MM::Integer,  false, pActOnTimeoutTrys); 

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::Initialize> CreateProperty(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutTrys).c_str() << " = " << sTimeoutTrys << "), ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;


    // Read status data
    unsigned int nLength = 256;
    unsigned char sResponse[256];
    ret = CheckStatus(sResponse, nLength);

    if (ret != DEVICE_OK) return ret;


    ret = UpdateStatus();
    if (ret != DEVICE_OK) return ret;

    // Create  property for debug log flag
    int nDebugLogFlag = SigmaKoki::Instance()->GetDebugLogFlag();
    CPropertyAction* pActDebugLogFlag = new CPropertyAction (this, &SigmaKokiCtrl::OnDebugLogFlag);
	ret = CreateProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_DebugLogFlagLabel).c_str(), CDeviceUtils::ConvertToString(nDebugLogFlag), MM::Integer, false, pActDebugLogFlag);

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 0)
    {
        osMessage.str("");
        osMessage << "SigmaKokiCtrl::Initialize> CreateProperty(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_DebugLogFlagLabel).c_str() << " = " << nDebugLogFlag << "), ReturnCode = " << ret;
        this->LogMessage(osMessage.str().c_str());
    }

	m_yInitialized = true;
    SigmaKoki::Instance()->SetDeviceAvailable(true);

    return DEVICE_OK;
}

//
// check controller's status bytes
//
int SigmaKokiCtrl::CheckStatus(unsigned char* sResponse, unsigned int nLength)
{
    std::ostringstream osMessage;
    unsigned char sCommand[8] ="\r\n?:N\r\n";
    int ret = WriteCommand(sCommand, 8);

    if (ret != DEVICE_OK) return ret;

    //unsigned int nBufLen = 256;
    //unsigned char sAnswer[256];

    memset(sResponse, 0, nLength);
    ret = ReadMessage(sResponse, 8);
//"FINE-01r"
	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::CheckStatus::ReadMessage> (ReturnCode = " << ret << ")return msg=("<<sResponse<<")";
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

//
// shutdown the controller
//
int SigmaKokiCtrl::Shutdown()
{ 
    m_yInitialized = false;
    SigmaKoki::Instance()->SetDeviceAvailable(false);
    return DEVICE_OK;
}

//////////////// Action Handlers (Hub) /////////////////

//
// check for valid communication port
//
int SigmaKokiCtrl::OnPort(MM::PropertyBase* pProp, MM::ActionType pAct)
{
    std::ostringstream osMessage;

	osMessage.str("");

    if (pAct == MM::BeforeGet)
    {
        pProp->Set(SigmaKoki::Instance()->GetSerialPort().c_str());
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnPort> (BeforeGet::PORT=<" << SigmaKoki::Instance()->GetSerialPort().c_str() << ">";
			osMessage << " PROPSET=<" << SigmaKoki::Instance()->GetSerialPort().c_str() << ">)";
		}
	}
    else if (pAct == MM::AfterSet)
    {
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnPort> (AfterSet::PORT=<" << SigmaKoki::Instance()->GetSerialPort().c_str() << ">";
		}
        if (m_yInitialized)
        {
            pProp->Set(SigmaKoki::Instance()->GetSerialPort().c_str());
			if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
			{
				osMessage << "Initialized::SET=<" << SigmaKoki::Instance()->GetSerialPort().c_str() << ">";
				this->LogMessage(osMessage.str().c_str());
			}
            return DEVICE_INVALID_INPUT_PARAM;
        }
        pProp->Get(SigmaKoki::Instance()->GetSerialPort());
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << " SPROPGET=<" << SigmaKoki::Instance()->GetSerialPort().c_str() << ">)";
		}
    }
	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		this->LogMessage(osMessage.str().c_str());
	}
    return DEVICE_OK;
}

//
// get/set debug log flag
//
int SigmaKokiCtrl::OnDebugLogFlag(MM::PropertyBase* pProp, MM::ActionType pAct)
{
    long lDebugLogFlag = (long)SigmaKoki::Instance()->GetDebugLogFlag();
    std::ostringstream osMessage;

	osMessage.str("");

    if (pAct == MM::BeforeGet)
    {
        pProp->Set(lDebugLogFlag);
        if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
        {
			osMessage << "<SigmaKokiCtrl::OnDebugLogFalg> (BeforeGet::<" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_DebugLogFlagLabel).c_str() << "> PROPSET=<" << lDebugLogFlag << ">)";
        }
    }
    else if (pAct == MM::AfterSet)
    {
        pProp->Get(lDebugLogFlag);
        SigmaKoki::Instance()->SetDebugLogFlag((int)lDebugLogFlag);
        if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
        {
            osMessage << "<SigmaKokiCtrl::OnDebugLogFalg> (AfterSet::<" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_DebugLogFlagLabel).c_str() << "> PROPSET=<" << lDebugLogFlag << ">)";
        }
    }

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
        this->LogMessage(osMessage.str().c_str());
    }

    return DEVICE_OK;
}

//
// Set current position as origin (0,0) coordinate of the controller.
//
int SigmaKokiCtrl::SetOrigin()
{
    unsigned char sCommand[6] = { 0x6F, SigmaKoki::SigmaKoki_TxTerm, 0x0A, 0x00, 0x00, 0x00 };
    int ret = WriteCommand(sCommand, 3);

    std::ostringstream osMessage;

	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		osMessage.str("");
		osMessage << "<SigmaKoki::SigmaKoki::SetOrigin> (ReturnCode=" << ret << ")";
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret!=DEVICE_OK) return ret;

    unsigned char sResponse[64];

    memset(sResponse, 0, 64);
    ret = ReadMessage(sResponse, 2);

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::CheckStatus::SetOrigin> (ReturnCode = " << ret << ")";
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    bool yCommError = CheckError(sResponse[0]);

    char sCommStat[30];
    if (yCommError)
        sprintf(sCommStat, "Error Code ==> <%2x>", sResponse[0]);
    else
        strcpy(sCommStat, "Success");

    ret = SetProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CommStateLabel).c_str(), sCommStat);

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

//
// Set resolution.
//
int SigmaKokiCtrl::SetResolution(long lResolution)
{
    std::ostringstream osMessage;
    //unsigned char sCmdSet[6] = { 0x56, 0x00, 0x00, SigmaKoki::SigmaKoki_TxTerm, 0x0A, 0x00 };
    //unsigned char sResponse[64];
    //int ret = DEVICE_OK;
    //char sCommStat[30];
    //bool yCommError = false;

    //if (SigmaKoki::Instance()->GetResolution() == 50)
    //    lVelocity = (lVelocity & 0x7FFF) | 0x8000;
    // else
    //    lVelocity = lVelocity & 0x7FFF;

    //sCmdSet[1] = (unsigned char)((lVelocity & 0xFF00) / 256);
    //sCmdSet[2] = (unsigned char)(lVelocity & 0xFF);
        
    //ret = WriteCommand(sCmdSet, 5);

	//if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    //{
	//	osMessage.str("");
	//	osMessage << "<SigmaKokiCtrl::SetVelocity> = " << lVelocity << ", ReturnCode = " << ret;
	//	this->LogMessage(osMessage.str().c_str());
	//}

    //if (ret != DEVICE_OK) return ret;

    //ret = ReadMessage(sResponse, 2);

    //if (ret != DEVICE_OK) return ret;

    //SigmaKoki::Instance()->SetVelocity(lVelocity);

    //yCommError = CheckError(sResponse[0]);
    //if (yCommError)
    //    sprintf((char*)sCommStat, "Error Code ==> <%2x>", sResponse[0]);
    //else
    //    strcpy(sCommStat, "Success");

    //ret = SetProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CommStateLabel).c_str(), sCommStat);

    //if (ret != DEVICE_OK) return ret;

	SigmaKoki::Instance()->SetResolution(lResolution);

    return DEVICE_OK;
}

//
// Set velocity.
//
int SigmaKokiCtrl::SetVelocity(long lVelocity)
{
    std::ostringstream osMessage;
    unsigned char sCmdSet[6] = { 0x56, 0x00, 0x00, SigmaKoki::SigmaKoki_TxTerm, 0x0A, 0x00 };
    unsigned char sResponse[64];
    int ret = DEVICE_OK;
    char sCommStat[30];
    bool yCommError = false;

    if (SigmaKoki::Instance()->GetResolution() == 50)
        lVelocity = (lVelocity & 0x7FFF) | 0x8000;
    else
        lVelocity = lVelocity & 0x7FFF;

    sCmdSet[1] = (unsigned char)((lVelocity & 0xFF00) / 256);
    sCmdSet[2] = (unsigned char)(lVelocity & 0xFF);
        
    ret = WriteCommand(sCmdSet, 5);

	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::SetVelocity> = " << lVelocity << ", ReturnCode = " << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    ret = ReadMessage(sResponse, 2);

    if (ret != DEVICE_OK) return ret;

    SigmaKoki::Instance()->SetVelocity(lVelocity);

    yCommError = CheckError(sResponse[0]);
    if (yCommError)
        sprintf((char*)sCommStat, "Error Code ==> <%2x>", sResponse[0]);
    else
        strcpy(sCommStat, "Success");

    ret = SetProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CommStateLabel).c_str(), sCommStat);

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

//
// Set Motion Mode
//
int SigmaKokiCtrl::SetMotionMode(long lMotionMode)
{
    std::ostringstream osMessage;
    unsigned char sCommand[6] = { 0x00, SigmaKoki::SigmaKoki_TxTerm, 0x0A, 0x00, 0x00, 0x00 };
    unsigned char sResponse[64];
    int ret = DEVICE_OK;
    char sCommStat[30];
    bool yCommError = false;
        
    if (lMotionMode == 0)
        sCommand[0] = 'a';
    else
        sCommand[0] = 'b';

    ret = WriteCommand(sCommand, 3);

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::SetMotionMode> = [" << lMotionMode << "," << sCommand[0] << "], Returncode =" << ret;
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    ret = ReadMessage(sResponse, 2);

    if (ret != DEVICE_OK) return ret;

    SigmaKoki::Instance()->SetMotionMode(lMotionMode);

    yCommError = CheckError(sResponse[0]);
    if (yCommError)
        sprintf((char*)sCommStat, "Error Code ==> <%2x>", sResponse[0]);
    else
        strcpy((char*)sCommStat, "Success");

    ret = SetProperty(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CommStateLabel).c_str(), (const char*)sCommStat);

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

//
// stop and interrupt Z stage motion
//
int SigmaKokiCtrl::Stop()
{
    unsigned char sCommand[6] = { 0x03, SigmaKoki::SigmaKoki_TxTerm, 0x0A, 0x00, 0x00, 0x00 };

    int ret = WriteCommand(sCommand, 3);

    ostringstream osMessage;

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		osMessage.str("");
		osMessage << "<SigmaKoki::SigmaKokiCtrl::Stop> (ReturnCode = " << ret << ")";
		this->LogMessage(osMessage.str().c_str());
	}

    return ret;
}

/*
 * Resolution as returned by device is in 0 or 1 of Bits 15
 */
int SigmaKokiCtrl::OnResolution(MM::PropertyBase* pProp, MM::ActionType eAct)
{
    std::ostringstream osMessage;
    int ret = DEVICE_OK;
	long lResolution = (long)SigmaKoki::Instance()->GetResolution();

	osMessage.str("");

    if (eAct == MM::BeforeGet)
    {
        pProp->Set(lResolution);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnResolution> BeforeGet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_ResolutionLabel).c_str() << " = [" << lResolution << "], ReturnCode = " << ret;
		}
    }
    else if (eAct == MM::AfterSet)
    {
        pProp->Get(lResolution);

        ret = SetResolution(lResolution);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnResolution> AfterSet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_ResolutionLabel).c_str() << " = [" << lResolution << "], ReturnCode = " << ret;
		}
    }

	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}



/*
 * Speed as returned by device is in um/s
 */
int SigmaKokiCtrl::OnSpeed(MM::PropertyBase* pProp, MM::ActionType eAct)
{
    std::ostringstream osMessage;
    int ret = DEVICE_OK;
    long lVelocity = SigmaKoki::Instance()->GetVelocity();

	osMessage.str("");

    if (eAct == MM::BeforeGet)
    {
        pProp->Set(lVelocity);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnSpeed> BeforeGet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_VelocityLabel).c_str() << " = [" << lVelocity << "], ReturnCode = " << ret;
		}
    }
    else if (eAct == MM::AfterSet)
    {
        pProp->Get(lVelocity);

        ret = SetVelocity(lVelocity);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnSpeed> AfterSet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_VelocityLabel).c_str() << " = [" << lVelocity << "], ReturnCode = " << ret;
		}
    }

	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

/*
 * Speed as returned by device is in um/s
 */
int SigmaKokiCtrl::OnMotionMode(MM::PropertyBase* pProp, MM::ActionType eAct)
{
    std::string sMotionMode;
    std::ostringstream osMessage;
    long lMotionMode = (long)SigmaKoki::Instance()->GetMotionMode();
    int ret = DEVICE_OK;

	osMessage.str("");

    if (eAct == MM::BeforeGet)
    {
        pProp->Set(lMotionMode);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnMotionMode> BeforeGet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_MotionMode).c_str() << " = " << lMotionMode << "), ReturnCode = " << ret;
		}
    }
    else if (eAct == MM::AfterSet)
    {
        pProp->Get(lMotionMode);

        ret = SetMotionMode(lMotionMode);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnSpeed> AfterSet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_MotionMode).c_str() << " = " << lMotionMode <<  "), ReturnCode = " << ret;
		}
    }    
	
	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

/*
 * Set/Get Timeout Interval
 */
int SigmaKokiCtrl::OnTimeoutInterval(MM::PropertyBase* pProp, MM::ActionType eAct)
{
    std::ostringstream osMessage;
    long lTimeoutInterval = (long)SigmaKoki::Instance()->GetTimeoutInterval();
    int ret = DEVICE_OK;

	osMessage.str("");

    if (eAct == MM::BeforeGet)
    {
        pProp->Set(lTimeoutInterval);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnTimeoutInterval> BeforeGet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutInterval).c_str() << " = " << lTimeoutInterval << "), ReturnCode = " << ret;
		}
    }
    else if (eAct == MM::AfterSet)
    {
        pProp->Get(lTimeoutInterval);
        SigmaKoki::Instance()->SetTimeoutInterval((int)lTimeoutInterval);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnTimeoutInterval> AfterSet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutInterval).c_str() << " = " << SigmaKoki::Instance()->GetTimeoutInterval() <<  "), ReturnCode = " << ret;
		}
    }

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		this->LogMessage(osMessage.str().c_str());
	}

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

/*
 * Set/Get Timeout Trys
 */
int SigmaKokiCtrl::OnTimeoutTrys(MM::PropertyBase* pProp, MM::ActionType eAct)
{
    std::ostringstream osMessage;
    long lTimeoutTrys = (long)SigmaKoki::Instance()->GetTimeoutTrys();
    int ret = DEVICE_OK;

	osMessage.str("");

    if (eAct == MM::BeforeGet)
    {
        pProp->Set(lTimeoutTrys);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnTimeoutTrys> BeforeGet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutTrys).c_str() << " = " << lTimeoutTrys << "), ReturnCode = " << ret;
		}
    }
    else if (eAct == MM::AfterSet)
    {
        pProp->Get(lTimeoutTrys);
        SigmaKoki::Instance()->SetTimeoutTrys((int)lTimeoutTrys);

		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::OnTimeoutTrys> AfterSet(" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_TimeoutTrys).c_str() << " = " << SigmaKoki::Instance()->GetTimeoutTrys() <<  "), ReturnCode = " << ret;
		}
    }

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
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
int SigmaKokiCtrl::WriteCommand(unsigned char* sCommand, int nLength)
{
    int ret = DEVICE_OK;
    ostringstream osMessage;

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::WriteCommand> (Command=";
		char sHex[4] = { NULL, NULL, NULL, NULL };
		for (int n=0; n < nLength; n++)
		{
			SigmaKoki::Instance()->Byte2Hex((const unsigned char)sCommand[n], sHex);
			osMessage << "[" << n << "]=<" << sHex << ">";
		}
		osMessage << ")";
		this->LogMessage(osMessage.str().c_str());
	}

    for (int nBytes = 0; nBytes < nLength && ret == DEVICE_OK; nBytes++)
    {
        ret = WriteToComPort(SigmaKoki::Instance()->GetSerialPort().c_str(), (const unsigned char*)&sCommand[nBytes], 1);
        CDeviceUtils::SleepMs(1);
    }

    if (ret != DEVICE_OK) return ret;

    return DEVICE_OK;
}

//
// Read a message from serial port
//
int SigmaKokiCtrl::ReadMessage(unsigned char* sResponse, int nBytesRead)
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
        ret = (GetCoreCallback())->ReadFromSerial(pDevice, SigmaKoki::Instance()->GetSerialPort().c_str(), (unsigned char *)&sAnswer[lRead], (unsigned long)nLength-lRead, lByteRead);
       
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage.str("");
			osMessage << "<SigmaKokiCtrl::ReadMessage> (ReadFromSerial = (" << nBytesRead << "," << lRead << "," << lByteRead << ")::<";
		
			for (unsigned long lIndx=0; lIndx < lByteRead; lIndx++)
			{
				// convert to hext format
				SigmaKoki::Instance()->Byte2Hex(sAnswer[lRead+lIndx], sHex);
				osMessage << "[" << sHex  << "]";
			}
			osMessage << ">";
			this->LogMessage(osMessage.str().c_str());
		}

        // concade new string
        lRead += lByteRead;
//
//        if (lRead > 2)
//        {
//            yRead = (sAnswer[0] == 0x30 || sAnswer[0] == 0x31 || sAnswer[0] == 0x32 || sAnswer[0] == 0x34 || sAnswer[0] == 0x38) &&
//                    (sAnswer[1] == 0x0D) &&
//                    (sAnswer[2] == 0x0D);
//        }
//        else if (lRead == 2)
//        {
//            yRead = (sAnswer[0] == 0x0D) && (sAnswer[1] == 0x0D);
//        }
//
//        yRead = yRead || (lRead >= (unsigned long)nBytesRead);
//
         yRead =   (lRead >= (unsigned long)nBytesRead);
        if (yRead) break;
        
        // check for timeout
        yTimeout = ((double)(GetClockTicksUs() - lStartTime) / 10000. ) > (double) m_nAnswerTimeoutMs;
        if (!yTimeout) CDeviceUtils::SleepMs(3);

		//if (SigmaKoki::Instance()->GetDebugLogFlag() > 2)
		//{
		//	osMessage.str("");
		//	osMessage << "<SigmaKokiCtrl::ReadMessage> (ReadFromSerial = (" << nBytesRead << "," << lRead << "," << yRead << yTimeout << ")";
		//	this->LogMessage(osMessage.str().c_str());
		//}
    }

    // block/wait for acknowledge, or until we time out
    // if (!yRead || yTimeout) return DEVICE_SERIAL_TIMEOUT;
    // SigmaKoki::Instance()->ByteCopy(sResponse, sAnswer, nBytesRead);
    // if (checkError(sAnswer[0])) ret = DEVICE_SERIAL_COMMAND_FAILED;

	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage.str("");
		osMessage << "<SigmaKokiCtrl::ReadMessage> (ReadFromSerial = <";
	}

	for (unsigned long lIndx=0; lIndx < (unsigned long)nBytesRead; lIndx++)
	{
		sResponse[lIndx] = sAnswer[lIndx];
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			SigmaKoki::Instance()->Byte2Hex(sResponse[lIndx], sHex);
			osMessage << "[" << sHex  << ",";
			SigmaKoki::Instance()->Byte2Hex(sAnswer[lIndx], sHex);
			osMessage << sHex  << "]";
		}
	}

	if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
	{
		osMessage << ">";
		this->LogMessage(osMessage.str().c_str());
	}

    return DEVICE_OK;
}

//
// check the error code for the message returned from serial communivation
//
bool SigmaKokiCtrl::CheckError(unsigned char bErrorCode)
{
    // if the return message is 2 bytes message including CR
    unsigned int nErrorCode = 0;
    ostringstream osMessage;

	osMessage.str("");

    // check 4 error code
    if (bErrorCode == SigmaKoki::SigmaKoki_SP_OVER_RUN)
    {
        // Serial command buffer over run
        nErrorCode = MPError::MPERR_SerialOverRun;       
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
    }
    else if (bErrorCode == SigmaKoki::SigmaKoki_FRAME_ERROR)
    {
        // Receiving serial command time out
        nErrorCode = MPError::MPERR_SerialTimeout;       
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
    }
    else if (bErrorCode == SigmaKoki::SigmaKoki_BUFFER_OVER_RUN)
    {
        // Serial command buffer full
        nErrorCode = MPError::MPERR_SerialBufferFull;       
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
    }
    else if (bErrorCode == SigmaKoki::SigmaKoki_BAD_COMMAND)
    {
        // Invalid serial command
        nErrorCode = MPError::MPERR_SerialInpInvalid;       
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
    }
    else if (bErrorCode == SigmaKoki::SigmaKoki_MOVE_INTERRUPTED)
    {
        // Serial command interrupt motion
        nErrorCode = MPError::MPERR_SerialIntrupMove;       
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
    }
    else if (bErrorCode == 0x00)
    {
        // No response from serial port
        nErrorCode = MPError::MPERR_SerialZeroReturn;
		if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
		{
			osMessage << "<SigmaKokiCtrl::checkError> ErrorCode=[" << MPError::Instance()->GetErrorText(nErrorCode).c_str() << "])";
		}
    }

    if (SigmaKoki::Instance()->GetDebugLogFlag() > 1)
    {
		this->LogMessage(osMessage.str().c_str());
	}

    return (nErrorCode!=0);
}


