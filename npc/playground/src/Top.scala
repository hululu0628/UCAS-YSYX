package cpu

import chisel3._
import chisel3.util._
import cpu.decode._
import cpu.exu._

class CPUIO extends Bundle {
	val inst 	= Input(UInt(32.W))
	val pc		= Output(UInt(32.W))

	//val mem_wen 	= Output(Bool())
	//val mem_addr 	= Output(UInt(32.W))
	//val mem_wdata 	= Output(UInt(32.W))
	//val mem_rdata 	= Input(UInt(32.W))

	val debug = new Bundle {
		val pc = Output(UInt(32.W))
		val wen = Output(Bool())
		val data = Output(UInt(32.W))
	}
}

class Top extends Module{
	val io = IO(new CPUIO)
	/**
	  * Modules of CPU
	  */
	val decoder = Module(new Decoder())
	val regfile = Module(new Regfile())
	val immgen = Module(new ImmGen())
	val bru = Module(new BRU())
	val alu = Module(new ALU())
	val ebreak_handler = Module(new EbreakHandler())

	dontTouch(regfile.io)
	dontTouch(alu.io)

	/**
	  * Alias
	  */
	val inst = io.inst
	val wen = regfile.io.wen
	val waddr = regfile.io.waddr
	val wdata = regfile.io.wdata
	val rdata1 = regfile.io.rdata1
	val rdata2 = regfile.io.rdata2

	/**
	  * Wire
	  */
	val result = Wire(UInt(32.W))
	val alu_A = Wire(UInt(32.W))
	val alu_B = Wire(UInt(32.W))

	/**
	  * Instruction Fetch
	  */
	val pc = RegInit(0x80000000L.U(32.W))

	when(decoder.io.out.exType === ExType.Jalr) {
		pc := bru.io.target;
	} .otherwise {
		pc := pc + 4.U
	}

	io.pc := pc

	/**
	  * Decoder
	  */
	decoder.io.inst := inst

	/**
	  * RegFile
	  */
	regfile.io.wen := decoder.io.out.wenR
	regfile.io.waddr := decoder.io.out.inst.rd
	regfile.io.wdata := result
	regfile.io.raddr1 := decoder.io.out.inst.rs1
	regfile.io.raddr2 := decoder.io.out.inst.rs2

	/**
	  * ALU
	  */
	immgen.io.inst := decoder.io.out.inst
	immgen.io.immType := decoder.io.out.immType
	alu_A := MuxLookup(decoder.io.out.src1From, 0.U(32.W))(Seq(
		SrcFrom.RS1 -> rdata1,
		SrcFrom.RS2 -> rdata2,
		SrcFrom.PC -> pc,
		SrcFrom.Imm -> immgen.io.imm
	))
	alu_B := MuxLookup(decoder.io.out.src2From, 0.U(32.W))(Seq(
		SrcFrom.RS1 -> rdata1,
		SrcFrom.RS2 -> rdata2,
		SrcFrom.PC -> pc,
		SrcFrom.Imm -> immgen.io.imm
	))
	alu.io.A := alu_A
	alu.io.B := alu_B
	alu.io.aluType := decoder.io.out.aluType

	/**
	  * BRU
	  */
	bru.io.pc := pc
	bru.io.imm := immgen.io.imm
	bru.io.reg := rdata1
	bru.io.exType := decoder.io.out.exType

	/**
	  * Write Back
	  */
	result := MuxLookup(decoder.io.out.exType, alu.io.result)(Seq(
		ExType.Lui -> immgen.io.imm,
		ExType.Jal -> (pc + 4.U),
		ExType.Jalr -> (pc + 4.U)
	))

	/* ebreak */
	ebreak_handler.io.inst_ebreak := decoder.io.out.isEbreak


	/**
	  * Debug Module
	  */

	io.debug.pc := pc
	io.debug.wen := wen
	io.debug.data := wdata
}
