package cpu.mem

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu.decode._
import cpu._

trait memfunc {
	/* Select because of aligned requirement */
	def getAlignedAddr(addr: UInt, lstype: UInt): UInt = {
		MuxLookup(lstype, 0.U(32.W))(Seq(
			LSLen.word -> (addr(31, 0) & "hffff_fffc".U),
			LSLen.half -> (addr(31, 0) & "hffff_fffc".U),
			LSLen.byte -> (addr(31, 0) & "hffff_fffc".U)
		))
	}
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
}

class DataSRAM extends AXI4LiteBase with memfunc {
	val dpiMem = Module(new DPIMem())
	dpiMem.io.valid := false.B
	dpiMem.io.wen := false.B
	dpiMem.io.wmask := 0.U(4.W)
	dpiMem.io.addr := 0.U(32.W)
	dpiMem.io.wdata := 0.U(32.W)
	
	// random access latency
	val rcnt = RegInit(0.U(5.W))
	val rlfsr = LFSR(5, io.arvalid && io.arready, Some(BigInt(0b00101)))
	when(io.arvalid && io.arready) {
		rcnt := rlfsr
	}
	when(rcnt =/= 0.U) {
		rcnt := rcnt - 1.U
	}
	when(rcnt === 0.U) {
		dpiMem.io.valid := true.B
		dpiMem.io.wmask := io.wstrb
		dpiMem.io.addr := raddr
	}
	io.arready := rstate === r_idle
	io.rvalid := rstate === r_waitrdata && rcnt === 0.U
	io.rdata := dpiMem.io.rdata
	io.rresp := RespEncoding.OKAY

	val wcnt = RegInit(0.U(5.W))
	val wlfsr = LFSR(5, io.wvalid && io.wready, Some(BigInt(0b01001)))
	when(io.arvalid && io.arready) {
		wcnt := wlfsr
	}
	when(wastate === aw_fire && wstate === w_fire) {
		when(wcnt =/= 0.U) {
			wcnt := wcnt - 1.U
		}
		when(wcnt === 0.U) {
			dpiMem.io.valid := true.B
			dpiMem.io.wen := true.B
			dpiMem.io.wmask := io.wstrb
			dpiMem.io.addr := waddr
			dpiMem.io.wdata := io.wdata
		}
	}

	io.awready := wastate === aw_idle || wastate === aw_waitvalid
	io.wready := wstate === w_idle || wstate === w_waitvalid
	io.bvalid := wastate === aw_fire && wstate === w_fire && wcnt === 0.U
	io.bresp := RespEncoding.OKAY
}
