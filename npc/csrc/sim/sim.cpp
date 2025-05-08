#include <verilated.h>
#include "VTop.h"
#include <common.h>
#include <debug.h>

void monitor_run(VTop *top);
void difftest_step();

VerilatedContext* contextp = new VerilatedContext;
static VTop *top = new VTop{contextp};

static bool is_exit = false;

extern "C" void ebreak_handler(unsigned char inst_ebreak)
{
	is_exit = inst_ebreak ? true : is_exit;
}

void reg_modify(VTop *top)
{
	cpu.pc = top->io_debug_npc;
	if(top->io_debug_wen && top->io_debug_waddr != 0)
	{
		cpu.gpr[top->io_debug_waddr] = top->io_debug_data;
	}
}

void trace_and_difftest()
{
	difftest_step();
}

void sim_once()
{
	top->clock = !top->clock;
	top->eval();
	reg_modify(top);

}

void sim_step(uint64_t n)
{
	for(; n > 0; n--)
	{
		sim_once();
		trace_and_difftest();
		if(!(npc_state.state == NPC_RUNNING)) break;
	}


	top->reset = 1;
	top->clock = 0;
	top->eval();
	top->clock = 1;
	top->eval();
	top->reset = 0;

	while(!is_exit)
	{
		top->clock = !top->clock;
		top->eval();
		if(top->clock == 0)
		{
			monitor_run(top);
		}
	}
	delete top;
}
