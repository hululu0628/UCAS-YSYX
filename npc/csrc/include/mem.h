#pragma once

#include <common.h>

uint8_t* guest_to_host(paddr_t paddr);
paddr_t host_to_guest(uint8_t *haddr);

uint32_t guest_read(paddr_t addr, int len);
void guest_write(paddr_t addr, int len, word_t data);
