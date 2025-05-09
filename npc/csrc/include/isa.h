#pragma once

#include <common.h>

enum {REG_ZERO, REG_RA, REG_SP, REG_GP, REG_TP, REG_T0, REG_T1, REG_T2,
      REG_S0, REG_S1, REG_A0, REG_A1, REG_A2, REG_A3, REG_A4, REG_A5,
      REG_A6, REG_A7, REG_S2, REG_S3, REG_S4, REG_S5, REG_S6, REG_S7,
      REG_S8, REG_S9, REG_S10,REG_S11};

extern const char *regs[];
extern const char *csr_names[];

void isa_reg_display();
word_t isa_reg_str2val(const char *s, bool *success);
