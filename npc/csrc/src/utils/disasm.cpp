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

#include "debug.h"
#include <dlfcn.h>
#include <capstone/capstone.h>
#include <common.h>
#include <sim/sim.h>

using cs_disasm_func_t = size_t (*)(csh handle, const uint8_t *code,
    size_t code_size, uint64_t address, size_t count, cs_insn **insn);
using cs_free_func_t = void (*)(cs_insn *insn, size_t count);
using cs_open_func_t = cs_err (*)(cs_arch arch, cs_mode mode, csh *handle);

static cs_disasm_func_t cs_disasm_dl;
static cs_free_func_t cs_free_dl;

static csh handle;

void init_disasm() {
#ifndef CAPSTONE_PATH
	Log_Error("No capstone path given! Exiting..."); 
	exit(1);
#endif
	char dlib_path[] = str(CAPSTONE_PATH);
	void *dl_handle;
	dl_handle = dlopen(dlib_path, RTLD_LAZY);
	assert(dl_handle);

	cs_open_func_t cs_open_dl = NULL;
	cs_open_dl = (cs_open_func_t)dlsym(dl_handle, "cs_open");
	assert(cs_open_dl);

	cs_disasm_dl = (cs_disasm_func_t)dlsym(dl_handle, "cs_disasm");
	assert(cs_disasm_dl);

	cs_free_dl = (cs_free_func_t)dlsym(dl_handle, "cs_free");
	assert(cs_free_dl);

	cs_arch arch = CS_ARCH_RISCV;
	cs_mode mode = CS_MODE_RISCV32;
	int ret = cs_open_dl(arch, mode, &handle);
	assert(ret == CS_ERR_OK);
}

void itrace_disasm(uint64_t pc, uint8_t *code, int nbyte) {
	cs_insn *insn;
	size_t count = cs_disasm_dl(handle, code, nbyte, pc, 0, &insn);
	assert(count == 1);
	log_write("[" << std::setw(5) << std::dec << excuted_inst_num << "]" << 
		"PC: 0x" << std::hex << top->io_debug_pc << 
		"    " << std::left << std::setw(8) << insn->mnemonic << std::right << " " << insn->op_str);
	cs_free_dl(insn, count);
}
