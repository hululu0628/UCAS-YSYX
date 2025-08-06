#include "trap.h"
int main()
{
	asm volatile("li a0, 0;"
		"li a1, 0x10000000;"	// UART_TX
		"li t1, 0x41;"		// 0x41 = 'A'
		"la a2, again;"
		"li t2, 0x00008067;"	// 0x00008067 = ret
		"again:"
		"sb t1, (a1);"
		"sw t2, (a2);"
		"fence.i;"		// Ensure the store is visible before the jump
		"j again;"
		);
}