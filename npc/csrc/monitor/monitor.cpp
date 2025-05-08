#include <common.h>
#include <debug.h>
#include "VTop.h"
#include <verilated.h>

CPU_state cpu = {};

void sdb_mainloop();

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

void monitor_start()
{
	sdb_mainloop();
}
