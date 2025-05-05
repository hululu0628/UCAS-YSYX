package cpu.exu

import chisel3._
import chisel3.util._
import cpu.decode._

class ImmGenIO extends Bundle {
  	val inst = Input(new StaticInst)
	val immType = Input(ImmType())
 	val imm = Output(UInt(32.W))
}

class ImmGen extends Module {
	val io = IO(new ImmGenIO)
	
	io.imm := MuxLookup(io.immType, 0.U)(Seq(
		ImmType.IType -> Cat(Fill(20, io.inst.code(31)), io.inst.code(31, 20)),
		ImmType.UType -> Cat(io.inst.code(31, 12), 0.U(12.W)),
		ImmType.JType -> Cat(Fill(11, io.inst.code(31)), io.inst.code(19, 12), io.inst.code(20), io.inst.code(30, 21), 0.U(1.W)),
		ImmType.SType -> Cat(Fill(20, io.inst.code(31)), io.inst.code(31, 25), io.inst.code(11, 7)),
		ImmType.BType -> Cat(Fill(19, io.inst.code(31)), io.inst.code(7), io.inst.code(30, 25), io.inst.code(11, 8), 0.U(1.W))
	))
}