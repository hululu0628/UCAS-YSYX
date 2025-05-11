-include $(NPC_HOME)/config/include/config/auto.conf
-include $(NPC_HOME)/config/include/config/auto.conf.cmd

TOP ?= Top

PLATFORM ?= verilator

BUILD_DIR = $(NPC_HOME)/build

OBJ_DIR = $(BUILD_DIR)/obj_dir

PRJ = playground

CONFIG_DIR = $(NPC_HOME)/config

CSRC = $(NPC_HOME)/csrc

VSRC = $(NPC_HOME)/vsrc

NXDC_FILES = $(NPC_HOME)/constr/top.nxdc

VERILATOR = verilator

VERILATOR_FLAGS = --cc --exe --build -j 0 -Wall -Wno-PINCONNECTEMPTY -Wno-UNUSEDSIGNAL

ARGS_DIFF = --diff=$(shell find $(NEMU_HOME)/build/ -type f -name "*-so")

override ARGS ?= --log=$(shell pwd)/build/npc-log.txt
override ARGS += $(ARGS_DIFF)
override ARGS += -b

IMG ?=

FILELIST_MK = $(shell find -L $(CSRC)/src -name "filelist.mk")
include $(FILELIST_MK)

# project source
CSRCS-y += $(shell find -L $(DIRS-y) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
VSRCS = $(shell find $(abspath $(VSRC)) -name "*.v" -or -name "*.sv")
CSRCS = $(CSRCS-y)


$(shell mkdir -p $(BUILD_DIR))


test:
	mill -i $(PRJ).test

verilog:
# $(call git_commit, "generate verilog")
	mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC) --split-verilog

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
CXXFLAGS += $(INCFLAGS)

-include $(NPC_HOME)/scripts/$(PLATFORM)-sim.mk


-include ../Makefile
