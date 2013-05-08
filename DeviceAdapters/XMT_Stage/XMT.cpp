
#ifdef WIN32
   #include <windows.h>
   #define snprintf _snprintf 
#else
#endif

#include "XMT.h"
 
#include "ControllerCom.h"
#include "../../MMDevice/ModuleInterface.h"
///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////
MODULE_API void InitializeModuleData()
{
   AddAvailableDeviceName(ControllerComDevice::DeviceName_, "XMTStageCOM Controller");
 
}

MODULE_API MM::Device* CreateDevice(const char* deviceName)
{
   if (deviceName == 0)
      return 0;

   if (strcmp(deviceName, ControllerComDevice::DeviceName_) == 0)
   {
      ControllerComDevice* s = new ControllerComDevice();
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
 
