#ifndef __RISCV_TRAP_H__
#define __RISCV_TRAP_H__

#include <common.h>

#define INTER(code) (0x80000000 | code)
#define EXCP(code) (code)

#endif