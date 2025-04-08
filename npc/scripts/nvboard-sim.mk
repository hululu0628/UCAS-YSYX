-include $(NVBOARD_HOME)/scripts/nvboard.mk

SRCS += $(VSRCS) $(CSRCS) $(NVBOARD_ARCHIVE)

SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@


CSRCS += $(SRC_AUTO_BIND)


nvbuild:
#$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@-rm -rf $(BUILD_DIR)/*
	$(VERILATOR) $(VERILATOR_FLAGS) --top-module $(TOP) $(SRCS) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) -o $(abspath $(BUILD_DIR)/$(TOP))

nvsim: nvbuild
	@$(BUILD_DIR)/$(TOP)