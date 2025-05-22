package cpu

import chisel3._
import chisel3.util._

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
	val s_idle :: s_waitrdata :: s_waitrready :: Nil = Enum(3)
	val rstate = RegInit(s_idle)
	rstate := MuxLookup(rstate, s_idle)(Seq(
		s_idle -> Mux(io.arvalid && io.arready, s_waitrdata, s_idle),
		s_waitrdata -> Mux(io.rvalid, Mux(io.rready, s_idle, s_waitrready), s_waitrdata),
		s_waitrready -> Mux(io.rvalid && io.rready, s_idle, s_waitrready)
	))
	val raddr = RegEnable(io.araddr, io.arvalid && io.arready)
	val rport = RegEnable(io.arport, io.arvalid && io.arready)
	
}
