//////////////////////////////////////////////////////////////////////////////
// FILE:          XMTE517.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   XMTE517s Controller Driver
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
#include "XMTE517Error.h"
#include "XMTE517Ctrl.h"
#include "XMTE517ZStage.h"

using namespace std;

XMTE517* g_pXMTE517;
MPError* g_pMPError;

///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////

//
// Initialize the MMDevice name
//
MODULE_API void InitializeModuleData()
{
    g_pXMTE517 = XMTE517::Instance();       // Initiate the XMTE517 instance
    g_pMPError = MPError::Instance();   // Initiate the MPError instance

	// initialize the controller device name
	AddAvailableDeviceName( XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevName).c_str(),  XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevName).c_str());

	// initialize the Z stage device name
	AddAvailableDeviceName(XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZStageDevName).c_str(), XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZStageDevName).c_str());
}

//
// Creating the MMDevice
//
MODULE_API MM::Device* CreateDevice(const char* sDeviceName)
{
    // checking for null pinter
    if (sDeviceName == 0) return 0;

    if (strcmp(sDeviceName, XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_CtrlDevName).c_str()) == 0) 
    {
        // if device name is XMTE517 Controller, create the XMTE517 device
        XMTE517Ctrl*  pCtrlDev = new XMTE517Ctrl();
        return pCtrlDev;
    }

    if (strcmp(sDeviceName, XMTE517::Instance()->GetMPStr(XMTE517::XMTSTR_ZStageDevName).c_str()) == 0)
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
        sMessage << "<XMTE517::ClearPort> port = (" <<  sPort << ") :: clearBuffer(" << lRead << ")  = (" << sClear << ")";
        core.LogMessage(&device, sMessage.str().c_str(), false);

        // verify the read operation
        if (ret != DEVICE_OK) return ret;                                                           
    } 

    // upon successful restting the port
    return DEVICE_OK;                                                           
} 

bool            XMTE517::m_yInstanceFlag      = false;        // instance flag
bool            XMTE517::m_yDeviceAvailable   = false;        // XMTE517 devices availability
int				XMTE517::m_nDebugLogFlag		= 0;			// XMTE517 debug log flag
XMTE517*          XMTE517::m_pXMTE517             = NULL;         // single copy XMTE517
int             XMTE517::m_nResolution        = 10;           // XMTE517 resolution
int             XMTE517::m_nMotionMode        = 0;            // motor motion mode
int             XMTE517::m_nUm2UStep          = 25;           // unit to convert um to uStep
int             XMTE517::m_nUStep2Nm          = 40;           // unit to convert uStep to nm
int             XMTE517::m_nTimeoutInterval   = 10000;        // timeout interval
int             XMTE517::m_nTimeoutTrys       = 5;            // timeout trys
long            XMTE517::m_lVelocity          = 18000;         // velocity
double          XMTE517::m_dPositionX         = 0.00;         // X Position
double          XMTE517::m_dPositionY         = 0.00;         // Y Position
double          XMTE517::m_dPositionZ         = 0.00;         // Z Position
//int           XMTE517::m_nNumberOfAxes      = 3;            // number of axes attached to the controller, initial set to zero
std::string XMTE517::m_sPort;                                 // serial port symbols

XMTE517::XMTE517()
{
    XMTE517::m_sMPStr[XMTE517::XMTSTR_CtrlDevName]       = "XMTE517 Controller";					// XMTE517 Controllet device name
    XMTE517::m_sMPStr[XMTE517::XMTSTR_XYStgaeDevName]    = "XMTE517 XY Stage";						// XMTE517 XY Stage device name
    XMTE517::m_sMPStr[XMTE517::XMTSTR_ZStageDevName]     = "XMTE517 Z Stage";						// MP286 Z Stage device name
    XMTE517::m_sMPStr[XMTE517::XMTSTR_XMTE517Version]      = "2.05.056";							// XMTE517 adpater version number
    XMTE517::m_sMPStr[XMTE517::XMTSTR_LogFilename]       = "XMTE517Log.txt";						// XMTE517 Logfile name
	XMTE517::m_sMPStr[XMTE517::XMTSTR_CtrlDevNameLabel]  = "M.00 Controller ";					// XMTE517 Controller device name label
	XMTE517::m_sMPStr[XMTE517::XMTSTR_CtrlDevDescLabel]  = "M.01 Controller ";					// XMTE517 Controller device description label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_FirmwareVerLabel]  = "M.02 Firmware Version";				// XMTE517 FIRMWARE VERSION label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_XMTE517VerLabel]     = "M.03 XMTE517 Adapter Version";			// XMTE517 ADAPTER VERSION label
	XMTE517::m_sMPStr[XMTE517::XMTSTR_DebugLogFlagLabel] = "M.04 Debug Log Flag";				// XMTE517 Debug Lg Flag Label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_CommStateLabel]    = "M.05 XMTE517 Comm. Status";			// XMTE517 COMM. STATUS label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_ResolutionLabel]   = "M.06 Resolution (10 or 50)";			// XMTE517 RESOLUION label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_AccelLabel]        = "M.07 Acceleration";					// XMTE517 ACCELERATION label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_Um2UStepUnit]      = "M.08 um to uStep";					// XMTE517 um to ustep label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_UStep2NmUnit]      = "M.09 uStep to nm";					// XMTE517 ustep to nm label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_VelocityLabel]     = "M.10 Velocity (uStep/s)";			// XMTE517 VELOCITY label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_MotionMode]        = "M.11 Mode (0=ABS/1=REL)";			// XMTE517 MODE label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_SetOrigin]         = "M.12 Origin (1=set)";                // XMTE517 ORIGIN label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_TimeoutInterval]   = "M.13 Timeout Interval (ms)";         // XMTE517 Timeout Interval
    XMTE517::m_sMPStr[XMTE517::XMTSTR_TimeoutTrys]       = "M.14 Timeout Trys";                  // XMTE517 Timeout Trys
	XMTE517::m_sMPStr[XMTE517::XMTSTR_XYDevNameLabel]    = "M.15 XY Stage ";						// XMTE517 XY stage device name label
	XMTE517::m_sMPStr[XMTE517::XMTSTR_XYDevDescLabel]    = "M.16 XY Stage ";						// XMTE517 XY stage device description label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_SetPositionX]      = "M.17 Set Position X (um)";			// XMTE517 set POSITION X label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_SetPositionY]      = "M.18 Set Position Y (um)";			// XMTE517 set POSITION Y label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_GetPositionX]      = "M.19 Get Position X (uStep)";	    // XMTE517 get POSITION X label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_GetPositionY]      = "M.20 Get Position Y (uStep)";		// XMTE517 get POSITION Y label
	XMTE517::m_sMPStr[XMTE517::XMTSTR_ZDevNameLabel]     = "M.21 Z Stage ";						// XMTE517 Z stage device name label
	XMTE517::m_sMPStr[XMTE517::XMTSTR_ZDevDescLabel]     = "M.22 Z Stage ";						// XMTE517 Z stage device description label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_SetPositionZ]      = "M.23 Set Position Z (um)";			// XMTE517 set POSITION Z label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_GetPositionZ]      = "M.24 Get Position Z (uStep)";		// XMTE517 get POSITION Z label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_PauseMode]         = "M.25 Pause (0=continue/1=pause)";    // property PAUSE label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_Reset]             = "M.26 Reset (1=reset)";               // property RESET label
    XMTE517::m_sMPStr[XMTE517::XMTSTR_Status]            = "M.27 Status (1=update)";             // property STATUS label
}

