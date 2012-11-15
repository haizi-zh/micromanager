/*
 * E761_Ctrl.cpp
 *
 *  Created on: Nov 9, 2012
 *      Author: Zephyre
 */

#include "E761_Ctrl.h"
#include "PI_E761.h"
#include "E7XX_GCS_DLL.h"

using namespace std;

E761_Ctrl* E761_Ctrl::m_pInstance(NULL);
std::map<int, std::string> E761_Ctrl::m_strMap;
char E761_Ctrl::errorMsg[MM::MaxStrLength];

// global driver thread lock
MMThreadLock g_E761DriverLock;

int E761_Ctrl::initConstStrings() {
	m_strMap[STR_CtrlDevName] = "PI-E761 Controller";
	m_strMap[STR_XYStageDevName] = "PI-E761 XY Stage";
	m_strMap[STR_ZStageDevName] = "PI-E761 Z Stage";
	m_strMap[STR_PROP_NAME] = "Name";
	m_strMap[STR_PROP_DESC] = "Description";
	m_strMap[STR_PROP_REBOOT] = "Reboot on Initialization";
	m_strMap[STR_PROP_STARTMONITOR] = "Start the position monitor";
	m_strMap[STR_PROP_BOARDID] = "Board Id";
	m_strMap[STR_PROP_XPOSITION] = "X Position";
	m_strMap[STR_PROP_YPOSITION] = "Y Position";
	m_strMap[STR_PROP_POSITION] = "Position";
	m_strMap[STR_PROP_TRVRANGE] = "Travel Range(um)";
	m_strMap[STR_PROP_XSERVO] = "X Servo Mode";
	m_strMap[STR_PROP_YSERVO] = "Y Servo Mode";
	m_strMap[STR_PROP_LASTERR] = "Last Error";
	m_strMap[STR_CtrlDesc] =
			"Physik Instrumente(PI) E761 Piezo Stage Controller";
	m_strMap[STR_XYStageDesc] =
			"Physik Instrumente(PI) E761 Piezo Stage Controller";
	m_strMap[STR_ZStageDesc] =
			"Physik Instrumente(PI) E761 Piezo Stage Controller";
	m_strMap[STR_PROP_SERVO] = "Servo Mode";
	return DEVICE_OK;
}

E761_Ctrl::E761_Ctrl() :
		m_initialized(false), m_boardId(1), m_debugLogFlag(false), m_pXYStage(
				NULL), m_pZStage(NULL), m_stopMonitorFlag(false), m_monitorIntvMs(
				500), m_startMonitor(true), m_exitMonitorEvent(NULL), m_minIntervalMs(
				20), m_checkedTimeStamp(0) {
	InitializeDefaultErrorMessages();
	// The custom PI-E761 error messages are kept here.
	SetErrorText(PI_E761_ERROR_CODE, errorMsg);

	int ret;
	char propName[MM::MaxStrLength];
	char msg[MM::MaxStrLength];

	// Name
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getConstString(STR_PROP_NAME).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getConstString(STR_CtrlDevName).c_str(), MM::String,
			true);

	// Description
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getConstString(STR_PROP_DESC).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getConstString(STR_CtrlDesc).c_str(), MM::String, true);

	// Board Id
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getConstString(STR_PROP_BOARDID).c_str());
	char strBoardId[32];
	_snprintf_s(strBoardId, sizeof(strBoardId), _TRUNCATE, "%d", m_boardId);
	CPropertyAction* pActOnBoardId = new CPropertyAction(this,
			&E761_Ctrl::OnBoardId);
	ret = CreateProperty(propName, strBoardId, MM::Integer, false,
			pActOnBoardId, true);

	// Reboot
	CPropertyAction* pActReboot = new CPropertyAction(this,
			&E761_Ctrl::OnReboot);
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getConstString(STR_PROP_REBOOT).c_str());
	ret = CreateProperty(propName, "False", MM::String, false, pActReboot,
			true);
	AddAllowedValue(propName, "True");
	AddAllowedValue(propName, "False");

	// Monitor
	CPropertyAction* pActMonitor = new CPropertyAction(this,
			&E761_Ctrl::OnMonitor);
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getConstString(STR_PROP_STARTMONITOR).c_str());
	ret = CreateProperty(propName, "False", MM::String, false, pActMonitor,
			true);
	AddAllowedValue(propName, "True");
	AddAllowedValue(propName, "False");

	m_pInstance = this;
}

