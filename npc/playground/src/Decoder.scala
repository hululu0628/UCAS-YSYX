package cpu

import chisel3._
import chisel3.util._

// 暂时先用def，等到需要切分执行阶段时再拉出信号
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

object ImmType extends ChiselEnum{
	val IType,SType,BType,UType,JType = Value
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

trait LdType {
	def lbu = BitPat("b100")
	def lw = BitPat("b010")
}

trait StType {
	def sb = BitPat("b000")
	def sw = BitPat("b010")
}

trait AluRType {
	def add_funct7 = BitPat("b0000000")

	def add_funct3 = BitPat("b000")
}

trait AluIType {
	def addi = BitPat("b000")
}


trait EbreakType {
	def ebreak = BitPat("b00000000000100000000000001110011")
}

trait InstJudge extends DecoderBase with OpType with LdType with StType with AluRType with AluIType with EbreakType {
	def isLoad = opcode === load
	def isStore = opcode === store
	def isAluR = opcode === alu_r
	def isAluI = opcode === alu_i
	def isBranch = opcode === branch

	def isAddi = isAluI && funct3 === addi
	def isAdd = isAluR && funct3 === add_funct3 && funct7 === add_funct7
	def isLbu = isLoad && funct3 === lbu
	def isLw = isLoad && funct3 === lw
	def isSb = isStore && funct3 === sb
	def isSw = isStore && funct3 === sw
	def isLui = opcode === lui
	def isAuipc = opcode === auipc
	def isJal = opcode === jal
	def isJalr = opcode === jalr && funct3 === BitPat("b000")
	def isEbreak = instr === ebreak
}

// TODO: get aluop and imm type
trait ImmJudge
trait AluopJudge

class DecoderIO extends Bundle {
	val instr = Input(UInt(32.W))
}

class Decoder extends InstJudge with ImmJudge with AluopJudge{
	val io = IO(new DecoderIO)
	instr := io.instr
}