pkglibdir = $(DEVICEADAPTERPATH)
AM_CXXFLAGS = $(MMDEVAPI_CXXFLAGS) -fpermissive
pkglib_LTLIBRARIES = libmmgr_dal_ScionCam.la
libmmgr_dal_ScionCam_la_SOURCES = ScionCamera.cpp ScionCamera.h capture.cpp utilities.cpp
libmmgr_dal_ScionCam_la_LIBADD = $(MMDEVAPI_LIBADD)
libmmgr_dal_ScionCam_la_LDFLAGS = $(MMDEVAPI_LDFLAGS) -framework sfwlib -framework sfwcore

EXTRA_DIST = ScionCamera.vcproj capture.h device.h iformat.h imageinfo.h interface.h settings.h sfwlib.h thread.h resource.h ScionCamera.rc MMConfig_scion.cfg license.txt