E761_Ctrl::~E761_Ctrl() {
	Shutdown();
}

int E761_Ctrl::OnTravelRange(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;
	if (eAct == MM::BeforeGet) {
		double tmin[3];
		double tmax[3];
		if (!E7XX_qTMN(m_devId, m_axisNames, tmin))
			return getErrorMsg();
		if (!E7XX_qTMX(m_devId, m_axisNames, tmax))
			return getErrorMsg();
		char msg[MM::MaxStrLength];
		_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
				"X:[%f~%f], Y:[%f~%f], Z:[%f~%f]", tmin[0], tmax[0], tmin[1],
				tmax[1], tmin[2], tmax[2]);
		pProp->Set(msg);
	}
	return DEVICE_OK;
}

int E761_Ctrl::OnReboot(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;
	if (eAct == MM::BeforeGet) {
		string val = m_reboot ? "True" : "False";
		pProp->Set(val.c_str());
	} else if (eAct == MM::AfterSet) {
		string val;
		pProp->Get(val);
		m_reboot = (val.compare("True") == 0);
	}
	return DEVICE_OK;
}

int E761_Ctrl::OnMonitor(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;
	if (eAct == MM::BeforeGet) {
		string val = m_startMonitor ? "True" : "False";
		pProp->Set(val.c_str());
	} else if (eAct == MM::AfterSet) {
		string val;
		pProp->Get(val);
		m_startMonitor = (val.compare("True") == 0);
	}
	return DEVICE_OK;
}

void E761_Ctrl::checkIn() {
	E761_DriverGuard guard;
	m_checkedTimeStamp = GetTickCount();
}

bool E761_Ctrl::Busy() {
	E761_DriverGuard guard;
	return GetTickCount() - m_checkedTimeStamp < m_minIntervalMs;
}

int E761_Ctrl::OnBoardId(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;
	if (eAct == MM::BeforeGet) {
		pProp->Set(m_boardId);
	} else if (eAct == MM::AfterSet) {
		pProp->Get(m_boardId);
	}
	return DEVICE_OK;
}

void E761_Ctrl::getAxisName(char* px, char* py, char* pz) {
	if (px)
		*px = m_axisNames[0];
	if (py)
		*py = m_axisNames[1];
	if (pz)
		*pz = m_axisNames[2];
}

int E761_Ctrl::getErrorMsg() {
	return getErrorMsg(NULL);
}

int E761_Ctrl::getErrorMsg(const char* msg) {
	int errorNo = E7XX_GetError(m_devId);
	char str[MM::MaxStrLength];
	E7XX_TranslateError(errorNo, str, MM::MaxStrLength);

	if (msg != NULL)
		::_snprintf_s(errorMsg, MM::MaxStrLength, _TRUNCATE,
				"PI error code: %d, reason: %s. Custom message: %s", errorNo,
				str, msg);
	else
		::_snprintf_s(errorMsg, MM::MaxStrLength, _TRUNCATE,
				"PI error code: %d, reason: %s.", errorNo, str);
	SetErrorText(PI_E761_ERROR_CODE, errorMsg);
	if (m_pXYStage != NULL)
		m_pXYStage->updateErrorText(PI_E761_ERROR_CODE, errorMsg);
	if (m_pZStage != NULL)
		m_pZStage->updateErrorText(PI_E761_ERROR_CODE, errorMsg);
	return PI_E761_ERROR_CODE;
}

