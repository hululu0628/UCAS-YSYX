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

#include <isa.h>
#include <sim/sdb.h>
#include <mem/mem.h>
#include <device/map.h>
#include <debug.h>

#define IO_SPACE_MAX (32 * 1024 * 1024) // 0x2000000, 0xa0000000 ~ 0xa1ffffff

static uint8_t *io_space = NULL;
static uint8_t *p_space = NULL;

// why the data type of size is int
uint8_t* new_space(int size) {
	uint8_t *p = p_space;
	// page aligned;
	size = (size + (PAGE_SIZE - 1)) & ~PAGE_MASK;
	p_space += size;
	assert(p_space - io_space < IO_SPACE_MAX);
	return p;
}

static void check_bound(IOMap *map, paddr_t addr) {
	if (map == NULL) {
		Assert(map != NULL, 
			"address (" << FMT_PADDR(addr) << ") is out of bound at pc = " << FMT_WORD(cpu.pc));
	} else {
		Assert(addr <= map->high && addr >= map->low,
			"address (" << FMT_PADDR(addr) << ") is out of bound {" << map->name << "} [" 
			<< FMT_PADDR(map->low) << ", " << FMT_PADDR(map->high)<< "] at pc = " 
			<< FMT_WORD(cpu.pc));
	}
}

// offset: offset from the start of the space
// len: byte, half-word, word, double-word(if ISA64);
static void invoke_callback(io_callback_t c, paddr_t offset, int len, bool is_write) {
	if (c != NULL) { c(offset, len, is_write); }
}

void free_map()
{
	if (io_space)
    		free(io_space);
}

void init_map() {
	io_space = (uint8_t *)malloc(IO_SPACE_MAX);
	assert(io_space);
	p_space = io_space;
}

word_t map_read(paddr_t addr, int len, IOMap *map) {
	assert(len >= 1 && len <= 8);
	check_bound(map, addr);
	paddr_t offset = addr - map->low;
	invoke_callback(map->callback, offset, len, false); // prepare data to read
	word_t ret = host_read((void *)((uintptr_t)map->space + offset), len);

	IFDEF(CONFIG_DTRACE, trace_rdevice(addr, len, ret, map->name);)

	return ret;
}

void map_write(paddr_t addr, int len, word_t data, IOMap *map) {
	assert(len >= 1 && len <= 8);
	check_bound(map, addr);
	paddr_t offset = addr - map->low;
	host_write((void *)((uintptr_t)map->space + offset), len, data);
	invoke_callback(map->callback, offset, len, true);

	IFDEF(CONFIG_DTRACE, trace_wdevice(addr, len, data, map->name);)
}
