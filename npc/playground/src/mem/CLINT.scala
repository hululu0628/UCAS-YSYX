package cpu.mem

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu._

class CLINTImp extends DeviceBase {
	override val device = "clint"
	val mtime = RegInit(0.U(64.W))
	mtime := mtime + 1.U

	val rcnt = RegInit(0.U(5.W))
	val rlfsr = LFSR(5, io.arvalid && io.arready, Some(BigInt(0b00101)))
	val addr = RegEnable(io.araddr, 0.B.asTypeOf(io.araddr), io.arvalid && io.arready)
	when(io.arvalid && io.arready) {
		rcnt := rlfsr
	}
	when(rcnt =/= 0.U) {
		rcnt := rcnt - 1.U
	}
	io.arready := rstate === r_idle
	io.rvalid := rstate === r_waitrdata && rcnt === 0.U
	io.rdata := Mux(addr === 0xa0000048L.U, mtime(31, 0), 
			Mux(addr === 0xa000004cL.U, mtime(63, 32), 0.U))
	io.rresp := RespEncoding.OKAY
}
