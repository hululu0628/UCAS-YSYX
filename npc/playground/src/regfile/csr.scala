package cpu.regfile

import chisel3._
import chisel3.util._
import cpu.decode._

object CSROp {
	def write = "b00".U
	def set = "b01".U
	def clear = "b10".U
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

class CSRIO extends Bundle {
	val pc = Input(UInt(32.W))
	val addr = Input(UInt(12.W))
	val exType = Input(ExType())
	val fuType = Input(FuType())
	val src1_reg = Input(UInt(32.W))
	val uimm = Input(UInt(32.W))
	val rdata = Output(UInt(32.W))
}

trait CSRList {
	// Add new csr here
	def csrPair = List(
		("mstatus", 0x300.U),
		("mtvec", 0x305.U),
		("mepc", 0x341.U),
		("mcause", 0x342.U)
	)
	def csrName = csrPair.unzip._1
	def csrAddr = csrPair.unzip._2

	// get csr by name
	val csrNameHT = csrName.zip(csrAddr).map { case (name, addr) => (name -> CSRBase(addr))}.toMap
	// get csr by index
	val csrList = csrName.map { name => csrNameHT(name) }
}

class CSR extends Module with CSRList{
	val io = IO(new CSRIO)

	val fuType = io.fuType
	val addr = io.addr
	val reg = io.src1_reg
	val uimm = io.uimm

	val op = WireInit(0.U(2.W))
	val src = WireInit(0.U(32.W))
	val rdata = WireInit(0.U(32.W))

	val csrHitVec = csrList.map { case csr => addr === csr.getaddr }.toVector

	// This may be modified when there's a lot of CSR
	when(io.exType === ExType.CSR) {
		op := MuxLookup(fuType, 3.U(2.W))(Seq(
			CSRType.csrrw -> CSROp.write,
			CSRType.csrrs -> CSROp.set,
			CSRType.csrrc -> CSROp.clear,
			CSRType.csrrwi -> CSROp.write,
			CSRType.csrrsi -> CSROp.set,
			CSRType.csrrci -> CSROp.clear
		))
		src := Mux((fuType === CSRType.csrrwi || 
			    fuType === CSRType.csrrsi || 
			    fuType === CSRType.csrrci), uimm, reg)
		csrHitVec.zipWithIndex.foreach { case (hit, i) =>
			when(hit) {
				rdata := csrList(i).rdata
				csrList(i).write(csrList(i).rdata, reg, op)
		}}
	} .elsewhen(io.exType === ExType.Ecall) {
		op := CSROp.write
		rdata := csrNameHT("mtvec").rdata
		csrNameHT("mepc").write(csrNameHT("mepc").rdata, io.pc, op)
		csrNameHT("mcause").write(csrNameHT("mcause").rdata, 0xb.U(32.W), op)
	} .elsewhen(io.exType === ExType.Mret) {
		op := CSROp.write
		rdata := csrNameHT("mepc").rdata
	}

	io.rdata := rdata
}