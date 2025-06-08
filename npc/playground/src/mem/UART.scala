package cpu.mem

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu._

class UARTImp extends DeviceBase {
	override val device = "uart"
	// random access latency
	val wcnt = RegInit(0.U(5.W))
	val wlfsr = LFSR(5, io.wvalid && io.wready, Some(BigInt(0b01001)))
	val writtenFlag = RegInit(false.B)
	when(io.awvalid && io.awready) {
		wcnt := wlfsr
	}
	when(wastate === aw_fire && wstate === w_fire) {
		when(wcnt =/= 0.U) {
			wcnt := wcnt - 1.U
		}
	}
	when(io.bvalid && io.bready) {
		writtenFlag := false.B
	}
	when(wcnt === 0.U && !writtenFlag) {
		printf("%c", io.wdata(7,0))
		writtenFlag := true.B
	}

	io.awready := wastate === aw_idle
	io.wready := wstate === w_idle
	io.bvalid := wastate === aw_fire && wstate === w_fire && wcnt === 0.U
	io.bresp := RespEncoding.OKAY
}
