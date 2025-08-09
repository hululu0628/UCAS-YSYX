package cpu.wb

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._
import cpu.exu._
import upack.Ext

class W2DOut extends Bundle {
	val regWdata = UInt(32.W)
	val Imm = UInt(32.W)
	val regRdata = UInt(32.W)
	val info = new DecodedInst
}

class W2EOut extends Bundle {
	val regWdata = UInt(32.W)
	val Imm = UInt(32.W)
	val regRdata = UInt(32.W)
	val info = new DecodedInst
	val accValid = Bool()
	val accAddr = UInt(32.W)
}

class WBUIO extends Bundle {
	val in = Flipped(Decoupled(new LSUOut))
	val fromWbFlushICache = Output(Bool())
	val w2f = Decoupled(new Bundle {val nextpc = UInt(32.W)})
	val w2d = Decoupled(new W2DOut)
	val w2e = Decoupled(new W2EOut)
}

class WBU extends Module {
	val io = IO(new WBUIO)

	val in = io.in.bits
	val w2d = io.w2d.bits
	val w2e = io.w2e.bits
	val w2f = io.w2f.bits
	val nextpc = io.w2f.bits.nextpc
	val result = w2d.regWdata
	val resultEXU = result

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

	w2d.info := in.info
	w2d.Imm := in.result.imm
	w2d.regRdata := in.result.rdata1

	w2e.info := in.info
	w2e.Imm := in.result.imm
	w2e.regRdata := in.result.rdata1
	w2e.regWdata := resultEXU
	w2e.accValid := in.info.exType === ExType.Store || in.info.exType === ExType.Load
	w2e.accAddr := in.result.alu

	// for single cpu
	io.in.ready := true.B
	io.w2f.valid := true.B
	io.w2d.valid := io.in.valid
	io.w2e.valid := io.in.valid

	io.fromWbFlushICache := in.info.exType === ExType.FENCEI

	val commit = io.w2d.fire
	val Perf_loadNum = PerfCnt("loadNum", commit && in.info.exType === ExType.Load, 64)
	val Perf_storeNum = PerfCnt("storeNum", commit && in.info.exType === ExType.Store, 64)
	val Perf_csrNUM = PerfCnt("csrNum", commit && in.info.exType === ExType.CSR, 64)
	val Perf_branchNum = PerfCnt("branchNum", commit && in.info.exType === ExType.Branch, 64)
	val Perf_calcNum = PerfCnt("calcNum", 
		commit && (in.info.exType === ExType.AluR || in.info.exType === ExType.AluI ||
		in.info.exType === ExType.Lui || in.info.exType === ExType.Auipc), 64)
}
