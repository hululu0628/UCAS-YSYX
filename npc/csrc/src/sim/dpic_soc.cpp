#include <common.h>
#include <debug.h>
#include <sim/sdb.h>
#include <mem/mem.h>

extern "C" void flash_read(int32_t addr, int32_t *data) { assert(0); }
extern "C" void mrom_read(int32_t addr, int32_t *data) 
{ 
	*(uint32_t *)data = host_read(guest_to_host(addr), 4); // ignore bound check
	IFDEF(CONFIG_MTRACE, trace_rmem((paddr_t)addr, *(word_t *)data);)
}
