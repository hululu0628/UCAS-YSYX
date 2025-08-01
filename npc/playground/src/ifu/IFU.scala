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
	val instMaster = Flipped(new AXI4IO)
}

class InstFetch extends Module {
	val io = IO(new IFIO)

	val instMaster = io.instMaster
	instMaster.setMasterDefault()
	val instReqFire = instMaster.arvalid && instMaster.arready
	val instGet = instMaster.rvalid && instMaster.rready && instMaster.rlast

	val pc = RegEnable(io.writeback.bits.nextpc, NPCParameters.deviceTab("flash").base.U(32.W), io.writeback.fire)

	// state machine for fetching from isram
	val i_idle :: i_waitready :: i_waitrdata :: Nil = Enum(3)
	val f2RAMState = RegInit(i_waitready)
	f2RAMState := MuxLookup(f2RAMState, i_idle)(Seq(
		i_idle -> Mux(io.writeback.fire, i_waitready, i_idle),
		i_waitready -> Mux(instReqFire, i_waitrdata, i_waitready),
		i_waitrdata -> Mux(instGet, i_idle, i_waitrdata)
	))
	instMaster.arvalid := (f2RAMState === i_waitready)
	instMaster.araddr := pc
	instMaster.arlen := 0.U(8.W)
	instMaster.arsize := TransferSize.WORD
	instMaster.arburst := BrustType.INCR
	instMaster.rready := (f2RAMState === i_waitrdata) && io.out.ready

	// state machine for connecting different stages
	val w2fState = Module(new StateMachine("master"))
	w2fState.io.valid := io.writeback.valid
	w2fState.io.ready := io.writeback.ready
	val f2dState = Module(new StateMachine("slave"))
	f2dState.io.valid := instGet
	f2dState.io.ready := io.out.ready

	io.out.bits.inst.code := instMaster.rdata
	io.out.bits.pc := pc

	io.writeback.ready := io.out.ready || w2fState.io.state === w2fState.s_waitvalid
	io.out.valid := (instMaster.rvalid && instMaster.rready) || f2dState.io.state === f2dState.s_waitready

	// perf counter
	val Perf_instFetchNum = PerfCnt("instFetchNum", instMaster.rvalid && instMaster.rready, 64)
	val Perf_ifuLat = PerfCnt("ifuLat", true.B, 64, io.writeback.fire, io.out.fire)
	val Perf_fetchLat = PerfCnt("fetchLat", true.B, 64, instMaster.arvalid, instMaster.rready && instMaster.rlast)
}
