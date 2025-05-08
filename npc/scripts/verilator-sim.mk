SRCS += $(VSRCS) $(CSRCS)

sim:
	@-rm -rf $(BUILD_DIR)/*
	verilator $(VERILATOR_FLAGS) --top-module $(TOP) $(SRCS) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) -o $(BUILD_DIR)/$(TOP)


wave: VERILATOR_FLAGS += --trace-fst
wave: CXXFLAGS += -DTRACE_PATH=$(BUILD_DIR)/trace.fst
wave: sim

run: sim
	$(BUILD_DIR)/$(TOP) $(ARGS) $(IMG)
run-wave: wave
	$(BUILD_DIR)/$(TOP) $(ARGS) $(IMG)
