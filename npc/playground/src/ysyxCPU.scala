package cpu

import chisel3._
import chisel3.util._
import cpu.ifu._
import cpu.decode._
import cpu.exu._
import cpu.mem.cache._
import cpu.wb._

class CPUIO extends Bundle {
	val interrupt = Input(Bool())
	val master = Flipped(new AXI4IO)
	val slave = new AXI4IO
}

class ysyxCPU extends Module{
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
	val debug = Module(new DPIDebug())

	/**
	  * Stage Connect
	  */
	StageConnectMulti(ifu.io.out, idu.io.in)
	StageConnectMulti(idu.io.out, exu.io.decode)
	StageConnectMulti(exu.io.out, wbu.io.in)
	StageConnectSingle(wbu.io.w2e, exu.io.writeback)
	StageConnectSingle(wbu.io.w2f, ifu.io.writeback)
	
	if(NPCParameters.cache.enableICache) {
		val icache = Module(new ICache())
		icache.io.instSlave <> ifu.io.instMaster
		bus.io.instSlave <> icache.io.icacheMaster
	} else {
		bus.io.instSlave <> ifu.io.instMaster
	}
	
	bus.io.dataSlave <> exu.io.dataMaster
	
	/* ebreak */
	ebreak_handler.io.inst_ebreak := wbu.io.w2e.bits.info.isEbreak

	/* io out */
	io.master <> bus.io.out
	io.slave <> DontCare
	dontTouch(io.master)
	dontTouch(io.slave)

	/**
	  * Debug Module
	  */
	debug.io.reset := reset.asUInt
	debug.io.valid := wbu.io.w2e.valid
	debug.io.pc := wbu.io.w2e.bits.info.pc
	debug.io.npc := wbu.io.w2f.bits.nextpc
	debug.io.inst := wbu.io.w2e.bits.info.inst.code
	debug.io.wen := wbu.io.w2e.bits.info.wenR
	debug.io.waddr := wbu.io.w2e.bits.info.inst.rd
	debug.io.data := wbu.io.w2e.bits.regWdata
}
