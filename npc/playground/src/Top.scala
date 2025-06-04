package cpu

import chisel3._
import chisel3.util._
import cpu.ifu._
import cpu.decode._
import cpu.exu._
import cpu.wb._

class CPUIO extends Bundle {
	val debug = new Bundle {
		val valid = Output(Bool())
		val pc = Output(UInt(32.W))
		val npc = Output(UInt(32.W))
		val inst = Output(new StaticInst)
		val wen = Output(Bool())
		val waddr = Output(UInt(5.W))
		val data = Output(UInt(32.W))
	}
}

class Top extends Module{
	val io = IO(new CPUIO)
	/**
	  * Modules of processor
	  */
	val ifu = Module(new InstFetch())
	val idu = Module(new Decoder())
	val exu = Module(new EXU())
	val wbu = Module(new WBU())
	val ebreak_handler = Module(new EbreakHandler())
	val bus = Module(new AXI4Bus())

	/**
	  * Stage Connect
	  */
	StageConnectMulti(ifu.io.out, idu.io.in)
	StageConnectMulti(idu.io.out, exu.io.decode)
	StageConnectMulti(exu.io.out, wbu.io.in)
	StageConnectSingle(wbu.io.w2e, exu.io.writeback)
	StageConnectSingle(wbu.io.w2f, ifu.io.writeback)
	
	bus.io.instin <> ifu.io.instin
	bus.io.datain <> exu.io.datain
	
	/* ebreak */
	ebreak_handler.io.inst_ebreak := wbu.io.w2e.bits.info.isEbreak

	/**
	  * Debug Module
	  */
	io.debug.valid := wbu.io.w2e.valid
	io.debug.pc := wbu.io.w2e.bits.info.pc
	io.debug.npc := wbu.io.w2f.bits.nextpc
	io.debug.inst := wbu.io.w2e.bits.info.inst
	io.debug.wen := wbu.io.w2e.bits.info.wenR
	io.debug.waddr := wbu.io.w2e.bits.info.inst.rd
	io.debug.data := wbu.io.w2e.bits.regWdata
}
