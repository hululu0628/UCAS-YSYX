#include <am.h>
#include "ysyxsoc.h"

void __am_uart_rx(AM_UART_RX_T *rx) {
	if((inb(UART_BASE + UART_LSR) & UART_LSR_DR) == 0) 
		rx->data = -1;
	else
		rx->data = inb(UART_BASE + UART_RX);
}
