package cpu.ifu

import chisel3._
import chisel3.util._

class InstSRAM extends Module {
	val io = IO(new Bundle {
		val pc = Input(UInt(32.W))
		val valid = Output(Bool())
		val inst = Output(UInt(32.W))
	})
	val dpiFetch = Module(new DPIFetch())
	val s_idle :: s_instready :: Nil = Enum(2)
	val state = RegInit(s_idle)

	dpiFetch.io.pc := io.pc

	state := MuxLookup(state, s_idle)(Seq(
		s_idle -> (s_instready),
		s_instready -> (s_idle)
	))
	io.valid := state === s_instready
	io.inst := dpiFetch.io.inst
}
