#ifndef CACHE_SIM_H
#define CACHE_SIM_H

#include <stdio.h>
#include <stdint.h>
#include <assert.h>
#include <autoconf.h>

#define TAG_LEN (32 - CONFIG_INDEX_LEN - CONFIG_OFFSET_LEN)
#define INDEX_LEN CONFIG_INDEX_LEN
#define INDEX_MASK ((1 << CONFIG_INDEX_LEN) - 1)
#define OFFSET_LEN CONFIG_OFFSET_LEN
#define DATALINE_SIZE (1 << CONFIG_OFFSET_LEN)
#define DATALINE_SIZE_WORD (DATALINE_SIZE / sizeof(uint32_t))
#define NUM_BLOCKS CONFIG_BLOCK_NUM
#define NUM_SETS (1 << CONFIG_INDEX_LEN)

void init_cachesim();
int parse_args(int argc, char *argv[]);
void run_cachesim();
void end_sim();

extern FILE *cache_fp;

typedef struct {
	uint32_t tag;
	uint8_t valid;
} cache_line_t;

typedef struct {
#ifdef CONFIG_LRU
	uint32_t order;
#endif
#ifdef CONFIG_FIFO
	uint64_t entry_time;
#endif
	cache_line_t line;
} cache_block_t;

#ifdef CONFIG_PLRU
extern uint32_t acc_bits[NUM_SETS];
#endif 

extern cache_block_t cache_sets[NUM_SETS][NUM_BLOCKS];

#endif // CACHE_SIM_H