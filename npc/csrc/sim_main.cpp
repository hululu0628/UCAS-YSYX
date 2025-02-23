#include <Vtop.h>
#include <nvboard.h>

void nvboard_bind_all_pins(Vtop* top);

static Vtop top;

void single_cycle() {
	top.clk = 0; top.eval();
	top.clk = 1; top.eval();
}

void reset(int n) {
	top.rst = 1;
	while (n-- > 0) single_cycle();
	top.rst = 0;
}

int main()
{
	nvboard_bind_all_pins(&top);
	nvboard_init();
	reset(10);
	while(1) {
		nvboard_update();
		single_cycle();
	}
	nvboard_quit();

	return 0;
}