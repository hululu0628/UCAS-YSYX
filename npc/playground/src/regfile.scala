package cpu

import chisel3._
import chisel3.util._

class RegIO extends Bundle {
	val wen = Input(Bool())
	val waddr = Input(UInt(5.W))

	val raddr1 = Input(UInt(5.W))
	val raddr2 = Input(UInt(5.W))

	val wdata = Input(UInt(32.W))

	val rdata1 = Output(UInt(32.W))
	val rdata2 = Output(UInt(32.W))
}

class Regfile extends Module {
	val io = IO(new RegIO)
	val regfiles = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

	when(io.wen && io.waddr =/= 0.U) {
		regfiles(io.waddr) := io.wdata
	}

	io.rdata1 := Mux(io.raddr1 === 0.U, 0.U(32.W), regfiles(io.raddr1))
	io.rdata2 := Mux(io.raddr2 === 0.U, 0.U(32.W), regfiles(io.raddr2))
}