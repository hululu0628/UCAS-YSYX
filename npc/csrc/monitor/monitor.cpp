#include <isa.h>
#include <debug.h>
#include "VTop.h"
#include <verilated.h>

CPU_state cpu = {};

void init_sim();
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
	init_sim();
}

void monitor_start()
{
	sdb_mainloop();

	if(npc_state.state == NPC_QUIT)
	{
		std::cout << "Exiting NPC..." << std::endl;
	}
	else
	{
		std::cout << "Exiting NPC with error..." << std::endl;
	}
}
