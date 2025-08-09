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
	val isBrHazard = Input(Bool())
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

	val nextpc = Wire(UInt(32.W))
	val pc = RegInit(NPCParameters.deviceTab("flash").base.U(32.W))
	val isBlocked = RegInit(false.B)
	when(io.isBrHazard) {
		isBlocked := true.B
	}.elsewhen(!io.isBrHazard) {
		isBlocked := false.B
	}
	nextpc := Mux(isBlocked, io.writeback.bits.nextpc, pc + 4.U)
	when(!io.isBrHazard && isBlocked) {
		pc := io.writeback.bits.nextpc
	} .elsewhen(io.out.fire) {
		pc := pc + 4.U
	}

	// state machine for fetching from isram
	val i_waitready :: i_waitrdata :: Nil = Enum(2)
	val f2RAMState = RegInit(i_waitready)
	f2RAMState := MuxLookup(f2RAMState, i_waitready)(Seq(
		i_waitready -> Mux(instReqFire, i_waitrdata, i_waitready),
		i_waitrdata -> Mux(instGet, i_waitready, i_waitrdata)
	))
	instMaster.arvalid := (f2RAMState === i_waitready) && !io.isBrHazard
	instMaster.araddr := Mux(isBlocked, nextpc, pc)
	instMaster.arlen := 0.U(8.W)
	instMaster.arsize := TransferSize.WORD
	instMaster.arburst := BrustType.INCR
	instMaster.rready := (f2RAMState === i_waitrdata) && io.out.ready

	io.out.bits.inst.code := instMaster.rdata
	io.out.bits.pc := pc

	io.writeback.ready := true.B
	io.out.valid := (instMaster.rvalid && instMaster.rready)

	// perf counter
	val Perf_instFetchNum = PerfCnt("instFetchNum", instMaster.rvalid && instMaster.rready, 64)
	val Perf_ifuLat = PerfCnt("ifuLat", true.B, 64, io.writeback.fire && !io.isBrHazard, io.out.fire)
	val Perf_fetchLat = PerfCnt("fetchLat", true.B, 64, instMaster.arvalid, instMaster.rready && instMaster.rlast)
}
