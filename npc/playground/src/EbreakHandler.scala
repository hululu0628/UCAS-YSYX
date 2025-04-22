package cpu

import chisel3._
import chisel3.util._

class EbreakHandler extends BlackBox with HasBlackBoxInline{
	val io = IO(new Bundle {
		val inst_ebreak = Input(Bool())
	})
	
	setInline(
		"ebreak_handler.sv",
		"""
		|module EbreakHandler(
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