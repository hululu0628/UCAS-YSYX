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
	val exuAddr = Input(UInt(12.W))
	val exuExType = Input(ExType())
	val wbuAddr = Input(UInt(12.W))
	val wbuExType = Input(ExType())
	val wbuFuType = Input(FuType())
	val wbuPC = Input(UInt(32.W))
	val wbuSrc1 = Input(UInt(32.W))
	val wbuImm = Input(UInt(32.W))

	val exuRdata = Output(UInt(32.W))
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

	val exuAddr = io.exuAddr
	val exuExType = io.exuExType
	val wbuAddr = io.wbuAddr
	val wbuExType = io.wbuExType
	val wbuFuType = io.wbuFuType
	val wbuPC = io.wbuPC
	val wbuSrc1 = io.wbuSrc1
	val wbuImm = io.wbuImm

	val op = WireInit(0.U(2.W))
	val src = WireInit(0.U(32.W))
	val rdata = WireInit(0.U(32.W))

	val csrHitVecEX = csrList.map { case csr => exuAddr === csr.getaddr }.toVector
	val csrHitVecWB = csrList.map { case csr => wbuAddr === csr.getaddr }.toVector

	// This may be modified when there's a lot of CSR
	when(exuExType === ExType.CSR) {
		csrHitVecEX.zipWithIndex.foreach { 
			case (hit, i) => when(hit) {rdata := csrList(i).rdata}
		}
	} .elsewhen(exuExType === ExType.Ecall) {
		rdata := csrNameHT("mtvec").rdata
	} .elsewhen(exuExType === ExType.Mret) {
		rdata := csrNameHT("mepc").rdata
	} .otherwise {
		rdata := 0.U
	}

	when(wbuExType === ExType.CSR) {
		op := MuxLookup(wbuFuType, 3.U(2.W))(Seq(
			CSRType.csrrw -> CSROp.write,
			CSRType.csrrs -> CSROp.set,
			CSRType.csrrc -> CSROp.clear,
			CSRType.csrrwi -> CSROp.write,
			CSRType.csrrsi -> CSROp.set,
			CSRType.csrrci -> CSROp.clear
		))
		csrHitVecWB.zipWithIndex.foreach { 
			case (hit, i) => when(hit) {csrList(i).write(csrList(i).rdata, wbuSrc1, op)}
		}
	} .elsewhen(wbuExType === ExType.Ecall) {
		op := CSROp.write
		csrNameHT("mepc").write(csrNameHT("mepc").rdata, wbuPC, op)
		csrNameHT("mcause").write(csrNameHT("mcause").rdata, 0xb.U(32.W), op)
	} .elsewhen(wbuExType === ExType.Mret) {
		op := CSROp.write
		csrNameHT("mepc").write(csrNameHT("mepc").rdata, wbuPC, op)
	}

	io.exuRdata := rdata
}