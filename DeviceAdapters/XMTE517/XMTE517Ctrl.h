//////////////////////////////////////////////////////////////////////////////
// FILE:          XMTE517Ctrl.h
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   XMTE517 Micromanipulator Controller
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
// AUTHOR:        Lon Chu (lonchu@yahoo.com) created on June 2011
//

#ifndef _XMTE517CTRL_H_
#define _XMTE517CTRL_H_

#include "../../MMDevice/MMDevice.h"
#include "../../MMDevice/DeviceBase.h"

//
// MAP285 is a micromanipulator controller from Sutter Instrument Comapny.
// It accept remote serial input to conrol micromanipulator.
//
class XMTE517Ctrl : public CGenericBase<XMTE517Ctrl>
{
    public:

        // contructor & destructor
        // .......................
        XMTE517Ctrl();
        ~XMTE517Ctrl();

        // Device API
        // ---------
        int Initialize();
        int Shutdown();

        void GetName(char* pszName) const;

        // XMTE517 doesn't support equivalent command 
        // return false for now
        bool Busy() { return false; }


        int DeInitialize() { m_yInitialized = false; return DEVICE_OK; };
        bool Initialized() { return m_yInitialized; };

        int SetMotionMode(long lMotionMode);

        // action interface
        // ---------------
        int OnPort(MM::PropertyBase* pProp, MM::ActionType eAct);
		int OnDebugLogFlag(MM::PropertyBase* pProp, MM::ActionType eAct);
        int OnMotionMode(MM::PropertyBase* pProp, MM::ActionType eAct);
        int OnTimeoutInterval(MM::PropertyBase* pProp, MM::ActionType eAct);
        int OnTimeoutTrys(MM::PropertyBase* pProp, MM::ActionType eAct);

    private:

        int WriteCommand(unsigned char* sCommand, int nLength);
        int ReadMessage(unsigned char* sResponse, int nBytesRead);

        // montoring controller status
        // ---------------------------
        int CheckStatus(unsigned char* sResponse, unsigned int nLength);

        std::string   m_sCommand;             // Command exchange with MMCore
        std::string   m_sPort;                // serial port id
        bool          m_yInitialized;         // controller initialized flag
        int           m_nAnswerTimeoutMs;     // maximum waiting time for receiving reolied message
        int           m_nAnswerTimeoutTrys;   // timeout trys
};

#endif  // _XMTE517CTRL_H_
