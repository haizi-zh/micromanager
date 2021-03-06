//////////////////////////////////////////////////////////////////////////////
// FILE:          StepMotorZStage.h 
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   Z  Stage
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

#ifndef _StepMotorZSTAGE_H_
#define _StepMotorZSTAGE_H_

#include "../../MMDevice/MMDevice.h"
#include "../../MMDevice/DeviceBase.h"
#include "StepMotor.h"

//
// define Z stage class that is atached to the StepMotor controller
//
class ZStage : public CStageBase<ZStage>
{
public:
    ZStage();			// Z stage constructor
    ~ZStage();			// Z stage destructor

    // Device API
    // ----------

    // Z stage initialization & shutdown
    int Initialize();	
    int Shutdown();

    // Get Z stage device name
    void GetName(char* pszName) const;

    // Busy is not aplicable for StepMotor
    // It will return false always
    bool Busy() { return false; }

    // Stage API
    // ---------

	// setup motion mode (1: relative, 0: relative
	int SetMotionMode(long lMotionMode);

    // Move Z stage to position in um
    int SetPositionUm(double dZPosUm);
	int SetRelativePositionUm(double dZPosUm);

    // Get Z stage position in um
    int GetPositionUm(double& dZPosUm);

    int GetPositionSteps(long& dZPosUm){return DEVICE_OK;};
    int SetPositionSteps(long dZPosUm){return DEVICE_OK;};

    // Set Z stage origin
    int SetOrigin(){return DEVICE_OK;};

    // Stop Z stage motion
//    int Stop();

    // Get limits of Zstage
    // This function is not applicable for
    // StepMotor, the function will return DEVICE_OK
    // insttead.
    int GetLimits(double& /*min*/, double& /*max*/) { return DEVICE_OK/*DEVICE_UNSUPPORTED_COMMAND*/; }

    // action interface
    // ----------------
    int OnGetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct);
    int OnSetPositionZ(MM::PropertyBase* pProp, MM::ActionType eAct);
    int OnMotionMode(MM::PropertyBase* pProp, MM::ActionType eAct);
    int OnReleasePower(MM::PropertyBase* pProp, MM::ActionType eAct);
    int SetReleasePower(int ReleasePower);
    int SetRunDelay(int RunDelay);
    int SetStartDelay(int StartDelay);
    int OnSetRunDelay(MM::PropertyBase* pProp, MM::ActionType eAct);
    int OnSetStartDelay(MM::PropertyBase* pProp, MM::ActionType eAct);
    // Sequence functions
    int IsStageSequenceable(bool& isSequenceable) const { isSequenceable = false; return DEVICE_OK;}
    int GetStageSequenceMaxLength(long& /*nrEvents*/) const  {return DEVICE_OK;}
    int StartStageSequence() {return DEVICE_OK;}
    int StopStageSequence() {return DEVICE_OK;}
    int LoadStageSequence(std::vector<double> /*positions*/) const {return DEVICE_OK;}
    int ClearStageSequence() {return DEVICE_OK;}
    int AddToStageSequence(double /*position*/) {return DEVICE_OK;}
    int SendStageSequence() {return DEVICE_OK;} 
    bool IsContinuousFocusDrive() const {return true;}

private:

    int WriteCommand(unsigned char* sCommand, int nLength);
    int ReadMessage(unsigned char* sResponse, int nBytesRead);
    //int GetCommand(const std::string& cmd, std::string& response);

    //std::string m_sPort;              // serial port
    bool        m_yInitialized;         // z stage initialization flag
    int         m_nAnswerTimeoutMs;     // timeout value of Z stage waiting for response message
    int         m_nAnswerTimeoutTrys;   // timeout trys
    long        m_lSpeed;               // z stage move speed (same as XY stage move speed   
    double      m_dStepSizeUm;          // Z stage converting unit betweek step and um
    //double    m_dOriginZ;             // Z stage origin
};

#endif  // _StepMotorZSTAGE_H_