XMTE517::~XMTE517()
{
    if (m_pXMTE517) delete m_pXMTE517;
    m_yInstanceFlag = false;
}

XMTE517* XMTE517::Instance()
{
    if(!m_yInstanceFlag)
    {
        m_pXMTE517 = new XMTE517();
        m_yInstanceFlag = true;
    }

    return m_pXMTE517;
}

//
// Get XMTE517 constant string
//
std::string XMTE517::GetMPStr(int nMPStrCode) const
{ 
   string sText;        // XMTE517 String

   if (m_pXMTE517 != NULL)
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
int XMTE517::ByteCopy(unsigned char* bDst, const unsigned char* bSrc, int nLength)
{
    int nBytes = 0;
    if (bSrc == NULL || bDst == NULL) return(nBytes);
    for (nBytes = 0; nBytes < nLength; nBytes++) bDst[nBytes] = bSrc[nBytes];
    return nBytes;
}

//
// Convert byte data to hex string
//
void XMTE517::Byte2Hex(const unsigned char bByte, char* sHex)
{
    char sHexDigit[16] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    sHex[2] =  NULL;
    sHex[1] = sHexDigit[(int)(bByte & 0xF)];
    sHex[0] = sHexDigit[(int)(bByte / 0x10)];
    return;
}


void XMTE517::PackageCommand(const char* cmd,byte* data,byte* buf)
{
	//CMD
	buf[0] = '@';
	buf[1] = (byte)cmd[0];
	buf[2] = (byte)cmd[1];
	buf[3] = (byte)cmd[2];
	//data
	if (data == NULL){
		buf[4] = 'X';
		buf[5] = 'X';
		buf[6] = 'X';
		buf[7] = 'X';
	}
	else{
		buf[4] = data[0];
		buf[5] = data[1];
		buf[6] = data[2];
		buf[7] = data[3];
	}
	//checkSum
	buf[8] = checkSumCalc(buf, 0, 8);
}

float  XMTE517::RawToFloat(byte* rawData,int offset)
{
	if (rawData[offset + 0] >= 0x80)
	{
		// Negative number
		return -(float)((rawData[offset + 0] - 0x80) * 256 + rawData[offset + 1]
		                                                             + (rawData[offset + 2] * 256 + rawData[offset + 3]) * 0.001);
	}
	else
	{
		return (float)(rawData[offset + 0] * 256 + rawData[offset + 1]
		                                                   + (rawData[offset + 2] * 256 + rawData[offset + 3]) * 0.001);
	}
}
void XMTE517::FloatToRaw(float val,byte* rawData)
{
	if (val < 0)
	{
		val *= -1;
		int a = (int)val;
		rawData[0] = (byte)(a / 256 + 0x80);
		rawData[1] = (byte)(a % 256);
		a = (int)((val - a) * 1000);
		rawData[2] = (byte)(a / 256);
		rawData[3] = (byte)(a % 256);
	}
	else
	{
		int a = (int)val;
		byte temp = 0;
		temp = (byte)(a / 256);
		rawData[0] = temp;
		temp = (byte)(a % 256);
		rawData[1] =  temp;

		a = (int)((val - a) * 1000);
		temp = (byte)(a / 256);
		rawData[2] = temp;
		temp = (byte)(a % 256);
		rawData[3] = temp;
	}
}
byte  XMTE517::checkSumCalc(byte* data,int offset,int count)
{
	byte checksum = data[offset];
	for (int i = offset + 1; i < offset + count; ++i)
	{
		checksum = (byte)(checksum ^ data[i]);
	}
	return checksum;

}
