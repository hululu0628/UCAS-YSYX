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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

// this should be enough
#define BUF_LEN 65535

static char buf[BUF_LEN] = {};
static char code_buf[BUF_LEN + 128] = {}; // a little larger than `buf`
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";

uint32_t buf_len = 0;
uint32_t success = 1;

static uint32_t choose(uint32_t n)
{
	return rand() % n;
}
static void gen(char c)
{
	if(buf_len >= BUF_LEN / 2 || !success)
	{
		success = 0;
		return;
	}
	buf[buf_len] = c;
	buf[buf_len + 1] = '\0';
	buf_len++;
}
static void gen_rand_whitespace()
{
	int length = choose(5);
	while(length--)
	{
		gen(' ');
	}
}
static void gen_rand_op()
{
	gen_rand_whitespace();
	switch(choose(4))
	{
		case 0: gen('+'); break;
		case 1: gen('-'); break;
		case 2: gen('*'); break;
		case 3: gen('/'); break;
	}
	gen_rand_whitespace();
}
static void gen_rand_num()
{
	gen_rand_whitespace();
	int length = choose(3) + 1;
	int n = choose(10);
	if(n == 0)
	{
		gen('1');
	}
	else
	{
		gen('0' + n);
	}
	length--;
	while(length--)
	{
		gen('0' + choose(10));
	}
	gen_rand_whitespace();
}

static void gen_rand_expr()
{
	if(buf_len >= BUF_LEN / 2 || !success)
	{
		success = 0;
		return;
	}
	switch (choose(3))
	{
		case 0: gen_rand_num(); break;
		case 1: gen('('); gen_rand_expr(); gen(')'); break;
		case 2: gen_rand_expr(); gen_rand_op(); gen_rand_expr(); break;
	}
}

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
  for (i = 0; i < loop; i ++) {
    buf_len = 0;
    success = 1;
    gen_rand_expr();
    //printf("test\n");
    if(!success)
    {
    	i--;
    	continue;
    }
    //printf("test\n");


    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);


    int ret = system("gcc /tmp/.code.c -o /tmp/.expr -Werror");
    if (ret != 0)
    {
      i--;
      continue;
    }


    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}
