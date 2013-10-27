///////////////////////////////////////////////////////////////////////////////
// FILE:          StepMotor.h
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   StepMotor Micromanipulator Controller
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

#ifndef _StepMotor_H_
#define _StepMotor_H_

#include <string>
#include <map>	
#include <Windows.h>
#include <Winbase.h>
// Global function to reset the serial port
int ClearPort(MM::Device& device, MM::Core& core, const char* port);

//
// class StepMotor define all global varaiable in singleton class
// it can only be accessed via Instance().
//
class StepMotor
{
public:
    ~StepMotor();   // Destructor
    typedef int MPStr;
    enum _MPStr
    {
        SMSTR_CtrlDevName       = 0,            // StepMotor controller device name
        SMSTR_XYStgaeDevName    = 1,            // StepMotor XY stage device name
        SMSTR_ZStageDevName     = 2,            // StepMotor Z stage device name
        SMSTR_StepMotorVersion      = 3,            // StepMotor adapter version
        SMSTR_LogFilename       = 4,            // StepMotor log filename
        SMSTR_CtrlDevNameLabel  = 5,            // StepMotor controller device name label
		SMSTR_CtrlDevDescLabel  = 6,			// StepMotor controller device decription label
        SMSTR_FirmwareVerLabel  = 7,            // StepMotor FIRMWARE VERSION label
        SMSTR_StepMotorVerLabel     = 8,            // StepMotor Adapter version label
        SMSTR_DebugLogFlagLabel = 9,            // StepMotor Debug Log Flag label
        SMSTR_CommStateLabel    = 10,           // StepMotor COMM. STATUS label
        SMSTR_ResolutionLabel   = 11,           // StepMotor RESOLUION label
        SMSTR_AccelLabel        = 12,           // StepMotor ACCELERATION label
        SMSTR_Um2UStepUnit      = 13,           // StepMotor um to ustep label
        SMSTR_UStep2NmUnit      = 14,           // StepMotor ustep to nm label
        SMSTR_VelocityLabel     = 15,           // StepMotor VELOCITY label
        SMSTR_MotionMode        = 16,           // StepMotor MODE label
        SMSTR_SetOrigin         = 17,           // StepMotor ORIGIN label
        SMSTR_TimeoutInterval   = 18,           // StepMotor Timeout Interval label
        SMSTR_TimeoutTrys       = 19,           // StepMotor Timeout Trys label
        SMSTR_XYDevNameLabel    = 20,           // StepMotor controller device name label
		SMSTR_XYDevDescLabel    = 21,			// StepMotor controller device decription label
        SMSTR_SetPositionX      = 22,           // StepMotor POSITION X label
        SMSTR_SetPositionY      = 23,           // StepMotor POSITION Y label
        SMSTR_GetPositionX      = 24,           // StepMotor CURRENT POSITION X label
        SMSTR_GetPositionY      = 25,           // StepMotor CURRENT POSITION Y label
        SMSTR_ZDevNameLabel     = 26,           // StepMotor controller device name label
		SMSTR_ZDevDescLabel     = 27,			// StepMotor controller device decription label
        SMSTR_SetPositionZ      = 28,           // StepMotor POSITION Z label
        SMSTR_GetPositionZ      = 29,           // StepMotor CURRENT POSITION Z label
        SMSTR_PauseMode         = 30,           // property PAUSE label
        SMSTR_Reset             = 31,           // property RESET label
        SMSTR_Status            = 32,           // property STATUS label
        SMSTR_Um2UStepLabel      = 33,           // StepMotor um to ustep label
        SMSTR_StageMirror      = 34,           // StepMotor um to ustep label
        SMSTR_CommLabel      = 35,           // StepMotor um to ustep label
    };

    enum
    {
        StepMotor_TxTerm            = 0x0D,         // EOL transmit symbole
        StepMotor_RxTerm            = 0x0D,         // EOL receiving symbol
                                                //////////////////////////////////////
                                                // Serial CommunicationError codes
                                                //////////////////////////////////////
        StepMotor_SP_OVER_RUN		= 0x30,			// The previous character was not unloaded before the latest was received
        StepMotor_FRAME_ERROR		= 0x31,			// The vald stop bits was not received during the appropriate time period
        StepMotor_BUFFER_OVER_RUN	= 0x32,			// The input buffer is filled and CR has not been received
        StepMotor_BAD_COMMAND		= 0x34,			// Input cannot be interpreted -- command byte not valid
        StepMotor_MOVE_INTERRUPTED	= 0x38			// A requested move was interrupted by input of serial port.  This code is
                                                // ORed with any other error code.  The value normally returned is "*",
                                                // i.e., 8 ORed with 4. "4" is reported on the vacuum fluorescent display
    };

