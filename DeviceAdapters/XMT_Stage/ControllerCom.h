#ifndef _XMTStage_CONTROLLER_H_
#define _XMTStage_CONTROLLER_H_

#include "../../MMDevice/MMDevice.h"
#include "../../MMDevice/DeviceBase.h"

class ControllerComDevice : public CStageBase<ControllerComDevice>
{
public:
	ControllerComDevice();
	~ControllerComDevice();

	// Device API
	// ----------
	int Initialize();
	int Shutdown();

	void SetFactor_UmToDefaultUnit(double dUmToDefaultUnit, bool bHideProperty = true);

	void CreateProperties();

	static const char* DeviceName_;
	static const char* UmToDefaultUnitName_;
	void GetName(char* pszName) const;
	bool Busy();


	int OnPort(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnUmInDefaultUnit(MM::PropertyBase* pProp, MM::ActionType eAct);

	bool CommandWithAnswer(const byte* cmd,int len, BYTE* answer, int nExpectedLines = -1);
	bool SendCOMCommand(const byte* command,int len);
	bool ReadCOMAnswer(BYTE* answer, int nExpectedLines = -1);
	int GetLastError() const { return lastError_; }

	double umToDefaultUnit_;


	// Stage API
	// ---------
	float RawToFloat(byte* rawData,int offset);
	void  FloatToRaw(float value,byte* rawData);
	void PackageCommand(const char* cmd,byte* data,byte * buf);
	byte checkSumCalc(byte* data,int offset,int count);
	int SetPositionUm(double pos);
	int GetPositionUm(double& pos);
	int SetPositionSteps(long steps);
	int GetPositionSteps(long& steps);
	int SetOrigin();
	int GetLimits(double& min, double& max);

	// action interface
	// ----------------
	int OnStepSizeUm(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnAxisLimit(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnPosition(MM::PropertyBase* pProp, MM::ActionType eAct);
	int OnVelocity(MM::PropertyBase* pProp, MM::ActionType eAct);

	// Sequence functions
	int IsStageSequenceable(bool& isSequenceable) const {isSequenceable = false; return DEVICE_OK;}
	int GetStageSequenceMaxLength(long& nrEvents) const  {nrEvents = 0; return DEVICE_OK;}
	int StartStageSequence() {return DEVICE_OK;}
	int StopStageSequence() {return DEVICE_OK;}
	int LoadStageSequence(std::vector<double> positions) {return DEVICE_OK;}
	bool IsContinuousFocusDrive() const {return false;}

private:
	std::string port_;
	int lastError_;
	bool initialized_;
	bool bShowProperty_UmToDefaultUnit_;
	bool needConnectFirst_;
	double stepSizeUm_;
	double axisLimitUm_;
};
#endif //_XMTStage_CONTROLLER_H_
