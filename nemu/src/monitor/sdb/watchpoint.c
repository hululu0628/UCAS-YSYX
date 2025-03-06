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

#include "common.h"
#include "sdb.h"
#include "utils.h"

WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
    wp_pool[i].attribute = WATCH_POINT;
  }

  head = NULL;
  free_ = wp_pool;
}

/* TODO: Implement the functionality of watchpoint */

WP * new_wp(point_tp type)
{
	if(free_ == NULL)
	{
		printf("No enough watchpoints!\n");
		assert(0);
	}
	WP *wp = free_;
	free_ = free_->next;
	wp->next = head;
	head = wp;
	wp->attribute = type;
	return wp;
}

bool free_wp(WP *wp)
{
	if(wp == NULL || head == NULL)
	{
		printf("Invalid watchpoint!\n");
		return false;
	}
	
	if(wp == head)
	{
		head = head->next;
		wp->next = free_;
		free_ = wp;
	}
	else
	{
		WP *p = head;
		while(p->next != wp)
		{
			if(p->next == NULL)
				return false;
			p = p->next;
		}
		p->next = wp->next;
		wp->next = free_;
		free_ = wp;
	}
	return true;
}

void check_watchpoints()
{
	WP *p = head;
	bool success = true;
	while(p != NULL)
	{
		word_t new_val = expr(p->expr, &success);
		if(!success)
		{
			printf("Invalid expression!\n");
			assert(0);
		}
		if(new_val != p->old_val)
		{
			if(p->attribute == WATCH_POINT)
			{
				printf("------------------------------------------------\n");
				printf("Hit watchpoint %d: %s\n", p->NO, p->expr);
				printf("Old value = 0x%x\n", p->old_val);
				printf("New value = 0x%x\n", new_val);
				printf("------------------------------------------------\n");
				p->old_val = new_val;
			}
			else if(p->attribute == BREAK_POINT)
			{
				printf("------------------------------------------------\n");
				printf("Hit breakpoint %d\n", p->NO);
				printf("------------------------------------------------\n");
			}
			if(nemu_state.state == NEMU_RUNNING)
				nemu_state.state = NEMU_STOP;
		}
		p = p->next;
	}
}

void print_watchpoints()
{
	WP * p = head;
	if(p == NULL)
		printf("No watchpoints!\n");
	while(p != NULL)
	{
		printf("Num     Expr\n");
		printf("%-8d%s\n", p->NO, p->expr);
		p = p->next;
	}

}
