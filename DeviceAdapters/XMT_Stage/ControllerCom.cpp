#ifdef WIN32
#include <windows.h>
#define snprintf _snprintf
#endif

#include "ControllerCom.h"
#include "XMT.h"
#include "../../MMDevice/ModuleInterface.h"
#include <string>
#include <map>
#include <algorithm>
using namespace std;

const char* ControllerComDevice::DeviceName_ = "XMTStageController";
const char* ControllerComDevice::UmToDefaultUnitName_ = "um in default unit";
const char* g_PI_ZStageAxisLimitUm = "Limit_um";
const char* g_PI_ZStageStepSize = "StepSizeUm";

ControllerComDevice::ControllerComDevice()
: port_(""),
  initialized_(false),
  lastError_(DEVICE_OK),
  umToDefaultUnit_(1),
  bShowProperty_UmToDefaultUnit_(false),
  needConnectFirst_(false),
  stepSizeUm_(10),
  axisLimitUm_(100)
{
	InitializeDefaultErrorMessages();
}

ControllerComDevice::~ControllerComDevice()
{
	Shutdown();
}

void ControllerComDevice::SetFactor_UmToDefaultUnit(double dUmToDefaultUnit, bool bHideProperty)
{
	umToDefaultUnit_ = dUmToDefaultUnit;
	if (bHideProperty)
	{
		bShowProperty_UmToDefaultUnit_ = false;
	}

}

void ControllerComDevice::CreateProperties()
{
	// create pre-initialization properties
	// ------------------------------------

	// Name
	CreateProperty(MM::g_Keyword_Name, DeviceName_, MM::String, true);

	// Description
	CreateProperty(MM::g_Keyword_Description, "XMTCOM Adapter", MM::String, true);

	CPropertyAction* pAct;

	// Port
	pAct = new CPropertyAction (this, &ControllerComDevice::OnPort);
	CreateProperty(MM::g_Keyword_Port, "Undefined", MM::String, false, pAct, true);

	if (bShowProperty_UmToDefaultUnit_)
	{
		// axis limit in um
		pAct = new CPropertyAction (this, &ControllerComDevice::OnUmInDefaultUnit);
		CreateProperty(ControllerComDevice::UmToDefaultUnitName_, "0.001", MM::Float, false, pAct, true);
	}

	CreateProperty(MM::g_Keyword_Name, DeviceName_, MM::String, true);

	// Description
	CreateProperty(MM::g_Keyword_Description, "XMTZStage Adapter", MM::String, true);

	// axis limit in um
	pAct = new CPropertyAction (this, &ControllerComDevice::OnAxisLimit);
	CreateProperty(g_PI_ZStageAxisLimitUm, "100.0", MM::Float, false, pAct, true);

	// axis limits (assumed symmetrical)
	pAct = new CPropertyAction (this, &ControllerComDevice::OnPosition);
	CreateProperty(MM::g_Keyword_Position, "0.0", MM::Float, false, pAct);
	pAct = new CPropertyAction (this, &ControllerComDevice::OnWorkMode);
	CreateProperty("WorkMode", "High", MM::String, false, pAct);
	vector<string> WorkModeValues;
	WorkModeValues.push_back("High");
	WorkModeValues.push_back("Low");
	SetAllowedValues("WorkMode", WorkModeValues);
}
int ControllerComDevice::OnWorkMode(MM::PropertyBase* pProp, MM::ActionType eAct){
	if (eAct == MM::BeforeGet)
	{
	}
	else if (eAct == MM::AfterSet)
	{
		if (initialized_)
		{
			string Mode;
			pProp->Get(Mode);
		byte buf[9];
		if (Mode.compare("High") == 0)
		{
			PackageCommand("TQH",NULL,buf);
	SendCOMCommand(buf,9);
		}
		if (Mode.compare("Low") == 0)
		{
			PackageCommand("TQL",NULL,buf);
	SendCOMCommand(buf,9);
		}
		}
	}

	return DEVICE_OK;
}
int ControllerComDevice::OnPort(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		pProp->Set(port_.c_str());
	}
	else if (eAct == MM::AfterSet)
	{
		if (initialized_)
		{
			// revert
			pProp->Set(port_.c_str());
			return ERR_PORT_CHANGE_FORBIDDEN;
		}

		pProp->Get(port_);
	}

	return DEVICE_OK;
}

