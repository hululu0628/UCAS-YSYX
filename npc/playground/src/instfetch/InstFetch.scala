package cpu.instfetch

import chisel3._
import chisel3.util._

class InstFetch extends BlackBox with HasBlackBoxInline {
	val io = IO(new Bundle{
		val pc = Input(UInt(32.W))
		val inst = Output(UInt(32.W))
	})
	setInline(
		"InstFetch.sv",
		"""
		|module InstFetch(
		|	input [31:0] pc,
		|	output reg [31:0] inst
		|);
		|import "DPI-C" function int pmem_read(input int raddr);
		|always @(*) begin
		|	inst = pmem_read(pc);
		|end
		|endmodule
		""".stripMargin
	)
}