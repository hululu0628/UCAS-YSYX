#pragma once

#include <common.h>

word_t host_read(void *addr, int len);
void host_write(void *addr, int len, word_t data);

/**
 * PADDR
 */
#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)

#define in_pmem(addr) (addr - CONFIG_MBASE < CONFIG_MSIZE)

uint8_t* guest_to_host(paddr_t paddr);
paddr_t host_to_guest(uint8_t *haddr);

uint8_t* guest_to_host_flash(paddr_t paddr);
paddr_t host_to_guest_flash(uint8_t *haddr);

uint8_t* guest_to_host_psram(paddr_t paddr);
paddr_t host_to_guest_psram(uint8_t *haddr);

uint8_t* guest_to_host_sdram(paddr_t paddr);
paddr_t host_to_guest_sdram(uint8_t *haddr);

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

int checkAccBound(int valid, word_t addr);

/**
 * VADDR
 */
#define PAGE_SHIFT        12
#define PAGE_SIZE         (1ul << PAGE_SHIFT)
#define PAGE_MASK         (PAGE_SIZE - 1)

// SoC flash api
extern void flash_write(uint32_t addr, uint32_t *data, int len);
