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

#include "common.h"
#include "macro.h"
#include "memory/soc.h"
#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
int soc_device_num = 0;
soc_device_t soc_devices[MAX_SOC_DEVICES] = {0}; // 16 devices max

IFDEF(CONFIG_MROM, static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {};)
IFDEF(CONFIG_SRAM, static uint8_t sram[CONFIG_SRAM_SIZE] PG_ALIGN = {};)
#endif

int get_soc_index(paddr_t addr)
{
	for(int i = 0; i < soc_device_num; i++)
	{
		if(soc_devices[i].base <= addr && addr < soc_devices[i].base + soc_devices[i].size)
		{
			return i;
		}
	}
	return -1; // not found
}

uint8_t* guest_to_host(paddr_t paddr, int soc_idx)
{
	if(unlikely(soc_idx < 0 || soc_idx >= soc_device_num)) {
		panic("Invalid SoC index: %d", soc_idx);
	}
	return soc_devices[soc_idx].mem + paddr - soc_devices[soc_idx].base;
}
paddr_t host_to_guest(uint8_t *haddr, int soc_idx)
{
	if(unlikely(soc_idx < 0 || soc_idx >= soc_device_num)) {
		panic("Invalid SoC index: %d", soc_idx);
	}
	return soc_devices[soc_idx].base + (haddr - soc_devices[soc_idx].mem);
}

static word_t pmem_read(paddr_t addr, int len, int soc_idx) {
  word_t ret = host_read(guest_to_host(addr, soc_idx), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data, int soc_idx) {
  host_write(guest_to_host(addr, soc_idx), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound at pc = " FMT_WORD,
      addr, cpu.pc);
}

void init_soc()
{
	#ifdef CONFIG_MROM
	soc_devices[soc_device_num++] = (soc_device_t) {
		.name = "mrom",
		.base = CONFIG_MROM_BASE,
		.size = CONFIG_MROM_SIZE,
		.mem = mrom,
		.priv = PRIV_R | PRIV_X
	};
	#endif
	#ifdef CONFIG_SRAM
	soc_devices[soc_device_num++] = (soc_device_t) {
		.name = "sram",
		.base = CONFIG_SRAM_BASE,
		.size = CONFIG_SRAM_SIZE,
		.mem = sram,
		.priv = PRIV_R | PRIV_W
	};
	#endif

	
	for(int i = 0; i < soc_device_num; i++)
	{
		Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT(i), PMEM_RIGHT(i));
		#ifdef CONFIG_MEM_RANDOM
		memset(soc_devices[i].mem, rand(), soc_devices[i].size);
		#endif
	}
}

word_t paddr_read(paddr_t addr, int len) 
{
	int soc_idx;
	if (likely((soc_idx = get_soc_index(addr)) != -1)) 
	{
		word_t ret = pmem_read(addr, len, soc_idx);
		#ifdef CONFIG_MTRACE
		void trace_mem(int op, paddr_t addr, int len, word_t data);
		trace_mem(0, addr, len, ret);
		#endif
		return ret;
	}
	IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
	out_of_bound(addr);
	return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) 
{
	int soc_idx;
	if (likely((soc_idx = get_soc_index(addr)) != -1)) 
	{ 
		pmem_write(addr, len, data, soc_idx);
		#ifdef CONFIG_MTRACE
		void trace_mem(int op, paddr_t addr, int len, word_t data);
		trace_mem(1, addr, len, data);
		#endif
		return; 
	}
	IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
	out_of_bound(addr);
}
