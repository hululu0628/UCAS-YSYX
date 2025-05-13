#pragma once

#include "autoconf.h"

#include <iostream>
#include <iomanip>
#include <string>
#include <stdlib.h>
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

#define FMT_PADDR(addr) "0x" << std::hex << std::setw(8) << std::setfill('0') << addr << std::setfill(' ')
#define FMT_WORD(data) "0x" << std::hex << std::setw(8) << std::setfill('0') << data << std::setfill(' ')

using sword_t = int32_t;
using word_t = uint32_t;
using paddr_t = word_t;
using vaddr_t = word_t;
using ioaddr_t = uint16_t;

using NPC_state = struct NPC_state {
	NPC_STATE state;
	int halt_ret;
};

using CPU_state = struct CPU_state {
    word_t gpr[NR_GPR];
    word_t csr[NR_CSR];
    vaddr_t pc;
};

extern NPC_state npc_state;

extern CPU_state cpu;

extern char *log_file;
extern char *elf_file;
extern char *img_file;
extern char *diff_file;
extern char *wave_file;
