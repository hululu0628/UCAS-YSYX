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

struct debug_signal {
	bool reset; 
	bool valid;
	bool wen;
	word_t pc;
	word_t npc;
	word_t inst;
	word_t waddr;
	word_t data;
};
extern struct debug_signal debug_signal;
