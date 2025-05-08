#include <common.h>
#include <debug.h>
#include <mem.h>

static uint8_t pmem[PMEM_SIZE] __attribute((aligned(4096)));

uint8_t* guest_to_host(uint32_t paddr) { return paddr - PMEM_START + pmem; }
uint32_t host_to_guest(uint8_t *haddr) { return haddr - pmem + PMEM_START; }

void init_mem()
{
	memset(pmem, 0, PMEM_SIZE);
	std::cout << "physical memory area [0x" << std::hex << PMEM_START << ", 0x" << PMEM_START + PMEM_SIZE << "]" << std::endl;
}

uint32_t host_read(void * addr, int len)
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

extern "C" unsigned pmem_read(unsigned raddr)
{
	if(raddr == 0)
		return 0;
	unsigned res = guest_read(raddr & 0xFFFFFFFC, 4);
	return res;
}
extern "C" void pmem_write(unsigned waddr, unsigned wdata, unsigned char wmask)
{
	int begin, end;
	for(begin = 0; begin < 4; begin++)
	{
		if(wmask & 0x1)
			break;
		wmask >>= 1;
	}
	for(end = begin; end < 5; end++)
	{
		if(!wmask)
			break;
		wmask >>= 1;
	}
	guest_write(waddr + begin, end - begin, wdata >> (begin * 8));
}
