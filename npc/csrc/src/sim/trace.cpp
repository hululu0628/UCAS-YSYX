#include <isa.h>
#include <sdb.h>
#include <sim.h>
#include <debug.h>

#ifdef CONFIG_TRACE

#ifdef CONFIG_ITRACE

void trace_instruction()
{
	log_write("["<< std::dec << excuted_inst_num << "]" << 
		"PC: 0x" << std::hex << top->io_debug_pc << 
		"\twen: " << (unsigned)top->io_debug_wen << 
		"\treg: [" << regs[top->io_debug_waddr] << 
	  	"]\t data: 0x" << top->io_debug_data);
}

#endif

#ifdef CONFIG_MTRACE

void trace_rmem(paddr_t addr, word_t data)
{
	log_write("[PC: 0x" << std::hex << top->io_debug_pc << "] Aligned memory read at 0x" << addr << " with data 0x" << data);
}
void trace_wmem(paddr_t addr, word_t data, unsigned char mask)
{
	log_write("[PC: 0x" << std::hex << top->io_debug_pc << "] Aligned memory write at 0x" << addr << " with data 0x" << data 
		<< "   Mask: 0x" << std::hex << (unsigned)mask);
}

#endif


#endif
