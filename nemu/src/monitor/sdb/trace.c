#include <device/map.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <elf.h>
#include <cpu/decode.h>

#ifdef CONFIG_ITRACE 
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
#endif

#ifdef CONFIG_IRINGBUF

#define IRINGBUF_SIZE 32
#define LOGBUF_SIZE 128

	static char iringbuf[IRINGBUF_SIZE][LOGBUF_SIZE];
	static int iringbuf_idx = 0;
	static unsigned iringbuf_cnt = 0;
	static int error_ptr = -1;
	static const int inst_after_error = 10;

// called after the excution of an instruction
void insert_instr(Decode * s)
{
	if(error_ptr != -1 && (error_ptr + inst_after_error) % 32 == iringbuf_idx)
		return;

	memcpy(iringbuf[iringbuf_idx], s->logbuf, LOGBUF_SIZE);

	// mark the error instruction, this may take effect difftest enabled
	if(nemu_state.state == NEMU_ABORT || nemu_state.halt_ret != 0)
		error_ptr = iringbuf_idx;

	iringbuf_idx = (iringbuf_idx + 1) % 32;
	iringbuf_cnt = iringbuf_cnt < 32 ? iringbuf_cnt + 1 : 32;
}

void print_ringbuf()
{
	int idx;
	if(error_ptr != -1)
	{
		printf("SDB: List execution sequences that contain the error instruction\n");
		if(iringbuf_cnt < 32)
			idx = 0;
		else
			idx = iringbuf_idx;
		for(int i = 0; i < iringbuf_cnt; i++, idx++)
		{
			if(idx % 32 == error_ptr)
				printf("--> %s\n", iringbuf[idx % 32]);
			else
				printf("    %s\n", iringbuf[idx % 32]);
		}
	}
}

#endif

#ifdef CONFIG_MTRACE

#define MEM_READ 0
#define MEM_WRITE 1

// called in paddr.c
void trace_mem(int op, paddr_t addr, int len, word_t data)
{
	if(addr < CONFIG_MTRACE_END && addr >= CONFIG_MTRACE_START)
	{
		if(op == MEM_READ)
			log_write("SDB: (" FMT_PADDR ") Memory read at " FMT_PADDR " with data " FMT_WORD "\n", cpu.pc, addr, data);
		else
			log_write("SDB: (" FMT_PADDR ") Memory write at " FMT_PADDR " with data " FMT_WORD "\n", cpu.pc, addr, data);
	}
}

#endif

#ifdef CONFIG_FTRACE

#define FUNC_CALL 0
#define FUNC_RET 1

MUXDEF(CONFIG_RV64, Elf64_Shdr, Elf32_Shdr) strtab_shdr;
MUXDEF(CONFIG_RV64, Elf64_Shdr, Elf32_Shdr) sym_shdr;

FILE * elf_fp = NULL;

void init_ftrace(const char * elf_file)
{
	unsigned long ret;
	MUXDEF(CONFIG_RV64, Elf64_Ehdr, Elf32_Ehdr) ehdr;

	FILE * fp = fopen(elf_file, "rb");
	Assert(fp, "Can not open \"%s\"", elf_file);
	Log("Open ELF file \"%s\" successfully", elf_file);
	elf_fp = fp;

	ret = fread(&ehdr, sizeof(ehdr), 1, fp);
	assert(ret != 0);
	Assert(
		ehdr.e_ident[0] == 0x7f &&
		ehdr.e_ident[1] == 'E' &&
		ehdr.e_ident[2] == 'L' &&
		ehdr.e_ident[3] == 'F',
		"Invalid ELF file"
	);

	ret = fseek(fp, ehdr.e_shoff, SEEK_SET);

	/* suppose that there's only one SYMTAB section */
	for(int i = 0; i < ehdr.e_shnum; i++)
	{
		ret = fread(&sym_shdr, sizeof(sym_shdr), 1, fp);
		if(sym_shdr.sh_type == SHT_SYMTAB)
			break;
	}
	/* find the corresponding STRTAB section using sh_link, which may only work when OS/ABI is SV */
	ret = fseek(fp,ehdr.e_shoff + sym_shdr.sh_link * sizeof(strtab_shdr), SEEK_SET);
	ret = fread(&strtab_shdr, sizeof(strtab_shdr), 1, fp);
	
}

// called in inst.c
void trace_func(paddr_t addr, int op)
{
	unsigned long ret;
	MUXDEF(CONFIG_RV64, Elf64_Sym, Elf32_Sym) sym;
	if(elf_fp == NULL)
		return;

	ret = fseek(elf_fp, strtab_shdr.sh_offset, SEEK_SET);
	assert(ret == 0);

	char * strtab = malloc(strtab_shdr.sh_size);	// get the string table
	
	ret = fread(strtab, strtab_shdr.sh_size, 1, elf_fp);

	ret = fseek(elf_fp, sym_shdr.sh_offset, SEEK_SET);
	
	
	for(int i = 0; i < sym_shdr.sh_size; i += sizeof(sym))
	{
		ret = fread(&sym, sizeof(sym), 1, elf_fp);
		if(MUXDEF(CONFIG_RV64, ELF64_ST_TYPE, ELF32_ST_TYPE)(sym.st_info) == STT_FUNC 
		   && addr >= sym.st_value && addr < sym.st_value + sym.st_size)
		{
			if(op == FUNC_CALL)
				log_write("SDB: (pc=" FMT_PADDR ") call   " FMT_PADDR " (in %s)\n", cpu.pc, addr, strtab + sym.st_name);
			else if(op == FUNC_RET)
				log_write("SDB: (pc=" FMT_PADDR ") return " FMT_PADDR " (in %s)\n", cpu.pc, addr, strtab + sym.st_name);
		}
	}
	free(strtab);
}

#endif

#ifdef CONFIG_DTRACE
void trace_device(paddr_t addr, int len, IOMap * map, int is_write)
{
	Log("SDB: (pc=" FMT_PADDR ") %s %d bytes from device %s at address " FMT_PADDR, cpu.pc, is_write ? "write" : "read", len, map->name, addr);
}
#endif