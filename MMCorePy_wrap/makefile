PROJ_NAME := MMCorePy
PREFIX := _
POSTFIX := 
EXTENSION := .pyd
PYD_FILE := MMCoreJ.jar
TARGET_NAME := $(PREFIX)$(PROJ_NAME)$(POSTFIX)$(EXTENSION)
CONFIG := Release
TARGET = $(CONFIG)/$(TARGET_NAME)
OBJ_FILES := MMCorePy_wrap.o
MM_ROOT := ../..
THIRD_PARTY := $(MM_ROOT)/3rdpartypublic
PY_FILE := MMCorePy.py
IFLAGS := -I"$(WIN_SDK)/Include" -I"$(VC_SDK)/include" -I"$(PYTHON27_HOME)\Lib\site-packages\numpy\core\include\numpy" -I"$(PYTHON27_HOME)\include"
PREPROCS := _WINDOWS _USRDLL _WINDLL _MBCS WIN32 _LIB MODULE_EXPORTS
PREPROC_FLAGS := $(patsubst %,-D%,$(PREPROCS))

CFLAGS := -c -nologo -W4 -WX- -Oy- -Gm -EHa -GS -fp:precise -Zc:wchar_t -Zc:forScope -GR -Gd -wd"4290" -analyze- -errorReport:queue
LINK_FLAGS = -INCREMENTAL -NOLOGO -DLL -DEBUG -SUBSYSTEM:WINDOWS -DYNAMICBASE:NO -IMPLIB:"$(CONFIG)/$(PROJ_NAME).lib" -MACHINE:X86 -ERRORREPORT:QUEUE
MM_LIB := mmcorer.lib
CXX := cl
LIBPATH := $(BOOST)/stage/lib $(MM_ROOT)/trunk/lib_Win32 $(PYTHON27_HOME)/libs $(PYTHON27_HOME)/DLLs
LIB_FLAGS := $(patsubst %,-LIBPATH:"%",$(LIBPATH)) -LIBPATH:"$(WIN_SDK)/Lib" -LIBPATH:"$(VC_SDK)/lib" 
LIBS = $(MM_LIB) IPHlpApi.lib

.PHONY: all full clean pre-build main-build post-build

all: CFLAGS += -Zi -O2 -MD
all: LINK_FLAGS += -OPT:REF -OPT:ICF -TLBID:1 
all: pre-build main-build post-build

full: clean all

debug: CONFIG := Debug
debug: PREPROCS += _DEBUG
debug: CFLAGS += -ZI -Od -RTC1 -MDd
debug: LINK_FLAGS += -TLBID:1
debug: MM_LIB := mmcored.lib
debug: pre-build main-build post-build

pre-build:
	@echo Building $(PROJ_NAME): pre-building, configuration: $(CONFIG)
	@test -d $(CONFIG) || mkdir $(CONFIG)
	$(if $(wildcard $(CONFIG)/*.o),-@mv -f $(CONFIG)/*.o .,)
	$(if $(wildcard $(CONFIG)/*.pdb),-@mv -f $(CONFIG)/*.pdb .,)
	$(if $(wildcard $(CONFIG)/*.idb),-@mv -f $(CONFIG)/*.idb .,)
	@echo Pre-building done.
	@echo Target: $(TARGET)
	@echo.
	
main-build: $(TARGET) $(PY_FILE) 

$(PY_FILE) MMCorePy_wrap.cxx: MMCorePy.i
	@echo swig processing...
	swig -python -c++ -o MMCorePy_wrap.cxx MMCorePy.i
	@echo.	

$(TARGET): $(OBJ_FILES)
	@echo Building target: $@, configuration: $(CONFIG).
	@echo Invoking: Linker \(link\)
	"$(VC_SDK)/bin/link" $(LINK_FLAGS) -OUT:$(TARGET) $< $(LIBS) $(LIB_FLAGS)
	@echo Finished building target: $@
	@echo.
	
$(OBJ_FILES): MMCorePy_wrap.cxx MMCorePy_wrap.h
	@echo Building file: $<
	@echo Invoking: C++ Compiler \(cl\)
	cl $(CFLAGS) $(PREPROC_FLAGS) $(IFLAGS) -Fo$@ $<
	@echo Finished building: $<, out: $@
	@echo.
	
post-build:
	@echo $(PROJ_NAME): post-build:
	-@mv -f *.o $(CONFIG)
	-@mv -f *.pdb $(CONFIG)
	-@mv -f *.idb $(CONFIG)
	@cp -v -f $(PY_FILE) $(MM_ROOT)/trunk/bin_Win32
	@cp -v -f $(TARGET) $(MM_ROOT)/trunk/bin_Win32
	@echo.
	
clean:
	@echo Cleaning project...
	@-rm -rf Debug Release
	@-rm -rf $(DEPENDENCY)
	$(if $(wildcard *.o),-@rm -f *.o,)
	$(if $(wildcard *.pdb),-@rm -f *.pdb,)
	$(if $(wildcard *.idb),-@rm -f *.idb,)
	$(if $(wildcard *.d),-@rm -f *.d,)
	$(if $(wildcard MMCorePy_wrap.cxx),-@rm -f MMCorePy_wrap.cxx,)
	$(if $(wildcard MMCorePy_wrap.h),-@rm -f MMCorePy_wrap.h,)
	$(if $(wildcard $(PY_FILE)),-@rm -f $(PY_FILE),)
	@echo Cleaned.
	@echo.	
