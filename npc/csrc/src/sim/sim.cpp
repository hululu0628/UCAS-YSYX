#include <signal.h>
#include "common.h"
#include "difftest/difftest.h"
#include "utils.h"
#include "verilated.h"
#include <isa.h>
#include <debug.h>
#include <mem/mem.h>
#include <sim/sdb.h>
#include <sim/sim.h>
#ifdef CONFIG_NVBOARD
#include <nvboard.h>
#endif

struct debug_signal debug_signal;

#ifdef CONFIG_NVBOARD
void nvboard_bind_all_pins(VTop* top);
#endif

void monitor_run(VTop *top);
void difftest_step();
void device_update();

word_t blocked_cycle = 0;
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

void sig_handler(int signum)
{
	npc_state.state = NPC_ABORT;
}

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

extern "C" void set_debug(unsigned reset, unsigned valid, unsigned pc, unsigned npc, unsigned inst,
	unsigned wen, unsigned waddr, unsigned data)
{
	debug_signal.reset = reset;
	debug_signal.valid = valid;
	debug_signal.pc = pc;
	debug_signal.npc = npc;
	debug_signal.inst = inst;
	debug_signal.wen = wen;
	debug_signal.waddr = waddr;
	debug_signal.data = data;
}


void wave_dump()
{
#ifndef CONFIG_NO_WAVE
	if(CONFIG_WAVE_BEGIN <= excuted_inst_num && excuted_inst_num < CONFIG_WAVE_END)
		tfp->dump(contextp->time());
	contextp->timeInc(1000 / CONFIG_SIM_FREQ_M);
#endif
}

void init_sim()
{
	signal(SIGINT, sig_handler);

	contextp = new VerilatedContext;
	top = new VTop{contextp};

#ifdef CONFIG_NVBOARD
	nvboard_bind_all_pins(top);
	nvboard_init();
#endif

#ifndef CONFIG_NO_WAVE
	contextp->traceEverOn(true);
	tfp->set_time_unit("ns");
	top->trace(tfp, 99);
	tfp->open(wave_file);
	Log("Ready for wave dump: " << wave_file);
#endif
	top->reset = 1;
	top->clock = 0;
	top->eval();
	wave_dump();

	IFDEF(CONFIG_NVBOARD, nvboard_update();)

	// 10 cycles delay waiting chiplink reset
	for(int i = 0; i < 20; i++)
	{
		top->clock = !top->clock;
		top->eval();
		wave_dump();
		IFDEF(CONFIG_NVBOARD, nvboard_update();)
	}
	Log("Reset CPU successfully");
}

void reg_modify(VTop *top)
{
	cpu.pc = debug_signal.npc;
	if(debug_signal.wen && debug_signal.waddr != 0)
	{
		cpu.gpr[debug_signal.waddr] = debug_signal.data;
	}
}

void trace_and_difftest()
{
	if(debug_signal.reset)
	{
		difftest_skip_ref();
	}
	else
	{
		if(!debug_signal.valid)
		{
			blocked_cycle++;
			if(blocked_cycle > CONFIG_AUTOQUIT_CYCLE)
			{
				npc_state.state = NPC_ABORT;
				Log_Error("Quit because of blocked cycle");
				return;
			}
		}
		else
		{
			blocked_cycle = 0;
			excuted_inst_num++;

			IFDEF(CONFIG_DIFFTEST, difftest_step();)
			IFDEF(CONFIG_ITRACE, trace_instruction();)
			IFDEF(CONFIG_FTRACE, trace_func(debug_signal.pc, debug_signal.inst);)
			IFDEF(CONFIG_WATCHPOINT, check_watchpoints();)
		}
	}
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
	if(npc_state.state == NPC_END)
	{
		stdout_write("No running simulatioon");
		return;
	}
	npc_state.state = NPC_RUNNING;
	for(; n > 0; n--)
	{
		sim_once();
		IFDEF(CONFIG_NVBOARD, nvboard_update();)
		trace_and_difftest();
		if(npc_state.state != NPC_RUNNING) break;
		IFDEF(CONFIG_DEVICE, device_update());
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
			stdout_write("[NPC] " << ANSI_FG_GREEN << "Hit GOOD Trap" << ANSI_NONE 
				<< " at PC = 0x" << std::hex << debug_signal.pc);
			stdout_write("insts: " << std::dec << excuted_inst_num);
			break;
		case NPC_ABORT:
			stdout_write("[NPC] " << ANSI_FG_RED << "Hit BAD Trap" << ANSI_NONE 
				<< " at PC = 0x" << std::hex << debug_signal.pc);
			stdout_write("insts: " << std::dec << excuted_inst_num);
			break;
		default:
			assert(0);
	}
}

void sim_end()
{
	delete top;
	delete contextp;
	IFDEF(CONFIG_NVBOARD, nvboard_quit();)
	IFNDEF(CONFIG_NO_WAVE, tfp->close();)
}
