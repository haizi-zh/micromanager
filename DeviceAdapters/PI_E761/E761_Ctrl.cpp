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

//int E761_Ctrl_ret = E761_Ctrl::initConstStrings();

int E761_Ctrl::initConstStrings() {
	m_strMap[STR_CtrlDevName] = "PI-E761 Controller";
	m_strMap[STR_XYStageDevName] = "PI-E761 XY Stage";
	m_strMap[STR_ZStageDevName] = "PI-E761 Z Stage";
	m_strMap[STR_PROP_NAME] = "Name";
	m_strMap[STR_PROP_DESC] = "Description";
	m_strMap[STR_PROP_BOARDID] = "Board Id";
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
		m_initialized(false), m_boardId(1), m_debugLogFlag(false) {
	InitializeDefaultErrorMessages();
	int ret;
	char propName[MM::MaxStrLength];
	char msg[MM::MaxStrLength];
	
	// Name
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getConstString(STR_PROP_NAME).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getConstString(STR_CtrlDevName).c_str(), MM::String,
			true);
	if (m_debugLogFlag) {
		_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
				"<E761_Ctrl::Initialize> CreateProperty(%s = %s), ReturnCode = %d",
				propName,
				E761_Ctrl::getConstString(STR_CtrlDevName).c_str(), ret);
		this->LogMessage(msg);
	}

	// Description
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getConstString(STR_PROP_DESC).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getConstString(STR_CtrlDesc).c_str(), MM::String,
			true);
	if (m_debugLogFlag) {
		_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
				"<E761_Ctrl::Initialize> CreateProperty(%s = %s), ReturnCode = %d",
				propName, E761_Ctrl::getConstString(STR_CtrlDesc).c_str(),
				ret);
		this->LogMessage(msg);
	}
	m_pInstance = this;
}

E761_Ctrl::~E761_Ctrl() {
	Shutdown();
}

int E761_Ctrl::OnBoardId(MM::PropertyBase* pProp, MM::ActionType eAct) {
	std::ostringstream osMessage;
	int ret = DEVICE_OK;

	osMessage.str("");
	if (eAct == MM::BeforeGet) {
		pProp->Set(m_boardId);
		if (m_pInstance->debugLogFlag()) {
			osMessage.str("");
			osMessage << "<E761_Ctrl::OnBoardId> BeforeGet("
					<< getInstance()->getConstString(STR_PROP_BOARDID).c_str()
					<< " = " << m_boardId << "), ReturnCode = " << ret;
			this->LogMessage(osMessage.str().c_str());
		}

	} else if (eAct == MM::AfterSet) {
		pProp->Get(m_boardId);

		if (m_pInstance->debugLogFlag()) {
			osMessage.str("");
			osMessage << "<E761_Ctrl::OnBoardId> AfterSet("
					<< getInstance()->getConstString(STR_PROP_BOARDID).c_str()
					<< " = " << m_boardId << "), ReturnCode = " << ret;
			this->LogMessage(osMessage.str().c_str());
		}
	}
	return ret;
}

void E761_Ctrl::getAxisName(char* px, char* py, char* pz) {
	if (px)
		*px = m_axisNames[0];
	if (py)
		*py = m_axisNames[1];
	if (pz)
		*pz = m_axisNames[2];
}

int E761_Ctrl::Initialize() {
	if (m_initialized)
		return DEVICE_OK;
	int ret;
	char msg[MM::MaxStrLength];

	int id = E7XX_ConnectPciBoard(1);
	if (id == -1)
		return id;
	m_devId = id;

	// Get axis names
	ret = E7XX_CST(m_devId, "", "ID-STAGE\nID-STAGE\nID-STAGE");
	if (!ret)
		return DEVICE_ERR;
	ret = E7XX_qSAI(m_devId, m_axisNames, sizeof(m_axisNames));
	if (!ret)
		return DEVICE_ERR;
	_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
			"<E761_Ctrl::Initialize> Axis names: %s", m_axisNames);
	this->LogMessage(msg);

	char propName[MM::MaxStrLength];

// Board Id
	sprintf(propName, getInstance()->getConstString(STR_PROP_BOARDID).c_str());
	char strBoardId[32];
	sprintf(strBoardId, "%d", m_boardId);
	CPropertyAction* pActOnBoardId = new CPropertyAction(this,
			&E761_Ctrl::OnBoardId);
	ret = CreateProperty(propName, strBoardId, MM::Integer, false,
			pActOnBoardId);
	if (m_pInstance->debugLogFlag()) {
		_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
				"<E761_Ctrl::Initialize> CreateProperty(%s = %d), ReturnCode = %d",
				propName, m_boardId, ret);
		this->LogMessage(msg);
	}

	ret = UpdateStatus();
	if (ret != DEVICE_OK)
		return ret;

	m_initialized = true;
	return ret;
}

