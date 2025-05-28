package cpu.exu

import chisel3._
import chisel3.util._
import cpu._
import cpu.decode._
import cpu.wb._
import cpu.regfile._
import cpu.mem.DataSRAM

class EXUOut extends Bundle {
	val result = new Bundle {
		val alu = UInt(32.W)
		val csr = UInt(32.W)
		val imm = UInt(32.W)
		val mem = UInt(32.W)
		val bruFlag = Bool()
		val rdata1 = UInt(32.W)
	}
	val info = new DecodedInst
}

class EXUIO extends Bundle {
	val decode = Flipped(Decoupled(new DecodedInst))
	val writeback = Flipped(Decoupled(new W2EOut))
	val out = Decoupled(new EXUOut)
}

class EXU extends Module {
	val io = IO(new EXUIO)
	val idIn = io.decode.bits
	val wbIn = io.writeback.bits
	val out = io.out.bits

	val regfile = Module(new Regfile())
	val csrCtrlBlock = Module(new CSR())
	val immgen = Module(new ImmGen())
	val bru = Module(new BRU())
	val alu = Module(new ALU())
	val mem = Module(new DataSRAM())

	val aluA = Wire(UInt(32.W))
	val aluB = Wire(UInt(32.W))

	// State machine for connecting different stages
	val d2eState = Module(new StateMachine("master"))
	d2eState.io.valid := io.decode.valid
	d2eState.io.ready := io.decode.ready
	val e2wState = Module(new StateMachine("slave"))
	e2wState.io.valid := io.out.valid
	e2wState.io.ready := io.out.ready

	/**
	  * ImmGen
	  */
	immgen.io.inst := idIn.inst
	immgen.io.immType := idIn.immType

	/**
	  * Regfile
	  */
	regfile.io.wen := wbIn.info.wenR && io.writeback.fire
	regfile.io.waddr := wbIn.info.inst.rd
	regfile.io.wdata := wbIn.regWdata
	regfile.io.raddr1 := idIn.inst.rs1
	regfile.io.raddr2 := idIn.inst.rs2

	/**
	  * CSR
	  */
	csrCtrlBlock.io.exuAddr := idIn.inst.imm12
	csrCtrlBlock.io.exuExType := idIn.exType
	csrCtrlBlock.io.wen := io.writeback.fire
	csrCtrlBlock.io.wbuAddr := wbIn.info.inst.imm12
	csrCtrlBlock.io.wbuExType := wbIn.info.exType
	csrCtrlBlock.io.wbuFuType := wbIn.info.fuType
	csrCtrlBlock.io.wbuImm := Cat(0.U(27.W), wbIn.info.inst.uimm)
	csrCtrlBlock.io.wbuPC := wbIn.info.pc
	csrCtrlBlock.io.wbuSrc1 := wbIn.regRdata

	/**
	  * ALU
	  */
	aluA := MuxLookup(idIn.src1From, 0.U(32.W))(Seq(
		SrcFrom.RS1 -> regfile.io.rdata1,
		SrcFrom.RS2 -> regfile.io.rdata2,
		SrcFrom.PC -> idIn.pc,
		SrcFrom.Imm -> immgen.io.imm
	))
	aluB := MuxLookup(idIn.src2From, 0.U(32.W))(Seq(
		SrcFrom.RS1 -> regfile.io.rdata1,
		SrcFrom.RS2 -> regfile.io.rdata2,
		SrcFrom.PC -> idIn.pc,
		SrcFrom.Imm -> immgen.io.imm
	))
	alu.io.A := aluA
	alu.io.B := aluB
	alu.io.aluType := MuxLookup(idIn.exType, idIn.fuType)(Seq(
		ExType.Branch -> AluType.add,
	))

	/**
	  * BRU
	  */
	bru.io.src1 := regfile.io.rdata1
	bru.io.src2 := regfile.io.rdata2
	bru.io.fuType := idIn.fuType

	/**
	  * Memory
	  */
	val memAddr = alu.io.result
	// state machine for AXI4Lite load
	val r_idle :: r_waitready :: r_waitrdata :: Nil = Enum(3)
	val el2RAMState = RegInit(r_idle)
	el2RAMState := MuxLookup(el2RAMState, r_idle)(Seq(
		r_idle -> Mux(io.decode.fire, r_waitready, r_idle),
		r_waitready -> Mux(mem.io.arvalid, Mux(mem.io.arready, r_waitrdata, r_waitready), Mux(io.decode.fire, r_waitready, r_idle)),
		r_waitrdata -> Mux(mem.io.rvalid && mem.io.rready, r_idle, r_waitrdata)
	))
	mem.io.arvalid := idIn.exType === ExType.Load && (el2RAMState === r_waitready)
	mem.io.araddr := mem.getAlignedAddr(memAddr, idIn.lsLength)
	mem.io.arport := AxPortEncoding.genPortCode(Seq(AxPortEncoding.unpriv, AxPortEncoding.secure, AxPortEncoding.daccess))
	mem.io.rready := (el2RAMState === r_waitrdata) && io.out.ready
	val ldata = mem.getldata(mem.io.rdata, idIn.lsLength, idIn.loadSignExt, memAddr(1, 0))
	// state machine for AXI4Lite store
	val w_idle :: w_waitwfire :: w_waitbvalid :: Nil = Enum(2)
	val es2RAMState = RegInit(w_idle)
	es2RAMState := MuxLookup(es2RAMState, w_idle)(Seq(
		w_idle -> Mux(io.decode.fire, w_waitwfire, w_idle),
		w_waitwfire -> Mux(mem.io.awvalid && mem.io.wvalid, 
			Mux(mem.io.awready && mem.io.wready, w_waitbvalid, w_waitbvalid), 
			Mux(io.decode.fire, w_waitbvalid, w_idle)
			),
		w_waitbvalid -> Mux(mem.io.bvalid && mem.io.bready, w_idle, w_waitbvalid)
	))
	mem.io.awvalid := idIn.exType === ExType.Store && (es2RAMState === w_waitwfire)
	mem.io.awaddr := mem.getAlignedAddr(memAddr, idIn.lsLength)
	mem.io.awport := AxPortEncoding.genPortCode(Seq(AxPortEncoding.unpriv, AxPortEncoding.secure, AxPortEncoding.daccess))
	mem.io.wvalid := idIn.exType === ExType.Store && (es2RAMState === w_waitwfire)
	mem.io.wdata := mem.getwdata(regfile.io.rdata2, idIn.lsLength, memAddr(1, 0))
	mem.io.wstrb := mem.getwmask(idIn.lsLength, memAddr(1, 0))
	mem.io.bready := (es2RAMState === w_waitbvalid) && io.out.ready

	// output
	out.info := idIn
	out.result.alu := alu.io.result
	out.result.csr := csrCtrlBlock.io.exuRdata
	out.result.imm := immgen.io.imm
	out.result.mem := ldata
	out.result.bruFlag := bru.io.br_flag
	out.result.rdata1 := regfile.io.rdata1

	// for multi-cycle cpu
	io.decode.ready := io.out.ready || d2eState.io.state === d2eState.s_waitvalid
	io.writeback.ready := true.B
	io.out.valid := MuxLookup(idIn.exType, RegNext(io.decode.fire))(Seq(
		ExType.Load -> (mem.io.rvalid && mem.io.rready),
		ExType.Store -> (mem.io.bvalid && mem.io.bready)
	))
}
