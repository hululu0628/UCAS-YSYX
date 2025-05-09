#include "utils.h"
#include <common.h>

void init_sim(int argc, char **argv);
void monitor_start();

int is_exit_status_bad()
{
	if(npc_state.state == NPC_QUIT || (npc_state.state == NPC_END && npc_state.halt_ret == 0))
	{
		std::cout << ANSI_FG_GREEN << "Exiting NPC..." << ANSI_NONE << std::endl;
		return 0;
	}
	else
	{
		std::cout << ANSI_FG_RED << "Exiting NPC with ERROR..." << ANSI_NONE << std::endl;
		return 1;
	}
}

int main(int argc, char *argv[])
{
	init_sim(argc, argv);

	monitor_start();

	return is_exit_status_bad();
}
