#include <verilated.h>
#include "VTop.h"
#include <isa.h>
#include <debug.h>
#include <sdb.h>

void monitor_run(VTop *top);
void difftest_step();

VerilatedContext* contextp = new VerilatedContext;
static VTop *top;

static bool is_end = false;

extern "C" void ebreak_handler(unsigned char inst_ebreak)
{
	if(inst_ebreak)
	{
		npc_state.halt_ret = cpu.gpr[REG_A0];
		if(cpu.gpr[REG_A0] == 0x1)
			npc_state.state = NPC_ABORT;
		else
			npc_state.state = NPC_END;
	}
}

void init_sim()
{
	top = new VTop{contextp};

	top->reset = 1;
	top->clock = 0;
	top->eval();
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
	// log_write("PC: 0x" << std::hex << top->io_debug_pc << "\twen: " << (unsigned)top->io_debug_wen << "\treg: " << regs[top->io_debug_waddr] << "\t data: 0x" << top->io_debug_data << std::endl);
	difftest_step();

	check_watchpoints();
}

void sim_once()
{
	top->clock = !top->clock;
	top->eval();
	top->reset = 0;
	top->clock = !top->clock;
	top->eval();
	reg_modify(top);
}

void sim_step(uint64_t n)
{
	npc_state.state = NPC_RUNNING;
	for(; n > 0; n--)
	{
		sim_once();
		trace_and_difftest();
		if(!(npc_state.state == NPC_RUNNING)) break;
	}
	if(npc_state.state == NPC_RUNNING)
	{
		npc_state.state = NPC_STOP;
	}
	switch (npc_state.state)
	{
		case NPC_RUNNING:
		case NPC_STOP:
		case NPC_QUIT:
			break;
		case NPC_END:
			std::cout << "[NPC] " << ANSI_FG_GREEN << "Hit GOOD Trap" << ANSI_NONE << std::endl;
			break;
		case NPC_ABORT:
			std::cout << "[NPC] " << ANSI_FG_RED << "Hit BAD Trap" << ANSI_NONE << std::endl;
			break;
		default:
			assert(0);
	}
}