int ControllerComDevice::OnUmInDefaultUnit(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		pProp->Set(umToDefaultUnit_);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(umToDefaultUnit_);
	}

	return DEVICE_OK;
}


int ControllerComDevice::Initialize()
{
	if (initialized_)
		return DEVICE_OK;
	byte buf[9];
	byte answer[8];
	PackageCommand("ABC",NULL,buf);
	SendCOMCommand(buf,9);
	PackageCommand("WEA",NULL,buf);
	SendCOMCommand(buf,9);
	PackageCommand("TQH",NULL,buf);
	SendCOMCommand(buf,9);
	PackageCommand("RB1",NULL,buf);
	if(!CommandWithAnswer(buf,9,answer,7)){
		initialized_ = false;
		return DEVICE_ERR;
	}
	initialized_ = true;
	return DEVICE_OK;
}

int ControllerComDevice::Shutdown()
{
	if (!initialized_)
		return DEVICE_OK;
	initialized_ = false;
	return DEVICE_OK;
}

bool ControllerComDevice::Busy()
{
	return false;
}

void ControllerComDevice::GetName(char* Name) const
{
	CDeviceUtils::CopyLimitedString(Name, DeviceName_);
}


bool ControllerComDevice::SendCOMCommand(const byte * command,int len)
{
	int ret = DEVICE_OK;
	int yTimeout = false;
	double m_nAnswerTimeoutMs = 1500;
	unsigned long lStartTime = GetClockTicksUs();
	for(int i=0;i<len && ret == DEVICE_OK;i++){
		ret = WriteToComPort(port_.c_str(), &command[i], 1);
		if(ret != DEVICE_OK){
			yTimeout = ((double)(GetClockTicksUs() - lStartTime) / 10000.) >  m_nAnswerTimeoutMs;
			if (!yTimeout) CDeviceUtils::SleepMs(3);
			continue;
		}
		if(yTimeout)
			return false;
	}
	return true;
}

bool ControllerComDevice::CommandWithAnswer(const byte* command,int len, BYTE* answer, int nExpectedLen)
{
	if (!SendCOMCommand(command,len))
		return DEVICE_ERR;
	return ReadCOMAnswer(answer, nExpectedLen);
}

bool ControllerComDevice::ReadCOMAnswer(BYTE* answer, int nExpectedLines)
{
	int i = 0;
	std::string rec;
	bool dataReady = false;
	int yTimeout = false;

	double m_nAnswerTimeoutMs = 1500;
	unsigned long lStartTime = GetClockTicksUs();
	while(!yTimeout && i<nExpectedLines){
		int ret = GetSerialAnswer(port_.c_str(),"",rec);
		if(rec.c_str()[0] == '@'){
			dataReady = true;
		}
		if(!dataReady){
			yTimeout = ((double)(GetClockTicksUs() - lStartTime) / 10000.) >  m_nAnswerTimeoutMs;
			if (!yTimeout) CDeviceUtils::SleepMs(3);
			continue;
		}
		else{
			answer[i++] = (byte)rec.c_str()[0];
		}
	}
	if(yTimeout)
		return false;
	if(checkSumCalc(answer,0,6) != answer[6])
		return false;
	answer[7] = '\0';
	return true;

}

int ControllerComDevice::SetPositionSteps(long steps)
{
	double pos = steps * stepSizeUm_;
	return SetPositionUm(pos);
}

int ControllerComDevice::GetPositionSteps(long& steps)
{
	return DEVICE_OK;
	double pos;
	int ret = GetPositionUm(pos);
	if (ret != DEVICE_OK)
		return ret;
	steps = (long) ((pos / stepSizeUm_) + 0.5);
	return DEVICE_OK;
}

