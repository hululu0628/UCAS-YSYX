package cpu

import chisel3._
import chisel3.util._

class DPIDebugIO extends Bundle {
	val valid = Output(Bool())
	val pc = Output(UInt(32.W))
	val npc = Output(UInt(32.W))
	val inst = Output(UInt(32.W))
	val wen = Output(Bool())
	val waddr = Output(UInt(5.W))
	val data = Output(UInt(32.W))
}

class DPIDebug extends BlackBox with HasBlackBoxInline {
	val io = IO(new DPIDebugIO)
	setInline(
		"DPIDebug.sv",
		"""
		|module DPIDebug(
		|	input valid,
		|	input [31:0] pc,
		|	input [31:0] npc,
		|	input [31:0] inst,
		|	input wen,
		|	input [4:0] waddr,
		|	input [31:0] data
		|);
		|import "DPI-C" function void debug(
		|	input int valid,
		|	input int pc,
		|	input int npc,
		|	input int inst,
		|	input int wen,
		|	input int waddr,
		|	input int data
		|);
		|always @(*) begin
		|	debug(
		|		valid,
		|		pc,
		|		npc,
		|		inst,
		|		wen,
		|		waddr,
		|		data
		|	)
		|end
		|endmodule
		""".stripMargin
	)
}
