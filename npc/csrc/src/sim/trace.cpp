#include <isa.h>
#include <sdb.h>
#include <sim.h>
#include <debug.h>

#ifdef CONFIG_TRACE

void itrace_disasm(uint64_t pc, uint8_t *code, int nbyte);

#ifdef CONFIG_ITRACE

void trace_instruction()
{
	IFDEF(CONFIG_ITRACE_OUTPUT,
		log_write("[" << std::setw(5) << std::dec << excuted_inst_num << "]" << 
			"PC: 0x" << std::hex << top->io_debug_pc << 
			" wen: " << (unsigned)top->io_debug_wen << 
			" [" << regs[top->io_debug_waddr] << 
			"] data: 0x" << std::left << std::setw(9) << top->io_debug_data << std::right);
	)

	IFDEF(CONFIG_ITRACE_DISASM,
		itrace_disasm(top->io_debug_pc, (uint8_t *)&top->io_debug_inst, 4);
	)
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