int ControllerComDevice::SetPositionUm(double pos)
{
	byte rawData[4];
	byte buf[10];
	FloatToRaw((float)pos,rawData);
	byte cmd[3];
	cmd[0] = 'T';
	cmd[1] = (char)0;
	cmd[2] = 'S';
	PackageCommand((const char*)cmd,rawData,buf);
	return SendCOMCommand(buf,9)?DEVICE_OK:DEVICE_ERR;
}

int ControllerComDevice::GetPositionUm(double& pos)
{
	byte buf[9];
	byte answer[8];
	PackageCommand("RB1",NULL,buf);
	return;
	;
	if(!CommandWithAnswer(buf,9,answer,7))
		return DEVICE_ERR;
	pos = (double)RawToFloat(answer,2);
	return DEVICE_OK;
}

int ControllerComDevice::SetOrigin()
{
	return DEVICE_UNSUPPORTED_COMMAND;
}

int ControllerComDevice::GetLimits(double& min, double& max)
{
	min = 0;
	max = axisLimitUm_;
	return DEVICE_OK;
	return DEVICE_UNSUPPORTED_COMMAND;
}

///////////////////////////////////////////////////////////////////////////////
// Action handlers
///////////////////////////////////////////////////////////////////////////////

int ControllerComDevice::OnStepSizeUm(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		pProp->Set(stepSizeUm_);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(stepSizeUm_);
	}

	return DEVICE_OK;
}

int ControllerComDevice::OnAxisLimit(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		pProp->Set(axisLimitUm_);
		SetPropertyLimits(MM::g_Keyword_Position, 0/*-axisLimitUm_*/, axisLimitUm_);
	}
	else if (eAct == MM::AfterSet)
	{
		pProp->Get(axisLimitUm_);
	}

	return DEVICE_OK;
}

int ControllerComDevice::OnPosition(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		if (!initialized_)
		{
			pProp->Set(0.0);
			return DEVICE_OK;
		}
		double pos;
		int ret = GetPositionUm(pos);
		if (ret != DEVICE_OK)
			return ret;

		pProp->Set(pos);
	}
	else if (eAct == MM::AfterSet)
	{
		if (!initialized_)
		{
			return DEVICE_OK;
		}
		double pos;
		pProp->Get(pos);
		int ret = SetPositionUm(pos);
		if (ret != DEVICE_OK)
			return ret;
		pProp->Get(pos);
	}

	return DEVICE_OK;
}

int ControllerComDevice::OnVelocity(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		double velocity = 0.0;
		pProp->Set(velocity);
		pProp->Set(0.0);
	}
	else if (eAct == MM::AfterSet)
	{
		double velocity = 0.0;
		pProp->Get(velocity);
	}

	return DEVICE_OK;
}

void ControllerComDevice::PackageCommand(const char* cmd,byte* data,byte* buf)
{
	//CMD
	buf[0] = '@';
	buf[1] = (byte)cmd[0];
	buf[2] = (byte)cmd[1];
	buf[3] = (byte)cmd[2];
	//data
	if (data == NULL){
		buf[4] = 'X';
		buf[5] = 'X';
		buf[6] = 'X';
		buf[7] = 'X';
	}
	else{
		buf[4] = data[0];
		buf[5] = data[1];
		buf[6] = data[2];
		buf[7] = data[3];
	}
	//checkSum
	buf[8] = checkSumCalc(buf, 0, 8);
}

float  ControllerComDevice::RawToFloat(byte* rawData,int offset)
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
void ControllerComDevice::FloatToRaw(float val,byte* rawData)
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
byte  ControllerComDevice::checkSumCalc(byte* data,int offset,int count)
{
	byte checksum = data[offset];
	for (int i = offset + 1; i < offset + count; ++i)
	{
		checksum = (byte)(checksum ^ data[i]);
	}
	return checksum;

}
