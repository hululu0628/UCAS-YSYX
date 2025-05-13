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
	val valid = Input(Bool())
	val wen = Input(Bool())
	val addr = Input(UInt(12.W))
	val fuType = Input(FuType())
	val src1_reg = Input(UInt(32.W))
	val uimm = Input(UInt(32.W))
	val rdata = Output(UInt(32.W))
}

trait CSRList{
	def csr_addr = List(
		0x300, 0x305, 0x341, 0x342
	)
	def csr_name = List(
		"mstatus", "mtvec", "mepc", "mcause"
	)
}

case class CSRBase(addr: UInt) {
	val csr = RegInit(0.U(32.W))
	def getaddr = addr
	def rdata = csr

	def select(op: UInt, reg: UInt, uimm: UInt) = {
		when(op === CSROp.write) {
			reg
		} .otherwise {
			uimm
		}
	}

	def write(data: UInt, mask: UInt, op: UInt): Unit = {
		csr := MuxLookup(op, csr)(Seq(
			CSROp.write -> data,
			CSROp.set -> (csr | mask),
			CSROp.clear -> (csr & ~mask)
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


	
}