#include <common.h>
#include <debug.h>
#include "VTop.h"
#include <verilated.h>

CPU_state cpu = {};
void difftest_step();

void init_cpu()
{
	cpu.pc = PROGRAM_ENTRY;
	for(int i = 0; i < NR_GPR; i++)
	{
		cpu.gpr[i] = 0;
	}
}

void init_monitor()
{
	init_cpu();
}

void trace_and_difftest()
{
	difftest_step();
}

void reg_modify(VTop *top)
{
	cpu.pc = top->io_debug_pc;
	if(top->io_debug_wen)
	{
		cpu.gpr[top->io_debug_waddr] = top->io_debug_data;
	}
}

void monitor_run(VTop *top)
{
	// Log
	log_stream << "PC: 0x" << std::hex << top->io_debug_pc << "\twaddr: 0x" << std::hex << (int)top->io_debug_waddr
			<< "\twdata: 0x" << std::hex << top->io_debug_data << "\twen: " << std::hex << (int)(top->io_debug_wen) << std::endl;
	reg_modify(top);

	trace_and_difftest();
}
