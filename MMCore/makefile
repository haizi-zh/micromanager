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
INCLUDE := $(BOOST) $(THIRD_PARTY)/zlib
IFLAGS := $(patsubst %,-I"%",$(INCLUDE)) -I"$(WIN_SDK)/Include" -I"$(VC_SDK)/include"
GCC_IFLAGS := $(patsubst %,-I"%",$(INCLUDE))
PREPROCS := _WINDOWS _USRDLL _WINDLL _MBCS WIN32 _LIB MODULE_EXPORTS
PREPROC_FLAGS = $(patsubst %,-D%,$(PREPROCS))
CFLAGS := -c -nologo -W3 -WX- -Oy- -Gm -EHa -GS -fp:precise -Zc:wchar_t -Zc:forScope -GR -Gd -wd"4290" -analyze- -errorReport:queue
LINK_FLAGS = -NOLOGO
LIBPATH := $(THIRD_PARTY)/zlib/bin_Win32
LIB_FLAGS := $(patsubst %,-LIBPATH:"%",$(LIBPATH)) -LIBPATH:"$(WIN_SDK)/Lib" -LIBPATH:"$(VC_SDK)/lib" 
LIBS := zlib.lib

.PHONY: all full clean pre-build main-build post-build

all: CFLAGS += -Zi -O2 -MD
all: POSTFIX := r
all: pre-build main-build post-build

full: clean all

debug: CONFIG := Debug
debug: PREPROCS += _DEBUG
debug: CFLAGS += -ZI -Od -RTC1 -MDd
debug: POSTFIX := d
debug: pre-build main-build post-build

-include $(DEPENDENCY)

%.d: %.cpp 
	@echo Building MMCore dependencies: $@...
	gcc -MM $(GCC_IFLAGS) $< | \
	sed '/$(subst \,\\,$(BOOST))/d' | \
	sed 's/\($*\.o\)[ :]*/\1 $@ :/' > $@
	@echo

%.d: $(MM_ROOT)/trunk/MMDevice/%.cpp
	@echo Building MMDevice dependencies: $@...
	gcc -MM $(GCC_IFLAGS) $< | \
	sed '/$(subst \,\\,$(BOOST))/d' | \
	sed 's/\($*\.o\)[ :]*/\1 $@ :/' > $@
	@echo

pre-build:
	@echo
	@echo Building $(PROJ_NAME): pre-building, configuration: $(CONFIG)
	@test -d $(CONFIG) || mkdir $(CONFIG)
	@echo Pre-building done.
	@echo

main-build: $(TARGET)

$(TARGET): $(OBJ_FILES) $(MM_OBJ_FILES)
	@echo Building target: $@, configuration: $(CONFIG).
	@echo Invoking: Linker \(link\)
	lib $(LINK_FLAGS) -OUT:$(TARGET) $^ $(LIBS) $(LIB_FLAGS)
	@echo Finished building target: $@
	@echo
	
$(OBJ_FILES): %.o: %.cpp
	@echo Building file: $<
	@echo Output file: $@
	@echo Invoking: C++ Compiler \(cl\)
	cl $(CFLAGS) $(PREPROC_FLAGS) $(IFLAGS) -Fo$@ $<
	@echo Finished building: $<, out: $@
	@echo

$(MM_OBJ_FILES): %.o: $(MM_ROOT)/trunk/MMDevice/%.cpp
	@echo Building file: $<
	@echo Invoking: C++ Compiler \(cl\)
	cl $(CFLAGS) $(PREPROC_FLAGS) $(IFLAGS) -Fo$@ $<
	@echo Finished building: $<, out: $@
	@echo

post-build:
	@echo $(PROJ_NAME): post-build:	
	@cp -fv $(TARGET) $(MM_ROOT)/trunk/lib_Win32
	@cp -fv $(TARGET) $(MM_ROOT)/trunk/lib_Win32/$(PREFIX)$(PROJ_NAME)$(EXTENSION)	
	@echo
	
clean:
	@echo Cleaning project...
	@-rm -rf Debug Release
	$(if $(wildcard *.o),-@rm -fv *.o,)
	$(if $(wildcard *.pdb),-@rm -fv *.pdb,)
	$(if $(wildcard *.idb),-@rm -fv *.idb,)
	@echo
