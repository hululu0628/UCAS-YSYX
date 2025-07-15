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
	if(addr < FLASH_START || addr + len > FLASH_START + FLASH_SIZE)
	{
		Log_Error("address = " << FMT_PADDR(addr) << " is out of bound of flash ["
	  		<< FMT_PADDR(FLASH_START) << ", " << FMT_PADDR(FLASH_START + FLASH_SIZE) << "] at pc = " FMT_WORD(cpu.pc));
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

extern "C" void psram_read(int32_t addr, int32_t *data)
{
	uint32_t paddr = (uint32_t)addr;
	*(uint32_t *)data = host_read(guest_to_host_psram(paddr & ~0x3), 4);
	IFDEF(CONFIG_MTRACE, trace_rmem((paddr_t)paddr, *(word_t *)data);)
}

extern "C" void psram_write(int32_t addr, int32_t data)
{
	uint32_t paddr = (uint32_t)addr;
	host_write(guest_to_host_psram(paddr), 1, (uint32_t)data);
	IFDEF(CONFIG_MTRACE, trace_wmem((paddr_t)(paddr), data, 0x1);)
}

extern "C" void sdram_read(int32_t addr, int32_t *data)
{
	uint32_t paddr = (uint32_t)addr;
	*(uint32_t *)data = host_read(guest_to_host_sdram(paddr & ~0x1), 2);
	// printf("Addr %x, data %x\n", paddr, *data);
	IFDEF(CONFIG_MTRACE, trace_rmem((paddr_t)paddr, *(word_t *)data);)
}

extern "C" void sdram_write(int32_t addr, int32_t data, int32_t mask)
{
	uint32_t paddr = (uint32_t)addr;
	// printf("Addr %x, data %x, mask %x   ", paddr, data, mask);
	switch(mask)
	{
		case 0x0:
			host_write(guest_to_host_sdram(paddr & ~0x1), 2, (uint32_t)data);
			break;
		case 0x1:
			host_write(guest_to_host_sdram(paddr | 0x1), 1, ((uint32_t)data >> 8));
			break;
		case 0x2:
			host_write(guest_to_host_sdram(paddr & ~0x1), 1, (uint32_t)data);
			break;
		case 0x3: default:
			break;
	}
	// printf("%x\n", *(uint32_t *)guest_to_host_sdram(paddr & ~0x1));
	IFDEF(CONFIG_MTRACE, trace_wmem((paddr_t)(paddr), data, 0x1);)
}
