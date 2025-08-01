#include <cachesim.h>

#define MAX_SIZE 1

uint32_t pc_trace[MAX_SIZE];

uint64_t hit_num = 0;
uint64_t miss_num = 0;

static void update_set_lru(uint32_t index, uint32_t block_idx)
{
	for (int i = 0; i < NUM_BLOCKS; i++) {
		if (cache_sets[index][i].line.valid) {
			if (i == block_idx) {
				cache_sets[index][i].order = 0;
			} else {
				cache_sets[index][i].order++;
			}
		}
	}
}

static uint32_t find_block_lru(uint32_t index)
{
	uint32_t max_order = 0;
	uint32_t block_idx = 0;

	for (int i = 0; i < NUM_BLOCKS; i++) {
		if (!cache_sets[index][i].line.valid) {
			return i;
		}
		if (cache_sets[index][i].order > max_order) {
			max_order = cache_sets[index][i].order;
			block_idx = i;
		}
	}
	return block_idx;
}

static void sim_cache(uint32_t pc)
{
	uint32_t index = (pc >> OFFSET_LEN) & INDEX_MASK;
	uint32_t tag = pc >> (OFFSET_LEN + INDEX_LEN);

	cache_block_t *block = &cache_sets[index][0];
	for(int i = 0; i < NUM_BLOCKS; i++) {
		if(block[i].line.valid && block[i].line.tag == tag) {
			hit_num++;
			update_set_lru(index, i);
			return;
		}
	}
	miss_num++;

	uint32_t new_bidx = find_block_lru(index);
	block[new_bidx].line.valid = 1;
	block[new_bidx].line.tag = tag;
	update_set_lru(index, new_bidx);
}

void run_cachesim(FILE *cache_fp)
{
	int ret;
	while((ret = fread(&pc_trace, sizeof(pc_trace), MAX_SIZE, cache_fp)) > 0)
	{
		for(int i = 0; i < ret; i++)
			sim_cache(pc_trace[i]);
	}
}

void end_sim()
{
	fclose(cache_fp);
	printf("Cache simulation finished.\n");
	printf("Hit count: %lu\n", hit_num);
	printf("Miss count: %lu\n", miss_num);
}
