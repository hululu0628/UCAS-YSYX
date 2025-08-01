#include <am.h>
#include <nemu.h>

extern char _pmem_start;

extern char _text, _etext, _erodata, _data, _edata, _bss_start, _bss_end;

extern char _heap_start, _heap_end;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, &_heap_end);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

void halt(int code) {
  nemu_trap(code);

  // should not reach here
  while (1);
}

void putch(char ch) {
  outb(SERIAL_PORT, ch);
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

void _trm_init() {
	int ret = main(mainargs);
	halt(ret);
}
