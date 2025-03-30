package cpu

import chisel3._
import chisel3.util._

/** Compute GCD using subtraction method. Subtracts the smaller from the larger until register y is zero. value in
  * register x is then the GCD
  */
class ALU extends Module {
	val io = IO(new Bundle{
		val aluop = Input(UInt(3.W))
		val result = Output(UInt(32.W))
		val test = Output(UInt(32.W))
	})

	val one_hot = Wire(Vec(6, Bool()))
	one_hot(0) := io.aluop === 0.U
	one_hot(1) := io.aluop === 1.U
	one_hot(2) := io.aluop === 2.U
	one_hot(3) := io.aluop === 3.U
	one_hot(4) := io.aluop === 4.U
	one_hot(5) := io.aluop === 5.U

	io.test := Mux1H(Seq(
		one_hot(0) -> 5.U,
		one_hot(1) -> 13.U,
		one_hot(2) -> 22.U,
		one_hot(3) -> 31.U,
		one_hot(4) -> 42.U,
		one_hot(5) -> 52.U,
	))
	

	io.result := MuxLookup(io.aluop, 0.U(32.W))(Seq(
		0.U -> 8.U,
		1.U -> 4.U,
		2.U -> 1.U,
		3.U -> 1.U,
		4.U -> 9.U,
		5.U -> 7.U,
	))

}
