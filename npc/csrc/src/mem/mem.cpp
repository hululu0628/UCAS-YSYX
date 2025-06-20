#include <common.h>
#include <debug.h>
#include <difftest/difftest.h>
#include <sim/sdb.h>
#include <mem/mem.h>
#include <device/mmio.h>

static uint8_t pmem[PMEM_SIZE] __attribute((aligned(4096)));
static uint8_t flash[CONFIG_FSIZE] __attribute((aligned(4096)));

static uint32_t flash_test[] = {
	0x100007b7, 
	0x04100713, 
	0x00e78023, 
	0x00a00713, 
	0x00e78023, 
	0x00008067
};

uint8_t* guest_to_host(uint32_t paddr) { return paddr - PMEM_START + pmem; }
uint32_t host_to_guest(uint8_t *haddr) { return haddr - pmem + PMEM_START; }

uint8_t* guest_to_host_flash(paddr_t paddr) { return paddr - CONFIG_FBASE + flash; }
paddr_t host_to_guest_flash(uint8_t *haddr) { return haddr - flash + CONFIG_FBASE; }

void init_mem()
{
	memset(pmem, 0, PMEM_SIZE);
	memset(flash, 0, CONFIG_FSIZE);
	// Initialize flash for testing
	flash_write(CONFIG_FBASE, flash_test, sizeof(flash_test)/4);

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

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr, paddr_t left, paddr_t right) {
  	Log_Error("address = " << FMT_PADDR(addr) << " is out of bound of pmem ["
	  << FMT_PADDR(left) << ", " << FMT_PADDR(right) << "] at pc = " FMT_WORD(cpu.pc));
}

word_t paddr_read(paddr_t addr, int len) 
{
	word_t ret;
	if (likely(in_pmem(addr))) 
	{
		ret = pmem_read(addr, len);
		return ret;
	}
	IFDEF(CONFIG_DEVICE, ret = mmio_read(addr, len); difftest_skip_ref(); return ret;)
	out_of_bound(addr, PMEM_LEFT, PMEM_RIGHT);
	return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) 
{

	if (likely(in_pmem(addr)))
	{ 
		pmem_write(addr, len, data);
		return; 
	}
	IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); difftest_skip_ref(); return;)
	out_of_bound(addr, PMEM_LEFT, PMEM_RIGHT);
}