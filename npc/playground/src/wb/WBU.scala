package cpu.wb

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._
import cpu.exu._

class W2EOut extends Bundle {
	val regWdata = UInt(32.W)
	val Imm = UInt(32.W)
	val regRdata = UInt(32.W)
	val info = new DecodedInst
}

class WBUIO extends Bundle {
	val in = Flipped(Decoupled(new EXUOut))
	val w2f = Decoupled(new Bundle {val nextpc = UInt(32.W)})
	val w2e = Decoupled(new W2EOut)
}

class WBU extends Module {
	val io = IO(new WBUIO)

	val in = io.in.bits
	val w2e = io.w2e.bits
	val nextpc = io.w2f.bits
	val result = w2e.regWdata

	/**
	  * State machine for connecting different stages
	  */
	val e2wState = Module(new StateMachine("master"))
	e2wState.io.valid := io.in.valid
	e2wState.io.ready := io.in.ready
	val w2fState = Module(new StateMachine("slave"))
	w2fState.io.valid := io.w2f.valid
	w2fState.io.ready := io.w2f.ready
	val w2eState = Module(new StateMachine("slave"))
	w2eState.io.valid := io.w2e.valid
	w2eState.io.ready := io.w2e.ready

	/**
	  * PC next
	  */
	when(in.result.bruFlag && in.info.exType === ExType.Branch) {
		nextpc := in.result.alu
	} .elsewhen(in.info.exType === ExType.Ecall || in.info.exType === ExType.Mret) {
		nextpc := in.result.csr
	} .otherwise {
		nextpc := in.info.pc + 4.U
	}

	/**
	  * mux result
	  */
	result := MuxLookup(in.info.exType, in.result.alu)(Seq(
		ExType.Lui -> in.result.imm,
		ExType.Branch -> (in.info.pc + 4.U),
		ExType.Load -> (in.result.mem),
		ExType.CSR -> (in.result.csr),
	))

	w2e.info := in.info
	w2e.Imm := in.result.imm
	w2e.regRdata := in.result.rdata1

	// for single cpu
	io.in.ready := io.w2f.fire || e2wState.io.state === e2wState.s_waitvalid
	io.w2e.valid := io.in.fire || w2eState.io.state === w2eState.s_waitready
	io.w2f.valid := io.in.fire || w2fState.io.state === w2fState.s_waitready
}
