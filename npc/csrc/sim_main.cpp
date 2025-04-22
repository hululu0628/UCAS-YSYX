#include <verilated.h>
#include "VTop.h"
#include "sim.h"

void ebreak_handler(int inst_ebreak)
{
	
}

int check(VTop * top)
{

}



int main()
{
	VTop * top = new VTop;

	top->clock = 0;
	top->reset = 1;
	top->eval();
	top->reset = 0;

	while(1)
	{
		top->io_instr = InstFetch(InstAddressTrans(top->io_pc));
		top->clock = !top->clock;
		top->eval();
		if(check(top))
		{
			std::cout << "Error" << std::endl;
			break;
		}
	}
}
