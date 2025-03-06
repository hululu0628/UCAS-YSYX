/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#ifndef __SDB_H__
#define __SDB_H__

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


#endif
