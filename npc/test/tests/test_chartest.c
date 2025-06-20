#include "trap.h"

uint32_t flash_read(uint32_t addr)
{
	uint32_t spi_ctrl, rx0;
	uint32_t raddr = (addr & 0xFFFFFF) | (0x03 << 24); // Read command is 0x03
	// when rx_neg = 1,
	// it is 63 here because the flash module get the address and send the first bit in the same cycle
	// which makes spi master meet 33 negedge since the first valid bit sent from flash
	// so I guess rx_neg = 0, but i can't find the description in the manual
	outl(SPI_BASE + SPI_CTRL, 64);
	outl(SPI_BASE + SPI_DIVIDER, 10);
	outl(SPI_BASE + SPI_TX_0, 0);
	outl(SPI_BASE + SPI_TX_1, raddr);
	outl(SPI_BASE + SPI_SS, 1);
	outl(SPI_BASE + SPI_CTRL, 64 | SPI_CTRL_GO);

	
	while((spi_ctrl = inl(SPI_BASE + SPI_CTRL)) & SPI_CTRL_GO);
	rx0 = inl(SPI_BASE + SPI_RX_0);
	outl(SPI_BASE + SPI_SS, 0);
	uint32_t res;
	res = 	(rx0 >> 24) |
		((rx0 & 0x00FF0000) >> 8) |
		((rx0 & 0x0000FF00) << 8) |
		((rx0 & 0x000000FF) << 24);
	return res;
}

int main()
{
	uint32_t * p = (uint32_t *)0x30000000;
	uint32_t * q = (uint32_t *)malloc(32);
	for(int i = 0; i < 8; i++)
	{
		q[i] = flash_read((uint32_t)&p[i]);
	}
	asm volatile (
		"jalr ra, %0, 0\n\t"
		:: "r"(q)
	);
	return 0;
}
