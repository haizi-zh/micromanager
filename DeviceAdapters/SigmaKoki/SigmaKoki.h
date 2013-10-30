///////////////////////////////////////////////////////////////////////////////
// FILE:          SigmaKoki.h
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   SigmaKoki Micromanipulator Controller
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

#ifndef _SigmaKoki_H_
#define _SigmaKoki_H_

#include <string>
#include <map>	

// Global function to reset the serial port
int ClearPort(MM::Device& device, MM::Core& core, const char* port);

//
// class SigmaKoki define all global varaiable in singleton class
// it can only be accessed via Instance().
//
class SigmaKoki
{
public:
    ~SigmaKoki();   // Destructor

    typedef int SKStr;
    enum _SKStr
    {
        SKSTR_CtrlDevName       = 0,            // SigmaKoki controller device name
        SKSTR_XYStgaeDevName    = 1,            // SigmaKoki XY stage device name
        SKSTR_ZStageDevName     = 2,            // SigmaKoki Z stage device name
        SKSTR_SigmaKokiVersion      = 3,            // SigmaKoki adapter version
        SKSTR_LogFilename       = 4,            // SigmaKoki log filename
        SKSTR_CtrlDevNameLabel  = 5,            // SigmaKoki controller device name label
		SKSTR_CtrlDevDescLabel  = 6,			// SigmaKoki controller device decription label
        SKSTR_FirmwareVerLabel  = 7,            // SigmaKoki FIRMWARE VERSION label
        SKSTR_SigmaKokiVerLabel     = 8,            // SigmaKoki Adapter version label
        SKSTR_DebugLogFlagLabel = 9,            // SigmaKoki Debug Log Flag label
        SKSTR_CommStateLabel    = 10,           // SigmaKoki COMM. STATUS label
        SKSTR_ResolutionLabel   = 11,           // SigmaKoki RESOLUION label
        SKSTR_AccelLabel        = 12,           // SigmaKoki ACCELERATION label
        SKSTR_Um2UStepUnit      = 13,           // SigmaKoki um to ustep label
        SKSTR_UStep2NmUnit      = 14,           // SigmaKoki ustep to nm label
        SKSTR_VelocityLabel     = 15,           // SigmaKoki VELOCITY label
        SKSTR_MotionMode        = 16,           // SigmaKoki MODE label
        SKSTR_SetOrigin         = 17,           // SigmaKoki ORIGIN label
        SKSTR_TimeoutInterval   = 18,           // SigmaKoki Timeout Interval label
        SKSTR_TimeoutTrys       = 19,           // SigmaKoki Timeout Trys label
        SKSTR_XYDevNameLabel    = 20,           // SigmaKoki controller device name label
		SKSTR_XYDevDescLabel    = 21,			// SigmaKoki controller device decription label
        SKSTR_SetPositionX      = 22,           // SigmaKoki POSITION X label
        SKSTR_SetPositionY      = 23,           // SigmaKoki POSITION Y label
        SKSTR_GetPositionX      = 24,           // SigmaKoki CURRENT POSITION X label
        SKSTR_GetPositionY      = 25,           // SigmaKoki CURRENT POSITION Y label
        SKSTR_ZDevNameLabel     = 26,           // SigmaKoki controller device name label
		SKSTR_ZDevDescLabel     = 27,			// SigmaKoki controller device decription label
        SKSTR_SetPositionZ      = 28,           // SigmaKoki POSITION Z label
        SKSTR_GetPositionZ      = 29,           // SigmaKoki CURRENT POSITION Z label
        SKSTR_PauseMode         = 30,           // property PAUSE label
        SKSTR_Reset             = 31,           // property RESET label
        SKSTR_Status            = 32            // property STATUS label
    };

    enum
    {
        SigmaKoki_TxTerm            = 0x0D,         // EOL transmit symbole
        SigmaKoki_RxTerm            = 0x0D,         // EOL receiving symbol
                                                //////////////////////////////////////
                                                // Serial CommunicationError codes
                                                //////////////////////////////////////
        SigmaKoki_SP_OVER_RUN		= 0x30,			// The previous character was not unloaded before the latest was received
        SigmaKoki_FRAME_ERROR		= 0x31,			// The vald stop bits was not received during the appropriate time period
        SigmaKoki_BUFFER_OVER_RUN	= 0x32,			// The input buffer is filled and CR has not been received
        SigmaKoki_BAD_COMMAND		= 0x34,			// Input cannot be interpreted -- command byte not valid
        SigmaKoki_MOVE_INTERRUPTED	= 0x38			// A requested move was interrupted by input of serial port.  This code is
                                                // ORed with any other error code.  The value normally returned is "*",
                                                // i.e., 8 ORed with 4. "4" is reported on the vacuum fluorescent display
    };

