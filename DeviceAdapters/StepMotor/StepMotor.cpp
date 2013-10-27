//////////////////////////////////////////////////////////////////////////////
// FILE:          StepMotor.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   StepMotors Controller Driver
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
#include "StepMotorError.h"
#include "StepMotorZStage.h"

using namespace std;

StepMotor* g_pStepMotor;
MPError* g_pMPError;

///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////

//
// Initialize the MMDevice name
//
MODULE_API void InitializeModuleData()
{
    g_pStepMotor = StepMotor::Instance();       // Initiate the StepMotor instance
    g_pMPError = MPError::Instance();   // Initiate the MPError instance


	// initialize the Z stage device name
	AddAvailableDeviceName(StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_ZStageDevName).c_str(), StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_ZStageDevName).c_str());
}

//
// Creating the MMDevice
//
MODULE_API MM::Device* CreateDevice(const char* sDeviceName)
{
    // checking for null pinter
    if (sDeviceName == 0) return 0;

    if (strcmp(sDeviceName, StepMotor::Instance()->GetMPStr(StepMotor::SMSTR_ZStageDevName).c_str()) == 0)
    {
        // if device name is Z Stage, create the Z Stage Device 
        ZStage* pZStage = new ZStage();
        return pZStage;
    }

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
        sMessage << "<StepMotor::ClearPort> port = (" <<  sPort << ") :: clearBuffer(" << lRead << ")  = (" << sClear << ")";
        core.LogMessage(&device, sMessage.str().c_str(), false);

        // verify the read operation
        if (ret != DEVICE_OK) return ret;                                                           
    } 

    // upon successful restting the port
    return DEVICE_OK;                                                           
} 
bool            StepMotor::m_isSetOrigin = false;            // singleton flag
bool            StepMotor::m_stageMirrorZ = false;            // singleton flag
HANDLE			StepMotor::m_hcomm = NULL;
int             StepMotor::m_PluseInterval = 1;
bool            StepMotor::m_yInstanceFlag      = false;        // instance flag
bool            StepMotor::m_yDeviceAvailable   = false;        // StepMotor devices availability
int				StepMotor::m_nDebugLogFlag		= 0;			// StepMotor debug log flag
StepMotor*      StepMotor::m_pStepMotor             = NULL;         // single copy StepMotor
int             StepMotor::m_nResolution        = 10;           // StepMotor resolution
int             StepMotor::m_nMotionMode        = 0;            // motor motion mode
int             StepMotor::m_nUm2UStep          = 50;           // unit to convert um to uStep
int             StepMotor::m_nUStep2Nm          = 40;           // unit to convert uStep to nm
int             StepMotor::m_nTimeoutInterval   = 10000;        // timeout interval
int             StepMotor::m_nTimeoutTrys       = 5;            // timeout trys
long            StepMotor::m_lVelocity          = 1000;         // velocity
double          StepMotor::m_dPositionX         = 0.00;         // X Position
double          StepMotor::m_dPositionY         = 0.00;         // Y Position
double          StepMotor::m_dPositionZ         = 0.00;         // Z Position
//int           StepMotor::m_nNumberOfAxes      = 3;            // number of axes attached to the controller, initial set to zero
std::string StepMotor::m_sPort = "COM1";                                 // serial port symbols

