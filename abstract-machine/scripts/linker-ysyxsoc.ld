ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }
MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  .text : {
    *(entry)
    *(.text*)
    _etext = .;
  } > mrom AT> mrom : text
  .rodata : {
    *(.rodata*)
    _erodata = .;
  } > mrom AT> mrom

  .data : 
  {
    _data = .;
    *(.data*)
    *(.sdata*)
    _edata = .;
  } > sram AT > mrom : data
  .bss :
  {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    _bss_end = .;
  } > sram AT > mrom
  _stack_top = .;
  _stack_pointer = . + 0x1000;
  _end = .;
  _heap_start = ALIGN(0x10);
}
