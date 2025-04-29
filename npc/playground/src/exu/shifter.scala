package cpu.exu

import chisel3._
import chisel3.util._
import cpu.decode._

class Shifter extends Module {
	val io = IO(new Bundle {
		val A = Input(UInt(32.W))
		val shamt = Input(UInt(5.W))
		val shiftop = AluType()
		val result = Output(UInt(32.W))
	})
	
	io.result := MuxLookup(io.shiftop, 0.U(32.W))(Seq(
		AluType.sll 	-> (io.A << io.shamt),
		AluType.srl 	-> (io.A >> io.shamt),
		AluType.sra 	-> (io.A.asSInt >> io.shamt).asUInt
	))
}