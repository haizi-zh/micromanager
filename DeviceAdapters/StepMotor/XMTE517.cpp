//////////////////////////////////////////////////////////////////////////////
// FILE:          XMTE517.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   XMTE517s Controller Driver


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
	AddAvailableDeviceName( XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_CtrlDevName).c_str(),  XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_CtrlDevName).c_str());

	// initialize the Z stage device name
	AddAvailableDeviceName(XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_ZStageDevName).c_str(), XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_ZStageDevName).c_str());
}

//
// Creating the MMDevice
//
MODULE_API MM::Device* CreateDevice(const char* sDeviceName)
{
    // checking for null pinter
    if (sDeviceName == 0) return 0;

    if (strcmp(sDeviceName, XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_CtrlDevName).c_str()) == 0) 
    {
        // if device name is XMTE517 Controller, create the XMTE517 device
        XMTE517Ctrl*  pCtrlDev = new XMTE517Ctrl();
        return pCtrlDev;
    }

    if (strcmp(sDeviceName, XMTE517::Instance()->GetXMTStr(XMTE517::XMTSTR_ZStageDevName).c_str()) == 0)
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
XMTE517*        XMTE517::m_pXMTE517             = NULL;         // single copy XMTE517
int             XMTE517::m_nMotionMode        = 0;            // motor motion mode
int             XMTE517::m_nTimeoutInterval   = 2000;        // timeout interval
int             XMTE517::m_nTimeoutTrys       = 5;            // timeout trys
double          XMTE517::m_dPositionZ         = 0.00;         // Z Position
std::string XMTE517::m_sPort;                                 // serial port symbols

XMTE517::XMTE517()
{
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_CtrlDevName]       = "XMTE517 Controller";					// XMTE517 Controllet device name
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_ZStageDevName]     = "XMTE517 Z Stage";						// MP286 Z Stage device name
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_XMTE517Version]      = "1.0.0";							// XMTE517 adpater version number
	XMTE517::m_sXMTStr[XMTE517::XMTSTR_CtrlDevNameLabel]  = "Controller ";					// XMTE517 Controller device name label
	XMTE517::m_sXMTStr[XMTE517::XMTSTR_CtrlDevDescLabel]  = "Controller ";					// XMTE517 Controller device description label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_FirmwareVerLabel]  = "Firmware Version";				// XMTE517 FIRMWARE VERSION label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_XMTE517VerLabel]   = "XMTE517 Adapter Version";			// XMTE517 ADAPTER VERSION label
	XMTE517::m_sXMTStr[XMTE517::XMTSTR_DebugLogFlagLabel] = "Debug Log Flag";				// XMTE517 Debug Lg Flag Label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_CommStateLabel]    = "XMTE517 Comm. Status";			// XMTE517 COMM. STATUS label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_MotionMode]        = "Mode (0=FAST/1=LOW)";			// XMTE517 MODE label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_SetOrigin]         = "Origin (1=set)";                // XMTE517 ORIGIN label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_TimeoutInterval]   = "Timeout Interval (ms)";         // XMTE517 Timeout Interval
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_TimeoutTrys]       = "Timeout Trys";                  // XMTE517 Timeout Trys
	XMTE517::m_sXMTStr[XMTE517::XMTSTR_ZDevNameLabel]     = "Z Stage ";						// XMTE517 Z stage device name label
	XMTE517::m_sXMTStr[XMTE517::XMTSTR_ZDevDescLabel]     = "Z Stage ";						// XMTE517 Z stage device description label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_SetPositionZ]      = "Set Position Z (um)";			// XMTE517 set POSITION Z label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_GetPositionZ]      = "Get Position Z (um)";		// XMTE517 get POSITION Z label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_Reset]             = "M.26 Reset (1=reset)";               // property RESET label
    XMTE517::m_sXMTStr[XMTE517::XMTSTR_Status]            = "M.27 Status (1=update)";             // property STATUS label
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
std::string XMTE517::GetXMTStr(int nXMTStrCode) const
{ 
   string sText;        // XMTE517 String

   if (m_pXMTE517 != NULL)
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
//
//Package command string to XMT format
//
void XMTE517::PackageCommand(const char cmd,byte* data,byte* buf)
{
	//CMD
	buf[0] = '@';
	buf[1] = (byte)cmd;
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
//
//Convert target position to XMT format
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
//
//checksum generator
//
byte  XMTE517::checkSumCalc(byte* data,int offset,int count)
{
	byte checksum = data[offset];
	for (int i = offset + 1; i < offset + count; ++i)
	{
		checksum = (byte)(checksum ^ data[i]);
	}
	return checksum;

}
