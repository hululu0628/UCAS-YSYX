package cpu.exu

import chisel3._
import chisel3.util._
import cpu.decode._

class BRU extends Module {
	val io = IO(new Bundle{
		val pc = Input(UInt(32.W))
		val imm = Input(UInt(32.W))
		val reg = Input(UInt(32.W))
		val exType = Input(ExType())
		val target = Output(UInt(32.W))
	})
	io.target := MuxLookup(io.exType, 0.U)(Seq(
		ExType.Jalr -> (io.reg + io.imm),
		ExType.Jal -> (io.pc + io.imm),
	))
}