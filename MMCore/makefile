PROJ_NAME := MMCore
PREFIX := 
POSTFIX := 
EXTENSION := .lib
TARGET_NAME = $(PREFIX)$(PROJ_NAME)$(POSTFIX)$(EXTENSION)
CONFIG := Release
TARGET = $(CONFIG)/$(TARGET_NAME)
OBJ_FILES := $(patsubst %.cpp,%.o,$(wildcard *.cpp))
MM_OBJ_FILES := DeviceUtils.o ImgBuffer.o
DEPENDENCY := $(patsubst %.o,%.d,$(OBJ_FILES) $(MM_OBJ_FILES))
MM_ROOT := ../..
THIRD_PARTY := $(MM_ROOT)/3rdpartypublic
BOOST := /c/boost_1_52_0

INCLUDE := $(BOOST) $(THIRD_PARTY)/zlib
IFLAGS := $(patsubst %,-I"%",$(INCLUDE)) -I"$(WIN_SDK)/Include" -I"$(VC_SDK)/include"
GCC_IFLAGS := $(patsubst %,-I%,$(INCLUDE))
PREPROCS := _WINDOWS _USRDLL _WINDLL _MBCS WIN32 _LIB MODULE_EXPORTS
PREPROC_FLAGS = $(patsubst %,-D%,$(PREPROCS))
CFLAGS := -c -nologo -W4 -WX- -Oy- -Gm -EHa -GS -fp:precise -Zc:wchar_t -Zc:forScope -GR -Gd -wd"4290" -analyze- -errorReport:queue
LINK_FLAGS = -NOLOGO
LIBPATH := $(BOOST)/stage/lib $(THIRD_PARTY)/zlib/bin_Win32
LIB_FLAGS := $(patsubst %,-LIBPATH:"%",$(LIBPATH)) -LIBPATH:"$(WIN_SDK)/Lib" -LIBPATH:"$(VC_SDK)/lib" 
LIBS := zlib.lib

.PHONY: all clean pre-build main-build post-build

all: CFLAGS += -Zi -O2 -MD
all: POSTFIX := r
all: pre-build main-build post-build

debug: CONFIG := Debug
debug: PREPROCS += _DEBUG
debug: CFLAGS += -ZI -Od -RTC1 -MDd
debug: POSTFIX := d
debug: pre-build main-build post-build

-include $(OBJ_FILES:.o=.d)
-include $(MM_OBJ_FILES:.o=.d)

pre-build:
	@echo
	@echo Building $(PROJ_NAME): pre-building, configuration: $(CONFIG)
	@test -d $(CONFIG) || mkdir $(CONFIG)
	$(if $(wildcard $(CONFIG)/*.o),-@mv -f $(CONFIG)/*.o .,)
	$(if $(wildcard $(CONFIG)/*.pdb),-@mv -f $(CONFIG)/*.pdb .,)
	@echo Pre-building done.
	@echo.

main-build: $(TARGET)

$(TARGET): $(OBJ_FILES) $(MM_OBJ_FILES)
	@echo Building target: $@, configuration: $(CONFIG).
	@echo Invoking: Linker \(link\)
	lib $(LINK_FLAGS) -OUT:$(TARGET) $^ $(LIBS) $(LIB_FLAGS)
	@echo Finished building target: $@
	@echo.
	
$(OBJ_FILES): %.o: %.cpp
	@echo Building file: $<
	@echo Output file: $@
	@echo Invoking: C++ Compiler \(cl\)
	cl $(CFLAGS) $(PREPROC_FLAGS) $(IFLAGS) -Fo$@ $<
	gcc -MM $(GCC_IFLAGS) $< > $(patsubst %.o,%.d,$@)
	@echo Finished building: $<, out: $@
	@echo.

$(MM_OBJ_FILES): %.o: $(MM_ROOT)/trunk/MMDevice/%.cpp
	@echo Building file: $<
	@echo Invoking: C++ Compiler \(cl\)
	cl $(CFLAGS) $(PREPROC_FLAGS) $(IFLAGS) -Fo$@ $<
	gcc -MM $(GCC_IFLAGS) $< > $(patsubst %.o,%.d,$@)
	@echo Finished building: $<, out: $@
	@echo. 

post-build:
	@echo MMCore: post-build:
	-@mv -f *.o $(CONFIG)
	-@mv -f *.pdb $(CONFIG)
	-@mv -f *.idb $(CONFIG)
	-@mv -f *.d $(CONFIG)
	-@cp $(TARGET) $(MM_ROOT)/trunk/lib_Win32
	-@cp $(TARGET) $(MM_ROOT)/trunk/lib_Win32/$(PREFIX)$(PROJ_NAME)$(EXTENSION)
	@echo.
	
clean:
	@echo Cleaning project...
	@-rm -rf Debug Release
	@-rm -f $(DEPENDENCY)
	@-rm -f *.o *.pdb *.idb *.d
	@echo.
