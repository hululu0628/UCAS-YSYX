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
