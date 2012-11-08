#space := 
#space += 
#WIN_SDK := $(subst \,\\\\,$(WIN_SDK))
#WIN_SDK := $(subst $(space),\\ ,$(WIN_SDK))
#VC_SDK := $(subst $(space),\ ,$(VC_SDK))
WIN_SDK := $(subst /,\,$(WIN_SDK))
VC_SDK := $(subst /,\,$(VC_SDK))

PROJ_NAME := MMCore
PREFIX := 
POSTFIX := 
EXTENSION := .lib
TARGET_NAME := $(PREFIX)$(PROJ_NAME)$(POSTFIX)$(EXTENSION)
ifeq ($(DEBUG), 1)
	CONFIG := Debug
	PF := d
else
	CONFIG := Release
	PF := r
endif
TARGET_PATH := $(CONFIG)
TARGET := $(TARGET_PATH)/$(TARGET_NAME)

OBJ_FILES := $(patsubst %.cpp,%.o,$(wildcard *.cpp))
MM_OBJ_FILES:=DeviceUtils.o ImgBuffer.o

MM_ROOT := ../..
THIRD_PARTY := $(MM_ROOT)/3rdpartypublic

#IDIR := $(subst /,\,$(THIRD_PARTY)/boost $(THIRD_PARTY)/zlib)
IDIR := $(subst /,\,c:/boost_1_51_0 $(THIRD_PARTY)/zlib)
IFLAGS := $(patsubst %,-I"%",$(IDIR)) -I"$(WIN_SDK)\Include" -I"$(VC_SDK)\VC\include"
GCC_IFLAGS := $(patsubst %,-I%,$(subst \,/,$(IDIR)))
PREPROCS := _WINDOWS _USRDLL _WINDLL _MBCS WIN32 _LIB MODULE_EXPORTS
ifeq ($(DEBUG), 1)
	PREPROCS += _DEBUG
	CFLAGS :=-c -ZI -nologo -W4 -WX- -Od -Oy- -Gm -EHa -RTC1 -MDd -GS -fp:precise -Zc:wchar_t -Zc:forScope -GR -Gd -wd"4290" -analyze- -errorReport:queue
else
	PREPROCS += NDEBUG
	CFLAGS :=-c -Zi -nologo -W4 -WX- -O2 -Oy- -Gm- -EHa -MD -GS -fp:precise -Zc:wchar_t -Zc:forScope -GR -Gd -wd"4290" -analyze- -errorReport:queue
endif
PREPROC_FLAGS := $(patsubst %,-D%,$(PREPROCS))

LINK_FLAGS := -NOLOGO
LIBPATH := $(subst /,\,$(THIRD_PARTY)/boost/stage/lib $(THIRD_PARTY)/zlib/bin_Win32)
LIB_FLAGS := $(patsubst %,-LIBPATH:"%",$(LIBPATH)) -LIBPATH:"$(WIN_SDK)\Lib" 
LIBS := zlib.lib

.PHONY: all clean pre-build main-build post-build
all: pre-build main-build post-build

-include $(OBJ_FILES:.o=.d)
-include $(MM_OBJ_FILES:.o=.d)

pre-build:
	@echo.
	@echo MMCore: pre-building...
	$(if $(wildcard $(TARGET_PATH)),,@mkdir $(TARGET_PATH))
	$(if $(wildcard $(TARGET_PATH)/*.o),-@mv -f $(TARGET_PATH)/*.o .,)
	$(if $(wildcard $(TARGET_PATH)/*.d),-@mv -f $(TARGET_PATH)/*.d .,)
	@echo Pre-building done.
	@echo.

main-build: $(TARGET)

$(TARGET): $(OBJ_FILES) $(MM_OBJ_FILES)
	@pwd
	@echo Building target: $@, configuration: $(CONFIG).
	@echo Invoking: Linker \(link\)
	lib $(LINK_FLAGS) -OUT:$(TARGET) $^ $(LIBS) $(LIB_FLAGS)
	@echo Finished building target: $@
	@echo.


$(OBJ_FILES): %.o: %.cpp
	@echo Building file: $<
	@echo Invoking: C++ Compiler \(cl\)	
	cl $(CFLAGS) $(PREPROC_FLAGS) $(IFLAGS) -Fo$@ $<
	gcc -MM $(GCC_IFLAGS) $< > $(patsubst %.o,%.d,$@)
	@echo Finished building: $<, out: $@
	@echo.

#	gcc -MM $(GCC_IFLAGS) $< > $(patsubst %.o,%.d,$@)
$(MM_OBJ_FILES): %.o: ../MMDevice/%.cpp
	@echo Building file: $<
	@echo Invoking: C++ Compiler \(cl\)
	cl $(CFLAGS) $(PREPROC_FLAGS) $(IFLAGS) -Fo$@ $<
	gcc -MM $(GCC_IFLAGS) $< > $(patsubst %.o,%.d,$@)
	@echo Finished building: $<, out: $@
	@echo. 
	
post-build:
	@echo MMCore: post-build:
	-mv -f *.o $(TARGET_PATH)
	-cp -f *.d $(TARGET_PATH)
	-cp $(TARGET) $(MM_ROOT)/trunk/lib_Win32
	-cp $(TARGET) $(MM_ROOT)/trunk/lib_Win32/$(basename $(TARGET_NAME))$(PF)$(suffix $(TARGET_NAME))
	@echo.
	
clean:
	@echo Cleaning project...
	$(if $(wildcard $(TARGET_PATH)), -rm -rf $(TARGET_PATH),)
	$(if $(wildcard *.d), -rm *.d,)
	@echo Cleaned.
	@echo.