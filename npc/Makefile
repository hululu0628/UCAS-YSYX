TOP = top

BUILD_DIR = ./build

OBJ_DIR = $(BUILD_DIR)/obj_dir

PRJ = playground

C_SRC = ./csrc

VERILOG_SRC = ./vsrc

NXDC_FILES = ./constr/top.nxdc

VERILATOR = verilator

VERILATOR_FLAGS = --cc --exe --build -j 0 -Wall

# constraint file
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

# project source
VSRCS = $(shell find $(abspath ./vsrc) -name "*.v")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CSRCS += $(SRC_AUTO_BIND)


include $(NVBOARD_HOME)/scripts/nvboard.mk

$(shell mkdir -p $(BUILD_DIR))

default: nvbuild

test:
	mill -i $(PRJ).test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(BUILD_DIR)

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

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog help reformat checkformat clean


# rules for verilator
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOP)\""

nvbuild: $(VSRCS) $(CSRCS) $(NVBOARD_ARCHIVE)
#$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@-rm -f $(BUILD_DIR)/
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_FLAGS) --top-module $(TOP) $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) -o $(abspath $(BUILD_DIR)/$(TOP))

nvsim:
	@$(BUILD_DIR)/$(TOP)

-include ../Makefile
