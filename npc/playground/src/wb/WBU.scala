package cpu.wb

import chisel3._
import chisel3.util._
import cpu.decode._
import cpu.exu._

class WBUOut extends Bundle {
	val nextpc = UInt(32.W)
	val regWdata = UInt(32.W)
	val Imm = UInt(32.W)
	val regRdata = UInt(32.W)
	val info = new DecodedInst
}

class WBUIO extends Bundle {
	val in = Flipped(Decoupled(new EXUOut))
	val out = Decoupled(new WBUOut)
}

class WBU extends Module {
	val io = IO(new WBUIO)

	val in = io.in.bits
	val out = io.out.bits
	val nextpc = out.nextpc
	val result = out.regWdata

	/**
	  * PC next
	  */
	when(in.result.bruFlag && in.info.exType === ExType.Branch) {
		nextpc := in.result.alu
	} .elsewhen(in.info.exType === ExType.Ecall || in.info.exType === ExType.Mret) {
		nextpc := in.result.csr
	} .otherwise {
		nextpc := in.info.pc + 4.U
	}

	/**
	  * mux result
	  */
	result := MuxLookup(in.info.exType, in.result.alu)(Seq(
		ExType.Lui -> in.result.imm,
		ExType.Branch -> (in.info.pc + 4.U),
		ExType.Load -> (in.result.mem),
		ExType.CSR -> (in.result.csr),
	))

	out.info := in.info
	out.Imm := in.result.imm
	out.regRdata := in.result.rdata1

	// for single cpu
	io.in.ready := true.B
	io.out.valid := true.B
}
