.section fsbl, "ax"
.globl bootloader_lv1
.type bootloader_lv1, @function

bootloader_lv1:
	lui a0, %hi(_lossbl_start)
	addi a0, a0, %lo(_lossbl_start)
	lui a1, %hi(_ssbl_start)
	addi a1, a1, %lo(_ssbl_start)
	lui a2, %hi(_ssbl_end)
	addi a2, a2, %lo(_ssbl_end)
L0:
	lw a3, 0(a0)
	sw a3, 0(a1)
	addi a0, a0, 4
	addi a1, a1, 4
	blt a1, a2, L0

	la a0, bootloader_lv2
	jr a0

.section ssbl, "ax"
.globl bootloader_lv2
.type bootloader_lv2, @function

bootloader_lv2:
	lui a0, %hi(_lotext)
	addi a0, a0, %lo(_lotext)
	lui a1, %hi(_text)
	addi a1, a1, %lo(_text)
	lui a2, %hi(_edata)
	addi a2, a2, %lo(_edata)
L1:
	lw a3, 0(a0)
	sw a3, 0(a1)
	addi a0, a0, 4
	addi a1, a1, 4
	blt a1, a2, L1

	lui a0, %hi(_bss_start)
	addi a0, a0, %lo(_bss_start)
	lui a1, %hi(_bss_end)
	addi a1, a1, %lo(_bss_end)
L2:
	sw zero, 0(a0)
	addi a0, a0, 4
	blt a0, a1, L2

	la a0, _trm_init
	jr a0
