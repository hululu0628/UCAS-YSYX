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

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

#define MAX_TOKEN_LEN 31
#define MAX_TOKEN_NUM 64 //32

typedef enum TK_TYPE{
  TK_NOTYPE, TK_DEC, TK_HEX, TK_REG, TK_LBRACKET, TK_RBRACKET, TK_PLUS, 
  TK_SUB, TK_MUL, TK_DIV, TK_EQ, TK_NEQ, TK_DEREF
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
	{"-? *((0x)|(0X))[0-9a-fA-F]+", TK_HEX},      // hex
	{"-? *[0-9]+", TK_DEC},      // decimal
	{"\\$[a-z0-9]+", TK_REG},      // register
	{"\\(", TK_LBRACKET},          // left bracket
	{"\\)", TK_RBRACKET},          // right bracket
	{"\\+", TK_PLUS},         // plus
	{"-", TK_SUB},          // sub
	{"\\*", TK_MUL},         // mul
	{"/", TK_DIV},          // div
	{"==", TK_EQ},        // equal
	{"!=", TK_NEQ}        // not equal
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
      printf("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
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
        
	if(( nr_token != 0 && 
	    (tokens[nr_token - 1].type == TK_DEC || tokens[nr_token - 1].type == TK_HEX || 
	     tokens[nr_token - 1].type == TK_RBRACKET || tokens[nr_token - 1].type == TK_REG)) 
	     && (rules[i].token_type == TK_DEC || rules[i].token_type == TK_HEX || rules[i].token_type == TK_REG))
	{
		continue;
	}
	

	char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

	/*
	Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);
	*/

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        if(rules[i].token_type != TK_NOTYPE)
	{
		if(substr_len > MAX_TOKEN_LEN)
		{
			printf(ANSI_FG_YELLOW "The length of the token is out of range!\n" ANSI_NONE);
			return false;
		}

		
		tokens[nr_token].type = rules[i].token_type;
		
		if(( nr_token == 0 || 
		    (tokens[nr_token - 1].type != TK_DEC && tokens[nr_token - 1].type != TK_HEX && 
		     tokens[nr_token - 1].type != TK_RBRACKET && tokens[nr_token - 1].type != TK_REG)) && 
		     (rules[i].token_type == TK_MUL))
		{
			tokens[nr_token].type = TK_DEREF;
		}
		
		if(rules[i].token_type == TK_DEC || rules[i].token_type == TK_HEX || rules[i].token_type == TK_REG)
		{
			int i, j;
			for(i = 0, j = 0; i < substr_len; i++)
			{
				if(substr_start[i] != ' ')
					tokens[nr_token].str[j++] = substr_start[i];
			}
			tokens[nr_token].str[j] = '\0';
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

/* Check if the expression is surrounded by a pair of parentheses 
   (which means a left bracket and a right bracket, and the two brackets are matched)
   */
static bool check_parentheses(int l, int r, bool *success)
{
	// Two brackets?
	if(tokens[l].type != TK_LBRACKET || tokens[r].type != TK_RBRACKET)
		return false;
	int cnt = 0;
	bool ret = true;
	// Are they matched?
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
	
	// check if the number of left brackets is equal to the number of right brackets
	if(cnt != 0)
	{
		printf("Invalid expression at %s(line %d)!\n", __func__, __LINE__);
		*success = false;
		ret = false;
	}
	return ret;
}

static sword_t eval(int l, int r, bool *success)
{
	if(l > r)
	{
		printf("Invalid expression at %s(line %d)!\n", __func__, __LINE__);
		*success = false;
		return 0;
	}
	else if(l == r)
	{
		if(tokens[l].type == TK_REG)
			return isa_reg_str2val(tokens[l].str, success);
		else
			return strtol(tokens[l].str, NULL, 0);
	}
	else if(check_parentheses(l, r, success))
		return eval(l + 1, r - 1, success);
	else
	{
		if(*success == false)
			return 0;
		int cnt = 0;	// +1 for left bracket, -1 for right bracket, 
		int op_pos = -1;
		TK_TYPE op_type = TK_NOTYPE;
		sword_t val1, val2;
		for(int i = l; i <= r; i++)
		{
			if(tokens[i].type == TK_LBRACKET)
				cnt++;
			else if(tokens[i].type == TK_RBRACKET)
				cnt--;
			else if(cnt == 0)
			{
				// Find the operator with the lowest precedence
				if(tokens[i].type == TK_EQ || tokens[i].type == TK_NEQ)
				{
					op_type = tokens[i].type;
					op_pos = i;
				}
				else if(tokens[i].type == TK_PLUS || tokens[i].type == TK_SUB)
				{
					if(op_type != TK_EQ && op_type != TK_NEQ)
					{
						op_type = tokens[i].type;
						op_pos = i;
					}
				}
				else if(tokens[i].type == TK_MUL || tokens[i].type == TK_DIV)
				{
					if(op_type != TK_PLUS && op_type != TK_SUB &&
					   op_type != TK_EQ && op_type != TK_NEQ)
					{
						op_type = tokens[i].type;
						op_pos = i;
					}
				}
				else if(tokens[i].type == TK_DEREF)
				{
					if(op_type != TK_PLUS && op_type != TK_SUB &&
					   op_type != TK_MUL && op_type != TK_DIV && 
					   op_type != TK_EQ && op_type != TK_NEQ)
					{
						op_type = tokens[i].type;
						op_pos = i;
					}
				}
			}
			else
			{
				Log("Number of left brackets is not equal to right brackets!\n");
				*success = false;
				return 0;
			}
		}
		if(op_pos == -1)
		{
			printf("Invalid expression at %s(line %d)!\n", __func__, __LINE__);
			*success = false;
			return 0;
		}
		// Calculate the value of the expression
		if(op_type != TK_DEREF)
			val1 = eval(l, op_pos - 1, success);
		val2 = eval(op_pos + 1, r, success);
		switch (op_type)
		{
			case TK_PLUS: return val1 + val2;
			case TK_SUB: return val1 - val2;
			case TK_MUL: return val1 * val2;
			case TK_DIV: return val1 / val2;
			case TK_EQ: return val1 == val2;
			case TK_NEQ: return val1 != val2;
			case TK_DEREF: return *((word_t *)guest_to_host(val2));
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
	word_t result = eval(0, nr_token - 1, success);
	return result;
}