int E761_Ctrl::Shutdown() {
	m_initialized = false;
	E7XX_CloseConnection(m_devId);
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
		return E761_ZStage::getInstance();
	else if (strcmp(deviceName,
			E761_Ctrl::getConstString(E761_Ctrl::STR_XYStageDevName).c_str())
			== 0)
		return E761_XYStage::getInstance();
	else if (strcmp(deviceName,
			E761_Ctrl::getConstString(E761_Ctrl::STR_CtrlDevName).c_str()) == 0)
		return E761_Ctrl::getInstance();

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
	string strZ = E761_Ctrl::getConstString(E761_Ctrl::STR_ZStageDevName).c_str();
	AddAvailableDeviceName(strZ.c_str(), strZ.c_str());
	string strCtrl =
			E761_Ctrl::getConstString(E761_Ctrl::STR_CtrlDevName).c_str();
	AddAvailableDeviceName(strCtrl.c_str(), strCtrl.c_str());
}

// windows DLL entry code
#ifdef WIN32
BOOL APIENTRY DllMain( HANDLE /*hModule*/,
		DWORD ul_reason_for_call,
		LPVOID /*lpReserved*/
)
{
	switch (ul_reason_for_call)
	{
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
	char msg[MM::MaxStrLength];

	// Name
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_NAME).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getInstance()->getConstString(
					E761_Ctrl::STR_XYStageDevName).c_str(), MM::String, true);
	if (E761_Ctrl::getInstance()->debugLogFlag()) {
		_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
				"<E761_XYStage::> CreateProperty(%s  %s), ReturnCode = %d\n",
				propName,
				E761_Ctrl::getInstance()->getConstString(
						E761_Ctrl::STR_XYStageDevName).c_str(), ret);
		this->LogMessage(msg);
	}

	// Description
	_snprintf(propName, MM::MaxStrLength,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_DESC).c_str());
	ret =
			CreateProperty(propName,
					E761_Ctrl::getInstance()->getConstString(
							E761_Ctrl::STR_XYStageDesc).c_str(), MM::String,
					true);
	if (E761_Ctrl::getInstance()->debugLogFlag()) {
		_snprintf(msg, MM::MaxStrLength,
				"<E761_XYStage::> CreateProperty(%s  %s), ReturnCode = %d\n",
				propName,
				E761_Ctrl::getInstance()->getConstString(
						E761_Ctrl::STR_XYStageDesc).c_str(), ret);
		this->LogMessage(msg);
	}
}

int E761_XYStage::Initialize() {
	if (m_initialized)
		return DEVICE_OK;

	// E761_Ctrl必须先初始化
	if (!E761_Ctrl::getInstance()->isInitialized())
		return DEVICE_NOT_CONNECTED;

	int ret = UpdateStatus();
	if (ret != DEVICE_OK)
		return ret;

	m_initialized = true;
	return DEVICE_OK;
}

int E761_XYStage::Shutdown() {
	m_initialized = false;
	return DEVICE_OK;
}

void E761_XYStage::GetName(char* name) const {
	CDeviceUtils::CopyLimitedString(name,
			E761_Ctrl::getConstString(
					E761_Ctrl::STR_XYStageDevName).c_str());
}

int E761_XYStage::SetPositionSteps(long lXPosSteps, long lYPosSteps) {
	double stepSizeX = GetStepSizeXUm();
	double stepSizeY = GetStepSizeYUm();
	double xUm = lXPosSteps * stepSizeX;
	double yUm = lYPosSteps * stepSizeY;

	double posUm[2] = { xUm, yUm };

	char axis[2];
	E761_Ctrl::getInstance()->getAxisName(axis, axis + 1, NULL);
	BOOL ret = E7XX_MOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm);
	if (!ret)
		return DEVICE_ERR;

	return DEVICE_OK;
}

