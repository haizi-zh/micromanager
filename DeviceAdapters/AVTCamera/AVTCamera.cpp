// AVTCamera.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"
#include <Windows.h>
#include "AVTCamera.h"

using namespace std;

/////////////////////////// START HERE /////////////////////////////////

// All the DVC error code will be returned plus this.
const int g_Err_Offset = 10000;

// External names used used by the rest of the system
// to load particular device from the "DemoCamera.dll" library
const char* g_AVTCameraDeviceName = "AVTCamera";

// singleton instance
AVTCamera* AVTCamera::instance_ = 0;
unsigned int AVTCamera::refCount_ = 0;

// global Andor driver thread lock
MMThreadLock g_AVTCamDriverLock;

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
	AddAvailableDeviceName(g_AVTCameraDeviceName, "AVT Guppy Camera");
}

MODULE_API MM::Device* CreateDevice(const char* deviceName)
{
	if (deviceName == 0)
		return 0;

	if (strcmp(deviceName, g_AVTCameraDeviceName) == 0)
		return AVTCamera::GetInstance();

	// ...supplied name not recognized
	return 0;
}

MODULE_API void DeleteDevice(MM::Device* pDevice)
{
	delete pDevice;
}

DriverGuard::DriverGuard(const AVTCamera * cam)
{
	g_AVTCamDriverLock.Lock();
}

DriverGuard::~DriverGuard()
{
	g_AVTCamDriverLock.Unlock();
}

AVTCamera::AVTCamera() :
				binSize_(1),
				depth_(8),
				initialized_(false),
				fullFrameX_(0),
				fullFrameY_(0),
				fullFrameBufferSize_(0),
				sequenceRunning_(false),
				timeout_(1000),
				thd_(0),
				exposure_(20),
				deinterlace_(0)
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
	thd_ = new SequenceThread(this);

}

AVTCamera::~AVTCamera()
{
	DriverGuard dg(this);

	refCount_--;
	if (refCount_ == 0) {
		// release resources
		if (initialized_) {
			Shutdown();
		}
		FGExitModule();

		// clear the instance pointer
		instance_ = NULL;
	}
	delete thd_;
}

AVTCamera* AVTCamera::GetInstance()
{
	instance_ = new AVTCamera();

	refCount_++;
	return instance_;
}

int AVTCamera::Initialize()
{
	if (initialized_)
		return DEVICE_OK;

	unsigned long ret;
	FGInitModule(NULL);
	initialized_ = true;

	// Find camera
	FGNODEINFO nodeInfo[3];
	unsigned long nodeCnt;
	ret = FGGetNodeList(nodeInfo, 3, &nodeCnt);
	if (ret != FCE_NOERROR || nodeCnt < 1)
		return ret + g_Err_Offset;

	assert(cam.Connect(&nodeInfo[0].Guid) == FCE_NOERROR);

	FGPINFO info;
	//	assert(cam.SetParameter(FGP_IMAGEFORMAT, MAKEIMAGEFORMAT(RES_SCALABLE, CM_Y8, 0)) == FCE_NOERROR);
	assert(cam.SetParameter(FGP_IMAGEFORMAT, MAKEDCAMFORMAT(7,2, CM_Y8)) == FCE_NOERROR);
	assert(cam.SetParameter(FGP_XPOSITION, 0) == FCE_NOERROR);
	assert(cam.SetParameter(FGP_YPOSITION, 0) == FCE_NOERROR);
	assert(cam.GetParameterInfo(FGP_XSIZE, &info) == FCE_NOERROR);
	assert(cam.SetParameter(FGP_XSIZE, info.MaxValue) == FCE_NOERROR);
	fullFrameX_ = info.MaxValue;
	assert(cam.GetParameterInfo(FGP_YSIZE, &info) == FCE_NOERROR);
	assert(cam.SetParameter(FGP_YSIZE, info.MaxValue) == FCE_NOERROR);
	fullFrameY_ = info.MaxValue;
	roi_.x = 0;
	roi_.y = 0;
	roi_.xSize = fullFrameX_;
	roi_.ySize = fullFrameY_;

	// Description
	if (!HasProperty(MM::g_Keyword_Description))
	{
		ret = CreateProperty(MM::g_Keyword_Description, "AVT Guppy camera adapter", MM::String, true);
		if (ret != DEVICE_OK)
			return ret;
	}

	// Camera name
	{
		char str[1024];
		assert(cam.GetDeviceName(str, 1024) == FCE_NOERROR);
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
		CPropertyAction* pAct = new CPropertyAction(this, &AVTCamera::OnDeInterlace);
		ret = CreateProperty(g_DeInterlace, "0", MM::Integer, false, pAct);
		if (ret != DEVICE_OK)
			return ret;
	}

	if (!HasProperty(g_Binning))
	{
		CPropertyAction* pAct = new CPropertyAction(this, &AVTCamera::OnBinning);
		ret = CreateProperty(g_Binning, "", MM::Integer, false, pAct);
		if (ret != DEVICE_OK)
			return ret;
	}

	fullFrameBufferSize_ = fullFrameX_ * fullFrameY_ * (depth_ / 8 + 1) * 3;
	fullFrameBuffer_ = new unsigned char[fullFrameBufferSize_];
	ResizeImageBuffer();
	return DEVICE_OK;
}

