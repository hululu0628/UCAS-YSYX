package cpu

import chisel3._
import chisel3.util._
import cpu._

// TODO: modify aluop
class ALU extends Module {
	val io = IO(new Bundle{
		val A = Input(UInt(32.W))
		val B = Input(UInt(32.W))
		val aluType = Input(UInt(4.W))
		val overflow = Output(Bool())
		val carryout = Output(Bool())
		val zero = Output(Bool())
		val result = Output(UInt(32.W))
	})

	val isand,isor,isxor,iscomp,iscompu,issum,issub = WireInit(false.B)
	io.aluType match {
		case AluType.Sum => issum := true.B
		case AluType.Sub => issub := true.B
		case AluType.And => isand := true.B
		case AluType.Or  => isor := true.B
		case AluType.Xor => isxor := true.B
		case AluType.Comp => iscomp := true.B
	}

	val complement = io.B ^ Fill(32, issub)

	val sum = Cat(0.U, io.A) + Cat(0.U, complement) + issub

	val temp_res = Wire(Vec(6, UInt(32.W)))


	io.overflow := (~io.A(31) & ~complement(31) & sum(31)) || (io.A(31) & complement(31) & ~sum(31))
	io.carryout := sum(32) ^ issub
	io.zero := io.result === 0.U
}
