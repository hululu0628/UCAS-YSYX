package cpu.mem.cache

import chisel3._
import chisel3.util._

import cpu._
import cpu.regfile.CSROp.set
import cpu.NPCParameters.cache

class ICacheIO extends Bundle {
	val instSlave = new AXI4IO
	val icacheMaster = Flipped(new AXI4IO)
}

class ICache extends Module {
	val io = IO(new ICacheIO)

	val addr = RegEnable(io.instSlave.araddr, 0.U, io.instSlave.arvalid && io.instSlave.arready)
	val tag = addr(NPCParameters.cache.addrWidth - 1, NPCParameters.cache.addrWidth - NPCParameters.cache.taglen)
	val index = addr(NPCParameters.cache.addrWidth - NPCParameters.cache.taglen - 1, NPCParameters.cache.offsetlen)
	val offset = addr(NPCParameters.cache.offsetlen - 1, 0)

	val len = RegInit(0.U(4.W))
	val dataWriteIn = RegInit(0.U(NPCParameters.cache.lineSize.W))

	val cache_data = Module(new ICacheData())
	val cache_meta = Module(new ICacheMetaData())
	cache_data.io.clock := clock
	cache_data.io.reset := reset
	cache_meta.io.clock := clock
	cache_meta.io.reset := reset

	val addrHit = WireInit(false.B)
	val cacheHit = WireInit(false.B)

	val c_idle :: c_check :: c_hit :: c_miss :: c_recv :: Nil = Enum(5)
	val state = RegInit(c_idle)
	state := MuxLookup(state, c_idle)(Seq(
		c_idle -> Mux(io.instSlave.arvalid && addrHit, c_check, c_idle),
		c_check -> Mux(cacheHit, c_hit, c_miss),
		c_hit -> Mux(io.instSlave.rvalid && io.instSlave.rready, c_idle, c_hit),
		c_miss -> Mux(io.icacheMaster.arready && io.icacheMaster.arvalid, c_recv, c_miss),
		c_recv -> Mux(
			io.icacheMaster.rvalid && io.icacheMaster.rready, 
			Mux(len === ((1 << (NPCParameters.cache.offsetlen - 2)) - 1).U, c_hit, c_miss), 
			c_recv
		)
	))

	when(cacheHit || state === c_hit) {
		len := 0.U
	} .elsewhen(state === c_recv && io.icacheMaster.rvalid && io.icacheMaster.rready) {
		len := len + 1.U
	}

	when(state === c_recv && io.icacheMaster.rvalid && io.icacheMaster.rready) {
		if(NPCParameters.cache.lineSize <= 32) {
			dataWriteIn := io.icacheMaster.rdata
		} else {
			dataWriteIn := Cat(io.icacheMaster.rdata, dataWriteIn(NPCParameters.cache.lineSize - 1, 32))
		}
	}

	cache_data.io.valid := true.B
	cache_data.io.wen := (len === ((1 << (NPCParameters.cache.offsetlen - 2)) - 1).U) && 
		state === c_recv && io.icacheMaster.rvalid && io.icacheMaster.rready
	cache_data.io.idx := index
	if(NPCParameters.cache.lineSize <= 32) {
		cache_data.io.wdata := io.icacheMaster.rdata
	} else {
		cache_data.io.wdata := Cat(io.icacheMaster.rdata, dataWriteIn(NPCParameters.cache.lineSize - 1, 32))
	}
	cache_meta.io.valid := true.B
	cache_meta.io.wen := (len === ((1 << (NPCParameters.cache.offsetlen - 2)) - 1).U) &&
		(state === c_recv && io.icacheMaster.rvalid && io.icacheMaster.rready)
	cache_meta.io.idx := index
	cache_meta.io.wtag := tag

	when(state === c_idle && io.instSlave.arvalid) {
		addrHit := (io.instSlave.araddr < NPCParameters.deviceTab("sram").base.U) ||
			(io.instSlave.araddr >= NPCParameters.deviceTab("sram").base.U + NPCParameters.deviceTab("sram").size.U)
	} .otherwise {
		addrHit := (addr < NPCParameters.deviceTab("sram").base.U) ||
			(addr >= NPCParameters.deviceTab("sram").base.U + NPCParameters.deviceTab("sram").size.U)
	}
	cacheHit := (state === c_check) && (cache_meta.io.rtag === tag) && cache_meta.io.isValid

