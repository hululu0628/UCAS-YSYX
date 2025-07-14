#include <am.h>
#include <klib-macros.h>
#include <ysyxsoc.h>

extern char _pmem_start;

extern char _text, _etext, _erodata, _data, _edata, _bss_start, _bss_end;

extern char _heap_start, _heap_end;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, &_heap_end);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

void putch(char ch) {
	while((inb(UART_BASE + UART_LSR) & UART_LSR_TFE) == 0); // wait for transmit FIFO empty
	outb(UART_BASE + UART_TX, ch);
}

void halt(int code) {
	asm volatile (
		"mv a0, %0\n\t"
		"ebreak\n\t"
		: : "r"(code)
	);
	while(1);
}

static inline int check_pc()
{
	uintptr_t pc;
	asm volatile (
		"auipc %0, 0x0\n\t"
		: "=r"(pc)
	);
	return (pc & 0x80000000) ? 1 : 0; // if PC is in psram
}


static inline void bootloader_lv2()
{
	char *src, *dst, *dst_end;
	// set src
	asm volatile (
		"lui %0, %%hi(_pmem_start)\n\t"
		"addi %0, %0, %%lo(_pmem_start)\n\t"
		: "=r"(src)
	);
	// set dst
	asm volatile (
		"lui %0, %%hi(_text)\n\t"
		"addi %0, %0, %%lo(_text)\n\t"
		: "=r"(dst)
	);
	// set dst_end
	asm volatile (
		"lui %0, %%hi(_edata)\n\t"
		"addi %0, %0, %%lo(_edata)\n\t"
		: "=r"(dst_end)
	);
	// move .text, .rodata and .data to psram
	while (dst < dst_end)
		*dst++ = *src++;

	// set dst to bss segment
	asm volatile (
		"lui %0, %%hi(_bss_start)\n\t"
		"addi %0, %0, %%lo(_bss_start)\n\t"
		: "=r"(dst)
	);
	// set dst_end
	asm volatile (
		"lui %0, %%hi(_bss_end)\n\t"
		"addi %0, %0, %%lo(_bss_end)\n\t"
		: "=r"(dst_end)
	);
	// Initialize BSS Segment
	for(dst = &_bss_start; dst < &_bss_end; dst++)
		*dst = 0;
	// jump to text segment in psram

	asm volatile (
		"lui a0, %%hi(_text)\n\t"
		"addi a0, a0, %%lo(_text)\n\t"
		"jr a0\n\t"
		::
	);
}

static inline void uart_init()
{
	outb(UART_BASE + UART_LCR, UART_LCR_BITS_8 | UART_LCR_DLAB); // write dll dlm
	// set baud rate
	outb(UART_BASE + UART_DLL, 0x01);
	outb(UART_BASE + UART_DLM, 0x00);
	outb(UART_BASE + UART_LCR, UART_LCR_BITS_8); // clear dlab and set 8 bits
}

void _trm_init() {
	uart_init();
	int ret = main(mainargs);
	halt(ret);
}
