#include <common.h>
#include <debug.h>
#include <sim/sdb.h>
#include <mem/mem.h>

extern "C" unsigned dpic_read(unsigned raddr)
{
	if(raddr == 0)
		return 0;
	unsigned res = paddr_read(raddr & 0xFFFFFFFC, 4);
	IFDEF(CONFIG_MTRACE, trace_rmem(raddr, res);)
	return res;
}
extern "C" void dpic_write(unsigned waddr, unsigned wdata, unsigned char wmask)
{
	int begin, end;
	IFDEF(CONFIG_MTRACE,trace_wmem(waddr, wdata, wmask);)
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
	paddr_write(waddr + begin, end - begin, wdata >> (begin * 8));
}
