package cpu.mem.cache

import chisel3._
import chisel3.util._

class ICacheIO extends Bundle {
	val instSlave = new AXI4IO
	val icacheMaster = Flipped(new AXI4IO)
}

class ICache extends Module {
	val io = IO(new ICacheIO)
	io.instSlave.setSlaveDefault()
	io.icacheMaster.setMasterDefault()

	val mem = Module(new ICacheMem())
	mem.io.clock := clock
	mem.io.reset := reset

	val hit = Wire(Bool())

	val c_idle :: c_check :: c_hit :: c_miss :: c_recv :: Nil = Enum(5)
	val state = RegInit(c_idle)
	state := MuxLookup(state, c_idle)(Seq(
		c_idle -> Mux(io.icacheMaster.arvalid, c_check, c_idle),
		c_check -> Mux(hit, c_hit, c_miss),
		c_hit -> Mux(io.instSlave.rvalid && io.instSlave.rready, c_idle, c_hit),
		c_miss -> Mux(io.icacheMaster.arready && io.icacheMaster.arvalid, c_recv, c_miss),
		c_recv -> Mux(io.icacheMaster.rvalid && io.icacheMaster.rready, c_hit, c_recv)
	))

	io.instSlave.arready := (state === c_idle)
	io.instSlave.rvalid := (state === c_hit)
	io.instSlave.rlast := (state === c_hit)
	io.instSlave.rdata := data
	io.instSlave.rresp := RespEncoding.OKAY

	io.icacheMaster.arvalid := (state === c_miss)
	io.icacheMaster.araddr := addr
	io.icacheMaster.arlen := 0.U(8.W)
	io.icacheMaster.arsize := TransferSize.WORD
	io.icacheMaster.arburst := BrustType.INCR
	io.icacheMaster.rready := (state === c_recv)

}

class ICache extends BlackBox with HasBlackBoxInline {
	val io = IO(new Bundle {
		val clock = Input(Clock())
		val reset = Input(Bool())
		val valid = Input(Bool())
		val wen = Input(Bool())
		val idx = Input(UInt(NPCParameters.cache.indexlen.W))
		val rdata = Output(UInt(NPCParameters.cache.blockSize.W))
		val wdata = Input(UInt(NPCParameters.cache.blockSize.W))
	})
}
