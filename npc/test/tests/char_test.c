#define UART_BASE 0x1L
#define UART_TX   1
void _start() {
	*(volatile char *)(UART_BASE + UART_TX) = 'A';
	*(volatile char *)(UART_BASE + UART_TX) = '\n';
	while (1);
}
