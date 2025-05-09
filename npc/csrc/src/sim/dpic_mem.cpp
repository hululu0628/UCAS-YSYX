#include <common.h>
#include <debug.h>
#include <mem.h>

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
