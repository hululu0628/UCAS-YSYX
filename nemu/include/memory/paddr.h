/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>
#include "soc.h"

#define PMEM_LEFT(idx)  (soc_devices[idx].base)
#define PMEM_RIGHT(idx) (soc_devices[idx].base + (soc_devices[idx].size - 1))
#define RESET_VECTOR (CONFIG_RESET_BASE + CONFIG_PC_RESET_OFFSET)

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr, int soc_idx);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr, int soc_idx);

static inline bool in_pmem(paddr_t addr) {
	return (get_soc_index(addr) == -1) ? false : true;
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

#endif
