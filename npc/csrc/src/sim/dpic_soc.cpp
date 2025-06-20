#include <common.h>
#include <debug.h>
#include <sim/sdb.h>
#include <mem/mem.h>

extern "C" void flash_read(int32_t addr, int32_t *data)
{
	uint32_t paddr = addr | 0x30000000;
	*(uint32_t *)data = host_read(guest_to_host_flash(paddr & ~0x3), 4);
	IFDEF(CONFIG_MTRACE, trace_rmem((paddr_t)paddr, *(word_t *)data);)
}

void flash_write(uint32_t addr, uint32_t *data, int len)
{
	if(addr < CONFIG_FBASE || addr + len > CONFIG_FBASE + CONFIG_FSIZE)
	{
		Log_Error("address = " << FMT_PADDR(addr) << " is out of bound of pmem ["
	  		<< FMT_PADDR(CONFIG_FBASE) << ", " << FMT_PADDR(CONFIG_FBASE + CONFIG_FSIZE) << "] at pc = " FMT_WORD(cpu.pc));
		assert(0);
	}
	for(int i = 0; i < len; i++)
	{
		host_write(guest_to_host_flash(addr + 4*i), 4, data[i]);
		IFDEF(CONFIG_MTRACE, trace_wmem((paddr_t)(addr + 4*i), data[i], 0x1);)
	}
}

extern "C" void mrom_read(int32_t addr, int32_t *data) 
{ 
	*(uint32_t *)data = host_read(guest_to_host(addr & ~0x3), 4); // ignore bound check
	IFDEF(CONFIG_MTRACE, trace_rmem((paddr_t)addr, *(word_t *)data);)
}
