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

	val isram = Module(new InstSRAM())

	val pc = RegEnable(io.writeback.bits.nextpc, 0x80000000L.U(32.W), io.writeback.fire)

	isram.io.pc := pc

	io.out.bits.inst.code := isram.io.inst

	io.out.bits.pc := pc

	// for single cpu
	io.writeback.ready := io.out.ready
	io.out.valid := isram.io.valid 
}
