
SRCS += $(VSRCS) $(CSRCS)

ifdef CONFIG_VCD
$(shell mkdir -p wave)
VERILATOR_FLAGS += --trace
override ARGS += --wave=wave/log.vcd
else ifdef CONFIG_FST
$(shell mkdir -p wave)
VERILATOR_FLAGS += --trace-fst
override ARGS += --wave=wave/log.fst
endif


sim:
	@-rm -rf $(BUILD_DIR)/*
	verilator $(VERILATOR_FLAGS) --top-module $(TOP) $(SRCS) \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) -o $(BUILD_DIR)/$(TOP)

run: sim
	@echo ${ARGS}
	$(BUILD_DIR)/$(TOP) $(ARGS) $(IMG)
