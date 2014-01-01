//////////////////////////////////////////////////////////////////////////////
// FILE:          StepMotor.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   StepMotors Controller Driver


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
#include "StepMotorCtrl.h"
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

	// initialize the controller device name
	AddAvailableDeviceName( StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_CtrlDevName).c_str(),  StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_CtrlDevName).c_str());

	// initialize the Z stage device name
	AddAvailableDeviceName(StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_ZStageDevName).c_str(), StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_ZStageDevName).c_str());
}

//
// Creating the MMDevice
//
MODULE_API MM::Device* CreateDevice(const char* sDeviceName)
{
    // checking for null pinter
    if (sDeviceName == 0) return 0;

    if (strcmp(sDeviceName, StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_CtrlDevName).c_str()) == 0) 
    {
        // if device name is StepMotor Controller, create the StepMotor device
        StepMotorCtrl*  pCtrlDev = new StepMotorCtrl();
        return pCtrlDev;
    }

    if (strcmp(sDeviceName, StepMotor::Instance()->GetXMTStr(StepMotor::XMTSTR_ZStageDevName).c_str()) == 0)
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

bool            StepMotor::m_yInstanceFlag      = false;        // instance flag
bool            StepMotor::m_yDeviceAvailable   = false;        // StepMotor devices availability
int				StepMotor::m_nDebugLogFlag		= 0;			// StepMotor debug log flag
StepMotor*        StepMotor::m_pStepMotor             = NULL;         // single copy StepMotor
int             StepMotor::m_nMotionMode        = 0;            // motor motion mode
int             StepMotor::m_nTimeoutInterval   = 2000;        // timeout interval
int             StepMotor::m_nTimeoutTrys       = 5;            // timeout trys
double          StepMotor::m_dPositionZ         = 0.00;         // Z Position
std::string StepMotor::m_sPort;                                 // serial port symbols

StepMotor::StepMotor()
{
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_CtrlDevName]       = "StepMotor Controller";					// StepMotor Controllet device name
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_ZStageDevName]     = "StepMotor Z Stage";						// MP286 Z Stage device name
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_StepMotorVersion]      = "1.0.0";							// StepMotor adpater version number
	StepMotor::m_sXMTStr[StepMotor::XMTSTR_CtrlDevNameLabel]  = "Controller ";					// StepMotor Controller device name label
	StepMotor::m_sXMTStr[StepMotor::XMTSTR_CtrlDevDescLabel]  = "Controller ";					// StepMotor Controller device description label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_FirmwareVerLabel]  = "Firmware Version";				// StepMotor FIRMWARE VERSION label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_StepMotorVerLabel]   = "StepMotor Adapter Version";			// StepMotor ADAPTER VERSION label
	StepMotor::m_sXMTStr[StepMotor::XMTSTR_DebugLogFlagLabel] = "Debug Log Flag";				// StepMotor Debug Lg Flag Label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_CommStateLabel]    = "StepMotor Comm. Status";			// StepMotor COMM. STATUS label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_MotionMode]        = "Mode (0=FAST/1=LOW)";			// StepMotor MODE label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_SetOrigin]         = "Origin (1=set)";                // StepMotor ORIGIN label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_TimeoutInterval]   = "Timeout Interval (ms)";         // StepMotor Timeout Interval
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_TimeoutTrys]       = "Timeout Trys";                  // StepMotor Timeout Trys
	StepMotor::m_sXMTStr[StepMotor::XMTSTR_ZDevNameLabel]     = "Z Stage ";						// StepMotor Z stage device name label
	StepMotor::m_sXMTStr[StepMotor::XMTSTR_ZDevDescLabel]     = "Z Stage ";						// StepMotor Z stage device description label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_SetPositionZ]      = "Set Position Z (um)";			// StepMotor set POSITION Z label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_GetPositionZ]      = "Get Position Z (um)";		// StepMotor get POSITION Z label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_Reset]             = "M.26 Reset (1=reset)";               // property RESET label
    StepMotor::m_sXMTStr[StepMotor::XMTSTR_Status]            = "M.27 Status (1=update)";             // property STATUS label
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
std::string StepMotor::GetXMTStr(int nXMTStrCode) const
{ 
   string sText;        // StepMotor String

   if (m_pStepMotor != NULL)
   {
       map<int, string>::const_iterator nIterator;
       nIterator = m_sXMTStr.find(nXMTStrCode);   
       if (nIterator != m_sXMTStr.end())
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
//
//Package command string to XMT format
//
void StepMotor::PackageCommand(const byte cmd,byte* data,byte* buf)
{
	//CMD
	buf[0] = '@';
	buf[1] =  cmd;
	//data
	if (data == NULL){
		buf[2] = 'X';
		buf[3] = 'X';
		buf[4] = 'X';
		buf[5] = 'X';
	}
	else{
		buf[2] = data[0];
		buf[3] = data[1];
		buf[4] = data[2];
		buf[5] = data[3];
	}
	//checkSum
	buf[6] = checkSumCalc(buf, 0, 6);
}
//
//parse raw data to readable format
//
float  StepMotor::RawToFloat(byte* rawData,int offset)
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
//
//Convert target position to XMT format
void StepMotor::FloatToRaw(float val,byte* rawData)
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
//
//checksum generator
//
byte  StepMotor::checkSumCalc(byte* data,int offset,int count)
{
	byte checksum = data[offset];
	for (int i = offset + 1; i < offset + count; ++i)
	{
		checksum = (byte)(checksum ^ data[i]);
	}
	return checksum;

}
