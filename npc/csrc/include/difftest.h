#pragma once

#include <common.h>
#include <dlfcn.h>

enum {DIFFTEST_TO_DUT, DIFFTEST_TO_REF};

extern void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(uint64_t n);
extern void (*ref_difftest_raise_intr)(uint64_t NO);
extern void (*ref_difftest_init)(int port);
