package cpu

import chisel3._
import chisel3.util._

class PerfCntBaseIO extends Bundle {
	val clock = Input(Clock())
	val reset = Input(Bool())
	val enable = Input(Bool())
}

class PerfCnt(name: String, width: Int) extends BlackBox with HasBlackBoxInline {
	val io = IO(new PerfCntBaseIO)
	override val desiredName = s"PerfCnt_$name"
	setInline(
		s"PerfCnt_$name.sv",
		s"""
		|module PerfCnt_$name (
		|	input clk,
		|	input reset,
		|	input enable
		|);
		|	reg [$width - 1:0] cnt;
		|	always @(posedge clk or posedge reset) begin
		|		if (reset) begin
		|			cnt <= 0;
		|		end else if (enable) begin
		|			cnt <= cnt + 1;
		|		end
		|	end
		|	final begin
		|		$$display("[Performance counter %s]: %ld", "$name", cnt);
		|	end
		|endmodule
		""".stripMargin
	)
}

object PerfCnt {
	def apply(name: String, info: String, enable: Bool, width: Int): PerfCnt = {
		val cnt = Module(new PerfCnt(name, width))
		cnt.io.enable := enable
		cnt
	}
}
