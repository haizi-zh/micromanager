///////////////////////////////////////////////////////////////////////////////
// FILE:          MMCamera.h
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   Skeleton code for the micro-manager camera adapter. Use it as
//                starting point for writing custom device adapters
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

#ifndef _MMCAMERA_H_
#define _MMCAMERA_H_

#include "../../MMDevice/DeviceBase.h"
#include "../../MMDevice/DeviceThreads.h"
#include "../../MMDevice/ImgBuffer.h"

#include "../../MMDevice/DeviceUtils.h"
//////////////////////////////////////////////////////////////////////////////
// Error codes
//
#define ERR_UNKNOWN_MODE         102

class  SequenceThread;

class TemplateNDGCamera : public CCameraBase<TemplateNDGCamera>  
{
public:
   TemplateNDGCamera();
   ~TemplateNDGCamera();
  
   // MMDevice API
   // ------------
   int Initialize();
   int Shutdown();
  
   void GetName(char* name) const;      
   double GetSequenceExposure();
   std::vector<double> exposureSequence_;
   MM::MMTime sequenceStartTime_;
   bool isSequenceable_;
   long sequenceMaxLength_;
   bool sequenceRunning_;
   long sequenceIndex_;
   long imageCounter_;
   int ThreadRun(MM::MMTime startTime);
   void GenerateImage(ImgBuffer& img, double exp);
   // MMCamera API
   // ------------
   int SnapImage();
   const unsigned char* GetImageBuffer();
   unsigned GetImageWidth() const;
   unsigned GetImageHeight() const;
   unsigned GetImageBytesPerPixel() const;
   unsigned GetBitDepth() const;
   long GetImageBufferSize() const;
   double GetExposure() const;
   void SetExposure(double exp);
   int SetROI(unsigned x, unsigned y, unsigned xSize, unsigned ySize); 
   int GetROI(unsigned& x, unsigned& y, unsigned& xSize, unsigned& ySize); 
   int ClearROI();
   int PrepareSequenceAcqusition();
   int StartSequenceAcquisition(double interval);
   int StartSequenceAcquisition(long numImages, double interval_ms, bool stopOnOverflow);
   int StopSequenceAcquisition();
   bool IsCapturing();
   int GetBinning() const;
   int SetBinning(int binSize);
   int IsExposureSequenceable(bool& seq) const {seq = false; return DEVICE_OK;}

   // action interface
   // ----------------
   int OnBinning(MM::PropertyBase* pProp, MM::ActionType eAct);
   int OnPixelType(MM::PropertyBase* pProp, MM::ActionType eAct);
   int OnGain(MM::PropertyBase* pProp, MM::ActionType eAct);

private:
   friend class  SequenceThread;
   static const int IMAGE_WIDTH = 720;
   static const int IMAGE_HEIGHT = 480;
   static const int MAX_BIT_DEPTH = 12;

    SequenceThread* thd_;
   int binning_;
   int bytesPerPixel_;
   double gain_;
   double exposureMs_;
   bool initialized_;
   ImgBuffer img_;
   int roiX_, roiY_;

   int ResizeImageBuffer();
   void GenerateImage();
   int InsertImage();
};

class SequenceThread : public MMDeviceThreadBase
{
   friend class TemplateNDGCamera;
   enum { default_numImages=1, default_intervalMS = 100 };
   public:
      SequenceThread(TemplateNDGCamera* pCam);
      ~SequenceThread();
      void Stop();
      void Start(long numImages, double intervalMs);
      bool IsStopped();
      void Suspend();
      bool IsSuspended();
      void Resume();
      double GetIntervalMs(){return intervalMs_;}                               
      void SetLength(long images) {numImages_ = images;}                        
      long GetLength() const {return numImages_;}
      long GetImageCounter(){return imageCounter_;}
      MM::MMTime GetStartTime(){return startTime_;}
      MM::MMTime GetActualDuration(){return actualDuration_;}
   private:                                                                     
      int svc(void) throw();
      TemplateNDGCamera* camera_;
      bool stop_;                                                               
      bool suspend_;
      long numImages_;                                                          
      long imageCounter_;                                                       
      double intervalMs_;                                                       
      MM::MMTime startTime_;
      MM::MMTime actualDuration_;
      MM::MMTime lastFrameTime_;
      MMThreadLock stopLock_;
      MMThreadLock suspendLock_;
}; 


#endif //_MMCAMERA_H_
