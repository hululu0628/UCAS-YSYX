#include <isa.h>
#include <sim.h>
#include <debug.h>

#ifdef CONFIG_TRACE

#ifdef CONFIG_ITRACE

void trace_instruction()
{
	if(excuted_inst_num < CONFIG_TRACE_END)
		log_write("PC: 0x" << std::hex << top->io_debug_pc << "\twen: " << (unsigned)top->io_debug_wen << "\treg: " << regs[top->io_debug_waddr] << "\t data: 0x" << top->io_debug_data);
}

#endif

#endif
