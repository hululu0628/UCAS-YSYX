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

#include "utils.h"
#include <isa.h>
#include <debug.h>
#include <mem.h>
#include <sdb.h>
#include <readline/readline.h>
#include <readline/history.h>

#define NR_CMD ARRLEN(cmd_table)

static int is_batch_mode = false;

void init_regex();
word_t expr(char *e, bool *success);
void sim_step(uint64_t n);

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() 
{
	static char *line_read = NULL;

	if (line_read) 
	{
		free(line_read);
		line_read = NULL;
	}

	line_read = readline("(npc) ");

	if (line_read && *line_read) 
	{
		add_history(line_read);
	}

	return line_read;
}

static int cmd_c(char *args)
{
	sim_step(-1);
	return 0;
}

static int cmd_s(char *args)
{
	char *steps = strtok(NULL, " ");
	uint64_t n = steps ? atoi(steps) : 1;
	sim_step(n);
	return 0;
}

static int cmd_info(char *args)
{
	char * arg = strtok(NULL, " ");
	if(arg == NULL)
	{
		printf("INFO: missing subcommand\n");
	}
	else if(strcmp(arg, "r") == 0 || strcmp(arg, "reg") == 0)
	{
		isa_reg_display();
	}
	else
	{
		printf("INFO: unknown subcommand \"%s\"\n", arg);
	}
	return 0;
}

static int cmd_x(char *args)
{
	char * arg;
	arg = strtok(NULL, " ");
	if(arg == NULL)
	{
		printf("X: missing N\n");
		return 0;
	}
	else
	{
		int n = atoi(arg);
		arg = strtok(NULL, " ");
		if(arg == NULL)
		{
			printf("X: missing EXPR\n");
			return 0;
		}
		else
		{
			word_t addr = strtol(arg, NULL, 0);
			for(int i = 0; i < n; i += 4)
			{
				printf(ANSI_FG_BLUE "0x%x" ANSI_NONE ":        ", addr);
				for(int j = 0; j < 4; j++)
				{
					if(j + i >= n)
						break;
					printf("0x%08x    ", (*(uint32_t *)(guest_to_host(addr))));
					addr += 4;
				}
				printf("\n");
			}
		}
	}
	return 0;
}

static int cmd_p(char *args)
{
	bool success = true;
	uint32_t result;
	if(args == NULL)
	{
		printf("P: missing EXPR\n");
		return 0;
	}
	result = expr(args, &success);

	if(success)
	{
		printf("0x%x\n", result);
	}
	else
	{
		printf("P: invalid expression\n");
	}
	return 0;
}

static int cmd_w(char *args)
{
	bool success = true;
	WP *wp = new_wp(WATCH_POINT);
	strcpy(wp->expr, args);
	wp->old_val = expr(wp->expr, &success);
	if(!success)
	{
		free_wp(wp);
		printf("W: invalid expression\n");
		return 0;
	}
	printf("Set watchpoint %d\n", wp->NO);
	return 0;
}

static int cmd_d(char *args)
{
	char * arg = strtok(NULL, " ");
	if(arg == NULL)
	{
		printf("D: missing N\n");
	}
	else
	{
		int n = atoi(arg);
		if(n >= 0 && n < NR_WP)
		{
			if(free_wp(&wp_pool[n]))
			{
				printf("Watchpoint %d deleted\n", n);
			}
			else
			{
				printf("D: failed to delete the watchpoint\n");
			}
		}
	}
	return 0;
}

static int cmd_b(char *args)
{
	bool success = true;
	WP *wp = new_wp(BREAK_POINT);
	strcpy(wp->expr, args);
	strcat(wp->expr, " == $pc");
	wp->old_val = expr(wp->expr, &success);
	wp->old_val = 0;
	if(!success)
	{
		free_wp(wp);
		printf("W: invalid expression\n");
		return 0;
	}
	printf("Set breakpoint %d\n", wp->NO);
	return 0;
}

static int cmd_q(char *args) 
{
	npc_state.state = NPC_QUIT;
	return -1;
}

static int cmd_help(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
	{ "help", "Display information about all supported commands", cmd_help },
	{ "c", "Continue the execution of the program", cmd_c },
	{ "s", "Step program. Usage: s [N]", cmd_s },
	{ "info", "Print the program status. Use formats such as \"info SUBCMD\".", cmd_info },
	{ "x", "Find and print the value of the expression EXPR. Use formats such as \"x N EXPR\".", cmd_x },
	{ "p", "Print the value of the expression EXPR. Use formats such as \"p EXPR\"", cmd_p },
	{ "w", "Set a watchpoint for the expression EXPR", cmd_w },
	{ "d", "Delete the watchpoint with the number N", cmd_d },
	{ "b", "Set a breakpoint at the address ADDR", cmd_b },
	{ "q", "Exit NEMU", cmd_q },

	/* TODO: Add more commands */

};

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("  %-6s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("  %-6s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
  stdout_write("Batch mode enabled");
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  // get conmandlines
  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb()
{
	init_regex();
	init_wp_pool();
}
