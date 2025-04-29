package cpu

import chisel3._
import chisel3.util._
import cpu.AluType.{None => None}

object StaticInst {
	def apply() = UInt(32.W)

	def opcode
}

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

object SrcFrom {
	def RS1 = "b00".U
	def RS2 = "b01".U
	def Imm = "b10".U
	def PC = "b11".U

	def apply() = UInt(2.W)
}

object ImmType {
	def IType = "b000".U
	def SType = "b001".U
	def BType = "b010".U
	def UType = "b011".U
	def JType = "b100".U
	def RType = "b101".U

	def apply() = UInt(3.W)
}

object ExType {
	def AluR = "b0000".U
	def AluI = "b0001".U
	def Load = "b0010".U
	def Store = "b0011".U
	def Branch = "b0100".U
	def Lui = "b0101".U
	def Auipc = "b0110".U
	def Jal = "b0111".U
	def Jalr = "b1000".U
	def Ebreak = "b1001".U

	def apply() = UInt(4.W)
}

object AluType {
	def add = "b0000".U
	def sub = "b0001".U
	def and = "b0010".U
	def or = "b0011".U
	def xor = "b0100".U
	def slt = "b0101".U
	def sltu = "b0110".U
	def sll = "b0111".U
	def srl = "b1000".U
	def sra = "b1001".U

	def apply() = UInt(4.W)
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

object LSLen {
	def byte = "b?00".U
	def half = "b?01".U
	def word = "b?10".U

	def apply() = UInt(2.W)
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
	val rs1From = SrcFrom() // rs1 source
	val rs2From = SrcFrom() // rs2 source
	val exType = ExType()
	val immType = ImmType()
	val aluType = Output(UInt(4.W))
	val lsLength = LSLen() // length of load/store data

	val inst = Output(UInt(32.W))
}

class DecoderIO extends Bundle {
	val instr = Input(UInt(32.W))
	val out = new DecodedInst
}

class Decoder extends InstJudge {
	val io = IO(new DecoderIO)
	instr := io.instr

	def InstDecode(inst: UInt): DecodedInst = {

	}
}