#include "sim.h"

int InstAddressTrans(word_t addr)
{
	return (addr - SPACE_START) / INSTR_LENGTH;
}
