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

#ifndef __ISA_RISCV_H__
#define __ISA_RISCV_H__

#include <common.h>

#define XLEN MUXDEF(CONFIG_RV64, 64, 32)

#define NR_GPR MUXDEF(CONFIG_RVE, 16, 32)
#define NR_CSR 4 // In PA3.1: mstatus, mtvec, mepc, mcause

/* NOTE: remember to modify diff_context_t in difftest.cc at the same time to enable difftest */
/* Notice that different csrs have different width. 
   In this implementation, we assume all csrs are word_t, 
   that is, it denpends on the XLEN. */
typedef struct {
  word_t gpr[NR_GPR];
  word_t csr[NR_CSR];
  vaddr_t pc;
} MUXDEF(CONFIG_RV64, riscv64_CPU_state, riscv32_CPU_state);

// decode
typedef struct {
  uint32_t inst;
} MUXDEF(CONFIG_RV64, riscv64_ISADecodeInfo, riscv32_ISADecodeInfo);

#define isa_mmu_check(vaddr, len, type) (MMU_DIRECT)

#endif
