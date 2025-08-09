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

class ysyxCPU extends Module with StageConflict {
	val io = IO(new CPUIO)
	/**
	  * Modules of processor
	  */
	val ifu = Module(new InstFetch())
	val idu = Module(new Decoder())
	val exu = Module(new EXU())
	val lsu = Module(new LSU())
	val wbu = Module(new WBU())
	val ebreak_handler = Module(new EbreakHandler())
	val bus = Module(new AXI4Bus())
	val debug = Module(new DPIDebug())

	/**
	  * Stage Connect
	  */
	StageConnectPipe(ifu.io.out, idu.io.in, false.B)
	StageConnectPipe(idu.io.out, exu.io.decode, false.B)
	StageConnectPipe(exu.io.out, lsu.io.exu, false.B)
	StageConnectPipe(lsu.io.out, wbu.io.in, false.B)
	StageConnectSingle(wbu.io.w2f, ifu.io.writeback)
	StageConnectSingle(wbu.io.w2d, idu.io.writeback)
	StageConnectSingle(wbu.io.w2e, exu.io.writeback)

	// Check Data Hazard
	val isDataHazard = Wire(Bool())
	isDataHazard := isRAW(idu.io.out.bits.decoded.inst.rs1, idu.io.out.bits.decoded.inst.rs2,
				exu.io.decode.bits.decoded.inst.rd, exu.io.decode.bits.decoded.wenR, exu.io.decode.valid,
				lsu.io.exu.bits.info.inst.rd, lsu.io.exu.bits.info.wenR, lsu.io.exu.valid,
				wbu.io.in.bits.info.inst.rd, wbu.io.in.bits.info.wenR, wbu.io.in.valid)
	idu.io.dataHazard := isDataHazard
	// Check Branch Hazard
	val isBrHazard = Wire(Bool())
	isBrHazard := isBrHazard(idu.io.out.bits.decoded.exType, idu.io.in.valid,
				exu.io.decode.bits.decoded.exType, exu.io.decode.valid,
				lsu.io.exu.bits.info.exType, lsu.io.exu.valid)
	ifu.io.isBrHazard := isBrHazard
	
	
	if(NPCParameters.cache.enableICache) {
		val icache = Module(new ICache())
		icache.io.flush := wbu.io.fromWbFlushICache
		icache.io.instSlave <> ifu.io.instMaster
		bus.io.instSlave <> icache.io.icacheMaster
	} else {
		bus.io.instSlave <> ifu.io.instMaster
	}
	
	bus.io.dataSlave <> lsu.io.dataMaster
	
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
	debug.io.accValid := wbu.io.w2e.bits.accValid
	debug.io.accAddr := wbu.io.w2e.bits.accAddr
}
