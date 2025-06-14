#define UART_BASE 0x10000000L
#define UART_TX   0x0L
// run in bare machine environment
int main() {
	*(volatile char *)(UART_BASE + UART_TX) = 'A';
	*(volatile char *)(UART_BASE + UART_TX) = '\n';
}
