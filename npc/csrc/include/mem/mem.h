#pragma once

#include <common.h>

word_t host_read(void *addr, int len);
void host_write(void *addr, int len, word_t data);

/**
 * PADDR
 */
#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)

#define in_pmem(addr) (addr - CONFIG_MBASE < CONFIG_MSIZE)

uint8_t* guest_to_host(paddr_t paddr);
paddr_t host_to_guest(uint8_t *haddr);

uint32_t guest_read(paddr_t addr, int len);
void guest_write(paddr_t addr, int len, word_t data);

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

/**
 * VADDR
 */
#define PAGE_SHIFT        12
#define PAGE_SIZE         (1ul << PAGE_SHIFT)
#define PAGE_MASK         (PAGE_SIZE - 1)
