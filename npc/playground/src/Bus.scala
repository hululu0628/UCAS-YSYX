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

	def setMasterDefault() = {
		this.arvalid := false.B
		this.araddr := 0.U
		this.arport := AxPortEncoding.unpriv
		this.rready := false.B
		this.awvalid := false.B
		this.awaddr := 0.U
		this.awport := AxPortEncoding.unpriv
		this.wvalid := false.B
		this.wdata := 0.U
		this.wstrb := 0.U
		this.bready := false.B
	}
	def setSlaveDefault() = {
		this.arready := false.B
		this.rvalid := false.B
		this.rdata := 0.U
		this.rresp := RespEncoding.OKAY
		this.awready := false.B
		this.wready := false.B
		this.bvalid := false.B
		this.bresp := RespEncoding.OKAY
	}
}

abstract class AXI4LiteSlaveBase extends Module {
	val io = IO(new AXI4LiteIO())
	io.setSlaveDefault()
	
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
	val aw_idle :: aw_fire :: Nil = Enum(2)
	val wastate = RegInit(aw_idle)
	wastate := MuxLookup(wastate, aw_idle)(Seq(
		aw_idle -> Mux(io.awvalid && io.awready, aw_fire, aw_idle),
		aw_fire -> Mux(io.bvalid && io.bready, aw_idle, aw_fire)
	))
	// write data state machine
	val w_idle :: w_fire :: Nil = Enum(2)
	val wstate = RegInit(w_idle)
	wstate := MuxLookup(wstate, w_idle)(Seq(
		w_idle -> Mux(io.wvalid && io.wready, w_fire, w_idle),
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

class AXIArbiter extends Module {
	val io = IO(new Bundle {
		val instin = new AXI4LiteIO()
		val datain = new AXI4LiteIO()
		val out = Flipped(new AXI4LiteIO())
	})
	val a_free :: a_inst :: a_rdata :: a_wdata :: Nil = Enum(4)
	val a_state = RegInit(a_free)
	a_state := MuxLookup(a_state, a_free)(Seq(
		a_free -> Mux(io.instin.arvalid, a_inst,
			   Mux(io.datain.arvalid, a_rdata, 
			   Mux(io.datain.awvalid || io.datain.wvalid, a_wdata, a_free))),
		a_inst -> Mux(io.instin.rvalid && io.instin.rready, a_free, a_inst),
		a_rdata -> Mux(io.datain.rvalid && io.datain.rready, a_free, a_rdata),
		a_wdata -> Mux(io.datain.bvalid && io.datain.bready, a_free, a_wdata)
	))

	io.instin.setSlaveDefault()
	io.datain.setSlaveDefault()
	io.out.setMasterDefault()
	when(a_state === a_inst) {
		io.out <> io.instin
		io.datain.setSlaveDefault()
	} .elsewhen(a_state === a_rdata || a_state === a_wdata) {
		io.out <> io.datain
		io.instin.setSlaveDefault()
	} .elsewhen(a_state === a_free) {
		io.out.setMasterDefault()
		io.instin.setSlaveDefault()
		io.datain.setSlaveDefault()
	}
}

class AXI1x2Bar extends Module {
	val io = IO(new Bundle {
		val in = new AXI4LiteIO()
		val sram = Flipped(new AXI4LiteIO())
		val mmio = Flipped(new AXI4LiteIO())
	})
	when(io.in.arvalid) {
		when(io.in.araddr >= NPCParameters.sramStart.U && io.in.araddr < (NPCParameters.sramStart + NPCParameters.sramSize).U) {
			io.sram <> io.in
			io.mmio.setMasterDefault()
		} .elsewhen(io.in.araddr >= NPCParameters.mmioStart.U && io.in.araddr < (NPCParameters.mmioStart + NPCParameters.mmioSize).U) {
			io.mmio <> io.in
			io.sram.setMasterDefault()
		} .otherwise {
			printf("ERROR: Address out of range 0x%x\n", io.in.araddr)
			io.sram.setMasterDefault()
			io.mmio.setMasterDefault()
		}
	}
}

class AXI4Bus extends Module {
	val io = IO(new Bundle {
		val instin = new AXI4LiteIO()
		val datain = new AXI4LiteIO()
	})
	val arbiter = Module(new AXIArbiter())
	val xbar = Module(new AXI1x2Bar())
	val sram = Module(new SRAMImp())
	arbiter.io.instin <> io.instin
	arbiter.io.datain <> io.datain
	xbar.io.in <> arbiter.io.out
	sram.io <> xbar.io.sram

}
