-include $(NPC_HOME)/config/include/config/auto.conf
-include $(NPC_HOME)/config/include/config/auto.conf.cmd

TOP ?= $(CONFIG_TOP_MODULE)

PLATFORM ?= verilator

BUILD_DIR = $(NPC_HOME)/build

OBJ_DIR = $(BUILD_DIR)/obj_dir

PRJ = playground

TESTCLASS ?= FormalTest

CONFIG_DIR = $(NPC_HOME)/config

CSRC += $(NPC_HOME)/csrc

VSRC += $(NPC_HOME)/vsrc
# for SoC
VSRC += $(YSYXSOC_HOME)/perip

NXDC_FILES = $(NPC_HOME)/constr/top.nxdc

VERILATOR = verilator

VERILATOR_FLAGS += -MMD --cc --exe --build -j 0 --timescale "1ns/1ns" --no-timing

VERILATOR_FLAGS += --top-module $(TOP)

ifeq ($(CONFIG_IGNORE_WARNINGS),n)
VERILATOR_FLAGS += -Wall -Wno-PINCONNECTEMPTY -Wno-UNUSEDSIGNAL
else
VERILATOR_FLAGS += -Wno-fatal
endif


ARGS_DIFF = --diff=$(shell find $(NEMU_HOME)/build/ -type f -name "*-so")

override ARGS ?= --log=$(shell pwd)/build/npc-log.log
override ARGS += $(ARGS_DIFF)
override ARGS +=

IMG ?=

FILELIST_MK = $(shell find -L $(CSRC)/src -name "filelist.mk")
include $(FILELIST_MK)

# project source
CSRCS-y += $(shell find -L $(DIRS-y) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
VSRCS = $(shell find $(abspath $(VSRC)) -name "*.v" -or -name "*.sv")
VSRCS += $(YSYXSOC_HOME)/build/ysyxSoCFull.v
CSRCS = $(CSRCS-y)


$(shell mkdir -p $(BUILD_DIR))

verilog:
# $(call git_commit, "generate verilog")
	@rm -rf $(NPC_HOME)/vsrc
	mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC) --split-verilog
test:
	mill -i $(PRJ).test.testOnly $(TESTCLASS)

sta: 
	make -C $(YSYX_HOME)/yosys-sta sta DESIGN=ysyxCPU \
	CLK_FREQ_MHZ=100 CLK_PORT_NAME=clock \
	RTL_FILES="$(filter-out %ysyxSoCFull.v, $(VSRCS))" \

help:
	mill -i $(PRJ).runMain Elaborate --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

# get mconf and conf from nemu.tools after 'make menuconfig' in nemu
# or you can install kconfig-frontends
menuconfig:
	make -C $(NPC_HOME)/config menuconfig

clean:
	-rm -rf $(BUILD_DIR)
clean-verilog:
	-rm -rf $(VSRC)

.PHONY: test verilog help reformat checkformat clean


# rules for verilator
LIBS += -lreadline
LDFLAGS += $(LIBS)

INC_PATH += $(CSRC)/include
INC_PATH += $(CONFIG_DIR)/include/generated

INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOP)\""

# for verilator
VINC_PATH += $(YSYXSOC_HOME)/perip/uart16550/rtl $(YSYXSOC_HOME)/perip/spi/rtl/

VINCFLAGS = $(addprefix -I, $(VINC_PATH))

-include $(NPC_HOME)/scripts/$(PLATFORM)-sim.mk

-include $(NPC_HOME)/scripts/test.mk

-include ../Makefile
