package cpu.ifu

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._
import cpu.wb._
import cpu.AxPortEncoding.genPortCode
import cpu.AxPortEncoding.unpriv

class IFUOut extends Bundle {
	val inst = new StaticInst
	val pc = UInt(32.W)
}

class IFIO extends Bundle {
	val writeback = Flipped(Decoupled(new Bundle { val nextpc = UInt(32.W) }))
	val out = Decoupled(new IFUOut)
	val isramin = Flipped(new AXI4LiteIO)
}

class InstFetch extends Module {
	val io = IO(new IFIO)

	val isramin = io.isramin
	isramin.setMasterDefault()

	val pc = RegEnable(io.writeback.bits.nextpc, 0x80000000L.U(32.W), io.writeback.fire)

	// state machine for fetching from isram
	val i_idle :: i_waitready :: i_waitrdata :: Nil = Enum(3)
	val f2RAMState = RegInit(i_waitready)
	f2RAMState := MuxLookup(f2RAMState, i_idle)(Seq(
		i_idle -> Mux(io.writeback.fire, i_waitready, i_idle),
		i_waitready -> Mux(isramin.arvalid && isramin.arready, i_waitrdata, i_waitready),
		i_waitrdata -> Mux(isramin.rvalid && isramin.rready, i_idle, i_waitrdata)
	))
	isramin.arvalid := (f2RAMState === i_waitready)
	isramin.araddr := pc
	isramin.arport := AxPortEncoding.genPortCode(Seq(AxPortEncoding.unpriv, AxPortEncoding.secure, AxPortEncoding.iaccess))
	isramin.rready := (f2RAMState === i_waitrdata) && io.out.ready

	// state machine for connecting different stages
	val w2fState = Module(new StateMachine("master"))
	w2fState.io.valid := io.writeback.valid
	w2fState.io.ready := io.writeback.ready
	val f2dState = Module(new StateMachine("slave"))
	f2dState.io.valid := isramin.rvalid && isramin.rready
	f2dState.io.ready := io.out.ready

	io.out.bits.inst.code := isramin.rdata
	io.out.bits.pc := pc

	io.writeback.ready := io.out.ready || w2fState.io.state === w2fState.s_waitvalid
	io.out.valid := (isramin.rvalid && isramin.rready) || f2dState.io.state === f2dState.s_waitready
}