int E761_Ctrl::OnLastError(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;
	if (eAct == MM::BeforeGet)
		pProp->Set(errorMsg);
	return DEVICE_OK;
}

DWORD E761_Ctrl::monitorThread(LPVOID param) {
	E761_Ctrl* pCtrl = E761_Ctrl::getInstance();
	E761_XYStage* pXY = pCtrl->getXYStage();
	E761_ZStage* pZ = pCtrl->getZStage();
	while (!pCtrl->m_stopMonitorFlag) {
		Sleep(pCtrl->m_monitorIntvMs);
		if (pXY != NULL && pXY->isIntialized()) {
			double x = 0, y = 0;
			pXY->GetPositionUm(x, y);
			pXY->OnPositionChanged(x, y);
		}
		if (pZ != NULL && pZ->isInitialized()) {
			double pos = 0;
			pZ->GetPositionUm(pos);
			pZ->OnPositionChanged(pos);
		}
	}
	SetEvent(pCtrl->m_exitMonitorEvent);
	return 0;
}

int E761_Ctrl::Initialize() {
	if (m_initialized)
		return DEVICE_OK;
	int ret;
	char msg[MM::MaxStrLength];

	int id = -1;
	if (!m_reboot)
		id = E7XX_ConnectPciBoard(m_boardId);
	// 濡傛灉m_reboot涓簍rue鎴栬�E7XX_ConnectPciBoard澶辫触浜嗭細
	if (id == -1) {
		id = E7XX_ConnectPciBoardAndReboot(m_boardId);
		if (id == -1)
			return DEVICE_ERR;
	}
	m_devId = id;

	// Get axis names
	//char cstBuf[MM::MaxStrLength];
	//if (!E7XX_qCST(m_devId, "", cstBuf, MM::MaxStrLength))
	//	return getErrorMsg();

	//if (!E7XX_qVST(m_devId, cstBuf, MM::MaxStrLength))
	//	return getErrorMsg();

	//ret = E7XX_CST(m_devId, NULL, "ID-STAGE\n ID-STAGE\n ID-STAGE\n");
	//if (!ret)
	//	return getErrorMsg();

	ret = E7XX_qSAI(m_devId, m_axisNames, sizeof(m_axisNames));
	if (!ret)
		return E761_Ctrl::getErrorMsg("Get axis names.");

	BOOL svoVal[] = { TRUE, TRUE, TRUE };
	ret = E7XX_SVO(m_devId, m_axisNames, svoVal);
	if (!ret)
		return E761_Ctrl::getErrorMsg("Get SVO status.");

	_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
			"<E761_Ctrl::Initialize> Axis names: %s", m_axisNames);
	this->LogMessage(msg);

	char propName[MM::MaxStrLength];

	// Travel range
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			getInstance()->getConstString(STR_PROP_TRVRANGE).c_str());
	CPropertyAction* pActOnTravelRange = new CPropertyAction(this,
			&E761_Ctrl::OnTravelRange);
	ret = CreateProperty(propName, "", MM::String, true, pActOnTravelRange);

	// Last error message
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			getInstance()->getConstString(STR_PROP_LASTERR).c_str());
	CPropertyAction* pActOnLastError = new CPropertyAction(this,
			&E761_Ctrl::OnLastError);
	ret = CreateProperty(propName, "", MM::String, true, pActOnLastError);

	ret = UpdateStatus();
	if (ret != DEVICE_OK)
		return ret;

	// Start the monitor thread
	m_exitMonitorEvent = CreateEventA(NULL, TRUE, FALSE, NULL);
	CreateThread(NULL, 0, monitorThread, this, 0, NULL);

	m_initialized = true;
	return ret;
}

int E761_Ctrl::Shutdown() {
	if (m_initialized) {
		m_stopMonitorFlag = true;
		m_initialized = false;
		E7XX_CloseConnection(m_devId);
		WaitForSingleObject(m_exitMonitorEvent, 2 * m_monitorIntvMs);
	}
	return DEVICE_OK;
}

