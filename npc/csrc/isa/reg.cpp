#include <isa.h>

const char *regs[] = {
	"$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
	"s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
	"a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
	"s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

const char *csr_names[] = {
	"mstatus", "mtvec", "mepc", "mcause"
};

void isa_reg_display() 
{
	printf("pc              0x%-20x%u\n", cpu.pc, cpu.pc);
	for (int i = 0; i < NR_GPR; i++) 
	{
		printf("%-16s0x%-20x%u\n", regs[i], cpu.gpr[i], cpu.gpr[i]);
	}
}

word_t isa_reg_str2val(const char *s, bool *success) 
{
	if (strcmp(s + 1, "pc") == 0) 
	{
		return cpu.pc;
	}
	for (int i = 0; i < NR_GPR; i++) 
	{
		if (strcmp(s + 1, regs[i]) == 0) 
		{
			*success = true;
			return cpu.gpr[i];
		}
	}
	*success = false;
	return 0;
}
