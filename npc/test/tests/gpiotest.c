#include "trap.h"

void test_sw_led()
{
	volatile char * buf = (volatile char *)0x10002000;
	unsigned value = 0;
	while(1)
	{
		for(int i = 0; i < 16; i++)
		{
			do {
				value = *((volatile unsigned *)(buf + 0x4));
			}while(value != (1 << i));
			*((unsigned *)buf) = 1 << i;
		}
	}
}

void test_seg()
{
	volatile char * buf = (volatile char *)0x10002000;
	*(unsigned *)(buf + 0x8) = 0x12345678;
}

int main()
{
	test_seg();
	while(1);
}
