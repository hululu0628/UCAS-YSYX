package cpu.ifu

import chisel3._
import chisel3.util._
import chisel3.util.random._

import cpu._

class InstSRAM extends AXI4LiteBase {
	val dpiFetch = Module(new DPIFetch())

	// random access latency
	val cnt = RegInit(0.U(5.W))
	val lfsr = LFSR(5, io.arvalid && io.arready, Some(BigInt(0b00101)))
	when(io.arvalid && io.arready) {
		cnt := lfsr
	}
	when(cnt =/= 0.U) {
		cnt := cnt - 1.U
	}
	when(cnt === 0.U) {
		dpiFetch.io.pc := raddr
	} .otherwise {
		dpiFetch.io.pc := 0.U
	}

	io.arready := rstate === r_idle
	io.rvalid := rstate === r_waitrdata && cnt === 0.U
	io.rdata := dpiFetch.io.inst
	io.rresp := RespEncoding.OKAY
}
