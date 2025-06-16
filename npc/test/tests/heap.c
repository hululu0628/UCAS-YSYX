#include "trap.h"

void fill_buffer_byte(uint8_t *buf, uint32_t size)
{
	for (int i = 0; i < size; i++)
	{
		buf[i] = (uint8_t)((uintptr_t)(&buf[i]));
	}
}
void fill_buffer_half(uint16_t *buf, uint32_t size)
{
	for (int i = 0; i < size; i++)
	{
		buf[i] = (uint16_t)((uintptr_t)(&buf[i]) & ~0x1);
	}
}
void fill_buffer_word(uint32_t *buf, uint32_t size)
{
	for (int i = 0; i < size; i++)
	{
		buf[i] = (uint32_t)((uintptr_t)(&buf[i]) & ~0x11);
	}
}

void check_buffer_byte(uint8_t *buf, uint32_t size)
{
	for (int i = 0; i < size; i++)
	{
		check(buf[i] == (uint8_t)((uintptr_t)(&buf[i])));
	}
}
void check_buffer_half(uint16_t *buf, uint32_t size)
{
	for (int i = 0; i < size; i++)
	{
		check(buf[i] == (uint16_t)(((uintptr_t)(&buf[i]) & ~0x1)));
	}
}
void check_buffer_word(uint32_t *buf, uint32_t size)
{
	for (int i = 0; i < size; i++)
	{
		check(buf[i] == (uint32_t)(((uintptr_t)(&buf[i]) & ~0x11)));
	}
}

int main()
{
	char *buf = (char *)0xf001000;
	fill_buffer_byte((uint8_t *)buf, 0x1000);
	check_buffer_byte((uint8_t *)buf, 0x1000);
	fill_buffer_half((uint16_t *)buf, 0x1000 / 2);
	check_buffer_half((uint16_t *)buf, 0x1000 / 2);
	fill_buffer_word((uint32_t *)buf, 0x1000 / 4);
	check_buffer_word((uint32_t *)buf, 0x1000 / 4);
	return 0;
}
