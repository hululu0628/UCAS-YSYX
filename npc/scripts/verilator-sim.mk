SRCS += $(VSRCS) $(CSRCS)

sim:
	@-rm -rf $(BUILD_DIR)/*
	verilator $(VERILATOR_FLAGS) --top-module $(TOP) $(SRCS) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) -o $(BUILD_DIR)/$(TOP)

build: VERILATOR_FLAGS -= --exe
build: SRCS -= $(CSRCS)
build: sim


wave: VERILATOR_FLAGS += --trace-fst
wave: CXXFLAGS += -DTRACE_PATH=$(BUILD_DIR)/trace.fst
wave: sim
