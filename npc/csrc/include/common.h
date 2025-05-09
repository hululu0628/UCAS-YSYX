#pragma once

#include <iostream>
#include <fstream>
#include <cassert>
#include <cstring>
#include <cstdint>
#include "macro.h"
#include "utils.h"

#define NR_GPR 32
#define NR_CSR 4

#define PMEM_START 0x80000000
#define PMEM_SIZE 0x8000000
#define PROGRAM_ENTRY PMEM_START

using sword_t = int32_t;
using word_t = uint32_t;
using paddr_t = word_t;
using vaddr_t = word_t;

using NPC_state = struct NPC_state{
	NPC_STATE state;
	int halt_ret;
};

using CPU_state = struct CPU_state{
    word_t gpr[NR_GPR];
    word_t csr[NR_CSR];
    vaddr_t pc;
};

extern NPC_state npc_state;

extern CPU_state cpu;
