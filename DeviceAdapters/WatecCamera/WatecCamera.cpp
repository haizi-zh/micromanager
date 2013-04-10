// WatecCamera.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"
#include <Windows.h>
#include "WatecCamera.h"
#include "include/CGVidEx.h"
using namespace std;

/////////////////////////// START HERE /////////////////////////////////

// All the DVC error code will be returned plus this.
const int g_Err_Offset = 10000;

// External names used used by the rest of the system
// to load particular device from the "DemoCamera.dll" library
const char* g_WatecCameraDeviceName = "WatecCamera";

// singleton instance
WatecCamera* WatecCamera::instance_ = 0;
unsigned int WatecCamera::refCount_ = 0;

// global Andor driver thread lock
MMThreadLock g_WatecCamDriverLock;

// Properties
const char g_CameraName[] = "Camera Name";
const char g_DeInterlace[] = "De-interlace Algorithm";
const char g_Binning[] = "Binning";
const char g_Label[] = "Label";

///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////

/**
 * List all suppoerted hardware devices here
 * Do not discover devices at runtime.  To avoid warnings about missing DLLs, Micro-Manager
 * maintains a list of supported device (MMDeviceList.txt).  This list is generated using 
 * information supplied by this function, so runtime discovery will create problems.
 */
MODULE_API void InitializeModuleData()
{
	AddAvailableDeviceName(g_WatecCameraDeviceName, "Watec Guppy Camera");
}

MODULE_API MM::Device* CreateDevice(const char* deviceName)
{
	if (deviceName == 0)
		return 0;

	if (strcmp(deviceName, g_WatecCameraDeviceName) == 0)
		return WatecCamera::GetInstance();

	// ...supplied name not recognized
	return 0;
}

MODULE_API void DeleteDevice(MM::Device* pDevice)
{
	delete pDevice;
}

DriverGuard::DriverGuard(const WatecCamera * cam)
{
	g_WatecCamDriverLock.Lock();
}

DriverGuard::~DriverGuard()
{
	g_WatecCamDriverLock.Unlock();
}

WatecCamera::WatecCamera() :
				binSize_(1),
				depth_(8),
				initialized_(false),
				fullFrameX_(0),
				fullFrameY_(0),
				fullFrameBufferSize_(0),
				sequenceRunning_(false),
				timeout_(1000),
				deinterlace_(1)
{
	InitializeDefaultErrorMessages();
	// add custom messages
	SetErrorText(ERR_BUSY_ACQUIRING, "Camera Busy.  Stop camera activity first.");
	SetErrorText(ERR_NO_AVAIL_AMPS, "No available amplifiers.");
	SetErrorText(ERR_TRIGGER_NOT_SUPPORTED, "Trigger Not supported.");
	SetErrorText(ERR_INVALID_VSPEED, "Invalid Vertical Shift Speed.");
	SetErrorText(ERR_INVALID_PREAMPGAIN, "Invalid Pre-Amp Gain.");
	SetErrorText(ERR_CAMERA_DOES_NOT_EXIST, "No Camera Found.  Make sure it is connected and switched on, and try again.");
	SetErrorText(ERR_SOFTWARE_TRIGGER_IN_USE, "Only one camera can use software trigger.");

	// Find cameras

}

WatecCamera::~WatecCamera()
{
	DriverGuard dg(this);

	refCount_--;
	if (refCount_ == 0) {
		// release resources
		if (initialized_) {
			Shutdown();
		}
		 

		// clear the instance pointer
		instance_ = NULL;
	}
}

WatecCamera* WatecCamera::GetInstance()
{
	instance_ = new WatecCamera();

	refCount_++;
	return instance_;
}

