#include "trap.h"

void send_byte(uint8_t byte)
{
	outl(SPI_BASE + SPI_CTRL, SPI_CTRL_LSB | SPI_CTRL_RXNEG | 0x10);
	outl(SPI_BASE + SPI_DIVIDER, 10);
	outl(SPI_BASE + SPI_TX_0, byte);
	outl(SPI_BASE + SPI_SS, (1 << 7));
	// set go_busy
	outl(SPI_BASE + SPI_CTRL, SPI_CTRL_LSB | SPI_CTRL_RXNEG | 0x10 | SPI_CTRL_GO);
}

uint8_t recv_byte()
{
	uint32_t spi_ctrl;
	while((spi_ctrl = inl(SPI_BASE + SPI_CTRL)) & SPI_CTRL_GO);
	uint32_t rx0 = inl(SPI_BASE + SPI_RX_0);
	outl(SPI_BASE + SPI_SS, 0);
	return (uint8_t)((rx0 >> 8) & 0xFF);
}

uint8_t byte_reverse(uint8_t byte)
{
	uint8_t reversed = 0;
	for(int i = 0; i < 8; i++)
	{
		reversed <<= 1;
		reversed |= (byte & 1);
		byte >>= 1;
	}
	return reversed;
}

int main()
{
	for(int i = 5; i < 256; i++)
	{
		send_byte(i);
		uint8_t byte = recv_byte();
		check(byte == byte_reverse(i));
	}
}
