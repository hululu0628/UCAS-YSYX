#include "verilated.h"
#include <isa.h>
#include <debug.h>
#include <sdb.h>
#include <sim.h>

void monitor_run(VTop *top);
void difftest_step();

word_t excuted_inst_num = 0;

#ifdef CONFIG_FST
VerilatedFstC* tfp = new VerilatedFstC;
#endif
#ifdef CONFIG_VCD
VerilatedVcdC* tfp = new VerilatedVcdC;
#endif

VerilatedContext* contextp;
VTop *top;

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

void wave_dump()
{
#ifndef CONFIG_NO_WAVE
	if(CONFIG_WAVE_BEGIN <= excuted_inst_num && excuted_inst_num < CONFIG_WAVE_END)
		tfp->dump(contextp->time());
	contextp->timeInc(1);
#endif
}

void init_sim()
{
	contextp = new VerilatedContext;
	top = new VTop{contextp};

#ifndef CONFIG_NO_WAVE
	contextp->traceEverOn(true);
	top->trace(tfp, 99);
	tfp->open(wave_file);
	Log("Ready for wave dump: " << wave_file);
#endif

	top->reset = 1;
	top->clock = 0;
	top->eval();
	wave_dump();
	Log("Reset CPU successfully");
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
	IFDEF(CONFIG_DIFFTEST, difftest_step();)
	IFDEF(CONFIG_ITRACE, trace_instruction();)
	IFDEF(CONFIG_WATCHPOINT, check_watchpoints();)
}

void sim_once()
{
	top->clock = !top->clock;
	top->eval();
	wave_dump();
	top->reset = 0;
	top->clock = !top->clock;
	top->eval();
	wave_dump();
	reg_modify(top);
}

void sim_step(uint64_t n)
{
	npc_state.state = NPC_RUNNING;
	for(; n > 0; n--)
	{
		sim_once();
		excuted_inst_num++;
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
			stdout_write("[NPC] " << ANSI_FG_GREEN << "Hit GOOD Trap" << ANSI_NONE);
			break;
		case NPC_ABORT:
			stdout_write("[NPC] " << ANSI_FG_RED << "Hit BAD Trap" << ANSI_NONE);
			break;
		default:
			assert(0);
	}
}

void sim_end()
{
	delete top;
	delete contextp;
	IFNDEF(CONFIG_NO_WAVE, tfp->close();)
}
