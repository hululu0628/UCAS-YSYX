#include <VGCD.h>

static VGCD top;

int main()
{
	top.reset = 1;
	top.clk = 0;
	top.eval();
	top.reset = 0;
	top.clk = 1;
	top.io_value1 = 0x12345678;
	top.io_value2 = 0x87654321;
	top.io_loadingValues = 1;
	top.eval();

	while(1)
	{
		top.clk = !top.clk;
		top.eval();
	}
	return 0;
}