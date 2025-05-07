#include <verilated.h>
#include "VTop.h"

static VTop *top;
static bool is_exit = false;

extern "C" void ebreak_handler(unsigned char inst_ebreak)
{
	is_exit = inst_ebreak ? true : is_exit;
}


void start_sim()
{
	top = new VTop;
	top->reset = 1;
	top->clock = 0;
	top->eval();
	top->reset = 0;
	while(!is_exit)
	{
		top->clock = !top->clock;
		top->eval();
	}
}
