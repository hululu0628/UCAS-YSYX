
LIBCAPSTONE = $(CSRC)/tools/capstone/repo/libcapstone.so.5
INC_PATH += $(CSRC)/tools/capstone/repo/include
CXXFLAGS += -DCAPSTONE_PATH=$(LIBCAPSTONE)
$(CSRC)/src/utils/disasm.cpp: $(LIBCAPSTONE)
$(LIBCAPSTONE):
	$(MAKE) -C $(CSRC)/tools/capstone
