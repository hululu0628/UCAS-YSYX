#include "common.h"
#include "mem.h"
#include <difftest.h>
#include <isa.h>

void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
void (*ref_difftest_regcpy)(void *dut, bool direction);
void (*ref_difftest_exec)(uint64_t n);
void (*ref_difftest_raise_intr)(uint64_t NO);
void (*ref_difftest_init)(int port);

void init_difftest(const char *ref_so_file, long img_size, int port) {
	assert(ref_so_file != NULL);

	void *handle;
	handle = dlopen(ref_so_file, RTLD_LAZY);
	assert(handle);

	printf("Loading %s\n", ref_so_file);

	ref_difftest_memcpy = (void (*)(paddr_t addr, void *buf, size_t n, bool direction))dlsym(handle, "difftest_memcpy");
	assert(ref_difftest_memcpy);

	ref_difftest_regcpy = (void (*)(void *dut, bool direction))dlsym(handle, "difftest_regcpy");
	assert(ref_difftest_regcpy);

	ref_difftest_exec = (void (*)(uint64_t n))dlsym(handle, "difftest_exec");
	assert(ref_difftest_exec);

	ref_difftest_raise_intr = (void (*)(uint64_t NO))dlsym(handle, "difftest_raise_intr");
	assert(ref_difftest_raise_intr);

	ref_difftest_init = (void (*)(int port))dlsym(handle, "difftest_init");
	assert(ref_difftest_init);

	ref_difftest_init(port);
	ref_difftest_memcpy(PMEM_START, guest_to_host(PMEM_START), img_size, DIFFTEST_TO_REF);
	ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);

	std::cout << "Difftest initialized" << std::endl;
}

static void check_regs(CPU_state *nemu)
{
	if(cpu.pc != nemu->pc)
	{
		std::cerr << "Error: PC mismatch: REF: 0x" << std::hex << nemu->pc << "\tDUT: 0x" << std::hex << cpu.pc << std::endl;
		isa_reg_display();
		npc_state.state = NPC_ABORT;
	}
	for(int i = 0; i < NR_GPR; i++)
	{
		if(cpu.gpr[i] != nemu->gpr[i])
		{
			std::cerr << "Error: [Next PC 0x" << std::hex << cpu.pc << "] "
			  << regs[i] << " mismatch: REF: 0x" << std::hex << nemu->gpr[i] << "\tDUT: 0x" << std::hex << cpu.gpr[i] << std::endl;
			isa_reg_display();
			npc_state.state = NPC_ABORT;
		}
	}
}

void difftest_step()
{
	CPU_state npc_ref;

	ref_difftest_exec(1);

	ref_difftest_regcpy(&npc_ref, DIFFTEST_TO_DUT);
	check_regs(&npc_ref);
}
