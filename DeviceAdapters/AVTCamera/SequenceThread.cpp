///////////////////////////////////////////////////////////////////////////////
// FILE:          SequenceThread.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters/MMCamera
//-----------------------------------------------------------------------------
// DESCRIPTION:   Impelements sequence thread for rendering live video.
//                Part of the skeleton code for the micro-manager camera adapter.
//                Use it as starting point for writing custom device adapters.
//                
// AUTHOR:        Nenad Amodaj, http://nenad.amodaj.com
//                
// COPYRIGHT:     University of California, San Francisco, 2011
//
// LICENSE:       This file is distributed under the BSD license.
//                License text is included with the source distribution.
//
//                This file is distributed in the hope that it will be useful,
//                but WITHOUT ANY WARRANTY; without even the implied warranty
//                of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//                IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//                CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//                INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//

#include "AVTCamera.h"


SequenceThread::SequenceThread(AVTCamera* pCam)
:intervalMs_(default_intervalMS)
,numImages_(default_numImages)
,imageCounter_(0)
,stop_(true)
,suspend_(false)
,camera_(pCam)
,startTime_(0)
,actualDuration_(0)
,lastFrameTime_(0)
{
};

SequenceThread::~SequenceThread() {};

void SequenceThread::Stop() {
	MMThreadGuard(this->stopLock_);
	stop_=true;
}

void SequenceThread::Start(long numImages, double intervalMs)
{
	MMThreadGuard(this->stopLock_);
	MMThreadGuard(this->suspendLock_);
	numImages_=numImages;
	intervalMs_=intervalMs;
	imageCounter_=0;
	stop_ = false;
	suspend_=false;
	activate();
	actualDuration_ = 0;
	startTime_= camera_->GetCurrentMMTime();
	lastFrameTime_ = 0;
}

bool SequenceThread::IsStopped(){
	MMThreadGuard(this->stopLock_);
	return stop_;
}

void SequenceThread::Suspend() {
	MMThreadGuard(this->suspendLock_);
	suspend_ = true;
}

bool SequenceThread::IsSuspended() {
	MMThreadGuard(this->suspendLock_);
	return suspend_;
}

void SequenceThread::Resume() {
	MMThreadGuard(this->suspendLock_);
	suspend_ = false;
}

int SequenceThread::svc(void) throw()
		{
	int ret=DEVICE_ERR;

	ret = camera_->cam.OpenCapture();
	if (ret != FCE_NOERROR)
		return ret ;
	ret = camera_->cam.StartDevice();
	if (ret != FCE_NOERROR)
		return ret;
	do
	{
		ret=camera_->ThreadRun(startTime_);
	} while (DEVICE_OK == ret && !IsStopped() && imageCounter_++ < numImages_-1);
	if (IsStopped())
		camera_->LogMessage("SeqAcquisition interrupted by the user\n");

	stop_=true;
	actualDuration_ = camera_->GetCurrentMMTime() - startTime_;
	camera_->OnThreadExiting();
	ret = camera_->cam.CloseCapture();
		if (ret != FCE_NOERROR)
			return ret ;
	return ret;
		}