StepMotor::StepMotor()
{
    StepMotor::m_sMPStr[StepMotor::SMSTR_CtrlDevName]       = "StepMotor Controller";					// StepMotor Controllet device name
    StepMotor::m_sMPStr[StepMotor::SMSTR_XYStgaeDevName]    = "StepMotor XY Stage";						// StepMotor XY Stage device name
    StepMotor::m_sMPStr[StepMotor::SMSTR_ZStageDevName]     = "StepMotor Z Stage";						// MP286 Z Stage device name
    StepMotor::m_sMPStr[StepMotor::SMSTR_StepMotorVersion]      = "2.05.056";							// StepMotor adpater version number
    StepMotor::m_sMPStr[StepMotor::SMSTR_LogFilename]       = "StepMotorLog.txt";						// StepMotor Logfile name
	StepMotor::m_sMPStr[StepMotor::SMSTR_CtrlDevNameLabel]  = "M.00 Controller ";					// StepMotor Controller device name label
	StepMotor::m_sMPStr[StepMotor::SMSTR_CtrlDevDescLabel]  = "M.01 Controller ";					// StepMotor Controller device description label
    StepMotor::m_sMPStr[StepMotor::SMSTR_FirmwareVerLabel]  = "M.02 Firmware Version";				// StepMotor FIRMWARE VERSION label
    StepMotor::m_sMPStr[StepMotor::SMSTR_StepMotorVerLabel]     = "M.03 StepMotor Adapter Version";			// StepMotor ADAPTER VERSION label
	StepMotor::m_sMPStr[StepMotor::SMSTR_DebugLogFlagLabel] = "M.04 Debug Log Flag";				// StepMotor Debug Lg Flag Label
    StepMotor::m_sMPStr[StepMotor::SMSTR_CommStateLabel]    = "M.05 StepMotor Comm. Status";			// StepMotor COMM. STATUS label
    StepMotor::m_sMPStr[StepMotor::SMSTR_ResolutionLabel]   = "M.06 Resolution (10 or 50)";			// StepMotor RESOLUION label
    StepMotor::m_sMPStr[StepMotor::SMSTR_AccelLabel]        = "M.07 Acceleration";					// StepMotor ACCELERATION label
    StepMotor::m_sMPStr[StepMotor::SMSTR_Um2UStepUnit]      = "M.08 um to uStep";					// StepMotor um to ustep label
    StepMotor::m_sMPStr[StepMotor::SMSTR_Um2UStepLabel]      = "Um2Step(pluse/um)";					// StepMotor um to ustep label
    StepMotor::m_sMPStr[StepMotor::SMSTR_UStep2NmUnit]      = "M.09 uStep to nm";					// StepMotor ustep to nm label
    StepMotor::m_sMPStr[StepMotor::SMSTR_VelocityLabel]     = "M.10 Velocity (uStep/s)";			// StepMotor VELOCITY label
    StepMotor::m_sMPStr[StepMotor::SMSTR_MotionMode]        = "M.11 Mode (0=ABS/1=REL)";			// StepMotor MODE label
    StepMotor::m_sMPStr[StepMotor::SMSTR_SetOrigin]         = "M.12 Origin (1=set)";                // StepMotor ORIGIN label
    StepMotor::m_sMPStr[StepMotor::SMSTR_TimeoutInterval]   = "M.13 Timeout Interval (ms)";         // StepMotor Timeout Interval
    StepMotor::m_sMPStr[StepMotor::SMSTR_TimeoutTrys]       = "M.14 Timeout Trys";                  // StepMotor Timeout Trys
	StepMotor::m_sMPStr[StepMotor::SMSTR_XYDevNameLabel]    = "M.15 XY Stage ";						// StepMotor XY stage device name label
	StepMotor::m_sMPStr[StepMotor::SMSTR_XYDevDescLabel]    = "M.16 XY Stage ";						// StepMotor XY stage device description label
    StepMotor::m_sMPStr[StepMotor::SMSTR_SetPositionX]      = "M.17 Set Position X (um)";			// StepMotor set POSITION X label
    StepMotor::m_sMPStr[StepMotor::SMSTR_SetPositionY]      = "M.18 Set Position Y (um)";			// StepMotor set POSITION Y label
    StepMotor::m_sMPStr[StepMotor::SMSTR_GetPositionX]      = "M.19 Get Position X (uStep)";	    // StepMotor get POSITION X label
    StepMotor::m_sMPStr[StepMotor::SMSTR_GetPositionY]      = "M.20 Get Position Y (uStep)";		// StepMotor get POSITION Y label
	StepMotor::m_sMPStr[StepMotor::SMSTR_ZDevNameLabel]     = "M.21 Z Stage ";						// StepMotor Z stage device name label
	StepMotor::m_sMPStr[StepMotor::SMSTR_ZDevDescLabel]     = "M.22 Z Stage ";						// StepMotor Z stage device description label
    StepMotor::m_sMPStr[StepMotor::SMSTR_SetPositionZ]      = "M.23 Set Position Z (um)";			// StepMotor set POSITION Z label
    StepMotor::m_sMPStr[StepMotor::SMSTR_GetPositionZ]      = "M.24 Get Position Z (uStep)";		// StepMotor get POSITION Z label
    StepMotor::m_sMPStr[StepMotor::SMSTR_PauseMode]         = "M.25 Pause (0=continue/1=pause)";    // property PAUSE label
    StepMotor::m_sMPStr[StepMotor::SMSTR_Reset]             = "M.26 Reset (1=reset)";               // property RESET label
    StepMotor::m_sMPStr[StepMotor::SMSTR_Status]            = "M.27 Status (1=update)";             // property STATUS label
    StepMotor::m_sMPStr[StepMotor::SMSTR_StageMirror]            = "StageMirrorZ";             // property STATUS label
    StepMotor::m_sMPStr[StepMotor::SMSTR_CommLabel]            = "Comm";             // property STATUS label
}

StepMotor::~StepMotor()
{
    if (m_pStepMotor) delete m_pStepMotor;
    m_yInstanceFlag = false;
}

StepMotor* StepMotor::Instance()
{
    if(!m_yInstanceFlag)
    {
        m_pStepMotor = new StepMotor();
        m_yInstanceFlag = true;
    }

    return m_pStepMotor;
}

//
// Get StepMotor constant string
//
std::string StepMotor::GetMPStr(int nMPStrCode) const
{ 
   string sText;        // StepMotor String

   if (m_pStepMotor != NULL)
   {
       map<int, string>::const_iterator nIterator;
       nIterator = m_sMPStr.find(nMPStrCode);   
       if (nIterator != m_sMPStr.end())
          sText = nIterator->second;
   }

   return sText;
}

//
// Copy byte data buffer for iLength
//
int StepMotor::ByteCopy(unsigned char* bDst, const unsigned char* bSrc, int nLength)
{
    int nBytes = 0;
    if (bSrc == NULL || bDst == NULL) return(nBytes);
    for (nBytes = 0; nBytes < nLength; nBytes++) bDst[nBytes] = bSrc[nBytes];
    return nBytes;
}

//
// Convert byte data to hex string
//
void StepMotor::Byte2Hex(const unsigned char bByte, char* sHex)
{
    char sHexDigit[16] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    sHex[2] =  NULL;
    sHex[1] = sHexDigit[(int)(bByte & 0xF)];
    sHex[0] = sHexDigit[(int)(bByte / 0x10)];
    return;
}

