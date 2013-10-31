//////////////////////////////////////////////////////////////////////////////
// FILE:          SigmaKoki.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   SigmaKokis Controller Driver
//                XY Stage
//                Z  Stage
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
// AUTHOR:        Lon Chu (lonchu@yahoo.com), created on March 2011
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
#include <time.h>
#include "../../MMCore/MMCore.h"
#include "../../MMDevice/ModuleInterface.h"
#include "../../MMDevice/DeviceUtils.h"
#include "SigmaKokiError.h"
#include "SigmaKokiCtrl.h"
#include "SigmaKokiZStage.h"

using namespace std;

SigmaKoki* g_pSigmaKoki;
MPError* g_pMPError;

///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////

//
// Initialize the MMDevice name
//
MODULE_API void InitializeModuleData()
{
    g_pSigmaKoki = SigmaKoki::Instance();       // Initiate the SigmaKoki instance
    g_pMPError = MPError::Instance();   // Initiate the MPError instance


	// initialize the controller device name
	AddAvailableDeviceName( SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevName).c_str(),  SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevName).c_str());

	// initialize the Z stage device name
	AddAvailableDeviceName(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_ZStageDevName).c_str(), SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_ZStageDevName).c_str());
}

//
// Creating the MMDevice
//
MODULE_API MM::Device* CreateDevice(const char* sDeviceName)
{
    // checking for null pinter
    if (sDeviceName == 0) return 0;

    //struct tm tmNewTime;
    //__time64_t lLongTime;

    //_time64(&lLongTime);                        // Get time as 64-bit integer.
    //                                            // Convert to local time.
    //_localtime64_s(&tmNewTime, &lLongTime );    // C4996

    //std::ofstream ofsLogfile;
    //ofsLogfile.open(SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_LogFilename).c_str(), ios_base::out | ios_base::app);

    //if (ofsLogfile.is_open())
    //{
    //    ofsLogfile << "[" << tmNewTime.tm_year << "::" << tmNewTime.tm_mon << "::" << tmNewTime.tm_mday << "::" << tmNewTime.tm_hour << "::" << tmNewTime.tm_min << "::" << tmNewTime.tm_sec << "]   ";
    //    ofsLogfile << "<SigmaKoki::CreateDevice> deviceName = (" << sDeviceName << ") :: SigmaKokiCtrl = (" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevName).c_str() << ") \n" << flush;
    //}

    if (strcmp(sDeviceName, SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_CtrlDevName).c_str()) == 0) 
    {
        // if device name is SigmaKoki Controller, create the SigmaKoki device
        SigmaKokiCtrl*  pCtrlDev = new SigmaKokiCtrl();
        return pCtrlDev;
    }

    if (strcmp(sDeviceName, SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_ZStageDevName).c_str()) == 0)
    {
        // if device name is Z Stage, create the Z Stage Device 
        ZStage* pZStage = new ZStage();
        return pZStage;
    }
    //else
    //{
    //    std::ostringstream sMessage;
    //    sMessage << "<SigmaKoki::CreateDevice> deviceName = (" << sDeviceName << ") :: SigmaKokiZ = (" << SigmaKoki::Instance()->GetSKStr(SigmaKoki::SKSTR_ZStageDevName).c_str() << ") ";
    //    std::cerr << sMessage.str().c_str() << "\n" << flush;
    //}

    //ofsLogfile.close();

    // device name is not recognized, return null
    return NULL;
}

//
// delete the device --> invoke device destructor
//
MODULE_API void DeleteDevice(MM::Device* pDevice)
{
    if (pDevice != 0) delete pDevice;
}

//
// General utility function
//
int ClearPort(MM::Device& device, MM::Core& core, const char* sPort)
{
    // Clear contents of serial port 
    const int nBufSize = 255;
    unsigned char sClear[nBufSize];                                                        
    unsigned long lRead = nBufSize;                                               
    int ret;

    // reset the communication port buffer
    while ((int) lRead == nBufSize)                                                     
    { 
        // reading from the serial port
        ret = core.ReadFromSerial(&device, sPort, sClear, nBufSize, lRead);

        std::ostringstream sMessage;
        sMessage << "<SigmaKoki::ClearPort> port = (" <<  sPort << ") :: clearBuffer(" << lRead << ")  = (" << sClear << ")";
        core.LogMessage(&device, sMessage.str().c_str(), false);

        // verify the read operation
        if (ret != DEVICE_OK) return ret;                                                           
    } 

    // upon successful restting the port
    return DEVICE_OK;                                                           
} 

bool            SigmaKoki::m_yInstanceFlag      = false;        // instance flag
bool            SigmaKoki::m_yDeviceAvailable   = false;        // SigmaKoki devices availability
int				SigmaKoki::m_nDebugLogFlag		= 2;			// SigmaKoki debug log flag
SigmaKoki*          SigmaKoki::m_pSigmaKoki             = NULL;         // single copy SigmaKoki
int             SigmaKoki::m_nResolution        = 10;           // SigmaKoki resolution
int             SigmaKoki::m_nMotionMode        = 0;            // motor motion mode
int             SigmaKoki::m_nUm2UStep          = 25;           // unit to convert um to uStep
int             SigmaKoki::m_nUStep2Nm          = 40;           // unit to convert uStep to nm
int             SigmaKoki::m_nTimeoutInterval   = 10000;        // timeout interval
int             SigmaKoki::m_nTimeoutTrys       = 5;            // timeout trys
long            SigmaKoki::m_lVelocity          = 18000;         // velocity
double          SigmaKoki::m_dPositionX         = 0.00;         // X Position
double          SigmaKoki::m_dPositionY         = 0.00;         // Y Position
double          SigmaKoki::m_dPositionZ         = 0.00;         // Z Position
//int           SigmaKoki::m_nNumberOfAxes      = 3;            // number of axes attached to the controller, initial set to zero
std::string SigmaKoki::m_sPort;                                 // serial port symbols