///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////
MODULE_API MM::Device* CreateDevice(const char* deviceName) {
	if (deviceName == 0)
		return 0;

	if (strcmp(deviceName,
			E761_Ctrl::getConstString(E761_Ctrl::STR_ZStageDevName).c_str())
			== 0)
		return new E761_ZStage();	//::getInstance();
	else if (strcmp(deviceName,
			E761_Ctrl::getConstString(E761_Ctrl::STR_XYStageDevName).c_str())
			== 0)
		return new E761_XYStage();	//::getInstance();
	else if (strcmp(deviceName,
			E761_Ctrl::getConstString(E761_Ctrl::STR_CtrlDevName).c_str()) == 0)
		return new E761_Ctrl();

	return DEVICE_OK;
}

MODULE_API void DeleteDevice(MM::Device* pDevice) {
	delete pDevice;
}

/**
 * List all suppoerted hardware devices here
 * Do not discover devices at runtime.  To avoid warnings about missing DLLs, Micro-Manager
 * maintains a list of supported device (MMDeviceList.txt).  This list is generated using
 * information supplied by this function, so runtime discovery will create problems.
 */
MODULE_API void InitializeModuleData() {
	string strXY = E761_Ctrl::getConstString(E761_Ctrl::STR_XYStageDevName);
	AddAvailableDeviceName(strXY.c_str(), strXY.c_str());
	string strZ =
			E761_Ctrl::getConstString(E761_Ctrl::STR_ZStageDevName).c_str();
	AddAvailableDeviceName(strZ.c_str(), strZ.c_str());
	string strCtrl =
			E761_Ctrl::getConstString(E761_Ctrl::STR_CtrlDevName).c_str();
	AddAvailableDeviceName(strCtrl.c_str(), strCtrl.c_str());
}

// windows DLL entry code
#ifdef WIN32
BOOL APIENTRY DllMain(HANDLE /*hModule*/, DWORD ul_reason_for_call, LPVOID /*lpReserved*/
) {
	switch (ul_reason_for_call) {
	case DLL_PROCESS_ATTACH:
	case DLL_THREAD_ATTACH:
	case DLL_THREAD_DETACH:
	case DLL_PROCESS_DETACH:
		break;
	}
	return TRUE;
}
#endif

std::string E761_Ctrl::getConstString(int strCode) {
//	if (m_strMap.size() == 0)
	initConstStrings();

	string sText;
	map<int, string>::const_iterator it = m_strMap.find(strCode);
	if (it != m_strMap.end())
		sText = it->second;
	return sText;
}

E761_Ctrl* E761_Ctrl::getInstance() {
	if (m_pInstance == NULL) {
		m_pInstance = new E761_Ctrl();
	}
	return m_pInstance;
}

void E761_Ctrl::GetName(char* name) const {
	CDeviceUtils::CopyLimitedString(name,
			E761_Ctrl::getConstString(STR_CtrlDevName).c_str());
}

E761_XYStage* E761_XYStage::m_pInstance(NULL);

E761_XYStage* E761_XYStage::getInstance() {
	if (m_pInstance == NULL) {
		m_pInstance = new E761_XYStage();
	}
	return m_pInstance;
}

E761_XYStage::E761_XYStage() :
		m_initialized(false) {
	InitializeDefaultErrorMessages();
	int ret;
	char propName[MM::MaxStrLength];

	// Name
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_NAME).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getInstance()->getConstString(
					E761_Ctrl::STR_XYStageDevName).c_str(), MM::String, true);

	// Description
	_snprintf(propName, MM::MaxStrLength,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_DESC).c_str());
	ret =
			CreateProperty(propName,
					E761_Ctrl::getInstance()->getConstString(
							E761_Ctrl::STR_XYStageDesc).c_str(), MM::String,
					true);

	E761_Ctrl::getInstance()->setXYStage(this);
}

