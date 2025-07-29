import chisel3._
import chisel3.util._

import cpu._
import cpu.mem.cache._

class ICacheTEST extends Module {
	val io = IO(new Bundle {
		val instSlave = new AXI4IO
	})
	val dut = Module(new ICache())
	val mem = Mem(32, UInt(32.W))
	dut.io.instSlave <> io.instSlave
}
