package cpu.exu

import chisel3._
import chisel3.util._
import cpu.decode._

// TODO: modify aluop
class ALU extends Module {
	val io = IO(new Bundle{
		val A = Input(UInt(32.W))
		val B = Input(UInt(32.W))
		val aluType = Input(AluType())
		val overflow = Output(Bool())
		val carryout = Output(Bool())
		val zero = Output(Bool())
		val result = Output(UInt(32.W))
	})
	val aluop = io.aluType
	val issub = aluop === AluType.sub
	val isslt = aluop === AluType.slt
	val issltu = aluop === AluType.sltu
	val complement = Wire(UInt(32.W))
	complement := io.B ^ Fill(32, issub)

	val sum = Cat(0.U, io.A) + Cat(0.U, complement) + issub
	val comp = ((sum(31)^io.overflow) & isslt) | (io.carryout & issltu);

	io.result := MuxCase(sum(31, 0), Seq(
		(aluop === AluType.and) -> (io.A & io.B),
		(aluop === AluType.or) -> (io.A | io.B),
		(aluop === AluType.xor) -> (io.A ^ io.B),
		(aluop === AluType.slt) -> Cat(0.U(31.W), comp),
		(aluop === AluType.sltu) -> Cat(0.U(31.W), comp)
	))

	io.overflow := (~io.A(31) & ~complement(31) & sum(31)) || (io.A(31) & complement(31) & ~sum(31))
	io.carryout := sum(32) ^ issub
	
	io.zero := io.result === 0.U
}