int WatecCamera::Initialize()
{
	if (initialized_)
		return DEVICE_OK;

	unsigned long ret;
	status = BeginCGCard(1, &m_hcg);
	CG_VERIFY(status);
	//Initialize DH
	CGSetVideoStandard(m_hcg, PAL);
	//包括(0-6)YUV422/16、RGB888/24、RGB565/16、RGB555/15、RGB8888/32、ALL8BIT/8、LIMITED8BIT/8，
	CGSetVideoFormat(m_hcg,RGB8888);
	//扫描模式，包括 FRAME、FIELD
	CGSetScanMode(m_hcg, FRAME);
	/*
	 *	晶振，包括CRY_OSC_35M、CRY_OSC_28M
	 *	对于DH-CG300图像卡，一般为CRY_OSC_35M，对于DH-QP300图像卡，一般为CRY_OSC_28M，
	 *	其他类型图像卡没有此硬件设置，但可以调用此接口，并返回CG_NOT_SUPPORT_INTERFACE信息
	 */
	CGSelectCryOSC(m_hcg, CRY_OSC_35M);
	//设置视频源路，视频源路VIDEO_SOURCE包括视频类型和序号，
	VIDEO_SOURCE source;
	source.type		= COMPOSITE_VIDEO;
	source.nIndex	= 0;
	CGSetVideoSource(m_hcg, source);
	//input output
	CGSetInputWindow(m_hcg, 0, 0,fullFrameX_,fullFrameY_);
	CGSetOutputWindow(m_hcg, 0, 0,fullFrameX_,fullFrameY_);

	roi_.x = 0;
	roi_.y = 0;
	roi_.xSize = fullFrameX_;
	roi_.ySize = fullFrameY_;

	// Description
	if (!HasProperty(MM::g_Keyword_Description))
	{
		ret = CreateProperty(MM::g_Keyword_Description, "Watec Guppy camera adapter", MM::String, true);
		if (ret != DEVICE_OK)
			return ret;
	}

	// Camera name
	{
		char str[1024];
		//		assert(cam.GetDeviceName(str, 1024) == FCE_NOERROR);
		camName_ = str;
	}
	if (!HasProperty(g_CameraName))
	{
		ret = CreateProperty(g_CameraName, camName_.c_str(), MM::String, true);
		if (ret != DEVICE_OK)
			return ret;
	}

	// Dummy property for getPixelSizeUm
	if (!HasProperty(g_Label))
	{
		ret = CreateProperty(g_Label, "Dummy", MM::String, false);
		assert(ret == DEVICE_OK);
	}

	// De-interlace
	if (!HasProperty(g_DeInterlace))
	{
		CPropertyAction* pAct = new CPropertyAction(this, &WatecCamera::OnDeInterlace);
		ret = CreateProperty(g_DeInterlace, "0", MM::Integer, false, pAct);
		if (ret != DEVICE_OK)
			return ret;
	}

	if (!HasProperty(g_Binning))
	{
		CPropertyAction* pAct = new CPropertyAction(this, &WatecCamera::OnBinning);
		ret = CreateProperty(g_Binning, "", MM::Integer, false, pAct);
		if (ret != DEVICE_OK)
			return ret;
	}

	fullFrameBufferSize_ = fullFrameX_ * fullFrameY_ * (depth_ / 8 + 1) * 3;
	fullFrameBuffer_ = new unsigned char[fullFrameBufferSize_];
	ResizeImageBuffer();
	return DEVICE_OK;
}

int WatecCamera::ResizeImageBuffer()
{
	// resize internal buffers
	// NOTE: we are assuming 16-bit pixel type
	const int bpp = (int)ceil(depth_/8.0);
	img_.Resize(roi_.xSize / binSize_, roi_.ySize / binSize_, bpp);
	return DEVICE_OK;
}

/**
 * Deactivate the camera, reverse the initialization process.
 */
int WatecCamera::Shutdown()
{
	if (initialized_)
	{
		StopCamera();
		 
		delete fullFrameBuffer_;
	}

	initialized_ = false;
	return DEVICE_OK;
}

void WatecCamera::StopCamera()
{
	DriverGuard dg(this);
  
	 
		return;
}

