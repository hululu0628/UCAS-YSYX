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
	/* Select data because memory only support 4byte-algined mem access */
	def getldata(memdata: UInt, ltype: UInt, sign: Bool, offset: UInt): UInt = {
		MuxLookup(ltype, 0.U(32.W))(Seq(
			LSLen.word -> memdata,
			LSLen.half -> Cat(Fill(16, sign && memdata((offset(1) << 4)+15.U)), (memdata >> (offset(1) << 4))(15, 0)),
			LSLen.byte -> Cat(Fill(24, sign && memdata((offset << 3) + 7.U)), (memdata >> (offset << 3))(7, 0))
		))
	}
	def getwdata(data: UInt, stype: UInt, offset: UInt): UInt = {
		MuxLookup(stype, 0.U(32.W))(Seq(
			LSLen.word -> data,
			LSLen.half -> (data << (offset(1) << 4)),
			LSLen.byte -> (data << (offset << 3))
		))
	}
	def getwmask(stype: UInt, offset: UInt): UInt = {
		MuxLookup(stype, 0.U(4.W))(Seq(
			LSLen.word -> "b00001111".U,
			LSLen.half -> (Cat(0.U(6.W), "b11".U) << offset(1, 0)),
			LSLen.byte -> (Cat(0.U(7.W), "b1".U) << offset(1, 0))
		))
	}
	mem.io.valid := decoder.io.out.exType === ExType.Load || decoder.io.out.exType === ExType.Store
	mem.io.addr := MuxLookup(decoder.io.out.lsLength, 0.U(32.W))(Seq(
		LSLen.word -> (mem_addr(31, 0) & "hffff_fffc".U),
		LSLen.half -> (mem_addr(31, 0) & "hffff_fffc".U),
		LSLen.byte -> (mem_addr(31, 0) & "hffff_fffc".U)
	))
	mem.io.wen := decoder.io.out.wenM
	mem.io.wdata := getwdata(rdata2, decoder.io.out.lsLength, mem_addr(1, 0))
	mem.io.wmask := getwmask(decoder.io.out.lsLength, mem_addr(1, 0))
	val ldata = getldata(mem.io.rdata, decoder.io.out.lsLength, decoder.io.out.loadSignExt, mem_addr(1, 0))

	/**
	  * Write Back
	  */
	result := MuxLookup(decoder.io.out.exType, alu.io.result)(Seq(
		ExType.Lui -> immgen.io.imm,
		ExType.Jal -> (pc + 4.U),
		ExType.Jalr -> (pc + 4.U),
		ExType.Load -> ldata
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