int E761_XYStage::Home() {
	E761_DriverGuard guard;
	char axis[3] = { 0, 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(axis, axis + 1, NULL);
	if (!E7XX_GOH(E761_Ctrl::getInstance()->getDeviceId(), axis))
		return E761_Ctrl::getInstance()->getErrorMsg("XYStage: Home");
	E761_Ctrl::getInstance()->checkIn();
	return DEVICE_OK;
}

int E761_XYStage::Initialize() {
	if (m_initialized)
		return DEVICE_OK;

	if (!E761_Ctrl::getInstance()->isInitialized())
		return DEVICE_NOT_CONNECTED;

	int ret;
	char propName[MM::MaxStrLength];

	// Position
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(
					E761_Ctrl::STR_PROP_XPOSITION).c_str());
	CPropertyAction* pActXPosition = new CPropertyAction(this,
			&E761_XYStage::OnXPosition);
	ret = CreateProperty(propName, "", MM::Float, false, pActXPosition);

	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(
					E761_Ctrl::STR_PROP_YPOSITION).c_str());
	CPropertyAction* pActYPosition = new CPropertyAction(this,
			&E761_XYStage::OnYPosition);
	ret = CreateProperty(propName, "", MM::Float, false, pActYPosition);

	// Servo
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_XSERVO).c_str());
	CPropertyAction* pActXServo = new CPropertyAction(this,
			&E761_XYStage::OnXServoMode);
	ret = CreateProperty(propName, "", MM::String, false, pActXServo);

	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_YSERVO).c_str());
	CPropertyAction* pActYServo = new CPropertyAction(this,
			&E761_XYStage::OnYServoMode);
	ret = CreateProperty(propName, "", MM::String, false, pActYServo);

	ret = UpdateStatus();
	if (ret != DEVICE_OK)
		return ret;

	m_initialized = true;
	return DEVICE_OK;
}

int E761_XYStage::OnXPosition(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;

	double x = 0, y = 0;
	if (eAct == MM::BeforeGet) {
		GetPositionUm(x, y);
		pProp->Set(x);
	} else if (eAct == MM::AfterSet) {
		GetPositionUm(x, y);
		pProp->Get(x);
		SetPositionUm(x, y);
	}
	OnXYStagePositionChanged(x, y);
	return DEVICE_OK;
}

