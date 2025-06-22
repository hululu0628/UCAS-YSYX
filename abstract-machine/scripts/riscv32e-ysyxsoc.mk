include $(AM_HOME)/scripts/isa/riscv.mk
include $(AM_HOME)/scripts/platform/ysyxsoc.mk
INC_PATH += $(NPC_HOME)/config/include/generated
CFLAGS  += -DISA_H=\"$(AM_HOME)/am/src/riscv/riscv.h\"
COMMON_CFLAGS += -march=rv32e_zicsr -mabi=ilp32e  # overwrite
LDFLAGS       += -melf32lriscv                    # overwrite
LDFLAGS       += --defsym=_pmem_start=0x30000000
LDSCRIPTS = $(AM_HOME)/scripts/linker-ysyxsoc.ld

AM_SRCS += riscv/ysyxsoc/libgcc/div.S \
           riscv/ysyxsoc/libgcc/muldi3.S \
           riscv/ysyxsoc/libgcc/multi3.c \
           riscv/ysyxsoc/libgcc/ashldi3.c \
           riscv/ysyxsoc/libgcc/unused.c
