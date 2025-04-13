package cpu

import chisel3._
import chisel3.util._

class Shifter extends Module {
	val io = IO(new Bundle {
		val A = Input(UInt(32.W))
		val shamt = Input(UInt(5.W))
		val shiftop = Input(UInt(2.W))
		val result = Output(UInt(32.W))
	})
	
	io.result := MuxLookup(io.shiftop, 0.U(32.W))(Seq(
		0b00.U 	-> (io.A << io.shamt),
		0b01.U 	-> (io.A >> io.shamt),
		0b11.U 	-> (io.A.asSInt >> io.shamt).asUInt
	))
}