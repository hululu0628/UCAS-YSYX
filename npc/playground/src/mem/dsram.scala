package cpu.mem

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu.decode._

class DataSRAM extends Module {
	val io = IO(new Bundle {
		val valid = Input(Bool())
		val ready = Output(Bool())
		val wen = Input(Bool())
		val addr = Input(UInt(32.W))
		val wdata = Input(UInt(32.W))
		val wmask = Input(UInt(8.W))
		val rdata = Output(UInt(32.W))
	})
	val dpiMem = Module(new DPIMem())
	val dataReg = RegEnable(dpiMem.io.rdata, io.valid)
	dpiMem.io.valid := io.valid
	dpiMem.io.wen := io.wen
	dpiMem.io.addr := io.addr
	dpiMem.io.wdata := io.wdata
	dpiMem.io.wmask := io.wmask

	val s_idle :: s_memready :: Nil = Enum(2)
	val state = RegInit(s_idle)
	state := MuxLookup(state, s_idle)(Seq(
		s_idle -> Mux(io.valid && !io.wen, s_memready, s_idle),
		s_memready -> Mux(io.valid && io.ready, s_idle, s_memready)
	))


	val cnt = RegInit(0.U(4.W))
	val random = LFSR(4, io.valid, Some(BigInt(7))) // random latency
	when(io.valid && !io.wen) {
		cnt := random
	}
	when(state === s_memready && cnt =/= 0.U) {
		cnt := cnt - 1.U
	}
	io.ready := state === s_memready && cnt === 0.U
	io.rdata := dataReg

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
