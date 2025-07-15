-include $(NVBOARD_HOME)/scripts/nvboard.mk
NXDC_FILES = constr/top.nxdc

ifdef CONFIG_VCD
$(shell mkdir -p wave)
VERILATOR_FLAGS += --trace
override ARGS += --wave=wave/log.vcd
else ifdef CONFIG_FST
$(shell mkdir -p wave)
VERILATOR_FLAGS += --trace-fst
override ARGS += --wave=wave/log.fst
endif

SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	@-rm -rf $(BUILD_DIR)/*
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@


CSRCS += $(SRC_AUTO_BIND)

SRCS += $(VSRCS) $(CSRCS) $(NVBOARD_ARCHIVE)


nvbuild: $(SRC_AUTO_BIND) $(NVBOARD_ARCHIVE)
#$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	verilator $(VERILATOR_FLAGS) $(VINCFLAGS) $(SRCS)\
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) -o $(BUILD_DIR)/$(TOP)

nvrun: nvbuild
	@echo ${ARGS}
	$(BUILD_DIR)/$(TOP) $(ARGS) $(IMG)
