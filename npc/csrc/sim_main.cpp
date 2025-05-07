#include <common.h>

void init_sim(int argc, char **argv);
void start_sim();

int main(int argc, char *argv[])
{
	init_sim(argc, argv);

	start_sim();

	return 0;
}