int E761_XYStage::GetPositionSteps(long& x, long& y) {
	double stepSizeX = GetStepSizeXUm();
	double stepSizeY = GetStepSizeYUm();

	double posUm[2];
	char axis[2];
	E761_Ctrl::getInstance()->getAxisName(axis, axis + 1, NULL);
	BOOL ret = E7XX_qMOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm);
	if (!ret)
		return DEVICE_ERR;

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
	char msg[MM::MaxStrLength];

	// Name
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_NAME).c_str());
	ret = CreateProperty(propName,
			E761_Ctrl::getInstance()->getConstString(
					E761_Ctrl::STR_ZStageDevName).c_str(), MM::String, true);
	if (E761_Ctrl::getInstance()->debugLogFlag()) {
		_snprintf_s(msg, MM::MaxStrLength, _TRUNCATE,
				"<E761_XYStage::> CreateProperty(%s  %s), ReturnCode = %d\n",
				propName,
				E761_Ctrl::getInstance()->getConstString(
						E761_Ctrl::STR_ZStageDevName).c_str(), ret);
		this->LogMessage(msg);
	}

	// Description
	_snprintf(propName, MM::MaxStrLength,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_DESC).c_str());
	ret =
			CreateProperty(propName,
					E761_Ctrl::getInstance()->getConstString(
							E761_Ctrl::STR_ZStageDesc).c_str(), MM::String,
					true);
	if (E761_Ctrl::getInstance()->debugLogFlag()) {
		_snprintf(msg, MM::MaxStrLength,
				"<E761_XYStage::> CreateProperty(%s  %s), ReturnCode = %d\n",
				propName,
				E761_Ctrl::getInstance()->getConstString(
						E761_Ctrl::STR_ZStageDesc).c_str(), ret);
		this->LogMessage(msg);
	}
}

int E761_ZStage::Initialize() {
	if (m_initialized)
		return DEVICE_OK;
	int ret;

	// E761_Ctrl必须先初始化
	if (!E761_Ctrl::getInstance()->isInitialized())
		return DEVICE_NOT_CONNECTED;

	char propName[MM::MaxStrLength];
	char msg[MM::MaxStrLength];
	CPropertyAction* pActOnServoMode = new CPropertyAction(this,
			&E761_ZStage::OnServoMode);
	// Name
	_snprintf_s(propName, MM::MaxStrLength, _TRUNCATE,
			E761_Ctrl::getInstance()->getConstString(E761_Ctrl::STR_PROP_SERVO).c_str());
	ret = CreateProperty(propName, "True", MM::String, false, pActOnServoMode);
	AddAllowedValue(propName, "False");
	if (ret != DEVICE_OK)
		return ret;

	ret = UpdateStatus();
	if (ret != DEVICE_OK)
		return ret;

	m_initialized = true;
	return ret;
}

int E761_ZStage::OnServoMode(MM::PropertyBase* pProp, MM::ActionType eAct) {
	BOOL val[1];
	char axis[1];
	E761_Ctrl* pCtrl = E761_Ctrl::getInstance();
	pCtrl->getAxisName(NULL, NULL, axis);

	if (eAct == MM::BeforeGet) {
		BOOL ret = E7XX_qSVO(pCtrl->getDeviceId(), axis, val);
		if (!ret)
			return DEVICE_ERR;
		m_servoMode = val[0] ? true : false;
		pProp->Set(m_servoMode ? "True" : "False");
	} else if (eAct == MM::AfterSet) {
		string strVal;
		pProp->Get(strVal);
		m_servoMode = (strVal.compare("True") == 0 ? true : false);
		val[0] = m_servoMode;
		BOOL ret = E7XX_SVO(pCtrl->getDeviceId(), axis, val);
		if (!ret)
			return DEVICE_ERR;
	}
	return DEVICE_OK;
}

int E761_ZStage::Shutdown() {
	m_initialized = false;
	return DEVICE_OK;
}

void E761_ZStage::GetName(char* name) const {
	CDeviceUtils::CopyLimitedString(name,
			E761_Ctrl::getConstString(
					E761_Ctrl::STR_ZStageDevName).c_str());
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
	double posUm[1] = { steps * stepSizeUm };

	char axis[1];
	E761_Ctrl::getInstance()->getAxisName(NULL, NULL, axis);
	if (!E7XX_MOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm))
		return DEVICE_ERR;

	return DEVICE_OK;
}

int E761_ZStage::GetPositionSteps(long& steps) {
	double posUm[1];
	char axis[1];
	E761_Ctrl::getInstance()->getAxisName(NULL, NULL, axis);
	BOOL ret = E7XX_qMOV(E761_Ctrl::getInstance()->getDeviceId(), axis, posUm);
	if (!ret)
		return DEVICE_ERR;

	steps = (long) (posUm[0] / stepSizeUm + 0.5);
	return DEVICE_OK;
}

E761_ZStage::~E761_ZStage() {
	Shutdown();
}