int AVTCamera::ResizeImageBuffer()
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
int AVTCamera::Shutdown()
{
	if (initialized_)
	{
		StopCamera();
		cam.Disconnect();

		delete fullFrameBuffer_;
	}

	initialized_ = false;
	return DEVICE_OK;
}

void AVTCamera::StopCamera()
{
	DriverGuard dg(this);
	unsigned long ret = cam.StopDevice();
	if (ret != FCE_NOERROR)
		return;
	ret = cam.CloseCapture();
	if (ret != FCE_NOERROR)
		return;
}

//added to use RTA
/**
 * Acquires a single frame.
 * Micro-Manager expects that this function blocks the calling thread until the exposure phase is over.
 * This wait is implemented by sleeping ActualInterval_ms_ - ReadoutTime_ + 0.99 ms.
 * Note that this is likely not long enough when using internal triggering.
 */
int AVTCamera::SnapImage()
{
	DriverGuard dg(this);

	if (sequenceRunning_)   // If we are in the middle of a SequenceAcquisition
		return ERR_BUSY_ACQUIRING;

	unsigned long ret = cam.OpenCapture();
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;
	ret = cam.StartDevice();
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;

	FGFRAME frame;
	ret = cam.GetFrame(&frame, timeout_);
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;

	int bpp = GetImageBytesPerPixel();
	int w = GetImageWidth();
	int h = GetImageHeight();
	switch (deinterlace_)
	{
	case 0:
		memcpy_s(fullFrameBuffer_, fullFrameBufferSize_, frame.pData, frame.Length);
		break;
	case 1:
		for (int i=0; i<GetImageHeight(); i++)
		{
			if (i % 2 == 0)
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + (i/2) * w, w);
			}
			else
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + ((i-1)/2 + h/2) * w, w);
			}
		}
		break;
	case 2:
		for (int i=0; i<GetImageHeight(); i++)
		{
			if (i % 2 == 0)
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + (i/2) * w, w);
			}
			else
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + ((i+1)/2 + h/2) * w, w);
			}
		}
		break;
	default:
		return ERR_INCOMPLETE_SNAP_IMAGE_CYCLE;
	}

	ret = cam.PutFrame(&frame);
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;

	ret = cam.CloseCapture();
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;

	return DEVICE_OK;
}

void AVTCamera::GetName(char* name) const 
{
	CDeviceUtils::CopyLimitedString(name, g_AVTCameraDeviceName);
}

const unsigned char* AVTCamera::GetImageBuffer()
{
	DriverGuard dg(this);

	return fullFrameBuffer_;
}

int AVTCamera::SetBinning(int bin)
{
	return DEVICE_OK;
}

void AVTCamera::SetExposure(double exp)
{
	exposure_ = exp;
}