//added to use RTA
/**
 * Acquires a single frame.
 * Micro-Manager expects that this function blocks the calling thread until the exposure phase is over.
 * This wait is implemented by sleeping ActualInterval_ms_ - ReadoutTime_ + 0.99 ms.
 * Note that this is likely not long enough when using internal triggering.
 */
int WatecCamera::SnapImage()
{
	DriverGuard dg(this);

	if (sequenceRunning_)   // If we are in the middle of a SequenceAcquisition
		return ERR_BUSY_ACQUIRING;
	CGSetVideoFormat(m_hcg,RGB8888);

	status = CGSnapShot(m_hcg, 0, 0, TRUE, 1);
	CG_VERIFY(status);

	DWORD dwImageSize =  fullFrameX_*fullFrameY_*4;
	HANDLE handle			= NULL;		//静态内存描述句柄
	BYTE *pStaticBuffer		= NULL;		//静态内存地址指针
	status = CGStaticMemLock(dwImageSize,dwImageSize, &handle, (VOID **)&pStaticBuffer);
	if (CG_SUCCESS(status)) {
		memcpy(fullFrameBuffer_, pStaticBuffer,dwImageSize);
	}
	CGStaticMemUnlock(handle);

	return DEVICE_OK;
}

void WatecCamera::GetName(char* name) const
{
	CDeviceUtils::CopyLimitedString(name, g_WatecCameraDeviceName);
}

const unsigned char* WatecCamera::GetImageBuffer()
{
	DriverGuard dg(this);

	return fullFrameBuffer_;
}

int WatecCamera::SetBinning(int bin)
{
	return DEVICE_OK;
}

void WatecCamera::SetExposure(double exp)
{
}

double WatecCamera::GetExposure() const
{
	DriverGuard dg(this);
	return 10;
}

int WatecCamera::SetROI(unsigned uX, unsigned uY, unsigned uXSize, unsigned uYSize)
{
	DriverGuard dg(this);
	if (Busy())
		return ERR_BUSY_ACQUIRING;

	//added to use RTA
	StopCamera();

	ROI oldRoi = roi_;

	uX = 0;
	uY = 0;
	roi_.x = uX * binSize_;
	roi_.y = uY * binSize_;
	roi_.xSize = uXSize * binSize_;
	roi_.ySize = uYSize * binSize_;

	if (roi_.x + roi_.xSize > fullFrameX_ || roi_.y + roi_.ySize > fullFrameY_)
	{
		roi_ = oldRoi;
		return ERR_INVALID_ROI;
	}

	// adjust image extent to conform to the bin size
	roi_.xSize -= roi_.xSize % binSize_;
	roi_.ySize -= roi_.ySize % binSize_;

 
	ResizeImageBuffer();
	OnPropertiesChanged();

	return DEVICE_OK;
}

int WatecCamera::GetROI(unsigned& uX, unsigned& uY, unsigned& uXSize, unsigned& uYSize)
{
	uX = roi_.x / binSize_;
	uY = roi_.y / binSize_;
	uXSize = roi_.xSize / binSize_;
	uYSize = roi_.ySize / binSize_;

	return DEVICE_OK;
}

int WatecCamera::ClearROI()
{
	DriverGuard dg(this);

 
	roi_.x = 0;
	roi_.y = 0;
 

	return SetROI(roi_.x, roi_.y, roi_.xSize, roi_.ySize);
}

int WatecCamera::OnDeInterlace(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		long tmp = deinterlace_;
		pProp->Set(tmp);
	}
	else if (eAct == MM::AfterSet)
	{
		long tmp;
		pProp->Get(tmp);
		deinterlace_ = tmp;
	}
	return DEVICE_OK;
}

int WatecCamera::OnBinning(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		long tmp = binSize_;
		pProp->Set(tmp);
	}
	else if (eAct == MM::AfterSet)
	{
		long tmp;
		pProp->Get(tmp);
		binSize_ = tmp;
	}
	return DEVICE_OK;
}
