ENTRY(_start)
PHDRS { fsbl PT_LOAD; ssbl PT_LOAD; text PT_LOAD; data PT_LOAD; }
MEMORY {
  mrom (rx) : ORIGIN = 0x20000000, LENGTH = 4K
  sram (rwx) : ORIGIN = 0x0f000000, LENGTH = 8K
  flash (rx) : ORIGIN = 0x30000000, LENGTH = 256M
  psram (rwx) : ORIGIN = 0x80000000, LENGTH = 4M
  sdram (rwx) : ORIGIN = 0x20000000, LENGTH = 512M
}

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  .fsbl : {
	_fsbl_start = .;
	*(entry)
	*(fsbl*)
	_fsbl_end = .;
  } > flash AT> flash : fsbl
  .ssbl : {
	_ssbl_start = .;
	*(ssbl*)
	_ssbl_end = .;
  } > sram AT> flash : ssbl
  _lossbl_start = LOADADDR(.ssbl);

  .text : ALIGN(16)
  {
    _text = .;
    *(.text*)
    _etext = .;
  } > psram AT> flash : text
  .rodata : {
    *(.rodata*)
    *(.srodata*)
    _erodata = .;
  } > psram AT> flash
  _lotext = LOADADDR(.text);

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