SigmaKoki::SigmaKoki()
{
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_CtrlDevName]       = "SigmaKoki Controller";					// SigmaKoki Controllet device name
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_XYStgaeDevName]    = "SigmaKoki XY Stage";						// SigmaKoki XY Stage device name
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_ZStageDevName]     = "SigmaKoki Z Stage";						// MP286 Z Stage device name
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_SigmaKokiVersion]      = "2.05.056";							// SigmaKoki adpater version number
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_LogFilename]       = "SigmaKokiLog.txt";						// SigmaKoki Logfile name
	SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_CtrlDevNameLabel]  = "M.00 Controller ";					// SigmaKoki Controller device name label
	SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_CtrlDevDescLabel]  = "M.01 Controller ";					// SigmaKoki Controller device description label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_FirmwareVerLabel]  = "M.02 Firmware Version";				// SigmaKoki FIRMWARE VERSION label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_SigmaKokiVerLabel]     = "M.03 SigmaKoki Adapter Version";			// SigmaKoki ADAPTER VERSION label
	SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_DebugLogFlagLabel] = "M.04 Debug Log Flag";				// SigmaKoki Debug Lg Flag Label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_CommStateLabel]    = "M.05 SigmaKoki Comm. Status";			// SigmaKoki COMM. STATUS label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_ResolutionLabel]   = "M.06 Resolution (10 or 50)";			// SigmaKoki RESOLUION label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_AccelLabel]        = "M.07 Acceleration";					// SigmaKoki ACCELERATION label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_Um2UStepUnit]      = "M.08 um to uStep";					// SigmaKoki um to ustep label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_UStep2NmUnit]      = "M.09 uStep to nm";					// SigmaKoki ustep to nm label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_VelocityLabel]     = "M.10 Velocity (uStep/s)";			// SigmaKoki VELOCITY label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_MotionMode]        = "M.11 Mode (0=ABS/1=REL)";			// SigmaKoki MODE label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_SetOrigin]         = "M.12 Origin (1=set)";                // SigmaKoki ORIGIN label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_TimeoutInterval]   = "M.13 Timeout Interval (ms)";         // SigmaKoki Timeout Interval
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_TimeoutTrys]       = "M.14 Timeout Trys";                  // SigmaKoki Timeout Trys
	SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_XYDevNameLabel]    = "M.15 XY Stage ";						// SigmaKoki XY stage device name label
	SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_XYDevDescLabel]    = "M.16 XY Stage ";						// SigmaKoki XY stage device description label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_SetPositionX]      = "M.17 Set Position X (um)";			// SigmaKoki set POSITION X label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_SetPositionY]      = "M.18 Set Position Y (um)";			// SigmaKoki set POSITION Y label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_GetPositionX]      = "M.19 Get Position X (uStep)";	    // SigmaKoki get POSITION X label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_GetPositionY]      = "M.20 Get Position Y (uStep)";		// SigmaKoki get POSITION Y label
	SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_ZDevNameLabel]     = "M.21 Z Stage ";						// SigmaKoki Z stage device name label
	SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_ZDevDescLabel]     = "M.22 Z Stage ";						// SigmaKoki Z stage device description label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_SetPositionZ]      = "M.23 Set Position Z (um)";			// SigmaKoki set POSITION Z label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_GetPositionZ]      = "M.24 Get Position Z (uStep)";		// SigmaKoki get POSITION Z label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_PauseMode]         = "M.25 Pause (0=continue/1=pause)";    // property PAUSE label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_Reset]             = "M.26 Reset (1=reset)";               // property RESET label
    SigmaKoki::m_sSKStr[SigmaKoki::SKSTR_Status]            = "M.27 Status (1=update)";             // property STATUS label
}

SigmaKoki::~SigmaKoki()
{
    if (m_pSigmaKoki) delete m_pSigmaKoki;
    m_yInstanceFlag = false;
}

SigmaKoki* SigmaKoki::Instance()
{
    if(!m_yInstanceFlag)
    {
        m_pSigmaKoki = new SigmaKoki();
        m_yInstanceFlag = true;
    }

    return m_pSigmaKoki;
}

//
// Get SigmaKoki constant string
//
std::string SigmaKoki::GetSKStr(int nSKStrCode) const
{ 
   string sText;        // SigmaKoki String

   if (m_pSigmaKoki != NULL)
   {
       map<int, string>::const_iterator nIterator;
       nIterator = m_sSKStr.find(nSKStrCode);   
       if (nIterator != m_sSKStr.end())
          sText = nIterator->second;
   }

   return sText;
}

//
// Copy byte data buffer for iLength
//
int SigmaKoki::ByteCopy(unsigned char* bDst, const unsigned char* bSrc, int nLength)
{
    int nBytes = 0;
    if (bSrc == NULL || bDst == NULL) return(nBytes);
    for (nBytes = 0; nBytes < nLength; nBytes++) bDst[nBytes] = bSrc[nBytes];
    return nBytes;
}

//
// Convert byte data to hex string
//
void SigmaKoki::Byte2Hex(const unsigned char bByte, char* sHex)
{
    char sHexDigit[16] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    sHex[2] =  NULL;
    sHex[1] = sHexDigit[(int)(bByte & 0xF)];
    sHex[0] = sHexDigit[(int)(bByte / 0x10)];
    return;
}

