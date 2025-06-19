#ifndef __SPI_H__
#define __SPI_H__

#define SPI_BASE	0x10001000L
#define SPI_RX_0	0x0L
#define SPI_RX_1	0x4L
#define SPI_RX_2	0x8L
#define SPI_RX_3	0xCL
#define SPI_TX_0	0x0L
#define SPI_TX_1	0x4L
#define SPI_TX_2	0x8L
#define SPI_TX_3	0xCL
#define SPI_CTRL	0x10L
#define SPI_DIVIDER	0x14L
#define SPI_SS	0x18L

#define SPI_CTRL_ASS	(1 << 13)
#define SPI_CTRL_IE	(1 << 12)
#define SPI_CTRL_LSB	(1 << 11)
#define SPI_CTRL_TXNEG	(1 << 10)
#define SPI_CTRL_RXNEG	(1 << 9)
#define SPI_CTRL_GO	(1 << 8)
#define SPI_CTRL_CHARMASK	(0x7F)

#define SPI_DIVIDER_MAKS	(0xFFFF)

#define SPI_SS_MASK	(0xFF)

#endif
