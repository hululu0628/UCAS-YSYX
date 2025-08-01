#include <common.h>
#include <sim/sim.h>

void init_sim(int argc, char **argv);
void monitor_start();
void sim_end();
void close_device();

int is_exit_status_bad()
{
	if(npc_state.state == NPC_QUIT || (npc_state.state == NPC_END && npc_state.halt_ret == 0))
	{
		stdout_write(ANSI_FG_GREEN << "Exiting NPC..." << ANSI_NONE);
		return 0;
	}
	else
	{
		stdout_write(ANSI_FG_RED << "Exiting NPC with ERROR..." << ANSI_NONE);
		return 1;
	}
}

int main(int argc, char *argv[])
{
	init_sim(argc, argv);

	monitor_start();

	sim_end();
	
	IFDEF(CONFIG_DEVICE, close_device());

	return is_exit_status_bad();
}
