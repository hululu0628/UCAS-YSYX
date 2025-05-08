#include <common.h>

void init_sim(int argc, char **argv);
void monitor_start();

int main(int argc, char *argv[])
{
	init_sim(argc, argv);

	monitor_start();

	return 0;
}
