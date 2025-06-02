package cpu

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu.mem._

object AxPortEncoding {
	def unpriv = "b000".U(3.W)
	def priv = "b001".U(3.W)
	def secure = "b000".U(3.W)
	def nonsecure = "b010".U(3.W)
	def daccess = "b000".U(3.W)
	def iaccess = "b100".U(3.W)

	def genPortCode(seq: Seq[UInt]): UInt = {
		val code = seq.reduce(_ | _)
		code
	}
}

object RespEncoding {
	def OKAY = 0.U(2.W)
	def EXOKAY = 1.U(2.W)
	def SLVERR = 2.U(2.W)
	def DECERR = 3.U(2.W)
}

class AXI4LiteIO extends Bundle {
	// Read Address Channel
	val arvalid = Input(Bool())
	val arready = Output(Bool())
	val araddr = Input(UInt(32.W))
	val arport = Input(UInt(3.W))
	// Read Data Channel
	val rvalid = Output(Bool())
	val rready = Input(Bool())
	val rdata = Output(UInt(32.W))
	val rresp = Output(UInt(2.W))
	// Write Address Channel
	val awvalid = Input(Bool())
	val awready = Output(Bool())
	val awaddr = Input(UInt(32.W))
	val awport = Input(UInt(3.W))
	// Write Data Channel
	val wvalid = Input(Bool())
	val wready = Output(Bool())
	val wdata = Input(UInt(32.W))
	val wstrb = Input(UInt(4.W))
	// Write Response Channel
	val bvalid = Output(Bool())
	val bready = Input(Bool())
	val bresp = Output(UInt(2.W))
}

abstract class AXI4LiteBase extends Module {
	val io = IO(new AXI4LiteIO())
	io <> DontCare
	
	// read state machine
	val r_idle :: r_waitrdata :: r_waitrready :: Nil = Enum(3)
	val rstate = RegInit(r_idle)
	rstate := MuxLookup(rstate, r_idle)(Seq(
		r_idle -> Mux(io.arvalid && io.arready, r_waitrdata, r_idle),
		r_waitrdata -> Mux(io.rvalid, Mux(io.rready, r_idle, r_waitrready), r_waitrdata),
		r_waitrready -> Mux(io.rvalid && io.rready, r_idle, r_waitrready)
	))
	// register address and port
	val raddr = RegEnable(io.araddr, io.arvalid && io.arready)
	val rport = RegEnable(io.arport, io.arvalid && io.arready)

	// write addr state machine
	val aw_idle :: aw_waitvalid :: aw_fire :: Nil = Enum(3)
	val wastate = RegInit(aw_idle)
	wastate := MuxLookup(wastate, aw_idle)(Seq(
		aw_idle -> Mux(io.awready, Mux(io.awvalid, aw_fire, aw_waitvalid), aw_idle),
		aw_waitvalid -> Mux(io.awvalid && io.awready, aw_fire, aw_waitvalid),
		aw_fire -> Mux(io.bvalid && io.bready, aw_idle, aw_fire)
	))
	// write data state machine
	val w_idle :: w_waitvalid :: w_fire :: Nil = Enum(3)
	val wstate = RegInit(w_idle)
	wstate := MuxLookup(wstate, w_idle)(Seq(
		w_idle -> Mux(io.wready, Mux(io.wvalid, w_fire, w_waitvalid), w_idle),
		w_waitvalid -> Mux(io.wvalid && io.wready, w_fire, w_waitvalid),
		w_fire -> Mux(io.bvalid && io.bready, w_idle, w_fire)
	))
	// write response state machine
	val b_idle :: b_waitready :: Nil = Enum(2)
	val bstate = RegInit(b_idle)
	bstate := MuxLookup(bstate, b_idle)(Seq(
		b_idle -> Mux(io.bvalid, Mux(io.bready, b_idle, b_waitready), b_idle),
		b_waitready -> Mux(io.bvalid && io.bready, b_idle, b_waitready)
	))
	val waddr = RegEnable(io.awaddr, io.awvalid && io.awready)
	val wport = RegEnable(io.awport, io.awvalid && io.awready)
	val wdata = RegEnable(io.wdata, io.wvalid && io.wready)
	val wstrb = RegEnable(io.wstrb, io.wvalid && io.wready)
}

class SRAMImp extends AXI4LiteBase {
	val dpiMem = Module(new DPIMem())
	dpiMem.io.valid := false.B
	dpiMem.io.wen := false.B
	dpiMem.io.wmask := 0.U(4.W)
	dpiMem.io.addr := 0.U(32.W)
	dpiMem.io.wdata := 0.U(32.W)
	
	// random access latency
	val rcnt = RegInit(0.U(5.W))
	val rlfsr = LFSR(5, io.arvalid && io.arready, Some(BigInt(0b00101)))
	when(io.arvalid && io.arready) {
		rcnt := rlfsr
	}
	when(rcnt =/= 0.U) {
		rcnt := rcnt - 1.U
	}
	when(rcnt === 0.U) {
		dpiMem.io.valid := true.B
		dpiMem.io.wmask := io.wstrb
		dpiMem.io.addr := raddr
	}
	io.arready := rstate === r_idle
	io.rvalid := rstate === r_waitrdata && rcnt === 0.U
	io.rdata := dpiMem.io.rdata
	io.rresp := RespEncoding.OKAY

	val wcnt = RegInit(0.U(5.W))
	val wlfsr = LFSR(5, io.wvalid && io.wready, Some(BigInt(0b01001)))
	when(io.arvalid && io.arready) {
		wcnt := wlfsr
	}
	when(wastate === aw_fire && wstate === w_fire) {
		when(wcnt =/= 0.U) {
			wcnt := wcnt - 1.U
		}
		when(wcnt === 0.U) {
			dpiMem.io.valid := true.B
			dpiMem.io.wen := true.B
			dpiMem.io.wmask := io.wstrb
			dpiMem.io.addr := waddr
			dpiMem.io.wdata := io.wdata
		}
	}

	io.awready := wastate === aw_idle || wastate === aw_waitvalid
	io.wready := wstate === w_idle || wstate === w_waitvalid
	io.bvalid := wastate === aw_fire && wstate === w_fire && wcnt === 0.U
	io.bresp := RespEncoding.OKAY
}

class AXIArbiter extends Module {
	val io = IO(new Bundle {
		val isramin = new AXI4LiteIO()
		val dsramin = new AXI4LiteIO()
		val out = Flipped(new AXI4LiteIO())
	})
	val a_free :: a_inst :: a_data :: Nil = Enum(3)
	val a_state = RegInit(a_free)
	a_state := MuxLookup(a_state, a_free)(Seq(
		a_free -> Mux(io.isramin.arvalid, a_inst,
			   Mux(io.dsramin.arvalid || io.dsramin.awvalid || io.dsramin.wvalid, a_data, a_free)),
		a_inst -> Mux(io.isramin.rvalid && io.isramin.rready, a_free, a_inst),
		a_data -> Mux(io.dsramin.bvalid && io.dsramin.bready, a_free, a_data)
	))

	io.isramin <> DontCare
	io.dsramin <> DontCare
	io.out <> DontCare

	when(a_state === a_inst) {
		io.out <> io.isramin
	} .elsewhen(a_state === a_data) {
		io.out <> io.dsramin
	} .otherwise {
		io.out.arvalid := false.B
		io.out.awvalid := false.B
		io.out.wvalid := false.B
		io.out.rready := false.B
		io.out.bready := false.B
	}
}

object AXI4Bus {
	val arbiter = Module(new AXIArbiter())
	val sram = Module(new SRAMImp())
	sram.io <> arbiter.io.out
}
