package cpu

import chisel3._
import chisel3.util._
import cpu.instfetch._
import cpu.decode._
import cpu.exu._
import cpu.mem._

class CPUIO extends Bundle {
	val debug = new Bundle {
		val pc = Output(UInt(32.W))
		val npc = Output(UInt(32.W))
		val inst = Output(UInt(32.W))
		val wen = Output(Bool())
		val waddr = Output(UInt(5.W))
		val data = Output(UInt(32.W))
	}
}

class Top extends Module{
	val io = IO(new CPUIO)
	/**
	  * Modules of processor
	  */
	val instfetch = Module(new InstFetch())
	val decoder = Module(new Decoder())
	val regfile = Module(new Regfile())
	val immgen = Module(new ImmGen())
	val bru = Module(new BRU())
	val alu = Module(new ALU())
	val mem = Module(new ysyxMem())
	val ebreak_handler = Module(new EbreakHandler())

	dontTouch(regfile.io)
	dontTouch(alu.io)

	/**
	  * Alias
	  */
	val inst = instfetch.io.inst
	val wen = regfile.io.wen
	val waddr = regfile.io.waddr
	val wdata = regfile.io.wdata
	val rdata1 = regfile.io.rdata1
	val rdata2 = regfile.io.rdata2

	val mem_addr = alu.io.result;

	/**
	  * Wires
	  */
	val result = Wire(UInt(32.W))
	val alu_A = Wire(UInt(32.W))
	val alu_B = Wire(UInt(32.W))

	/**
	  * Instruction Fetch
	  */
	val pc = RegInit(0x80000000L.U(32.W))
	val pc_next = Wire(UInt(32.W))
	pc := pc_next

	when(decoder.io.out.exType === ExType.Jalr) {
		pc_next := bru.io.target;
	} .otherwise {
		pc_next := pc + 4.U
	}

	instfetch.io.pc := pc

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
	  * Memory
	  */
	mem.io.valid := decoder.io.out.exType === ExType.Load || decoder.io.out.exType === ExType.Store
	mem.io.addr := mem.getAlignedAddr(mem_addr, decoder.io.out.lsLength)
	mem.io.wen := decoder.io.out.wenM
	mem.io.wdata := mem.getwdata(rdata2, decoder.io.out.lsLength, mem_addr(1, 0))
	mem.io.wmask := mem.getwmask(decoder.io.out.lsLength, mem_addr(1, 0))
	val ldata = mem.getldata(mem.io.rdata, decoder.io.out.lsLength, decoder.io.out.loadSignExt, mem_addr(1, 0))

	/**
	  * Write Back
	  */
	result := MuxLookup(decoder.io.out.exType, alu.io.result)(Seq(
		ExType.Lui -> immgen.io.imm,
		ExType.Jal -> (pc + 4.U),
		ExType.Jalr -> (pc + 4.U),
		ExType.Load -> (ldata)
	))

	/* ebreak */
	ebreak_handler.io.inst_ebreak := decoder.io.out.isEbreak

	/**
	  * Debug Module
	  */

	io.debug.pc := pc
	io.debug.npc := pc_next
	io.debug.inst := inst
	io.debug.wen := wen
	io.debug.waddr := waddr
	io.debug.data := wdata
}
