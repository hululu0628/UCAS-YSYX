ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }
MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
  flash : ORIGIN = 0x30000000, LENGTH = 256M
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  .text : {
    *(entry)
    *(.text*)
    _etext = .;
  } > flash AT> flash : text
  .rodata : {
    *(.rodata*)
    _erodata = .;
  } > flash AT> flash

  .data : 
  {
    _data = .;
    *(.data*)
    *(.sdata*)
    _edata = .;
  } > sram AT > flash : data
  .bss :
  {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    _bss_end = .;
  } > sram AT > flash
  _stack_top = .;
  . = . + 0x1000;
  _stack_pointer = .;
  _end = .;
  _heap_start = ALIGN(0x10);
}
