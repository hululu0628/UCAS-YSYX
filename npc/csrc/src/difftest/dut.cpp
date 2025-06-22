#include "sim/sim.h"
#include "debug.h"
#include <mem/mem.h>
#include <difftest/difftest.h>
#include <isa.h>

void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
void (*ref_difftest_regcpy)(void *dut, bool direction);
void (*ref_difftest_exec)(uint64_t n);
void (*ref_difftest_raise_intr)(uint64_t NO);
void (*ref_difftest_init)(int port);

bool skip_inst = false;

void init_difftest(const char *ref_so_file, long img_size, int port) {
	assert(ref_so_file != NULL);

	void *handle;
	handle = dlopen(ref_so_file, RTLD_LAZY);
	assert(handle);

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
	// can not initialize SRAM for it is on the SoC
	ref_difftest_memcpy(FLASH_START, guest_to_host_flash(FLASH_START), img_size, DIFFTEST_TO_REF);
	ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);

	Log("Difftest initialized");
}

static void check_regs(CPU_state *nemu)
{
	if(cpu.pc != nemu->pc)
	{
		Log_Error("Error: PC mismatch: REF: 0x" << std::hex << nemu->pc << "\tDUT: 0x" << std::hex << cpu.pc);
		isa_reg_display();
		npc_state.state = NPC_ABORT;
	}
	for(int i = 0; i < NR_GPR; i++)
	{
		if(cpu.gpr[i] != nemu->gpr[i])
		{
			Log_Error("Error: [PC 0x" << std::hex << debug_signal.pc << "] ["
			  << regs[i] << "] mismatch: REF: 0x" << std::hex << nemu->gpr[i] << "\tDUT: 0x" << std::hex << cpu.gpr[i]);
			isa_reg_display();
			npc_state.state = NPC_ABORT;
		}
	}
}

void difftest_step()
{
	CPU_state npc_ref;

	if(skip_inst)
	{
		ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
		skip_inst = false;
		return;
	}
	ref_difftest_exec(1);
	ref_difftest_regcpy(&npc_ref, DIFFTEST_TO_DUT);
	check_regs(&npc_ref);
}

void difftest_skip_ref()
{
	skip_inst = true;
}
