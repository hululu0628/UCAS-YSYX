.section entry, "ax"
.globl _start
.type _start, @function

# set stack pointer and call trm_init
_start:
  mv s0, zero
  la sp, _stack_pointer
  call _trm_init

.size _start, . - _start
