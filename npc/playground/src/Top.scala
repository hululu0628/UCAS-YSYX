package cpu

import chisel3._
import chisel3.util._

class CPUIO extends Bundle {
	val instr 	= Input(UInt(32.W))
	val pc		= Output(UInt(32.W))

	//val mem_wen 	= Output(Bool())
	//val mem_addr 	= Output(UInt(32.W))
	//val mem_wdata 	= Output(UInt(32.W))
	//val mem_rdata 	= Input(UInt(32.W))
}

class Top extends Module{
	val io = IO(new CPUIO)

	/**
	  * Modules of CPU
	  */
	val regfile = Module(new Regfile())
	val alu = Module(new ALU())
	val ebreak_handler = Module(new EbreakHandler())

	dontTouch(regfile.io)
	dontTouch(alu.io)

	val pc = RegInit(0x80000000L.U(32.W))

	pc := pc + 4.U
	io.pc := pc

	/* decode */
	val instr = io.instr

	val opcode = instr(6, 0)
	val rd = instr(11, 7)
	val rs1 = instr(19, 15)
	val rs2 = instr(24, 20)
	val funct3 = instr(14, 12)
	val funct7 = instr(31, 25)
	val shamt = instr(24, 20)

	val Rtype = opcode(5) & opcode(4) & ~opcode(2)
	val Itype_C = ~opcode(5) & opcode(4) & ~opcode(2)


	val inst_ebreak = WireInit(0.B)
	inst_ebreak := 	opcode === 0b1110011.U(7.W) && 
			funct3 === 0b000.U(3.W) && 
			funct7 === 0b0000000.U(7.W) && 
			rd === 0b00000.U(5.W) && 
			rs2 === 0b00001.U(5.W)


	/* regfile */

	

	regfile.io.wen := ~inst_ebreak
	regfile.io.waddr := rd
	regfile.io.wdata := alu.io.result
	regfile.io.raddr1 := rs1
	regfile.io.raddr2 := rs2

	val rdata1 = regfile.io.rdata1
	val rdata2 = regfile.io.rdata2


	/* ALU */
	val imm = Cat(Fill(20, instr(31)), instr(31, 20)) // I-type immediate

	alu.io.aluop := 0.U(3.W)
	alu.io.A := rdata1
	alu.io.B := imm

	/* ebreak */
	ebreak_handler.io.inst_ebreak := inst_ebreak

}

class EbreakHandler extends BlackBox with HasBlackBoxInline{
	val io = IO(new Bundle {
		val inst_ebreak = Input(Bool())
	})
	
	setInline(
		"Top.v",
		s"""
		module EbreakHandler(
		|	input inst_ebreak
		|);
		|	import "DPI-C" function void ebreak_handler(bit inst_ebreak);
		|
		|	always @(*) begin
		|		ebreak_handler(inst_ebreak);
		|	end
		|
		|endmodule
		""".stripMargin
	)
}