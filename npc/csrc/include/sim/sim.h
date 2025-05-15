#pragma once
#include <common.h>
#include <verilated.h>
#include "VTop.h"

#ifdef CONFIG_FST
#include "verilated_fst_c.h"
extern VerilatedFstC* tfp;
#endif

#ifdef CONFIG_VCD
#include "verilated_vcd_c.h"
extern VerilatedVcdC* tfp;
#endif

extern unsigned blocked_cycle;

extern VerilatedContext* contextp;
extern VTop *top;

extern word_t excuted_inst_num;

extern char* wave_file;
