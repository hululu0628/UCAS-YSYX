.section entry, "ax"
.globl _start
.type _start, @function

# Note that the least-significant 12 bits of _pmem_start and _start must be zero
_start:
  mv s0, zero
  lui sp, %hi(_stack_pointer) # set upper 20 bits of sp
  addi sp, sp, %lo(_stack_pointer) # set lower 12 bits of sp
  la a0, bootloader_lv1 // la use pc relative address, thus can be used in both LMA and VMA
  jr a0 # jump to _trm_init
