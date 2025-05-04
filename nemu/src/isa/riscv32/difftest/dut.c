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

#include <isa.h>
#include <cpu/difftest.h>

bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc) 
{
	// after a single step, does ref.pc update?
	if(ref_r->pc != cpu.pc)
	{
		Log("Next pc is different");
		printf("pc = " FMT_WORD ", ref_pc = " FMT_WORD "\n", cpu.pc, ref_r->pc);
		return false;
	}
	for(int i = 0; i < NR_GPR; i++)
	{
		if(ref_r->gpr[i] != cpu.gpr[i])
		{
			Log("reg[%d] is different after executing instruction at pc = " FMT_WORD, i, pc);
			printf("reg[%d] = " FMT_WORD ", ref_r[%d] = " FMT_WORD "\n", i, cpu.gpr[i], i, ref_r->gpr[i]);

			return false;
		}
	}
	return true;
}

void isa_difftest_attach() {
}
