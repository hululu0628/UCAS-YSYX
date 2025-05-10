package cpu.exu

import chisel3._
import chisel3.util._
import cpu.decode._

// TODO: modify aluop
class ALU extends Module {
	val io = IO(new Bundle{
		val A = Input(UInt(32.W))
		val B = Input(UInt(32.W))
		val aluType = Input(FuType())
		val overflow = Output(Bool())
		val carryout = Output(Bool())
		val zero = Output(Bool())
		val result = Output(UInt(32.W))
	})
	val aluop = io.aluType
	val issub = (aluop === AluType.sub) || (aluop === AluType.slt) || (aluop === AluType.sltu)
	val isslt = (aluop === AluType.slt)
	val issltu = (aluop === AluType.sltu)
	val issext = !issltu
	val complement_1 = Wire(UInt(33.W))
	val sum = Wire(UInt(33.W))
	val A = Wire(UInt(33.W))
	val comp = ((sum(31)^io.overflow) & isslt) | (io.carryout & issltu);

	A := Cat(io.A(31) & issext,io.A)
	complement_1 := (Cat(io.B(31) & issext, io.B) ^ Fill(33, issub)) + issub
	sum := A + complement_1;
	

	io.result := MuxCase(sum(31, 0), Seq(
		(aluop === AluType.and) -> (io.A & io.B),
		(aluop === AluType.or) -> (io.A | io.B),
		(aluop === AluType.xor) -> (io.A ^ io.B),
		(aluop === AluType.slt) -> Cat(0.U(31.W), comp),
		(aluop === AluType.sltu) -> Cat(0.U(31.W), comp),
		(aluop === AluType.sll) -> (io.A << io.B(4, 0))(31, 0),
		(aluop === AluType.srl) -> (io.A >> io.B(4, 0))(31, 0),
		(aluop === AluType.sra) -> ((io.A.asSInt >> io.B(4, 0)).asUInt)(31, 0),
	))

	io.overflow := (A(32) ^ sum(31)) && (complement_1(32) ^ sum(31))
	io.carryout := sum(32)
	
	io.zero := io.result === 0.U
}
