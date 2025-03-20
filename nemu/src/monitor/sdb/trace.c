#include "common.h"
#include <stdio.h>
#include <string.h>
#include <cpu/decode.h>

#define IRINGBUF_SIZE 32
#define LOGBUF_SIZE 128

#ifdef CONFIG_IRINGBUF
	static char iringbuf[IRINGBUF_SIZE][LOGBUF_SIZE];
	static int iringbuf_idx = 0;
	static unsigned iringbuf_cnt = 0;
	static int error_ptr = -1;
	static const int inst_after_error = 10;
#endif

void inst_log(Decode * s)
{
	char *p = s->logbuf;
	p += snprintf(p, sizeof(s->logbuf), FMT_WORD ":", s->pc);
	int ilen = s->snpc - s->pc;
	int i;
	uint8_t *inst = (uint8_t *)&s->isa.inst;
#ifdef CONFIG_ISA_x86
	for (i = 0; i < ilen; i ++) {
#else
	for (i = ilen - 1; i >= 0; i --) {
#endif
	p += snprintf(p, 4, " %02x", inst[i]);
	}
	int ilen_max = MUXDEF(CONFIG_ISA_x86, 8, 4);
	int space_len = ilen_max - ilen;
	if (space_len < 0) space_len = 0;
	space_len = space_len * 3 + 4;		// 3 spaces for each byte
	memset(p, ' ', space_len);
	p += space_len;

	void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
	disassemble(p, s->logbuf + sizeof(s->logbuf) - p,
		MUXDEF(CONFIG_ISA_x86, s->snpc, s->pc), (uint8_t *)&s->isa.inst, ilen);
}

void insert_instr(Decode * s)
{
	if(error_ptr != -1 && (error_ptr + inst_after_error) % 32 == iringbuf_idx)
		return;

	memcpy(iringbuf[iringbuf_idx], s->logbuf, LOGBUF_SIZE);

	if(nemu_state.state == NEMU_ABORT || nemu_state.halt_ret != 0)
		error_ptr = iringbuf_idx;

	iringbuf_idx = (iringbuf_idx + 1) % 32;
	iringbuf_cnt = iringbuf_cnt < 32 ? iringbuf_cnt + 1 : 32;
}

void print_ringbuf()
{
	int i;
	if(error_ptr != -1)
	{
		printf("SDB: List execution sequences that contain the error instruction\n");
		if(iringbuf_cnt < 32)
			i = 0;
		else
			i = (iringbuf_idx + 1) % 32;
		for(; i < iringbuf_cnt; i++)
		{
			if(i % 32 == error_ptr)
				printf("--> %s\n", iringbuf[i % 32]);
			else
				printf("    %s\n", iringbuf[i % 32]);
		}
	}
}

typedef enum {MEM_READ, MEM_WRITE} mtrace_t;

void trace_mem(mtrace_t op, paddr_t addr, int len, word_t data)
{
	if(addr < CONFIG_MTRACE_END && addr >= CONFIG_MTRACE_START)
	{
		if(op == MEM_READ)
			log_write("SDB: Memory read at " FMT_PADDR " with data " FMT_WORD "\n", addr, data);
		else
			log_write("SDB: Memory write at " FMT_PADDR " with data " FMT_WORD "\n", addr, data);
	}
}