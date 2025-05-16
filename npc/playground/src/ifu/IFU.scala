package cpu.ifu

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._
import cpu.wb._

class IFUOut extends Bundle {
	val inst = new StaticInst
	val pc = UInt(32.W)
}

class IFIO extends Bundle {
	val writeback = Flipped(Decoupled(new WBUOut))
	val out = Decoupled(new IFUOut)
}

class InstFetch extends Module {
	val io = IO(new IFIO)

	val isram = Module(new InstSRAM())

	val pc = RegEnable(io.writeback.bits.nextpc, 0x80000000L.U(32.W), io.writeback.fire)

	// state machine for fetching from isram
	val f2RAMState = Module(new StateMachine("slave"))
	f2RAMState.io.valid := RegNext(io.writeback.fire)
	f2RAMState.io.ready := isram.io.ready
	// state machine for connecting different stages
	val w2dState = Module(new StateMachine("master"))
	w2dState.io.valid := io.writeback.valid
	w2dState.io.ready := io.writeback.ready
	val f2dState = Module(new StateMachine("slave"))
	f2dState.io.valid := isram.io.valid
	f2dState.io.ready := io.out.ready

	isram.io.pc := pc
	isram.io.valid := true.B

	io.out.bits.inst.code := isram.io.inst
	io.out.bits.pc := pc

	io.writeback.ready := io.out.fire || w2dState.io.state === w2dState.s_waitvalid
	io.out.valid := isram.io.ready || f2dState.io.state === f2dState.s_waitready
}
