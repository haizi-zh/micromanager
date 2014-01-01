///////////////////////////////////////////////////////////////////////////////
// FILE:          XMTE517.h
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   XMTE517 Micromanipulator Controller
//                XY Stage
//                Z  Stage
//
// COPYRIGHT:     Sutter Instrument,
//                Mission Bay Imaging, San Francisco, 2011
//
// LICENSE:       This file is distributed under the BSD license.
//                License text is included with the source distribution.
//
//                This file is distributed in the hope that it will be useful,
//                but WITHOUT ANY WARRANTY; without even the implied warranty
//                of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//                IN NO EVENT SHALL THE COPYRIGHT OWNER(S) OR
//                CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//                INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
// AUTHOR:        Lon Chu (lonchu@yahoo.com) created on March 2011
//                Lon Chu (lonchu@yahoo.com) modified on 
//

#ifndef _XMTE517_H_
#define _XMTE517_H_

#include <string>
#include <map>	



// Global function to reset the serial port
int ClearPort(MM::Device& device, MM::Core& core, const char* port);

//
// class XMTE517 define all global varaiable in singleton class
// it can only be accessed via Instance().
//
class XMTE517
{
public:
    ~XMTE517();   // Destructor

    typedef int XMTStr;
    enum _XMTStr
    {
        XMTSTR_CtrlDevName       = 0,            // XMTE517 controller device name
        XMTSTR_XYStgaeDevName    = 1,            // XMTE517 XY stage device name
        XMTSTR_ZStageDevName     = 2,            // XMTE517 Z stage device name
        XMTSTR_XMTE517Version      = 3,            // XMTE517 adapter version
        XMTSTR_LogFilename       = 4,            // XMTE517 log filename
        XMTSTR_CtrlDevNameLabel  = 5,            // XMTE517 controller device name label
		XMTSTR_CtrlDevDescLabel  = 6,			// XMTE517 controller device decription label
        XMTSTR_FirmwareVerLabel  = 7,            // XMTE517 FIRMWARE VERSION label
        XMTSTR_XMTE517VerLabel     = 8,            // XMTE517 Adapter version label
        XMTSTR_DebugLogFlagLabel = 9,            // XMTE517 Debug Log Flag label
        XMTSTR_CommStateLabel    = 10,           // XMTE517 COMM. STATUS label
        XMTSTR_ResolutionLabel   = 11,           // XMTE517 RESOLUION label
        XMTSTR_AccelLabel        = 12,           // XMTE517 ACCELERATION label
        XMTSTR_Um2UStepUnit      = 13,           // XMTE517 um to ustep label
        XMTSTR_UStep2NmUnit      = 14,           // XMTE517 ustep to nm label
        XMTSTR_VelocityLabel     = 15,           // XMTE517 VELOCITY label
        XMTSTR_MotionMode        = 16,           // XMTE517 MODE label
        XMTSTR_SetOrigin         = 17,           // XMTE517 ORIGIN label
        XMTSTR_TimeoutInterval   = 18,           // XMTE517 Timeout Interval label
        XMTSTR_TimeoutTrys       = 19,           // XMTE517 Timeout Trys label
        XMTSTR_XYDevNameLabel    = 20,           // XMTE517 controller device name label
		XMTSTR_XYDevDescLabel    = 21,			// XMTE517 controller device decription label
        XMTSTR_SetPositionX      = 22,           // XMTE517 POSITION X label
        XMTSTR_SetPositionY      = 23,           // XMTE517 POSITION Y label
        XMTSTR_GetPositionX      = 24,           // XMTE517 CURRENT POSITION X label
        XMTSTR_GetPositionY      = 25,           // XMTE517 CURRENT POSITION Y label
        XMTSTR_ZDevNameLabel     = 26,           // XMTE517 controller device name label
		XMTSTR_ZDevDescLabel     = 27,			// XMTE517 controller device decription label
        XMTSTR_SetPositionZ      = 28,           // XMTE517 POSITION Z label
        XMTSTR_GetPositionZ      = 29,           // XMTE517 CURRENT POSITION Z label
        XMTSTR_PauseMode         = 30,           // property PAUSE label
        XMTSTR_Reset             = 31,           // property RESET label
        XMTSTR_Status            = 32            // property STATUS label
    };


