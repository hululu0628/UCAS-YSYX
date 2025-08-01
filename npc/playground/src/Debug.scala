package cpu

import chisel3._
import chisel3.util._

class DPIDebugIO extends Bundle {
	val reset = Input(UInt(32.W))
	val valid = Input(UInt(32.W))
	val pc = Input(UInt(32.W))
	val npc = Input(UInt(32.W))
	val inst = Input(UInt(32.W))
	val wen = Input(UInt(32.W))
	val waddr = Input(UInt(32.W))
	val data = Input(UInt(32.W))
}

class DPIDebug extends BlackBox with HasBlackBoxInline {
	val io = IO(new DPIDebugIO)
	if(NPCParameters.debugEnable) {
	setInline(
		"DPIDebug.sv",
		"""
		|module DPIDebug(
		|	input [31:0] reset,
		|	input [31:0] valid,
		|	input [31:0] pc,
		|	input [31:0] npc,
		|	input [31:0] inst,
		|	input [31:0] wen,
		|	input [31:0] waddr,
		|	input [31:0] data
		|);
		|import "DPI-C" function void set_debug(
		|	input int reset,
		|	input int valid,
		|	input int pc,
		|	input int npc,
		|	input int inst,
		|	input int wen,
		|	input int waddr,
		|	input int data
		|);
		|always @(*) begin
		|	set_debug(
		|		reset,
		|		valid,
		|		pc,
		|		npc,
		|		inst,
		|		wen,
		|		waddr,
		|		data
		|	);
		|end
		|endmodule
		""".stripMargin
	)
	} else {
		setInline(
			"DPIDebug.sv",
			"""
			|module DPIDebug(
			|	input [31:0] reset,
			|	input [31:0] valid,
			|	input [31:0] pc,
			|	input [31:0] npc,
			|	input [31:0] inst,
			|	input [31:0] wen,
			|	input [31:0] waddr,
			|	input [31:0] data
			|);
			|endmodule
			""".stripMargin
		)
	}
}
