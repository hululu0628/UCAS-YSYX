#ifndef __UART_H__
#define __UART_H__

#define UART_BASE	0x10000000L
#define UART_TX  	0x0L
#define UART_LCR  	0x3L
#define UART_LSR 	0x5L

#define UART_DLL	0x0L
#define UART_DLM	0x1L

#define UART_LCR_BITS_8 0x3L
#define UART_LCR_DLAB	0b10000000L

#define UART_LSR_TFE	0b00100000L
#define UART_LSR_TFI	0b01000000L

#endif
