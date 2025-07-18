package cpu.mem

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu._

class CLINTImp extends DeviceBase {
	override val device = "clint"
	val mtime = RegInit(0.U(64.W))
	mtime := mtime + 1.U

	val addr = RegEnable(io.araddr, 0.B.asTypeOf(io.araddr), io.arvalid && io.arready)
	io.arready := rstate === r_idle
	io.rvalid := rstate === r_waitrdata
	io.rdata := Mux(addr === 0x0200_0000L.U, mtime(31, 0), 
			Mux(addr === 0x0200_0004L.U, mtime(63, 32), 0.U))
	io.rresp := RespEncoding.OKAY
}
