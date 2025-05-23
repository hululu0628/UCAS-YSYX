package cpu.ifu

import chisel3._
import chisel3.util._

class DPIFetch extends BlackBox with HasBlackBoxInline {
	val io = IO(new Bundle{
		val pc = Input(UInt(32.W))
		val inst = Output(UInt(32.W))
	})
	setInline(
		"DPIFetch.sv",
		"""
		|module DPIFetch(
		|	input [31:0] pc,
		|	output reg [31:0] inst
		|);
		|import "DPI-C" function int dpic_read(input int raddr);
		|always @(*) begin
		|	inst = dpic_read(pc);
		|end
		|endmodule
		""".stripMargin
	)
}