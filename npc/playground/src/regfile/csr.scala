package cpu.regfile

import chisel3._
import chisel3.util._
import cpu.decode._

object CSROp {
	def write = "b00".U
	def set = "b01".U
	def clear = "b10".U
}

class CSRIO extends Bundle {
	val pc = Input(UInt(32.W))
	val addr = Input(UInt(12.W))
	val exType = Input(ExType())
	val fuType = Input(FuType())
	val src1_reg = Input(UInt(32.W))
	val uimm = Input(UInt(32.W))
	val rdata = Output(UInt(32.W))
}

trait CSRList{
	def csr_addr = List(
		0x300.U, 0x305.U, 0x341.U, 0x342.U
	)
	def csr_name = List(
		"mstatus", "mtvec", "mepc", "mcause"
	)
}

case class CSRBase(addr: UInt) {
	val csr = RegInit(0.U(32.W))
	def getaddr = addr
	def rdata = csr

	def write(csrdata: UInt, src: UInt, op: UInt): Unit = {
		csr := MuxLookup(op, csr)(Seq(
			CSROp.write -> src,
			CSROp.set -> (csrdata | src),
			CSROp.clear -> (csrdata & ~src)
		))	
	}
}

class CSR extends Module with CSRList{
	val io = IO(new CSRIO)

	val rdata = io.rdata
	val fuType = io.fuType
	val addr = io.addr
	val reg = io.src1_reg
	val uimm = io.uimm

	val mstatus = CSRBase(csr_addr(0))
	val mtvec = CSRBase(csr_addr(1))
	val mepc = CSRBase(csr_addr(2))
	val mcause = CSRBase(csr_addr(3))
	val csr_list = List(mstatus, mtvec, mepc, mcause)
	val csrHitVec = csr_list.map(_.getaddr === addr)

	val op = WireInit(0.U(2.W))
	val src = WireInit(0.U(32.W))
	rdata := 0.U(32.W)

	// This may be modified when there's a lot of CSR
	when(io.exType === ExType.CSR) {
		op := MuxLookup(fuType, 3.U(2.W))(Seq(
			CSRType.csrrw -> CSROp.write,
			CSRType.csrrs -> CSROp.set,
			CSRType.csrrs -> CSROp.clear,
			CSRType.csrrwi -> CSROp.write,
			CSRType.csrrsi -> CSROp.set,
			CSRType.csrrci -> CSROp.clear
		))
		src := Mux((fuType === CSRType.csrrwi || fuType === CSRType.csrrsi || fuType === CSRType.csrrci), uimm, reg)
		csr_list.zip(csrHitVec).foreach { case (csr, hit) =>
			when(hit) {
				rdata := csr.rdata
				csr.write(csr.rdata, reg, op)
		}}
	} .elsewhen(io.exType === ExType.Ecall) {
		op := CSROp.write
		rdata := mtvec.rdata
		mepc.write(mepc.rdata, io.pc, op)
		mcause.write(mcause.rdata, 0xb.U(32.W), op)
	} .elsewhen(io.exType === ExType.Mret) {
		op := CSROp.write
		rdata := mepc.rdata
	}
}