/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "isa.h"
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <locale.h>
#include <stdio.h>

/* The assembly code of instructions executed is only output to the screen
 * when the number of instructions executed is less than this value.
 * This is useful when you use the `si' command.
 * You can modify this value as you want.
 */
#define MAX_INST_TO_PRINT 10

CPU_state cpu = {};
uint64_t g_nr_guest_inst = 0;
static uint64_t g_timer = 0; // unit: us
static bool g_print_step = false;

void device_update();

static void trace_and_difftest(Decode *_this, vaddr_t dnpc) {
	if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(_this->logbuf)); }
	IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));

#ifdef CONFIG_ITRACE
	void inst_log(Decode *s);
	inst_log(_this);
#ifdef CONFIG_IRINGBUF
	void insert_instr(Decode *s);
	insert_instr(_this);
#endif
#endif

#ifdef CONFIG_ITRACE_COND
	if (ITRACE_COND) { log_write("%s\n", _this->logbuf); }
#endif

#ifdef CONFIG_ETRACE
	void trace_trap(Decode *s);
	trace_trap(_this);
#endif

#ifdef CONFIG_WATCHPOINT
/*** That is annoying, I think I should move the definition of these functions to sdb.h
     then move sdb.h to include directory later. ***/
	void check_watchpoints();
	check_watchpoints();
#endif
}

static void exec_once(Decode *s, vaddr_t pc) {
	s->pc = pc;
	s->snpc = pc;
	isa_exec_once(s);
	cpu.pc = s->dnpc;
}

static void execute(uint64_t n) 
{
  	Decode s;
  	for (;n > 0; n --) {
		exec_once(&s, cpu.pc);
		g_nr_guest_inst ++;
		trace_and_difftest(&s, cpu.pc);
		if (nemu_state.state != NEMU_RUNNING) break;
		IFDEF(CONFIG_DEVICE, device_update());
  	}
}

static void statistic() 
{
	IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
	Log("host time spent = " NUMBERIC_FMT " us", g_timer);
	Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
	if (g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
	else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}

void assert_fail_msg()
{
	isa_reg_display();	
	statistic();
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) 
{
	g_print_step = (n < MAX_INST_TO_PRINT);
	switch (nemu_state.state) {
		case NEMU_END: case NEMU_ABORT: case NEMU_QUIT:
		printf("Program execution has ended. To restart the program, exit NEMU and run again.\n");
		return;
	default: nemu_state.state = NEMU_RUNNING;
  	}	

	uint64_t timer_start = get_time();

	execute(n);

	uint64_t timer_end = get_time();
	g_timer += timer_end - timer_start;

	switch (nemu_state.state) {
		case NEMU_RUNNING: nemu_state.state = NEMU_STOP; break;

		case NEMU_END: case NEMU_ABORT:
		Log("nemu: %s at pc = " FMT_WORD,
			(nemu_state.state == NEMU_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
			(nemu_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
			ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
			nemu_state.halt_pc);
		// fall through
		case NEMU_QUIT: statistic();
		#ifdef CONFIG_IRINGBUF
			void print_ringbuf();
			print_ringbuf();
		#endif
	}
}
