ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }
MEMORY {
  mrom (rx) : ORIGIN = 0x20000000, LENGTH = 4K
  sram (rwx) : ORIGIN = 0x0f000000, LENGTH = 8K
  flash (rx) : ORIGIN = 0x30000000, LENGTH = 256M
  psram (rwx) : ORIGIN = 0x80000000, LENGTH = 4M
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  .text : {
    _text = .;
    *(entry)
    *(.text*)
    _etext = .;
  } > psram AT> flash : text
  .rodata : {
    *(.rodata*)
    *(.srodata*)
    _erodata = .;
  } > psram AT> flash

  .data : ALIGN(16)
  {
    _data = .;
    *(.data*)
    *(.sdata*)
    _edata = .;
  } > psram AT> flash : data

  .bss :
  {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    _bss_end = .;
  } > psram AT > flash 
  _stack_top = ORIGIN(sram);
  _stack_pointer = ORIGIN(sram) + LENGTH(sram);
  _heap_start = ALIGN(0x1000);
  _heap_end = ORIGIN(psram) + LENGTH(psram);
}