int E761_XYStage::OnXServoMode(MM::PropertyBase* pProp, MM::ActionType eAct) {
	char axis[2] = { 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(axis, NULL, NULL);
	return OnServoMode(pProp, eAct, axis);
}

int E761_XYStage::OnServoMode(MM::PropertyBase* pProp, MM::ActionType eAct,
		const char* axis) {
	E761_DriverGuard guard;
	if (eAct == MM::BeforeGet) {
		BOOL val[1];
		if (!E7XX_qSVO(E761_Ctrl::getInstance()->getDeviceId(), axis, val))
			return E761_Ctrl::getInstance()->getErrorMsg("XYStage: qSVO");
		pProp->Set(val[0] ? "True" : "False");
	} else if (eAct == MM::AfterSet) {
		string str;
		pProp->Get(str);
		BOOL val[1] = { (str.compare("True") == 0) ? TRUE : FALSE };
		if (!E7XX_SVO(E761_Ctrl::getInstance()->getDeviceId(), axis, val))
			return E761_Ctrl::getInstance()->getErrorMsg("XYStage: SVO");
	}
	return DEVICE_OK;
}

int E761_XYStage::OnYServoMode(MM::PropertyBase* pProp, MM::ActionType eAct) {
	char axis[2] = { 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(NULL, axis, NULL);
	return OnServoMode(pProp, eAct, axis);
}

int E761_XYStage::OnYPosition(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;
	double x = 0, y = 0;
	if (eAct == MM::BeforeGet) {
		GetPositionUm(x, y);
		pProp->Set(y);
	} else if (eAct == MM::AfterSet) {
		GetPositionUm(x, y);
		pProp->Get(y);
		SetPositionUm(x, y);
	}
	OnXYStagePositionChanged(x, y);
	return DEVICE_OK;
}

int E761_XYStage::Shutdown() {
	m_initialized = false;
	E761_Ctrl::getInstance()->setXYStage(NULL);
	return DEVICE_OK;
}

void E761_XYStage::GetName(char* name) const {
	CDeviceUtils::CopyLimitedString(name,
			E761_Ctrl::getConstString(E761_Ctrl::STR_XYStageDevName).c_str());
}

int E761_XYStage::SetPositionSteps(long lXPosSteps, long lYPosSteps) {
	E761_DriverGuard guard;

	double stepSizeX = GetStepSizeXUm();
	double stepSizeY = GetStepSizeYUm();
	double xUm = lXPosSteps * stepSizeX;
	double yUm = lYPosSteps * stepSizeY;

	double posUm[2] = { xUm, yUm };

	char axis[3] = { 0, 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(axis, axis + 1, NULL);
	BOOL ret = E7XX_MOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm);
	if (!ret)
		return E761_Ctrl::getInstance()->getErrorMsg("Set XY positions.");
	E761_Ctrl::getInstance()->checkIn();

	return DEVICE_OK;
}

int E761_XYStage::GetPositionSteps(long& x, long& y) {
	E761_DriverGuard guard;

	double stepSizeX = GetStepSizeXUm();
	double stepSizeY = GetStepSizeYUm();
	double posUm[2];
	char axis[3] = { 0, 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(axis, axis + 1, NULL);
	BOOL ret = E7XX_qMOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm);
	if (!ret)
		return E761_Ctrl::getInstance()->getErrorMsg("Get XY positions.");

	x = (long) (posUm[0] / stepSizeX + 0.5);
	y = (long) (posUm[1] / stepSizeY + 0.5);

	return DEVICE_OK;
}

E761_XYStage::~E761_XYStage() {
	Shutdown();
}

E761_ZStage* E761_ZStage::m_pInstance(NULL);

E761_ZStage* E761_ZStage::getInstance() {
	if (m_pInstance == NULL) {
		m_pInstance = new E761_ZStage();
	}
	return m_pInstance;
}

E761_ZStage::E761_ZStage() :
		stepSizeUm(0.001), m_initialized(false), m_servoMode(true) {
	InitializeDefaultErrorMessages();
	int ret;
	char propName[MM::MaxStrLength];

	// Name
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_NAME).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getInstance()->getConstString(
					E761_Ctrl::STR_ZStageDevName).c_str(), MM::String, true);

	// Description
	_snprintf(propName, MM::MaxStrLength,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_DESC).c_str());
	ret =
			CreateProperty(propName,
					E761_Ctrl::getInstance()->getConstString(
							E761_Ctrl::STR_ZStageDesc).c_str(), MM::String,
					true);

	E761_Ctrl::getInstance()->setZStage(this);
}

int E761_ZStage::Initialize() {
	if (m_initialized)
		return DEVICE_OK;
	int ret;

	if (!E761_Ctrl::getInstance()->isInitialized())
		return DEVICE_NOT_CONNECTED;

	char propName[MM::MaxStrLength];
	char msg[MM::MaxStrLength];
	CPropertyAction* pActOnServoMode = new CPropertyAction(this,
			&E761_ZStage::OnServoMode);

	// Servo mode
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_SERVO).c_str());
	ret = CreateProperty(propName, "True", MM::String, false, pActOnServoMode);
	AddAllowedValue(propName, "False");
	if (ret != DEVICE_OK)
		return ret;

	// Position
	double pos;
	GetPositionUm(pos);
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(
					E761_Ctrl::STR_PROP_POSITION).c_str());
	CPropertyAction* pActPosition = new CPropertyAction(this,
			&E761_ZStage::OnPosition);
	_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE, "%f", pos);
	ret = CreateProperty(propName, msg, MM::Float, false, pActPosition);

	ret = UpdateStatus();
	if (ret != DEVICE_OK)
		return ret;

	m_initialized = true;
	return ret;
}