    static SigmaKoki* Instance();                                                               // only interface for singleton
    std::string GetSKStr(int nSKStrCode) const;                                             // access prdefined strings
    static void SetDeviceAvailable(bool yFlag) { m_yDeviceAvailable = yFlag; }              // set SigmaKoki device availability
    static bool GetDeviceAvailability() { return m_yDeviceAvailable; }                      // get SigmaKoki device availability
	static int  GetDebugLogFlag() { return m_nDebugLogFlag; }								// get SigmaKoki debug log flag
	static void SetDebugLogFlag(int nDebugLogFlag) { m_nDebugLogFlag = nDebugLogFlag; }		// set SigmaKoki debug log flag
    static void SetVelocity(long lVelocity) { m_lVelocity = lVelocity; }                    // set SigmaKoki device velocity
    static long GetVelocity() { return m_lVelocity; }                                       // get SigmaKoki device velocity
    static void SetResolution(int nResolution) { m_nResolution = nResolution; }             // set SigmaKoki device resolution
    static int  GetResolution() { return m_nResolution; }                                   // get SigmaKoki resolution
    static void SetUm2UStep(int nUm2UStep) { m_nUm2UStep = nUm2UStep; }                     // set SigmaKoki Um to UStep conversion unit
    static int  GetUm2UStep() { return m_nUm2UStep; }                                       // get SigmaKoki Um to UStep conversion unit
    static void SetUStep2Nm(int nUStep2Nm) { m_nUStep2Nm = nUStep2Nm; }                     // set SigmaKoki UStep to Nm conversion unit
    static int  GetUStep2Nm() { return m_nUStep2Nm; }                                       // get SigmaKoki UStep to NM conversion unit
    static void SetMotionMode(int nMotionMode) { m_nMotionMode = nMotionMode; }             // set Motor motion mode
    static int  GetMotionMode() { return m_nMotionMode; }                                   // get Motor motion mode
    static void SetTimeoutInterval(int nInterval) { m_nTimeoutInterval = nInterval; }       // set Timwout Interval
    static int  GetTimeoutInterval() { return m_nTimeoutInterval; }                         // get Timeout Interval
    static void SetTimeoutTrys(int nTrys) { m_nTimeoutTrys = nTrys; }                       // set Timeout Trys
    static int  GetTimeoutTrys() { return m_nTimeoutTrys; }                                 // get Timeout Trys
    //static void SetNumberOfAxes(int nNumberOfAxes) { m_nNumberOfAxes = nNumberOfAxes; }   // set number of axes controlled by SigmaKoki
    //static int  GetNumberOfAxes() { return m_nNumberOfAxes; }                             // get numebr of axes controlled by SigmaKoki
    static void SetPositionX(double dPosition) { m_dPositionX = dPosition; }                // set position x
    static double GetPositionX() { return m_dPositionX; }                                   // get position x
    static void SetPositionY(double dPosition) { m_dPositionY = dPosition; }                // set position y
    static double GetPositionY() { return m_dPositionY; }                                   // get position y
    static void SetPositionZ(double dPosition) { m_dPositionZ = dPosition; }                // set position z
    static double GetPositionZ() { return m_dPositionZ; }                                   // get position z
    static std::string& GetSerialPort() { return m_sPort; }                                 // get serial port symbol
    static void SetSerialPort(std::string sPort) { m_sPort = sPort; }                       // set serial port symbol
    static int ByteCopy(unsigned char* bDst, const unsigned char* bSrc, int nLength);       // copy byte buffer for iLength
    static void Byte2Hex(const unsigned char bByte, char* sHex);                            // convert byte number to hex

protected:
    SigmaKoki();    // Constructor

private:
    static bool                 m_yInstanceFlag;            // singleton flag
    static SigmaKoki*               m_pSigmaKoki;                   // singleton copy
    static bool                 m_yDeviceAvailable;         // SigmaKoki availability
	static int					m_nDebugLogFlag;			// SigmaKoki debug log flag
    static int                  m_nResolution;              // SigmaKoki resolution
    static int                  m_nUm2UStep;                // unit to convert um to uStep
    static int                  m_nUStep2Nm;                // unit to convert uStep to nm
    static int                  m_nTimeoutInterval;         // timeout interval
    static int                  m_nTimeoutTrys;             // timeout trys
    //static int                m_nNumberOfAxes;            // number of SigmaKoki axes
    static int                  m_nMotionMode;              // motor motion mode
    static double               m_dPositionX;               // position X
    static double               m_dPositionY;               // position Y
    static double               m_dPositionZ;               // position Z
    static long                 m_lVelocity;                // SigmaKoki velocity
    static std::string          m_sPort;                    // serial port symbols
    std::map<int, std::string>  m_sSKStr;                   // constant strings
};

#endif  //_SigmaKoki_H_