#include <cachesim.h>

int main(int argc, char *argv[])
{
	parse_args(argc, argv);

	init_cachesim();

	run_cachesim(cache_fp);

	end_sim();
}
