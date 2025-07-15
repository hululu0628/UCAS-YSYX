
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


build:
	@-rm -rf $(BUILD_DIR)/*
	verilator $(VERILATOR_FLAGS) $(SRCS) $(VINCFLAGS)\
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) -o $(BUILD_DIR)/$(TOP)

run: 
	@echo ${ARGS}
	$(BUILD_DIR)/$(TOP) $(ARGS) $(IMG)
