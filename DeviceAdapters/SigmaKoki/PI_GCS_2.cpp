///////////////////////////////////////////////////////////////////////////////
// FILE:          PI_GCS_DLL.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   PI GCS DLL Controller Driver
//
// AUTHOR:        Nenad Amodaj, nenad@amodaj.com, 08/28/2006
//                Steffen Rau, s.rau@pi.ws, 10/03/2008
// COPYRIGHT:     University of California, San Francisco, 2006
//                Physik Instrumente (PI) GmbH & Co. KG, 2008
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
// CVS:           $Id: PI_GCS_2.cpp,v 1.13, 2011-07-26 11:24:54Z, Steffen Rau$
//

#ifdef WIN32
   #include <windows.h>
   #define snprintf _snprintf 
#else
#endif

// this adapter can use PI modules to communicate with older firmware
// versions or with interface not supported by micro-manager
// e.g. the C-843 controller is a PCI board needing a special driver,
// and the E-710 controller need a translation layer to understand
// These additional modules are provided on the PI product CDs and are available
// for Windows platforms, some of these libraries are also available for Linux
// No modules are available for MAC OS X, so in this code the "DLL" controller
// calling these modules is disabled for MAC OS X by using the preprocessor constand "__APPLE__"

#include "PI_GCS_2.h"
#ifndef __APPLE__
#include "PIGCSControllerDLL.h"
#endif
#include "PIGCSControllerCom.h"
#include "PIZStage_DLL.h"
#include "PIXYStage_DLL.h"
#include "../../MMDevice/ModuleInterface.h"
#include <algorithm>
#include <locale.h>

///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////
MODULE_API void InitializeModuleData()
{
   AddAvailableDeviceName("ZStage", "ZStage");

   AddAvailableDeviceName("SigmaKoki Controller", "SigmaKoki Controller");


MODULE_API MM::Device* CreateDevice(const char* deviceName)
{
   if (deviceName == 0)
      return 0;

   if (strcmp(deviceName, PIZStage::DeviceName_) == 0)
   {
      PIZStage* s = new PIZStage();
      return s;
   }
   if (strcmp(deviceName, PIXYStage::DeviceName_) == 0)
   {
      PIXYStage* s = new PIXYStage();
      return s;
   }

#ifndef __APPLE__
   if (strcmp(deviceName, PIGCSControllerDLLDevice::DeviceName_) == 0)
   {
      PIGCSControllerDLLDevice* s = new PIGCSControllerDLLDevice();
      s->CreateProperties();
      return s;
   }
#endif

   if (strcmp(deviceName, PIGCSControllerComDevice::DeviceName_) == 0)
   {
      PIGCSControllerComDevice* s = new PIGCSControllerComDevice();
      s->CreateProperties();
      return s;
   }

   if (	(strcmp(deviceName, "C-867") == 0)
	||	(strcmp(deviceName, "C-663.11") == 0)
	||	(strcmp(deviceName, "C-863.11") == 0)	)
   {
      PIGCSControllerComDevice* s = new PIGCSControllerComDevice();
      s->SetFactor_UmToDefaultUnit(0.001);
      s->CreateProperties();
      return s;
   }

   if (strcmp(deviceName, "E-517/E-545") == 0)
   {
      PIGCSControllerComDevice* s = new PIGCSControllerComDevice();
      s->SetFactor_UmToDefaultUnit(1.0);
      s->CreateProperties();
      return s;
   }

#ifndef __APPLE__
   if (strcmp(deviceName, "E-710") == 0)
   {
      PIGCSControllerDLLDevice* s = new PIGCSControllerDLLDevice();
      s->SetDLL("E7XX_GCS_DLL.dll");
      s->SetInterface("RS-232", "");
      s->ShowInterfaceProperties(true);
      s->CreateProperties();
      return s;
   }

   if (strcmp(deviceName, "C-843") == 0)
   {
      PIGCSControllerDLLDevice* s = new PIGCSControllerDLLDevice();
      s->SetDLL("C843_GCS_DLL.dll");
      s->SetInterface("PCI", "1");
      s->ShowInterfaceProperties(false);
      s->CreateProperties();
      return s;
   }
#endif  

   if( (strcmp(deviceName, "E-712") == 0)
    || (strcmp(deviceName, "E-753") == 0) )
   {
      PIGCSControllerComDevice* s = new PIGCSControllerComDevice();
      s->SetFactor_UmToDefaultUnit(1.0);
      s->CreateProperties();
      return s;
   }

   return 0;
}

MODULE_API void DeleteDevice(MM::Device* pDevice)
{
   delete pDevice;
}
 
