#include <am.h>
#include <klib-macros.h>
#include <ysyxsoc.h>

extern char _etext, _erodata, _data, _edata, _bss_start, _bss_end;

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

void putch(char ch) {
	outb(SERIAL_PORT, ch);
}

void halt(int code) {
	asm volatile (
		"mv a0, %0\n\t"
		"ebreak\n\t"
		: : "r"(code)
	);
	while(1);
}

void bootloader()
{
	char *src, *dst;
	src = &_erodata;
	dst = &_data;
	while (dst < &_edata)
		*dst++ = *src++;
	// Initialize BSS Segment
	for(dst = &_bss_start; dst < &_bss_end; dst++)
		*dst = 0;
}

void _trm_init() {
	bootloader();
	int ret = main(mainargs);
	halt(ret);
}