double AVTCamera::GetExposure() const
{
	DriverGuard dg(this);
	return exposure_;
}

int AVTCamera::SetROI(unsigned uX, unsigned uY, unsigned uXSize, unsigned uYSize)
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

	unsigned long ret = cam.SetParameter(FGP_XPOSITION, roi_.x);
	if (ret != FCE_NOERROR)
	{
		roi_ = oldRoi;
		return ret + g_Err_Offset;
	}
	ret = cam.SetParameter(FGP_YPOSITION, roi_.y);
	if (ret != FCE_NOERROR)
	{
		roi_ = oldRoi;
		return ret + g_Err_Offset;
	}
	ret = cam.SetParameter(FGP_XSIZE, roi_.xSize);
	if (ret != FCE_NOERROR)
	{
		roi_ = oldRoi;
		return ret + g_Err_Offset;
	}
	ret = cam.SetParameter(FGP_YSIZE, roi_.ySize);
	if (ret != FCE_NOERROR)
	{
		roi_ = oldRoi;
		return ret + g_Err_Offset;
	}

	ResizeImageBuffer();
	OnPropertiesChanged();

	return DEVICE_OK;
}

int AVTCamera::GetROI(unsigned& uX, unsigned& uY, unsigned& uXSize, unsigned& uYSize)
{
	uX = roi_.x / binSize_;
	uY = roi_.y / binSize_;
	uXSize = roi_.xSize / binSize_;
	uYSize = roi_.ySize / binSize_;

	return DEVICE_OK;
}

int AVTCamera::ClearROI()
{
	DriverGuard dg(this);

	FGPINFO info;
	roi_.x = 0;
	roi_.y = 0;
	unsigned ret = cam.GetParameterInfo(FGP_XSIZE, &info);
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;
	roi_.xSize = (int)info.MaxValue;
	ret = cam.GetParameterInfo(FGP_YSIZE, &info);
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;
	roi_.ySize = (int)info.MaxValue;

	return SetROI(roi_.x, roi_.y, roi_.xSize, roi_.ySize);
}

int AVTCamera::OnDeInterlace(MM::PropertyBase* pProp, MM::ActionType eAct)
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

int AVTCamera::OnBinning(MM::PropertyBase* pProp, MM::ActionType eAct)
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

int AVTCamera::PrepareSequenceAcqusition()
{
	if (IsCapturing())
		return DEVICE_CAMERA_BUSY_ACQUIRING;

	int ret = GetCoreCallback()->PrepareForAcq(this);
	if (ret != DEVICE_OK)
		return ret;

	return DEVICE_OK;
}


/**
 * Required by the MM::Camera API
 * Please implement this yourself and do not rely on the base class implementation
 * The Base class implementation is deprecated and will be removed shortly
 */
int AVTCamera::StartSequenceAcquisition(double interval) {

	return StartSequenceAcquisition(LONG_MAX, interval, false);
}

/**
 * Stop and wait for the Sequence thread finished
 */
int AVTCamera::StopSequenceAcquisition()
{
	if (!thd_->IsStopped()) {
		thd_->Stop();
		thd_->wait();
	}

	return DEVICE_OK;
}

/**
 * Simple implementation of Sequence Acquisition
 * A sequence acquisition should run on its own thread and transport new images
 * coming of the camera into the MMCore circular buffer.
 */
int AVTCamera::StartSequenceAcquisition(long numImages, double interval_ms, bool stopOnOverflow)
{
	if (IsCapturing())
		return DEVICE_CAMERA_BUSY_ACQUIRING;

	int ret = GetCoreCallback()->PrepareForAcq(this);
	if (ret != DEVICE_OK)
		return ret;
	thd_->Start(numImages, interval_ms);
	return DEVICE_OK;
}

/*
 * Inserts Image and MetaData into MMCore circular Buffer
 */
