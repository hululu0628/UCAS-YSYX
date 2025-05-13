#include <isa.h>
#include <sim/sdb.h>
#include <debug.h>
#include "VTop.h"
#include <verilated.h>

CPU_state cpu = {};

void init_sim();
void init_sdb();
void init_disasm();
void sdb_mainloop();

void init_cpu()
{
	cpu.pc = PROGRAM_ENTRY;
	for(int i = 0; i < NR_GPR; i++)
	{
		cpu.gpr[i] = 0;
	}
	Log("Registers initialized");
}

void init_monitor()
{
	init_cpu();
	init_sim();
	init_sdb();
	IFDEF(CONFIG_TRACE, init_disasm();)
	IFDEF(CONFIG_FTRACE, init_ftrace(elf_file);)
}

void monitor_start()
{
	sdb_mainloop();
}
