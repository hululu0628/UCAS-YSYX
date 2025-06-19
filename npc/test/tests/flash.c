#include "trap.h"

uint32_t flash_read(uint32_t addr)
{
	uint32_t spi_ctrl, rx0;
	uint32_t raddr = addr & 0xFFFFFF | (0x03 << 24); // Read command is 0x03
	outl(SPI_BASE + SPI_CTRL, SPI_CTRL_RXNEG | 64);
	outl(SPI_BASE + SPI_DIVIDER, 10);
	outl(SPI_BASE + SPI_TX_0, addr);
	outl(SPI_BASE + SPI_SS, 1);
	outl(SPI_BASE + SPI_CTRL, SPI_CTRL_RXNEG | 64 | SPI_CTRL_GO);

	
	while((spi_ctrl = inl(SPI_BASE + SPI_CTRL)) & SPI_CTRL_GO);
	rx0 = inl(SPI_BASE + SPI_RX_0);
	outl(SPI_BASE + SPI_SS, 0);
	return rx0;
}


