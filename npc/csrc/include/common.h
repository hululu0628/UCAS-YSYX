#pragma once

#include <iostream>
#include <cassert>
#include <cstring>
#include <cstdint>

#define NR_GPR 32
#define NR_CSR 4

#define PMEM_START 0x80000000
#define PMEM_SIZE 0x8000000
#define PROGRAM_ENTRY PMEM_START

enum NPC_STATE {NPC_RUNNING, NPC_STOP, NPC_ABORT, NPC_QUIT};

using word_t = uint32_t;
using paddr_t = word_t;
using vaddr_t = word_t;

using NPC_state = struct NPC_state{
	NPC_STATE state;
};

using CPU_state = struct CPU_state{
    word_t gpr[NR_GPR];
    word_t csr[NR_CSR];
    vaddr_t pc;
};

extern NPC_state npc_state;

extern CPU_state cpu;
