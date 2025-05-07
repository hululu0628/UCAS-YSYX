#include <ios>
#include <iterator>
#include <verilated.h>
#include "VTop.h"
#include <debug.h>

static VTop *top;
static bool is_exit = false;

extern "C" void ebreak_handler(unsigned char inst_ebreak)
{
	is_exit = inst_ebreak ? true : is_exit;
}


void start_sim()
{
	VerilatedContext* contextp = new VerilatedContext;
	top = new VTop{contextp};

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
		if(top->clock == 1)
		{
			log_stream << "PC: 0x" << std::hex << top->io_debug_pc << "\twaddr: 0x" << std::hex << (int)top->io_debug_waddr
			<< "\twdata: 0x" << std::hex << top->io_debug_data << "\twen: " << std::hex << (int)(top->io_debug_wen) << std::endl;
		}
	}
	delete top;
}
