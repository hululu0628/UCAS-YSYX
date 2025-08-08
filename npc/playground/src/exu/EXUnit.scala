package cpu.exu

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._
import cpu.wb._
import cpu.regfile._
import cpu.mem._

class EXUOut extends Bundle {
	val result = new Bundle {
		val alu = UInt(32.W)
		val csr = UInt(32.W)
		val imm = UInt(32.W)
		val bruFlag = Bool()
		val rdata1 = UInt(32.W)
		val rdata2 = UInt(32.W)
	}
	val info = new DecodedInst
}

class EXUIO extends Bundle {
	val decode = Flipped(Decoupled(new DecodedInst))
	val writeback = Flipped(Decoupled(new W2EOut))
	val out = Decoupled(new EXUOut)
}

class EXU extends Module with memfunc {
	val io = IO(new EXUIO)
	val idIn = io.decode.bits
	val wbIn = io.writeback.bits
	val out = io.out.bits

	val regfile = Module(new Regfile())
	val csrCtrlBlock = Module(new CSR())
	val immgen = Module(new ImmGen())
	val bru = Module(new BRU())
	val alu = Module(new ALU())

	val aluA = Wire(UInt(32.W))
	val aluB = Wire(UInt(32.W))

	/**
	  * ImmGen
	  */
	immgen.io.inst := idIn.inst
	immgen.io.immType := idIn.immType

	/**
	  * Regfile
	  */
	regfile.io.wen := wbIn.info.wenR && io.writeback.fire
	regfile.io.waddr := wbIn.info.inst.rd
	regfile.io.wdata := wbIn.regWdata
	regfile.io.raddr1 := idIn.inst.rs1
	regfile.io.raddr2 := idIn.inst.rs2

	/**
	  * CSR
	  */
	csrCtrlBlock.io.exuAddr := idIn.inst.imm12
	csrCtrlBlock.io.exuExType := idIn.exType
	csrCtrlBlock.io.wen := io.writeback.fire
	csrCtrlBlock.io.wbuAddr := wbIn.info.inst.imm12
	csrCtrlBlock.io.wbuExType := wbIn.info.exType
	csrCtrlBlock.io.wbuFuType := wbIn.info.fuType
	csrCtrlBlock.io.wbuImm := Cat(0.U(27.W), wbIn.info.inst.uimm)
	csrCtrlBlock.io.wbuPC := wbIn.info.pc
	csrCtrlBlock.io.wbuSrc1 := wbIn.regRdata

	/**
	  * ALU
	  */
	aluA := MuxLookup(idIn.src1From, 0.U(32.W))(Seq(
		SrcFrom.RS1 -> regfile.io.rdata1,
		SrcFrom.RS2 -> regfile.io.rdata2,
		SrcFrom.PC -> idIn.pc,
		SrcFrom.Imm -> immgen.io.imm
	))
	aluB := MuxLookup(idIn.src2From, 0.U(32.W))(Seq(
		SrcFrom.RS1 -> regfile.io.rdata1,
		SrcFrom.RS2 -> regfile.io.rdata2,
		SrcFrom.PC -> idIn.pc,
		SrcFrom.Imm -> immgen.io.imm
	))
	alu.io.A := aluA
	alu.io.B := aluB
	alu.io.aluType := MuxLookup(idIn.exType, idIn.fuType)(Seq(
		ExType.Branch -> AluType.add,
	))

	/**
	  * BRU
	  */
	bru.io.src1 := regfile.io.rdata1
	bru.io.src2 := regfile.io.rdata2
	bru.io.fuType := idIn.fuType

	// output
	out.info := idIn
	out.result.alu := alu.io.result
	out.result.csr := csrCtrlBlock.io.exuRdata
	out.result.imm := immgen.io.imm
	out.result.bruFlag := bru.io.br_flag
	out.result.rdata1 := regfile.io.rdata1
	out.result.rdata2 := regfile.io.rdata2

	// for multi-cycle cpu
	io.decode.ready := io.out.fire || !io.decode.valid
	io.writeback.ready := true.B
	io.out.valid := io.decode.valid
	
	val Perf_loadEXLat = PerfCnt("loadExLat", idIn.exType === ExType.Load, 64, io.decode.fire, io.out.fire)
	val Perf_storeEXLat = PerfCnt("storeExLat", idIn.exType === ExType.Store, 64, io.decode.fire, io.out.fire)
	val Perf_csrEXLat = PerfCnt("csrExLat", idIn.exType === ExType.CSR, 64, io.decode.fire, io.out.fire)
	val Perf_branchLat = PerfCnt("branchLat", idIn.exType === ExType.Branch, 64, io.decode.fire, io.out.fire)
	val Perf_calcLat = PerfCnt("calcLat", (idIn.exType === ExType.AluR || idIn.exType === ExType.AluI ||
		idIn.exType === ExType.Lui || idIn.exType === ExType.Auipc), 64, io.decode.fire, io.out.fire)
}
