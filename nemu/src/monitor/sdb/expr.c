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

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>
#include <stdbool.h>

#define MAX_TOKEN_LEN 31
#define MAX_TOKEN_NUM 65535 //32

typedef enum TK_TYPE{
  TK_NOTYPE, TK_EQ, TK_DEC, TK_PLUS, 
  TK_SUB, TK_MUL, TK_DIV, TK_LBRACKET, TK_RBRACKET
  /* TODO: Add more token types */
} TK_TYPE;

static struct rule {
  const char *regex;
  TK_TYPE token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +", TK_NOTYPE},    // spaces
  {"\\+", TK_PLUS},         // plus
  {"-", TK_SUB},          // sub
  {"\\*", TK_MUL},         // mul
  {"/", TK_DIV},          // div
  {"\\(", TK_LBRACKET},          // left bracket
  {"\\)", TK_RBRACKET},          // right bracket
  {"[0-9]+", TK_DEC},      // decimal
  {"==", TK_EQ},        // equal
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  TK_TYPE type;
  char str[MAX_TOKEN_LEN + 1];
} Token;

static Token tokens[MAX_TOKEN_NUM] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        if(rules[i].token_type != TK_NOTYPE)
	{
		if(substr_len > MAX_TOKEN_LEN)
		{
			printf(ANSI_FG_YELLOW"The length of the token is too long!\n"ANSI_NONE);
			return false;
		}
		tokens[nr_token].type = rules[i].token_type;
		if(rules[i].token_type == TK_DEC)
		{
			strncpy(tokens[nr_token].str, substr_start, substr_len);
			tokens[nr_token].str[substr_len] = '\0';
		}
		nr_token++;
	}
	
        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

static bool check_parentheses(int l, int r, bool *success)
{
	if(tokens[l].type != TK_LBRACKET || tokens[r].type != TK_RBRACKET)
		return false;
	int cnt = 0;
	bool ret = true;
	for(int i = l + 1; i < r; i++)
	{
		if(tokens[i].type == TK_LBRACKET)
			cnt++;
		else if(tokens[i].type == TK_RBRACKET)
			cnt--;
		if(cnt < 0)
		{
			ret = false;
			if(cnt < -1)
			{
				printf("Invalid expression at %s(line %d)!\n", __func__, __LINE__);
				*success = false;
				return ret;
			}
		}

	}
	
	if(cnt != 0)
	{
		printf("Invalid expression at %s(line %d)!\n", __func__, __LINE__);
		*success = false;
		ret = false;
	}
	return ret;
}

static uint32_t eval(int l, int r, bool *success)
{
	if(l > r)
	{
		printf("Invalid expression at %s(line %d)!\n", __func__, __LINE__);
		success = false;
		return 0;
	}
	else if(l == r)
		return strtol(tokens[l].str, NULL, 0);
	else if(check_parentheses(l, r, success))
		return eval(l + 1, r - 1, success);
	else
	{
		if(*success == false)
			return 0;
		int cnt = 0;
		int op_pos = -1;
		TK_TYPE op_type = TK_NOTYPE;
		uint32_t val1, val2;
		for(int i = l; i <= r; i++)
		{
			if(tokens[i].type == TK_LBRACKET)
				cnt++;
			else if(tokens[i].type == TK_RBRACKET)
				cnt--;
			else if(cnt == 0)
			{
				if(tokens[i].type == TK_PLUS || tokens[i].type == TK_SUB)
				{
					op_type = tokens[i].type;
					op_pos = i;
				}
				else if(tokens[i].type == TK_MUL || tokens[i].type == TK_DIV)
				{
					if(op_type != TK_PLUS && op_type != TK_SUB)
					{
						op_type = tokens[i].type;
						op_pos = i;
					}
				}
			}
		}
		if(op_pos == -1)
		{
			printf("Invalid expression at %s(line %d)!\n", __func__, __LINE__);
			*success = false;
			return 0;
		}
		
		val1 = eval(l, op_pos - 1, success);
		val2 = eval(op_pos + 1, r, success);
		switch (op_type)
		{
			case TK_PLUS: return val1 + val2;
			case TK_SUB: return val1 - val2;
			case TK_MUL: return val1 * val2;
			case TK_DIV: return val1 / val2;
			default: break;
		}
	}

	assert(0);

	return 0;
}

word_t expr(char *e, bool *success) {
	if (!make_token(e)) 
	{
		*success = false;
		return 0;
	}

	/* TODO: Insert codes to evaluate the expression. */
	uint32_t result = eval(0, nr_token - 1, success);
	printf("eval: %u\n", result);
	return result;
}