int E761_ZStage::Home() {
	E761_DriverGuard guard;
	char axis[2] = { 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(NULL, NULL, axis);
	if (!E7XX_GOH(E761_Ctrl::getInstance()->getDeviceId(), axis))
		return E761_Ctrl::getInstance()->getErrorMsg("ZStage: Home");
	E761_Ctrl::getInstance()->checkIn();
	return DEVICE_OK;
}

int E761_ZStage::OnPosition(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;

	double pos = 0;
	if (eAct == MM::BeforeGet) {
		GetPositionUm(pos);
		pProp->Set(pos);
	} else if (eAct == MM::AfterSet) {
		SetPositionUm(pos);
	}
	OnStagePositionChanged(pos);
	return DEVICE_OK;
}

int E761_ZStage::OnServoMode(MM::PropertyBase* pProp, MM::ActionType eAct) {
	E761_DriverGuard guard;

	BOOL val[1];
	char axis[2] = { 0, 0 };
	E761_Ctrl* pCtrl = E761_Ctrl::getInstance();
	pCtrl->getAxisName(NULL, NULL, axis);

	if (eAct == MM::BeforeGet) {
		BOOL ret = E7XX_qSVO(pCtrl->getDeviceId(), axis, val);
		if (!ret)
			return E761_Ctrl::getInstance()->getErrorMsg(
					"Get Z stage SVO status.");
		m_servoMode = val[0] ? true : false;
		pProp->Set(m_servoMode ? "True" : "False");
	} else if (eAct == MM::AfterSet) {
		string strVal;
		pProp->Get(strVal);
		m_servoMode = (strVal.compare("True") == 0 ? true : false);
		val[0] = m_servoMode;
		BOOL ret = E7XX_SVO(pCtrl->getDeviceId(), axis, val);
		if (!ret)
			return E761_Ctrl::getInstance()->getErrorMsg(
					"Set Z stage SVO status.");
	}
	return DEVICE_OK;
}

int E761_ZStage::Shutdown() {
	m_initialized = false;
	E761_Ctrl::getInstance()->setZStage(NULL);
	return DEVICE_OK;
}

void E761_ZStage::GetName(char* name) const {
	CDeviceUtils::CopyLimitedString(name,
			E761_Ctrl::getConstString(E761_Ctrl::STR_ZStageDevName).c_str());
}

int E761_ZStage::SetPositionUm(double pos) {
	long step = (long) (pos / stepSizeUm + 0.5);
	return SetPositionSteps(step);
}

int E761_ZStage::GetPositionUm(double& pos) {
	long step;
	int ret = GetPositionSteps(step);
	if (ret != DEVICE_OK)
		return ret;
	pos = step * stepSizeUm;
	return DEVICE_OK;
}

int E761_ZStage::SetPositionSteps(long steps) {
	E761_DriverGuard guard;

	double posUm[1] = { steps * stepSizeUm };
	char axis[2] = { 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(NULL, NULL, axis);
	if (!E7XX_MOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm))
		return E761_Ctrl::getInstance()->getErrorMsg("Set Z position.");
	E761_Ctrl::getInstance()->checkIn();
	OnStagePositionChanged(posUm[0]);

	return DEVICE_OK;
}

int E761_ZStage::GetPositionSteps(long& steps) {
	E761_DriverGuard guard;
	double posUm[1];
	char axis[2] = { 0, 0 };
	E761_Ctrl::getInstance()->getAxisName(NULL, NULL, axis);
	BOOL ret = E7XX_qMOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm);
	if (!ret)
		return E761_Ctrl::getInstance()->getErrorMsg("Get z position.");

	steps = (long) (posUm[0] / stepSizeUm + 0.5);
	return DEVICE_OK;
}

E761_ZStage::~E761_ZStage() {
	Shutdown();
}
