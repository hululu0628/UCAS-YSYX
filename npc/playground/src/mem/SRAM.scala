package cpu.mem

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu._

class SRAMImp extends AXI4LiteSlaveBase {
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
		dpiMem.io.addr := raddr
	}
	io.arready := rstate === r_idle
	io.rvalid := rstate === r_waitrdata && rcnt === 0.U
	io.rdata := dpiMem.io.rdata
	io.rresp := RespEncoding.OKAY

	val wcnt = RegInit(0.U(5.W))
	val wlfsr = LFSR(5, io.wvalid && io.wready, Some(BigInt(0b01001)))
	when(io.awvalid && io.awready) {
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

	io.awready := wastate === aw_idle
	io.wready := wstate === w_idle
	io.bvalid := wastate === aw_fire && wstate === w_fire && wcnt === 0.U
	io.bresp := RespEncoding.OKAY

}
