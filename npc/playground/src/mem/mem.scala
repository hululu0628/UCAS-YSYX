package cpu.mem

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._

trait memfunc {
	/* Select because of aligned requirement */
	/*
	def getAlignedAddr(addr: UInt, lstype: UInt): UInt = {
		MuxLookup(lstype, 0.U(32.W))(Seq(
			LSLen.word -> (addr(31, 0) & "hffff_fffc".U),
			LSLen.half -> (addr(31, 0) & "hffff_fffc".U),
			LSLen.byte -> (addr(31, 0) & "hffff_fffc".U)
		))
	}
	*/
	// do not support burst transfer number > 1
	def getldata(memdata: UInt, ltype: UInt, sign: Bool, offset: UInt): UInt = {
		MuxLookup(ltype, 0.U(32.W))(Seq(
			LSLen.word -> memdata,
			LSLen.half -> Cat(Fill(16, sign && memdata((offset(1) << 4)+15.U)), (memdata >> (offset(1) << 4))(15, 0)),
			LSLen.byte -> Cat(Fill(24, sign && memdata((offset << 3) + 7.U)), (memdata >> (offset << 3))(7, 0))
		))
	}
	def getldataNew(memdata: UInt, ltype: UInt, sign: Bool, offset: UInt): UInt = {
		MuxLookup(ltype, 0.U(32.W))(Seq(
			LSLen.word -> memdata,
			LSLen.half -> Cat(Fill(16, sign && memdata(15)), memdata(15, 0)),
			LSLen.byte -> Cat(Fill(24, sign && memdata(7)), memdata(7, 0))
		))
	}
	def getAxSize(lstype: UInt): UInt = {
		MuxLookup(lstype, 0.U(3.W))(Seq(
			LSLen.word -> TransferSize.WORD,
			LSLen.half -> TransferSize.HALF,
			LSLen.byte -> TransferSize.BYTE
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
			LSLen.word -> "b1111".U,
			LSLen.half -> (Cat(0.U(2.W), "b11".U) << offset(1, 0)),
			LSLen.byte -> (Cat(0.U(3.W), "b1".U) << offset(1, 0))
		))
	}
	def getwmaskNew(stype: UInt, offset: UInt): UInt = {
		MuxLookup(stype, 0.U(4.W))(Seq(
			LSLen.word -> "b1111".U,
			LSLen.half -> "b0011".U,
			LSLen.byte -> "b0001".U
		))
	}
}

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