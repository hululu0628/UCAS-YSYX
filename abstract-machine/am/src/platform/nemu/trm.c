#include <am.h>
#include <nemu.h>

extern char _heap_start;
int main(const char *args);

// _heap_start defined in linker script
// PMEM_END defined in LDFLAGS(see .mk file, different for different platforms)
Area heap = RANGE(&_heap_start, PMEM_END);
// defined in CFLAGS, modified if user provides arguments (mainargs)
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER);

// put char......
void putch(char ch) {
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  nemu_trap(code);

  // should not reach here
  while (1);
}

// called in start.S
void _trm_init() {
  int ret = main(mainargs);
  halt(ret);
}
