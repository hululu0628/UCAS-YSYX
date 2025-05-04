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

#ifndef __RISCV_REG_H__
#define __RISCV_REG_H__

#include <common.h>

#define REG_ZERO	0
#define REG_RA		1
#define REG_SP		2
#define REG_GP		3
#define REG_TP		4
#define REG_T0		5
#define REG_T1		6
#define REG_T2		7
#define REG_S0		8
#define REG_S1		9
#define REG_A0		10
#define REG_A1		11
#define REG_A2		12
#define REG_A3		13
#define REG_A4		14
#define REG_A5		15
#define REG_A6		16
#define REG_A7		17
#define REG_S2		18
#define REG_S3		19
#define REG_S4		20
#define REG_S5		21
#define REG_S6		22
#define REG_S7		23
#define REG_S8		24
#define REG_S9		25
#define REG_S10		26
#define REG_S11		27
#define REG_T3		28
#define REG_T4		29
#define REG_T5		30
#define REG_T6		31

static inline int check_reg_idx(int idx) {
	IFDEF(CONFIG_RT_CHECK, assert(idx >= 0 && idx < MUXDEF(CONFIG_RVE, 16, 32)));
	return idx;
}

#define gpr(idx) (cpu.gpr[check_reg_idx(idx)])


#define CSR_MSTATUS 0x300
#define CSR_MTVEC   0x305
#define CSR_MEPC    0x341
#define CSR_MCAUSE  0x342

typedef enum {
	MSTATUS,
	MTVEC,
	MEPC,
	MCAUSE
} csr_idx_t;

static inline int check_csr_idx(int idx) {
	csr_idx_t csr_idx;
	switch (idx) {
		case CSR_MSTATUS: csr_idx = MSTATUS; break;
		case CSR_MTVEC:   csr_idx = MTVEC;   break;
		case CSR_MEPC:    csr_idx = MEPC;    break;
		case CSR_MCAUSE:  csr_idx = MCAUSE;  break;
		default: assert(0);
	}
	return csr_idx;
}

#define csr(idx) (cpu.csr[check_csr_idx(idx)])

static inline const char* reg_name(int idx) {
	extern const char* regs[];
	return regs[check_reg_idx(idx)];
}

#endif
