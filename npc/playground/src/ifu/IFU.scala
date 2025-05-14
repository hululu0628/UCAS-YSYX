package cpu.ifu

import chisel3._
import chisel3.util._
import cpu.decode._
import cpu.wb._

class IFUOut extends Bundle {
	val inst = new StaticInst
	val pc = UInt(32.W)
}

class IFIO extends Bundle {
	val writeback = Flipped(Decoupled(new WBUOut))
	val out = Decoupled(new IFUOut)
}

class InstFetch extends Module {
	val io = IO(new IFIO)

	val dpi_fetch = Module(new DPIFetch())

	val pc = RegInit(0x80000000L.U(32.W))

	pc := io.writeback.bits.nextpc

	dpi_fetch.io.pc := pc

	io.out.bits.inst.code := dpi_fetch.io.inst

	io.out.bits.pc := pc

	// for single cpu
	io.writeback.ready := true.B
	io.out.valid := true.B
}
