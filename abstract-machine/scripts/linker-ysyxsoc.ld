ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }
MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  . = ORIGIN(mrom);
  .text : {
    *(entry)
    *(.text*)
    etext = .;
    _etext = .;
  } > mrom AT> mrom : text
  .rodata : {
    *(.rodata*)
  } > mrom AT> mrom

  . = ORIGIN(sram);
  .data : 
  AT (ADDR(.rodata) + SIZEOF(.rodata))
  {
    *(.data)
    edata = .;
    _data = .;
  } : data
  .bss :
  AT (LOADADDR(.data) + SIZEOF(.data))
  {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  }
  _stack_top = .;
  _stack_pointer = ALIGN(0x1000);
  end = .;
  _end = .;
  _heap_start = ALIGN(0x1000);
}
