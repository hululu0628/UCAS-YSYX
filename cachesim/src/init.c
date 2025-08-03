#include <cachesim.h>
#include <getopt.h>

FILE *cache_fp = NULL;
static const char *pc_trace_file = NULL;

cache_block_t cache_sets[NUM_SETS][NUM_BLOCKS];

int parse_args(int argc, char *argv[]) {
	const struct option table[] = {
		{"trace"    , required_argument, NULL, 'c'},
		{0          , 0                , NULL,  0 },
	};
	int o;
	while ( (o = getopt_long(argc, argv, "-c:", table, NULL)) != -1) {
		switch (o) {
			case 'c': pc_trace_file = optarg; break;
			case 1: pc_trace_file = optarg; break;
			default: return 0;
		}
	}
	return 0;
}

void init_cachesim()
{
	/* Open the trace file for reading */
	cache_fp = fopen(pc_trace_file, "rb");
	assert(cache_fp != NULL);
	// Information
	printf("PC trace for cachesim in: %s\n", pc_trace_file);
	printf("Cache Size:\n");
	printf("  %d sets, %d blocks per set, %d bytes per block\n",
	       NUM_SETS, NUM_BLOCKS, DATALINE_SIZE);
	
	for(int i = 0; i < NUM_SETS; i++) {
		for(int j = 0; j < NUM_BLOCKS; j++) {
			cache_sets[i][j].line.valid = 0;
			cache_sets[i][j].line.tag = 0;
		}
	}
}
