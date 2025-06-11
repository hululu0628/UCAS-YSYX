package cpu

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu.mem._

object TransferSize {
	def BYTE = 0.U(3.W)
	def HALF = 1.U(3.W)
	def WORD = 2.U(3.W)
	def DWORD = 3.U(3.W)
}

object BrustType {
	def FIXED = "b00".U(2.W)
	def INCR = "b01".U(2.W)
	def WRAP = "b10".U(2.W)
}

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

class AXI4IO extends Bundle {
	// Read Address Channel
	val arid = Input(UInt(4.W))
	val arvalid = Input(Bool())
	val arready = Output(Bool())
	val araddr = Input(UInt(32.W))
	val arlen = Input(UInt(8.W))
	val arsize = Input(UInt(3.W))
	val arburst = Input(UInt(2.W))
	val arport = Input(UInt(3.W))
	// Read Data Channel
	val rid = Output(UInt(4.W))
	val rvalid = Output(Bool())
	val rready = Input(Bool())
	val rdata = Output(UInt(32.W))
	val rlast = Output(Bool())
	val rresp = Output(UInt(2.W))
	// Write Address Channel
	val awid = Input(UInt(4.W))
	val awvalid = Input(Bool())
	val awready = Output(Bool())
	val awaddr = Input(UInt(32.W))
	val awlen = Input(UInt(8.W))
	val awsize = Input(UInt(3.W))
	val awburst = Input(UInt(2.W))
	val awport = Input(UInt(3.W))
	// Write Data Channel
	val wvalid = Input(Bool())
	val wready = Output(Bool())
	val wdata = Input(UInt(32.W))
	val wstrb = Input(UInt(4.W))
	val wlast = Input(Bool())
	// Write Response Channel
	val bid = Output(UInt(4.W))
	val bvalid = Output(Bool())
	val bready = Input(Bool())
	val bresp = Output(UInt(2.W))

	def setMasterDefault() = {
		this.arid := 0.U
		this.arvalid := false.B
		this.araddr := 0.U
		this.arlen := 0.U
		this.arsize := 0.U
		this.arburst := BrustType.INCR
		this.arport := AxPortEncoding.unpriv
		this.rready := false.B
		this.awid := 0.U
		this.awvalid := false.B
		this.awaddr := 0.U
		this.awlen := 0.U
		this.awsize := 0.U
		this.awburst := BrustType.INCR
		this.awport := AxPortEncoding.unpriv
		this.wvalid := false.B
		this.wdata := 0.U
		this.wstrb := 0.U
		this.wlast := false.B
		this.bready := false.B
	}
	def setSlaveDefault() = {
		this.arready := false.B
		this.rid := 0.U
		this.rvalid := false.B
		this.rdata := 0.U
		this.rlast := false.B
		this.rresp := RespEncoding.OKAY
		this.awready := false.B
		this.wready := false.B
		this.bid := 0.U
		this.bvalid := false.B
		this.bresp := RespEncoding.OKAY
	}
}

abstract class AXI4LiteSlaveBase extends Module {
	val io = IO(new AXI4IO())
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
		val instSlave = new AXI4IO()
		val dataSlave = new AXI4IO()
		val out = Flipped(new AXI4IO())
	})
	val a_free :: a_inst :: a_rdata :: a_wdata :: Nil = Enum(4)
	val a_state = RegInit(a_free)
	a_state := MuxLookup(a_state, a_free)(Seq(
		a_free -> Mux(io.instSlave.arvalid, a_inst,
			   Mux(io.dataSlave.arvalid, a_rdata, 
			   Mux(io.dataSlave.awvalid || io.dataSlave.wvalid, a_wdata, a_free))),
		a_inst -> Mux(io.instSlave.rvalid && io.instSlave.rready, a_free, a_inst),
		a_rdata -> Mux(io.dataSlave.rvalid && io.dataSlave.rready, a_free, a_rdata),
		a_wdata -> Mux(io.dataSlave.bvalid && io.dataSlave.bready, a_free, a_wdata)
	))

	io.instSlave.setSlaveDefault()
	io.dataSlave.setSlaveDefault()
	io.out.setMasterDefault()
	when(a_state === a_inst) {
		io.out <> io.instSlave
		io.dataSlave.setSlaveDefault()
	} .elsewhen(a_state === a_rdata || a_state === a_wdata) {
		io.out <> io.dataSlave
		io.instSlave.setSlaveDefault()
	} .elsewhen(a_state === a_free) {
		io.out.setMasterDefault()
		io.instSlave.setSlaveDefault()
		io.dataSlave.setSlaveDefault()
	}
}

class AXI1x2Bar extends Module {
	val io = IO(new Bundle {
		val in = new AXI4IO()
		val sram = Flipped(new AXI4IO())
		val mmio = Flipped(new AXI4IO())
	})
	val hitSRAM = (io.in.arvalid && 
		(io.in.araddr >= NPCParameters.sramStart.U && 
		 io.in.araddr < (NPCParameters.sramStart + NPCParameters.sramSize).U)) || 
		 ((io.in.awvalid) &&
		(io.in.awaddr >= NPCParameters.sramStart.U && 
		 io.in.awaddr < (NPCParameters.sramStart + NPCParameters.sramSize).U))
	val hitMMIO = (io.in.arvalid &&
		(io.in.araddr >= NPCParameters.mmioStart.U && 
		 io.in.araddr < (NPCParameters.mmioStart + NPCParameters.mmioSize).U)) || 
		 ((io.in.awvalid) &&
		(io.in.awaddr >= NPCParameters.mmioStart.U && 
		 io.in.awaddr < (NPCParameters.mmioStart + NPCParameters.mmioSize).U))

	val x_sram :: x_mmio :: Nil = Enum(2)
	val x_state = RegInit(x_sram)
	x_state := MuxLookup(x_state, x_sram)(Seq(
		x_sram -> Mux(hitMMIO, x_mmio, x_sram),
		x_mmio -> Mux(hitSRAM, x_sram, x_mmio)
	))

	io.in.setSlaveDefault()
	io.sram.setMasterDefault()
	io.mmio.setMasterDefault()
	when(hitSRAM) {
		io.sram <> io.in
		io.mmio.setMasterDefault()
	} .elsewhen(hitMMIO) {
		io.mmio <> io.in
		io.sram.setMasterDefault()
	} .otherwise {
		when(x_state === x_sram) {
			io.sram <> io.in
			io.mmio.setMasterDefault()
		} .elsewhen(x_state === x_mmio) {
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
		val instSlave = new AXI4IO()
		val dataSlave = new AXI4IO()
		val out = Flipped(new AXI4IO())
	})
	val arbiter = Module(new AXIArbiter())
	// val xbar = Module(new AXI1x2Bar())
	// val sram = Module(new SRAMImp())
	// val mmio = Module(new MMIO())
	arbiter.io.instSlave <> io.instSlave // ifu -> 2x1 arbiter in
	arbiter.io.dataSlave <> io.dataSlave // mem -> 2x1 arbiter in
	// xbar.io.in <> arbiter.io.out // 2x1 arbiter out -> 1x2 crossbar in
	// sram.io <> xbar.io.sram // 1x2 crossbar out -> sram
	// mmio.io.arbiterIn <> xbar.io.mmio // 1x2 crossbar out -> mmio
	io.out <> arbiter.io.out
}
