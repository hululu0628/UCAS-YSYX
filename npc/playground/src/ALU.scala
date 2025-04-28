package cpu

import chisel3._
import chisel3.util._

// TODO: modify aluop
class ALU extends Module {
	val io = IO(new Bundle{
		val A = Input(UInt(32.W))
		val B = Input(UInt(32.W))
		val aluop = Input(UInt(3.W))
		val overflow = Output(Bool())
		val carryout = Output(Bool())
		val zero = Output(Bool())
		val result = Output(UInt(32.W))
	})

	val issub = ~io.aluop(2) & (io.aluop(1) | ~io.aluop(0))

	val complement = io.B ^ Fill(32, issub)

	val sum = Cat(0.U, io.A) + Cat(0.U, complement) + issub

	val isand = io.aluop.andR
	val isor = io.aluop(2) & io.aluop(1) & ~io.aluop(0)
	val isxor = io.aluop(2) & ~io.aluop(1) & ~io.aluop(0)
	val iscomp = ~io.aluop(2) & io.aluop(1)
	val issum = ~io.aluop(2) & ~io.aluop(1)

	val temp_res = Wire(Vec(6, UInt(32.W)))
	temp_res(0) := io.A & io.B
	temp_res(1) := io.A | io.B
	temp_res(2) := io.A ^ io.B
	temp_res(3) := ~(io.A | io.B)
	temp_res(4) := sum(31,0)
	temp_res(5) := Cat(0.U(31.W), ((sum(31) ^ io.overflow) & ~io.aluop(0)) | (io.carryout & io.aluop(0)))
	io.result := 	((Fill(32, isand) & temp_res(0)) 	|
			(Fill(32, isor) & temp_res(1)) 		|
			(Fill(32, isxor) & temp_res(2)) 	|
			(Fill(32, iscomp) & temp_res(3)) 	|
			(Fill(32, issum) & temp_res(4)) 	|
			(Fill(32, issub) & temp_res(5)))

	io.overflow := (~io.A(31) & ~complement(31) & sum(31)) || (io.A(31) & complement(31) & ~sum(31))

	io.carryout := sum(32) ^ issub

	io.zero := io.result === 0.U

}