	io.icacheMaster <> io.instSlave
	when(addrHit) {
		io.instSlave.arready := (state === c_idle)
		io.instSlave.rvalid := (state === c_hit)
		io.instSlave.rlast := (state === c_hit)
		io.instSlave.rdata := MuxLookup(offset, 0.U)(
		(0 until (1 << NPCParameters.cache.offsetlen) by 4).map { i =>
			i.U -> cache_data.io.rdata((i/4 + 1) * 32 - 1, (i/4) * 32)
		}
		)
		io.instSlave.rresp := RespEncoding.OKAY

		io.icacheMaster.arvalid := (state === c_miss)
		io.icacheMaster.araddr := (addr(NPCParameters.cache.addrWidth - 1, NPCParameters.cache.offsetlen) << NPCParameters.cache.offsetlen) +
						(len << 2)
		io.icacheMaster.arlen := 0.U(8.W)
		io.icacheMaster.arsize := TransferSize.WORD
		io.icacheMaster.arburst := BrustType.INCR
		io.icacheMaster.rready := (state === c_recv)
	}

	val Perf_icacheHit = PerfCnt("icacheHit", cacheHit, 64)
	val Perf_icacheAcc = PerfCnt("icacheAcc", true.B, 64, state === c_check, state =/= c_check)
	val Perf_accTime = PerfCnt("accTime", true.B, 64, state === c_check, state =/= c_check)
	val Perf_missPenalty = PerfCnt("missPenalty", true.B, 64, state === c_miss, state === c_hit)
}

class ICacheData extends BlackBox with HasBlackBoxInline {
	val io = IO(new Bundle {
		val clock = Input(Clock())
		val reset = Input(Bool())
		val valid = Input(Bool())
		val wen = Input(Bool())
		val idx = Input(UInt(NPCParameters.cache.indexlen.W))
		val rdata = Output(UInt(NPCParameters.cache.lineSize.W))
		val wdata = Input(UInt(NPCParameters.cache.lineSize.W))
	})
	setInline(
		"ICacheData.sv",
		s"""
		|module ICacheData (
		|	input clock,
		|	input reset,
		|	input valid,
		|	input wen,
		|	input [${NPCParameters.cache.indexlen - 1}:0] idx,
		|	output [${NPCParameters.cache.lineSize - 1}:0] rdata,
		|	input [${NPCParameters.cache.lineSize - 1}:0] wdata
		|);
		|	reg [${NPCParameters.cache.lineSize - 1}:0] cache_data [0:${NPCParameters.cache.setNum - 1}];
		|	always @(posedge clock) begin
		|		if(valid && wen) begin
		|			cache_data[idx] <= wdata;
		|		end
		|	end
		|	assign rdata = (valid) ? cache_data[idx] : 0;
		|endmodule
		""".stripMargin
	)
}

class ICacheMetaData extends BlackBox with HasBlackBoxInline {
	val io = IO(new Bundle {
		val clock = Input(Clock())
		val reset = Input(Bool())
		val valid = Input(Bool())
		val wen = Input(Bool())
		val idx = Input(UInt(NPCParameters.cache.indexlen.W))
		val wtag = Input(UInt(NPCParameters.cache.taglen.W))
		val rtag = Output(UInt(NPCParameters.cache.taglen.W))
		val isValid = Output(Bool())
	})
	setInline(
		"ICacheMetaData.sv",
		s"""
		|module ICacheMetaData (
		|	input clock,
		|	input reset,
		|	input valid,
		|	input wen,
		|	input [${NPCParameters.cache.indexlen - 1}:0] idx,
		|	input [${NPCParameters.cache.taglen - 1}:0] wtag,
		|	output [${NPCParameters.cache.taglen - 1}:0] rtag,
		|	output isValid
		|);
		|	reg [${NPCParameters.cache.taglen - 1}:0] tag_data [0:${NPCParameters.cache.setNum - 1}];
		|	reg [0:0] block_valid [0:${NPCParameters.cache.setNum - 1}];
		|	always @(posedge clock) begin
		|		if(reset) begin
		|			integer i;
		|			for (i = 0; i < ${NPCParameters.cache.setNum}; i = i + 1) begin
		|				block_valid[i] <= 1'b0;
		|			end
		|		end
		|		if(valid && wen) begin
		|			tag_data[idx] <= wtag;
		|			block_valid[idx] <= 1'b1;
		|		end
		|	end
		|	assign rtag = (valid) ? tag_data[idx] : 0;
		|	assign isValid = block_valid[idx];
		|endmodule
		""".stripMargin
    )
}
