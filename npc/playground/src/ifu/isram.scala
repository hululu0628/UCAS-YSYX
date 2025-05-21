package cpu.ifu

import chisel3._
import chisel3.util._
import chisel3.util.random._

class InstSRAM extends Module {
	val io = IO(new Bundle {
		val pc = Input(UInt(32.W))
		val valid = Input(Bool())
		val ready = Output(Bool())
		val inst = Output(UInt(32.W))
	})
	val dpiFetch = Module(new DPIFetch())
	val instReg = RegEnable(dpiFetch.io.inst, io.valid)

	val s_idle :: s_instready :: Nil = Enum(2)
	val state = RegInit(s_idle)

	dpiFetch.io.pc := io.pc

	state := MuxLookup(state, s_idle)(Seq(
		s_idle -> Mux(io.valid, s_instready, s_idle),
		s_instready -> (Mux(io.valid && io.ready, s_idle, s_instready))
	))

	val cnt = RegInit(0.U(4.W))
	val random = LFSR(4, io.valid, Some(BigInt(10))) // random latency
	when(io.valid) {
		cnt := random
	}
	when(state === s_instready && cnt =/= 0.U) {
		cnt := cnt - 1.U
	}
	io.ready := state === s_instready && cnt === 0.U
	io.inst := instReg
}
