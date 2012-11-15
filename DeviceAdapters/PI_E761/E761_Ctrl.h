/*
 * E761_Ctrl.h
 *
 *  Created on: Nov 9, 2012
 *      Author: Zephyre
 */

#ifndef E761_CTRL_H_
#define E761_CTRL_H_

#include <Windows.h>
#include "../../MMDevice/DeviceBase.h"
#include "../../MMDevice/ImgBuffer.h"
#include "../../MMDevice/DeviceThreads.h"

class E761_XYStage;
class E761_ZStage;
// global driver thread lock
extern MMThreadLock g_E761DriverLock;

//////
// PI_E761_Control class
/////
class E761_Ctrl: public CGenericBase<E761_Ctrl> {
public:
	enum _MPStr {
		STR_CtrlDevName = 0,
		STR_XYStageDevName,
		STR_ZStageDevName,
		STR_Version,
		STR_PROP_NAME,
		STR_PROP_DESC,
		STR_PROP_REBOOT,
		STR_PROP_STARTMONITOR,
		STR_PROP_BOARDID,
		STR_PROP_LASTERR,
		STR_PROP_XPOSITION,
		STR_PROP_YPOSITION,
		STR_PROP_POSITION,
		STR_PROP_TRVRANGE,
		STR_PROP_XSERVO,
		STR_PROP_YSERVO,
		STR_XYStageDesc,
		STR_ZStageDesc,
		STR_CtrlDesc,
		STR_PROP_SERVO
	};

	static const int PI_E761_ERROR_CODE = 2000;

	static E761_Ctrl* getInstance();
	void GetName(char* name) const;
	static std::string getConstString(int strCode);
	int Initialize();
	bool isInitialized() {
		return m_initialized;
	}
	bool debugLogFlag() {
		return m_debugLogFlag;
	}
	bool Busy();
	int Shutdown();

	int OnBoardId(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnTravelRange(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnReboot(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnMonitor(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnLastError(MM::PropertyBase* pProp, MM::ActionType eAct);
	int getDeviceId() {
		return m_devId;
	}
	void getAxisName(char* px, char* py, char* pz);
	E761_Ctrl();
	int getErrorMsg();
	int getErrorMsg(const char* msg);
	void setXYStage(E761_XYStage* pStage) {
		m_pXYStage = pStage;
	}
	E761_XYStage* getXYStage() {
		return m_pXYStage;
	}
	void setZStage(E761_ZStage* pStage) {
		m_pZStage = pStage;
	}
	E761_ZStage* getZStage() {
		return m_pZStage;
	}

	// Some operations, such as setPosition, etc, will cause the device to be in 'busy' state.
	// Such operatioins will call this methods and check the current time stamp.
	void checkIn();

protected:
	virtual ~E761_Ctrl();

private:
	bool m_initialized;			// controller initialized flag
	bool m_debugLogFlag;			// Whether to log debug information
	bool m_reboot;
	long m_boardId;	//	PI_E761 board ID for initialization
	int m_devId;
	static E761_Ctrl* m_pInstance;
	E761_XYStage* m_pXYStage;
	E761_ZStage* m_pZStage;
	static std::map<int, std::string> m_strMap;
	char m_axisNames[32];
	// A flag to inform the monitor to stop.
	bool m_stopMonitorFlag;
	// Whether to start the monitor
	bool m_startMonitor;
	// The interval of the position monitor in milliseconds.
	int m_monitorIntvMs;
	HANDLE m_exitMonitorEvent;
	int m_checkedTimeStamp;
	// The minimal interval in ms between adjacent operations
	int m_minIntervalMs;

	static char errorMsg[MM::MaxStrLength];

	static int initConstStrings();
	static DWORD WINAPI monitorThread(LPVOID param);
};

//////
// PI_E761_Control class
/////
class E761_XYStage: public CXYStageBase<E761_XYStage> {
public:
	static E761_XYStage* getInstance();
	E761_XYStage();
	int Initialize();
	bool isIntialized() {
		return m_initialized;
	}
	void OnPositionChanged(double x, double y) {
		OnXYStagePositionChanged(x, y);
	}
	bool Busy() {
		return E761_Ctrl::getInstance()->Busy();
	}
	void updateErrorText(int code, const char* msg) {
		SetErrorText(code, msg);
	}
	int Shutdown();
	void GetName(char* name) const;
	int Home();
	int SetOrigin() {
		return DEVICE_OK;
	}
	int Stop() {
		return DEVICE_OK;
	}
	int IsXYStageSequenceable(bool& seq) const {
		seq = false;
		return DEVICE_OK;
	}
	int GetLimitsUm(double& xMin, double& xMax, double& yMin, double& yMax) {
		xMin = 0;
		xMax = 100;
		yMin = 0;
		yMax = 100;
		return DEVICE_OK;
	}
	int GetStepLimits(long& /*xMin*/, long& /*xMax*/, long& /*yMin*/,
			long& /*yMax*/) {
		return DEVICE_OK/*DEVICE_UNSUPPORTED_COMMAND*/;
	}
	double GetStepSizeXUm() {
		return 0.001;
	}
	double GetStepSizeYUm() {
		return 0.001;
	}
	int SetPositionSteps(long lXPosSteps, long lYPosSteps);
	int GetPositionSteps(long& x, long& y);

	int E761_XYStage::OnXPosition(MM::PropertyBase* pProp, MM::ActionType eAct);
	int E761_XYStage::OnYPosition(MM::PropertyBase* pProp, MM::ActionType eAct);
	int E761_XYStage::OnXServoMode(MM::PropertyBase* pProp,
			MM::ActionType eAct);
	int E761_XYStage::OnYServoMode(MM::PropertyBase* pProp,
			MM::ActionType eAct);
	int E761_XYStage::OnServoMode(MM::PropertyBase* pProp, MM::ActionType eAct,
			const char* axis);

protected:
	~E761_XYStage();

private:
	bool m_initialized;			// controller initialized flag
	static E761_XYStage* m_pInstance;
};

//////
// PI_E761_Control class
/////
class E761_ZStage: public CStageBase<E761_ZStage> {
public:
	static E761_ZStage* getInstance();
	E761_ZStage();
	bool Busy() {
		return E761_Ctrl::getInstance()->Busy();
	}
	void updateErrorText(int code, const char* msg) {
		SetErrorText(code, msg);
	}
	int Initialize();
	int Shutdown();
	void GetName(char* name) const;
	int SetPositionUm(double pos);
	int GetPositionUm(double& pos);
	int SetPositionSteps(long steps);
	int GetPositionSteps(long& steps);
	int Home();
	int SetOrigin() {
		return DEVICE_OK;
	}
	int GetLimits(double& lower, double& upper) {
		lower = 0;
		upper = 10;
		return DEVICE_OK;
	}
	int IsStageSequenceable(bool& seq) const {
		seq = false;
		return DEVICE_OK;
	}
	bool IsContinuousFocusDrive() const {
		return true;
	}
	int OnServoMode(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnPosition(MM::PropertyBase* pProp, MM::ActionType eAct);
	bool isInitialized() {
		return m_initialized;
	}
	void OnPositionChanged(double pos) {
		OnStagePositionChanged(pos);
	}

protected:
	~E761_ZStage();

private:
	bool m_initialized;			// controller initialized flag
	static E761_ZStage* m_pInstance;
	double stepSizeUm;
	bool m_servoMode;
};

class E761_DriverGuard
{
public:
	E761_DriverGuard() {
		g_E761DriverLock.Lock();
	}
   ~E761_DriverGuard(){
	   g_E761DriverLock.Unlock();
   }
};

#endif /* E761_CTRL_H_ */
