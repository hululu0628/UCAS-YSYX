package cpu.ifu

import chisel3._
import chisel3.util._
import chisel3.util.random._

import cpu._

class InstSRAM extends AXI4LiteBase {
	val dpiFetch = Module(new DPIFetch())

	val cnt = RegInit(0.U(32.W))
	val lfsr = LFSR(5, io.arvalid && io.arready, Some(BigInt(0b10010)))
}
