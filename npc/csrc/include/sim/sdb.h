#pragma once

#include <common.h>

#define NR_WP 32
#define WP_EXPR_LEN 32

#define WATCH_POINT 1
#define BREAK_POINT 2

typedef int point_tp;

typedef struct watchpoint {
	int NO;
	point_tp attribute;
	struct watchpoint *next;

	/* TODO: Add more members if necessary */
	char expr[WP_EXPR_LEN];
	word_t old_val;
} WP;

extern WP wp_pool[NR_WP];

word_t expr(char *e, bool *success);


void init_wp_pool();
WP * new_wp(point_tp type);
bool free_wp(WP *wp);
void print_watchpoints();
void check_watchpoints();

/* trace */
void trace_instruction();
void trace_rmem(paddr_t addr, word_t data);
void trace_wmem(paddr_t addr, word_t data, unsigned char mask);
void init_ftrace(char * elf_file);
void trace_func(paddr_t addr, word_t code);
void trace_rdevice(paddr_t addr, int len, word_t data, const char* name);
void trace_wdevice(paddr_t addr, int len, word_t data, const char* name);
