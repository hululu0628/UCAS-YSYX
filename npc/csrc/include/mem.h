#pragma once

#include <common.h>

uint8_t* guest_to_host(uint32_t paddr);
uint32_t host_to_guest(uint8_t *haddr);

uint32_t guest_read(uint32_t addr, int len);
void guest_write(uint32_t addr, int len, uint32_t data);
