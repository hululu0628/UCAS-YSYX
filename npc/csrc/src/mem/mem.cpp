#include <common.h>
#include <debug.h>
#include <mem/mem.h>
#include <device/mmio.h>

static uint8_t pmem[PMEM_SIZE] __attribute((aligned(4096)));

uint8_t* guest_to_host(uint32_t paddr) { return paddr - PMEM_START + pmem; }
uint32_t host_to_guest(uint8_t *haddr) { return haddr - pmem + PMEM_START; }

void init_mem()
{
	memset(pmem, 0, PMEM_SIZE);
	Log("Physical memory area [0x" << std::hex << PMEM_START << ", 0x" << PMEM_START + PMEM_SIZE << "]");
}

word_t host_read(void * addr, int len)
{
	word_t rdata;
	switch(len)
	{
		case 1: rdata = *(uint8_t *)(addr); break;
		case 2: rdata = *(uint16_t *)(addr); break;
		case 4: rdata = *(uint32_t *)(addr); break;
		default: assert(0);
	}
	return rdata;
}
void host_write(void * addr, int len, word_t data)
{
	switch(len)
	{
		case 1: *(uint8_t *)(addr) = data; break;
		case 2: *(uint16_t *)(addr) = data; break;
		case 4: *(uint32_t *)(addr) = data; break;
		default: assert(0);
	}
}
uint32_t guest_read(paddr_t addr, int len)
{
	//assert(addr >= PMEM_START && addr < PMEM_START + PMEM_SIZE);
	return host_read(guest_to_host(addr), len);
}
void guest_write(paddr_t addr, int len, word_t data)
{
	assert(addr >= PMEM_START && addr < PMEM_START + PMEM_SIZE);
	host_write(guest_to_host(addr), len, data);
}

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  	Log_Error("address = " << FMT_PADDR(addr) << " is out of bound of pmem ["
	  << FMT_PADDR(PMEM_LEFT) << ", " << FMT_PADDR(PMEM_RIGHT) << "] at pc = " FMT_WORD(cpu.pc));
}

word_t paddr_read(paddr_t addr, int len) 
{
	if (likely(in_pmem(addr))) 
	{
		word_t ret = pmem_read(addr, len);
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
	if (likely(in_pmem(addr)))
	{ 
		pmem_write(addr, len, data);
		#ifdef CONFIG_MTRACE
		void trace_mem(int op, paddr_t addr, int len, word_t data);
		trace_mem(1, addr, len, data);
		#endif
		return; 
	}
	IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
	out_of_bound(addr);
}