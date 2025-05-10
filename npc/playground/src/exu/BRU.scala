package cpu.exu

import chisel3._
import chisel3.util._
import cpu.decode._

class BRU extends Module {
	val io = IO(new Bundle{
		val src1 = Input(UInt(32.W))
		val src2 = Input(UInt(32.W))
		val fuType = Input(FuType())
		val br_flag = Output(Bool())
	})
	val zero = Wire(Bool())
	val overflow = Wire(Bool())
	val carryout = Wire(Bool())
	val A = Wire(UInt(33.W))
	val B = Wire(UInt(33.W))
	val sum = Wire(UInt(33.W))
	val issext = !((io.fuType === BrType.bltu) || (io.fuType === BrType.bgeu))

	A := Cat(io.src1(31) & issext, io.src1)
	B := (~Cat(io.src2(31) & issext, io.src2)) + 1.U
	sum := A + B
	
	zero := sum(31, 0) === 0.U
	overflow := (sum(31) ^ A(32)) && (sum(31) ^ B(32))
	carryout := sum(32)

	io.br_flag := MuxCase(false.B, Seq(
		(io.fuType === BrType.beq) -> zero,
		(io.fuType === BrType.bne) -> !zero,
		(io.fuType === BrType.blt) -> (sum(31) ^ overflow),
		(io.fuType === BrType.bge) -> !(sum(31) ^ overflow),
		(io.fuType === BrType.bltu) -> carryout,
		(io.fuType === BrType.bgeu) -> !carryout,
		(io.fuType === BrType.jal) -> true.B,
		(io.fuType === BrType.jalr) -> true.B,
	))
}