int AVTCamera::InsertImage()
{

	MM::MMTime timeStamp = this->GetCurrentMMTime();
	char label[MM::MaxStrLength];
	this->GetLabel(label);

	// Important:  metadata about the image are generated here:
	Metadata md;
	md.put("Camera", label);
	md.put(MM::g_Keyword_Metadata_StartTime, CDeviceUtils::ConvertToString(sequenceStartTime_.getMsec()));
	md.put(MM::g_Keyword_Elapsed_Time_ms, CDeviceUtils::ConvertToString((timeStamp - sequenceStartTime_).getMsec()));
	md.put(MM::g_Keyword_Metadata_ROI_X, CDeviceUtils::ConvertToString( (long)roi_.x));
	md.put(MM::g_Keyword_Metadata_ROI_Y, CDeviceUtils::ConvertToString( (long) roi_.y));

	imageCounter_++;

	char buf[MM::MaxStrLength];
	GetProperty(MM::g_Keyword_Binning, buf);
	md.put(MM::g_Keyword_Binning, buf);


	const unsigned char* pI;
	pI = GetImageBuffer();

	unsigned int w = GetImageWidth();
	unsigned int h = GetImageHeight();
	unsigned int b = GetImageBytesPerPixel();

	int ret = GetCoreCallback()->InsertImage(this, pI, w, h, b, md.Serialize().c_str());
	if (!stopOnOverflow_ && ret == DEVICE_BUFFER_OVERFLOW)
	{
		// do not stop on overflow - just reset the buffer
		GetCoreCallback()->ClearImageBuffer(this);
		// don't process this same image again...
		return GetCoreCallback()->InsertImage(this, pI, w, h, b, md.Serialize().c_str(), false);
	} else
		return ret;
}



/*
 * Do actual capturing
 * Called from inside the thread
 */

bool AVTCamera::IsCapturing() {
	return !thd_->IsStopped();
}
int  AVTCamera::ThreadRun (MM::MMTime startTime)
{
	int ret=DEVICE_ERR;
	GenerateImage(img_, GetSequenceExposure());
	return  InsertImage();
};
int AVTCamera::GenerateImage(ImgBuffer& img, double exp)
{
	static int index = 0;
	if(index>img_.Width()*img_.Height()-1)
		index = 0;
	//std::string pixelType;
	char buf[MM::MaxStrLength];
	GetProperty(MM::g_Keyword_PixelType, buf);
	std::string pixelType(buf);

	if (img_.Height() == 0 || img_.Width() == 0 || img_.Depth() == 0)
		return DEVICE_ERR;

	unsigned char* pBuf = fullFrameBuffer_;
	FGFRAME frame;
	int ret = cam.GetFrame(&frame,exp);
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;

	int bpp = GetImageBytesPerPixel();
	int w = GetImageWidth();
	int h = GetImageHeight();
	switch (deinterlace_)
	{
	case 0:
		memcpy_s(fullFrameBuffer_, fullFrameBufferSize_, frame.pData, frame.Length);
		break;
	case 1:
		for (int i=0; i<GetImageHeight(); i++)
		{
			if (i % 2 == 0)
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + (i/2) * w, w);
			}
			else
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + ((i-1)/2 + h/2) * w, w);
			}
		}
		break;
	case 2:
		for (int i=0; i<GetImageHeight(); i++)
		{
			if (i % 2 == 0)
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + (i/2) * w, w);
			}
			else
			{
				memcpy_s(fullFrameBuffer_ + i * w, fullFrameBufferSize_, frame.pData + ((i+1)/2 + h/2) * w, w);
			}
		}
		break;
	default:
		return ERR_INCOMPLETE_SNAP_IMAGE_CYCLE;
	}

	ret = cam.PutFrame(&frame);
	if (ret != FCE_NOERROR)
		return ret + g_Err_Offset;
	return DEVICE_OK;
}
double AVTCamera::GetSequenceExposure()
{
	if (exposureSequence_.size() == 0)
		return this->GetExposure();
	if (sequenceIndex_ > exposureSequence_.size() - 1)
		sequenceIndex_ = 0;
	double exposure = exposureSequence_[sequenceIndex_];
	sequenceIndex_++;
	if (sequenceIndex_ >= exposureSequence_.size())
	{
		sequenceIndex_ = 0;
	}
	return exposure;
}
