package cpu.exu

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._
import cpu.wb._
import cpu.regfile._
import cpu.mem._

class LSUOut extends Bundle {
	val result = new Bundle {
		val alu = UInt(32.W)
		val csr = UInt(32.W)
		val imm = UInt(32.W)
		val mem = UInt(32.W)
		val bruFlag = Bool()
		val rdata1 = UInt(32.W)
	}
	val info = new DecodedInst
}

class LSUIO extends Bundle {
	val exu = Flipped(DecoupledIO(new EXUOut))
	val dataMaster = Flipped(new AXI4IO)
	val out = Decoupled(new LSUOut)
}

class LSU extends Module with memfunc {
	val io = IO(new LSUIO)
	val exuIn = io.exu.bits
	val out = io.out.bits
	val dataMaster = io.dataMaster
	dataMaster.setMasterDefault()

	/**
	  * Memory
	  */
	val memAddr = exuIn.result.alu
	// state machine for AXI4Lite load
	val r_idle :: r_waitrdata :: Nil = Enum(2)
	val el2RAMState = RegInit(r_idle)
	el2RAMState := MuxLookup(el2RAMState, r_idle)(Seq(
		r_idle -> Mux(io.exu.valid && dataMaster.arvalid && dataMaster.arready, r_waitrdata, r_idle),
		r_waitrdata -> Mux(dataMaster.rvalid && dataMaster.rready && dataMaster.rlast, r_idle, r_waitrdata)
	))
	dataMaster.arvalid := exuIn.info.exType === ExType.Load && (el2RAMState === r_idle) && io.exu.valid
	dataMaster.araddr := memAddr
	dataMaster.arlen := 0.U
	dataMaster.arsize := getAxSize(exuIn.info.lsLength)
	dataMaster.arburst := BrustType.INCR
	dataMaster.rready := (el2RAMState === r_waitrdata) && io.out.ready && io.exu.valid
	val ldata = getldata(dataMaster.rdata, exuIn.info.lsLength, exuIn.info.loadSignExt, memAddr(1, 0))
	// state machine for AXI4Lite store
	val w_idle :: w_waitaw :: w_waitw :: w_waitbvalid :: Nil = Enum(4)
	val es2RAMState = RegInit(w_idle)
	es2RAMState := MuxLookup(es2RAMState, w_idle)(Seq(
		w_idle -> Mux(
			io.exu.valid,
			MuxLookup(Cat(dataMaster.awvalid && dataMaster.awready, dataMaster.wvalid && dataMaster.wready), w_idle)(Seq(
				"b00".U -> w_idle,
				"b01".U -> w_waitaw,
				"b10".U -> w_waitw,
				"b11".U -> w_waitbvalid
			)),
			w_idle
		),
		w_waitaw -> Mux(dataMaster.awvalid && dataMaster.awready, w_waitbvalid, w_waitaw),
		w_waitw -> Mux(dataMaster.wvalid && dataMaster.wready, w_waitbvalid, w_waitw),
		w_waitbvalid -> Mux(dataMaster.bvalid && dataMaster.bready, w_idle, w_waitbvalid)
	))
	dataMaster.awvalid := exuIn.info.exType === ExType.Store &&
				(es2RAMState === w_idle || es2RAMState === w_waitaw) &&
				io.exu.valid
	dataMaster.awaddr := memAddr
	dataMaster.awlen := 0.U
	dataMaster.awsize := getAxSize(exuIn.info.lsLength)
	dataMaster.awburst := BrustType.INCR
	dataMaster.wvalid := exuIn.info.exType === ExType.Store &&
				(es2RAMState === w_idle || es2RAMState === w_waitw) &&
				io.exu.valid
	dataMaster.wdata := getwdata(exuIn.result.rdata2, exuIn.info.lsLength, memAddr(1, 0))
	dataMaster.wstrb := getwmask(exuIn.info.lsLength, memAddr(1, 0))
	dataMaster.wlast := dataMaster.wvalid
	dataMaster.bready := (es2RAMState === w_waitbvalid) && io.out.ready && io.exu.valid

	// output
	out.info := exuIn.info
	out.result.alu := exuIn.result.alu
	out.result.csr := exuIn.result.csr
	out.result.imm := exuIn.result.imm
	out.result.mem := ldata
	out.result.bruFlag := exuIn.result.bruFlag
	out.result.rdata1 := exuIn.result.rdata1

	// for multi-cycle cpu
	io.exu.ready := io.out.fire || !io.exu.valid
	io.out.valid := MuxLookup(exuIn.info.exType, io.exu.valid)(Seq(
		ExType.Load -> (dataMaster.rvalid && dataMaster.rready),
		ExType.Store -> (dataMaster.bvalid && dataMaster.bready)
	))

	val Perf_memAccess = PerfCnt("memAcc", (dataMaster.rvalid && dataMaster.rready) || (dataMaster.bvalid && dataMaster.bready), 64)
	val Perf_memLatR = PerfCnt("memLatR", true.B, 64, dataMaster.arvalid, dataMaster.rlast && dataMaster.rready)
	val Perf_memLatW = PerfCnt("memLatW", true.B, 64, dataMaster.awvalid || dataMaster.wvalid, dataMaster.bvalid && dataMaster.bready)
}
