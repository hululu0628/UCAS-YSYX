package cpu

import chisel3._
import chisel3.util._
import cpu.AluType.{None => None}


class DecoderBase extends Module{
	val instr = Wire(UInt(32.W))

	def opcode = instr(6, 0)
	def rd = instr(11, 7)
	def rs1 = instr(19, 15)
	def rs2 = instr(24, 20)
	def funct3 = instr(14, 12)
	def funct7 = instr(31, 25)
	def shamt = instr(24, 20)
}

object SrcFrom extends ChiselEnum{
	val Rs1 = Value(0.U)
	val Imm = Value(1.U)
	val Rs2 = Value(2.U)
	val PC = Value(3.U)
}

object ImmType extends ChiselEnum{
	val IType = Value(0.U)
	val SType = Value(1.U)
	val BType = Value(2.U)
	val UType = Value(3.U)
	val JType = Value(4.U)
	val RType = Value(5.U)
}

object ExType extends ChiselEnum{
	val AluR = Value(0.U)
	val AluI = Value(1.U)
	val Load = Value(2.U)
	val Store = Value(3.U)
	val Branch = Value(4.U)
	val Lui = Value(5.U)
	val Auipc = Value(6.U)
	val Jal = Value(7.U)
	val Jalr = Value(8.U)
	val Ebreak = Value(9.U)
}

object AluType extends ChiselEnum{
	val None = Value(0.U)
	val Add = Value(1.U)
	val Sub = Value(2.U)
	val Slt = Value(3.U)
	val Sltu = Value(4.U)
	val And = Value(5.U)
	val Or = Value(6.U)
	val Xor = Value(7.U)
	val Sll = Value(9.U)
	val Srl = Value(10.U)
	val Sra = Value(11.U)

}

trait OpType {
	def load = BitPat("b0000011")
	def alu_i = BitPat("b0010011")
	def store = BitPat("b0100011")
	def alu_r = BitPat("b0110011")
	def branch = BitPat("b1100011")
	def lui = BitPat("b0110111")
	def auipc = BitPat("b0010111")
	def jal = BitPat("b1101111")
	def jalr = BitPat("b1100111")
}

trait LSLen {
	def byte = BitPat("b?00")
	def half = BitPat("b?01")
	def word = BitPat("b?10")
}

trait AluOp {
	def add_funct3 = BitPat("b000")
	def add_funct7 = BitPat("b0000000")
	def sub_funct3 = BitPat("b000")
	def sub_funct7 = BitPat("b0100000")
	def slt_funct3 = BitPat("b01?")
	def xor_funct3 = BitPat("b100")
	def or_funct3 = BitPat("b101")
	def and_funct3 = BitPat("b110")
	def sll_funct3 = BitPat("b001")
	def srl_funct3 = BitPat("b101")
	def sra_funct3 = BitPat("b101")
	def srl_funct7 = BitPat("b0000000")
	def sra_funct7 = BitPat("b0100000")
}

trait EbreakType {
	def ebreak = BitPat("b00000000000100000000000001110011")
}

trait InstJudge extends DecoderBase with OpType with LSLen with AluOp with EbreakType {
	def isLoad = opcode === load
	def isStore = opcode === store
	def isAluR = opcode === alu_r
	def isAluI = opcode === alu_i
	def isBranch = opcode === branch
	def isLui = opcode === lui
	def isAuipc = opcode === auipc
	def isJal = opcode === jal
	def isJalr = opcode === jalr && funct3 === BitPat("b000")
	def isEbreak = instr === ebreak
}

class DecodedInst extends Bundle {
	val exType = Output(UInt(6.W))
	val immType = Output(UInt(3.W))
	val aluType = Output(UInt(4.W))
	val lsLength = Output(UInt(2.W)) // length of load/store data
	val rs1From = Output(UInt(2.W)) // rs1 source
	val rs2From = Output(UInt(2.W)) // rs2 source

	val inst = Output(UInt(32.W))
}

class DecoderIO extends Bundle {
	val instr = Input(UInt(32.W))
	val out = new DecodedInst
}

class Decoder extends InstJudge {
	val io = IO(new DecoderIO)
	instr := io.instr

	io.out.inst := instr

	val PriorityOH = Cat(
		isAluI,
		isAluR,
		isLoad,
		isStore,
		isBranch,
		isLui,
		isAuipc,
		isJal,
		isJalr,
		isEbreak
	)
	
	io.out.rs1From := Mux1H(
		PriorityOH,
		Seq(
			SrcFrom.Rs1,
			SrcFrom.Rs1,
			SrcFrom.Rs1,
			SrcFrom.Rs2,
			SrcFrom.Rs1,
			SrcFrom.Imm,
			SrcFrom.Imm,
			SrcFrom.Imm,
			SrcFrom.Imm,
			SrcFrom.Imm
		)
	)
	io.out.rs2From := Mux1H(
		PriorityOH,
		Seq(
			SrcFrom.Rs2,
			SrcFrom.Rs2,
			SrcFrom.Imm,
			SrcFrom.Imm,
			SrcFrom.Rs2,
			SrcFrom.Imm,
			SrcFrom.Imm,
			SrcFrom.Imm,
			SrcFrom.Imm,
			SrcFrom.Imm
		)
	)
	io.out.exType := Mux1H(
		PriorityOH,
		Seq(
			ExType.AluI,
			ExType.AluR,
			ExType.Load,
			ExType.Store,
			ExType.Branch,
			ExType.Lui,
			ExType.Auipc,
			ExType.Jal,
			ExType.Jalr,
			ExType.Ebreak
		)
	)
	io.out.immType := Mux1H(
		PriorityOH,
		Seq(
			ImmType.IType,
			ImmType.IType,
			ImmType.IType,
			ImmType.SType,
			ImmType.BType,
			ImmType.UType,
			ImmType.UType,
			ImmType.JType,
			ImmType.IType,
			ImmType.IType
		)
	)
	io.out.aluType := Mux1H(
		PriorityOH,
		Seq(
			AluType.None,
			AluType.Add,
			AluType.Add,
			AluType.Slt,
			AluType.None,
			AluType.None,
			AluType.None,
			AluType.None,
			AluType.None,
			AluType.None
		)
	)

	// TODO
	when(isAluI){
		io.out.aluType := MuxCase(AluType.None, Array(
			(funct3 === add_funct3) -> AluType.Add
		))
	}

	when(isLoad || isStore){
		io.out.lsLength := MuxCase(3.U, Array(
			(funct3 === byte) -> 0.U,
			(funct3 === half) -> 1.U,
			(funct3 === word) -> 3.U
		))
	}

}