    static StepMotor* Instance();                                                               // only interface for singleton
    std::string GetMPStr(int nMPStrCode) const;                                             // access prdefined strings
    static void SetDeviceAvailable(bool yFlag) { m_yDeviceAvailable = yFlag; }              // set StepMotor device availability
    static bool GetDeviceAvailability() { return m_yDeviceAvailable; }                      // get StepMotor device availability
    static bool GetIsSetOrigin() { return m_isSetOrigin; }                      // get StepMotor device availability
    static bool GetStageMirrorZ() { return m_stageMirrorZ; }                      // get StepMotor device availability
    static void SetStageMirrorZ(bool flag) {m_stageMirrorZ = flag; }                      // get StepMotor device availability
    static void SetIsSetOrigin(bool flag) { m_isSetOrigin = flag; }                      // get StepMotor device availability
	static int  GetDebugLogFlag() { return m_nDebugLogFlag; }								// get StepMotor debug log flag
	static void SetDebugLogFlag(int nDebugLogFlag) { m_nDebugLogFlag = nDebugLogFlag; }		// set StepMotor debug log flag
    static void SetVelocity(long lVelocity) { m_lVelocity = lVelocity; }                    // set StepMotor device velocity
    static long GetVelocity() { return m_lVelocity; }                                       // get StepMotor device velocity
    static void SetResolution(int nResolution) { m_nResolution = nResolution; }             // set StepMotor device resolution
    static int  GetResolution() { return m_nResolution; }                                   // get StepMotor resolution
    static void SetUm2UStep(int nUm2UStep) { m_nUm2UStep = nUm2UStep; }                     // set StepMotor Um to UStep conversion unit
    static int  GetUm2UStep() { return m_nUm2UStep; }                                       // get StepMotor Um to UStep conversion unit
    static void SetUStep2Nm(int nUStep2Nm) { m_nUStep2Nm = nUStep2Nm; }                     // set StepMotor UStep to Nm conversion unit
    static int  GetUStep2Nm() { return m_nUStep2Nm; }                                       // get StepMotor UStep to NM conversion unit
    static void SetMotionMode(int nMotionMode) { m_nMotionMode = nMotionMode; }             // set Motor motion mode
    static int  GetMotionMode() { return m_nMotionMode; }                                   // get Motor motion mode
    static void SetTimeoutInterval(int nInterval) { m_nTimeoutInterval = nInterval; }       // set Timwout Interval
    static int  GetTimeoutInterval() { return m_nTimeoutInterval; }                         // get Timeout Interval
    static void SetTimeoutTrys(int nTrys) { m_nTimeoutTrys = nTrys; }                       // set Timeout Trys
    static int  GetTimeoutTrys() { return m_nTimeoutTrys; }                                 // get Timeout Trys
    //static void SetNumberOfAxes(int nNumberOfAxes) { m_nNumberOfAxes = nNumberOfAxes; }   // set number of axes controlled by StepMotor
    //static int  GetNumberOfAxes() { return m_nNumberOfAxes; }                             // get numebr of axes controlled by StepMotor
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
    static void SetPluseInterval(int interval){m_PluseInterval = interval;}
    static int GetPluseInterval(){return m_PluseInterval;}
    static HANDLE getCommHandle(){return m_hcomm;}
    static void setCommHandle(HANDLE hcomm){m_hcomm = hcomm;}

protected:
    StepMotor();    // Constructor

private:

    static HANDLE               m_hcomm;              			// comm handle
    static int                  m_PluseInterval;              // StepMotor resolution
    static bool                 m_yInstanceFlag;            // singleton flag
    static bool                 m_isSetOrigin;            // singleton flag
    static bool                 m_stageMirrorZ;            // singleton flag
    static StepMotor*           m_pStepMotor;                   // singleton copy
    static bool                 m_yDeviceAvailable;         // StepMotor availability
	static int					m_nDebugLogFlag;			// StepMotor debug log flag
    static int                  m_nResolution;              // StepMotor resolution
    static int                  m_nUm2UStep;                // unit to convert um to uStep
    static int                  m_nUStep2Nm;                // unit to convert uStep to nm
    static int                  m_nTimeoutInterval;         // timeout interval
    static int                  m_nTimeoutTrys;             // timeout trys
    //static int                m_nNumberOfAxes;            // number of StepMotor axes
    static int                  m_nMotionMode;              // motor motion mode
    static double               m_dPositionX;               // position X
    static double               m_dPositionY;               // position Y
    static double               m_dPositionZ;               // position Z
    static long                 m_lVelocity;                // StepMotor velocity
    static std::string          m_sPort;                    // serial port symbols
    std::map<int, std::string>  m_sMPStr;                   // constant strings
};

#endif  //_StepMotor_H_
