package cpu.mem

import chisel3._
import chisel3.util._
import cpu.decode._

class DPIMemIO extends Bundle {
	val valid = Input(Bool())
	val wen = Input(Bool())
	val addr = Input(UInt(32.W))
	val wdata = Input(UInt(32.W))
	val wmask = Input(UInt(8.W))
	val rdata = Output(UInt(32.W))
}

class DPIMem extends BlackBox with HasBlackBoxInline {
	val io = IO(new DPIMemIO)
	setInline(
		"DPIMem.sv",
		"""
		|module DPIMem(
		|	input valid,
		|	input wen,
		|	input [31:0] addr,
		|	input [31:0] wdata,
		|	input [7:0] wmask,
		|	output reg [31:0] rdata
		|);
		|import "DPI-C" function int dpic_read(input int raddr);
		|import "DPI-C" function void dpic_write(
		|input int waddr, input int wdata, input byte wmask);
		|always @(*) begin
		|	if (valid) begin
		|		if (wen) begin
		|			dpic_write(addr, wdata, wmask);
		|		end
		|		if(!wen) begin
		|			rdata = dpic_read(addr);
		|		end
		|		else begin
		|			rdata = 0;
		|		end
		|	end
		|	else begin
		|		rdata = 0;
		|	end
		|end
		|endmodule
		""".stripMargin
	)
}