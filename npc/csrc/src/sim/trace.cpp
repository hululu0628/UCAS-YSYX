#include <isa.h>
#include <sim/sdb.h>
#include <sim/sim.h>
#include <debug.h>
#include <elf.h>

#ifdef CONFIG_TRACE

void itrace_disasm(uint64_t pc, uint8_t *code, int nbyte);

#ifdef CONFIG_ITRACE

void trace_instruction()
{
	IFDEF(CONFIG_ITRACE_OUTPUT,
		log_write("[" << std::setw(5) << std::dec << excuted_inst_num << "]" << 
			"PC: 0x" << std::hex << debug_signal.pc << 
			" wen: " << (unsigned)debug_signal.wen << 
			" [" << regs[debug_signal.waddr] << 
			"] data: 0x" << std::left << std::setw(9) << debug_signal.data << std::right);
	)

	IFDEF(CONFIG_ITRACE_DISASM,
		itrace_disasm(debug_signal.pc, (uint8_t *)&debug_signal.inst, 4);
	)
}

#endif

#ifdef CONFIG_MTRACE

void trace_rmem(paddr_t addr, word_t data)
{
	log_write("[PC: 0x" << std::hex << debug_signal.pc << "] Aligned memory read at 0x" << addr << " with data 0x" << data);
}
void trace_wmem(paddr_t addr, word_t data, unsigned char mask)
{
	log_write("[PC: 0x" << std::hex << debug_signal.pc << "] Aligned memory write at 0x" << addr << " with data 0x" << data 
		<< "   Mask: 0x" << std::hex << (unsigned)mask);
}

#endif

#ifdef CONFIG_FTRACE

#define OPCODE_MASK 0x7f
#define GPR_MASK 0x1f
#define RD_SHIFT 7
#define RS1_SHIFT 15
#define JAL_OP 0b1101111
#define JALR_OP 0b1100111

enum { FUNC_CALL, FUNC_RET };

Elf32_Shdr strtab_shdr;
Elf32_Shdr sym_shdr;

FILE * elf_fp = NULL;

void init_ftrace(char * elf_file)
{
	unsigned long ret;
	MUXDEF(CONFIG_RV64, Elf64_Ehdr, Elf32_Ehdr) ehdr;

	FILE * fp = fopen(elf_file, "rb");
	Assert(fp, "Can not open " << elf_file);
	Log("Open ELF file " << elf_file << " successfully");
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

int ftraceCheck(word_t code, int * op)
{
	if((code & OPCODE_MASK) == JALR_OP)
	{
		if(((code >> RD_SHIFT) & GPR_MASK) == REG_RA)
		{
			*op = FUNC_CALL;
			return 1;
		}
		else if(((code >> RS1_SHIFT) & GPR_MASK) == REG_RA)
		{
			*op = FUNC_RET;
			return 1;
		}
	}
	else if((code & OPCODE_MASK) == JAL_OP)
	{
		if(((code >> RD_SHIFT) & GPR_MASK) == REG_RA)
		{
			*op = FUNC_CALL;
			return 1;
		}
	}
	return 0;
}

void trace_func(paddr_t addr, word_t code)
{
	int op;
	if(!ftraceCheck(code, &op))
		return;

	unsigned long ret;
	MUXDEF(CONFIG_RV64, Elf64_Sym, Elf32_Sym) sym;
	if(elf_fp == NULL)
		return;

	ret = fseek(elf_fp, strtab_shdr.sh_offset, SEEK_SET);
	assert(ret == 0);

	char * strtab = (char *)malloc(strtab_shdr.sh_size);	// get the string table
	
	ret = fread(strtab, strtab_shdr.sh_size, 1, elf_fp);

	ret = fseek(elf_fp, sym_shdr.sh_offset, SEEK_SET);
	
	
	for(int i = 0; i < sym_shdr.sh_size; i += sizeof(sym))
	{
		ret = fread(&sym, sizeof(sym), 1, elf_fp);
		if(MUXDEF(CONFIG_RV64, ELF64_ST_TYPE, ELF32_ST_TYPE)(sym.st_info) == STT_FUNC 
		   && addr >= sym.st_value && addr < sym.st_value + sym.st_size)
		{
			if(op == FUNC_CALL)
				log_write("[ SDB ]At PC = " << FMT_PADDR(addr) << "   call     " 
						<< FMT_PADDR(cpu.pc) << " (in " << strtab + sym.st_name <<")");
			else if(op == FUNC_RET)
				log_write("[ SDB ]At PC = " << FMT_PADDR(addr) << "   return   " 
						<< FMT_PADDR(cpu.pc) << " (in " << strtab + sym.st_name <<")");
		}
	}
	free(strtab);
}

#endif

#ifdef CONFIG_DTRACE
void trace_rdevice(paddr_t addr, int len, word_t data, const char* name)
{
	log_write(
		"[ SDB ]At PC = " << FMT_PADDR(debug_signal.pc) << "   read "
		<< len <<"-byte long Data 0x" << std::hex << data 
		<< " from device " << name <<" at Address " << FMT_PADDR(addr)
	);
}
void trace_wdevice(paddr_t addr, int len, word_t data, const char* name)
{
	log_write(
		"[ SDB ]At PC = " << FMT_PADDR(debug_signal.pc) << "   write "
		<< len <<"-byte long Data 0x"<< std::hex << data 
		<<" to device " << name <<" at Address " << FMT_PADDR(addr)
	);
}
#endif

#endif