    enum
    {
        XMTE517_TxTerm            = 0x0D,         // EOL transmit symbole
        XMTE517_RxTerm            = 0x0D,         // EOL receiving symbol
                                                //////////////////////////////////////
                                                // Serial CommunicationError codes
                                                //////////////////////////////////////
        XMTE517_SP_OVER_RUN		= 0x30,			// The previous character was not unloaded before the latest was received
        XMTE517_FRAME_ERROR		= 0x31,			// The vald stop bits was not received during the appropriate time period
        XMTE517_BUFFER_OVER_RUN	= 0x32,			// The input buffer is filled and CR has not been received
        XMTE517_BAD_COMMAND		= 0x34,			// Input cannot be interpreted -- command byte not valid
        XMTE517_MOVE_INTERRUPTED	= 0x38			// A requested move was interrupted by input of serial port.  This code is
                                                // ORed with any other error code.  The value normally returned is "*",
                                                // i.e., 8 ORed with 4. "4" is reported on the vacuum fluorescent display
    };

    static XMTE517* Instance();                                                               // only interface for singleton
    std::string GetXMTStr(int nXMTStrCode) const;                                             // access prdefined strings
    static void SetDeviceAvailable(bool yFlag) { m_yDeviceAvailable = yFlag; }              // set XMTE517 device availability
    static bool GetDeviceAvailability() { return m_yDeviceAvailable; }                      // get XMTE517 device availability
	static int  GetDebugLogFlag() { return m_nDebugLogFlag; }								// get XMTE517 debug log flag
	static void SetDebugLogFlag(int nDebugLogFlag) { m_nDebugLogFlag = nDebugLogFlag; }		// set XMTE517 debug log flag
    static void SetMotionMode(int nMotionMode) { m_nMotionMode = nMotionMode; }             // set Motor motion mode
    static int  GetMotionMode() { return m_nMotionMode; }                                   // get Motor motion mode
    static void SetTimeoutInterval(int nInterval) { m_nTimeoutInterval = nInterval; }       // set Timwout Interval
    static int  GetTimeoutInterval() { return m_nTimeoutInterval; }                         // get Timeout Interval
    static void SetTimeoutTrys(int nTrys) { m_nTimeoutTrys = nTrys; }                       // set Timeout Trys
    static int  GetTimeoutTrys() { return m_nTimeoutTrys; }                                 // get Timeout Trys
    static void SetPositionZ(double dPosition) { m_dPositionZ = dPosition; }                // set position z
    static double GetPositionZ() { return m_dPositionZ; }                                   // get position z
    static std::string& GetSerialPort() { return m_sPort; }                                 // get serial port symbol
    static void SetSerialPort(std::string sPort) { m_sPort = sPort; }                       // set serial port symbol
    static int ByteCopy(unsigned char* bDst, const unsigned char* bSrc, int nLength);       // copy byte buffer for iLength
    static void Byte2Hex(const unsigned char bByte, char* sHex);                            // convert byte number to hex
    static float RawToFloat(byte* rawData,int offset);
    static void  FloatToRaw(float value,byte* rawData);
    static void PackageCommand(const char cmd,byte* data,byte * buf);
    static byte checkSumCalc(byte* data,int offset,int count);
protected:
    XMTE517();    // Constructor

private:
    static bool                 m_yInstanceFlag;            // singleton flag
    static XMTE517*               m_pXMTE517;                   // singleton copy
    static bool                 m_yDeviceAvailable;         // XMTE517 availability
	static int					m_nDebugLogFlag;			// XMTE517 debug log flag
    static int                  m_nResolution;              // XMTE517 resolution
    static int                  m_nUm2UStep;                // unit to convert um to uStep
    static int                  m_nUStep2Nm;                // unit to convert uStep to nm
    static int                  m_nTimeoutInterval;         // timeout interval
    static int                  m_nTimeoutTrys;             // timeout trys
    //static int                m_nNumberOfAxes;            // number of XMTE517 axes
    static int                  m_nMotionMode;              // motor motion mode
    static double               m_dPositionX;               // position X
    static double               m_dPositionY;               // position Y
    static double               m_dPositionZ;               // position Z
    static long                 m_lVelocity;                // XMTE517 velocity
    static std::string          m_sPort;                    // serial port symbols
    std::map<int, std::string>  m_sXMTStr;                   // constant strings
};

#endif  //_XMTE517_H_
