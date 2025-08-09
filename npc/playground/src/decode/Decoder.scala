package cpu.decode

import chisel3._
import chisel3.util._

import cpu._
import cpu.regfile._
import cpu.ifu._
import cpu.wb._

class StaticInst extends Bundle {
	val code = UInt(32.W)

	def opcode = code(6, 0)
	def rd = code(11, 7)
	def rs1 = code(19, 15)
	def rs2 = code(24, 20)
	def funct3 = code(14, 12)
	def funct7 = code(31, 25)
	def shamt = code(24, 20)

	def imm12 = code(31, 20)
	def uimm = code(19, 15)
}

case class DecodeBase(
	val src1From: UInt = 0.U,
	val src2From: UInt = 0.U,
	val exType: UInt = 0.U,
	val immType: UInt = 0.U,
	val fuType: UInt = 0.U,
	val lsLength: UInt = 0.U,
	val wenR: Bool = false.B,
	val wenM: Bool = false.B,
	val loadSignExt: Bool = false.B
) {
	def generate: List[UInt] = List(src1From, src2From, exType, immType, fuType, lsLength, wenR, wenM, loadSignExt)
}

object RV32IDecode {
	// RV32I Base
	def LUI 	= BitPat("b?????????????????????????0110111")
	def AUIPC 	= BitPat("b?????????????????????????0010111")
	def JAL 	= BitPat("b?????????????????????????1101111")
	def JALR 	= BitPat("b?????????????????000?????1100111")
	def BEQ 	= BitPat("b?????????????????000?????1100011")
	def BNE 	= BitPat("b?????????????????001?????1100011")
	def BLT 	= BitPat("b?????????????????100?????1100011")
	def BGE 	= BitPat("b?????????????????101?????1100011")
	def BLTU 	= BitPat("b?????????????????110?????1100011")
	def BGEU 	= BitPat("b?????????????????111?????1100011")
	def LB 		= BitPat("b?????????????????000?????0000011")
	def LH 		= BitPat("b?????????????????001?????0000011")
	def LW 		= BitPat("b?????????????????010?????0000011")
	def LBU 	= BitPat("b?????????????????100?????0000011")
	def LHU 	= BitPat("b?????????????????101?????0000011")
	def SB 		= BitPat("b?????????????????000?????0100011")
	def SH 		= BitPat("b?????????????????001?????0100011")
	def SW 		= BitPat("b?????????????????010?????0100011")
	def ADDI 	= BitPat("b?????????????????000?????0010011")
	def SLTI 	= BitPat("b?????????????????010?????0010011")
	def SLTIU 	= BitPat("b?????????????????011?????0010011")
	def XORI 	= BitPat("b?????????????????100?????0010011")
	def ORI 	= BitPat("b?????????????????110?????0010011")
	def ANDI 	= BitPat("b?????????????????111?????0010011")
	def SLLI 	= BitPat("b0000000??????????001?????0010011")
	def SRLI 	= BitPat("b0000000??????????101?????0010011")
	def SRAI 	= BitPat("b0100000??????????101?????0010011")
	def ADD 	= BitPat("b0000000??????????000?????0110011")
	def SUB 	= BitPat("b0100000??????????000?????0110011")
	def SLL 	= BitPat("b0000000??????????001?????0110011")
	def SLT 	= BitPat("b0000000??????????010?????0110011")
	def SLTU	= BitPat("b0000000??????????011?????0110011")
	def XOR 	= BitPat("b0000000??????????100?????0110011")
	def SRL 	= BitPat("b0000000??????????101?????0110011")
	def SRA 	= BitPat("b0100000??????????101?????0110011")
	def OR  	= BitPat("b0000000??????????110?????0110011")
	def AND 	= BitPat("b0000000??????????111?????0110011")
	def ECALL 	= BitPat("b00000000000000000000000001110011")
	def EBREAK 	= BitPat("b00000000000100000000000001110011")

	// Zicsr extension
	def CSRRW 	= BitPat("b?????????????????001?????1110011")
	def CSRRS 	= BitPat("b?????????????????010?????1110011")
	def CSRRC 	= BitPat("b?????????????????011?????1110011")
	def CSRRWI 	= BitPat("b?????????????????101?????1110011")
	def CSRRSI 	= BitPat("b?????????????????110?????1110011")
	def CSRRCI 	= BitPat("b?????????????????111?????1110011")

	// Zifencei extension
	def FENCEI 	= BitPat("b?????????????????001?????0001111")

	def MRET 	= BitPat("b00110000001000000000000001110011")

	// for R_shifter instructions, src2 type is imm
	val table: Array[(BitPat,List[UInt])] = Array(
		LUI -> DecodeBase(SrcFrom.Imm, SrcFrom.PC, ExType.Lui, ImmType.UType, AluType.add, LSLen.word, wenR = true.B).generate,
		AUIPC -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Auipc, ImmType.UType, AluType.add, LSLen.word, wenR = true.B).generate,
		JAL -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Branch, ImmType.JType, BrType.jal, LSLen.word, wenR = true.B).generate,
		JALR -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Branch, ImmType.IType, BrType.jalr, LSLen.word, wenR = true.B).generate,
		BEQ -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Branch, ImmType.BType, BrType.beq, LSLen.word).generate,
		BNE -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Branch, ImmType.BType, BrType.bne, LSLen.word).generate,
		BLT -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Branch, ImmType.BType, BrType.blt, LSLen.word).generate,
		BGE -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Branch, ImmType.BType, BrType.bge, LSLen.word).generate,
		BLTU -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Branch, ImmType.BType, BrType.bltu, LSLen.word).generate,
		BGEU -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Branch, ImmType.BType, BrType.bgeu, LSLen.word).generate,
		LB -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Load, ImmType.IType, AluType.add, LSLen.byte, wenR = true.B, loadSignExt = true.B).generate,
		LH -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Load, ImmType.IType, AluType.add, LSLen.half, wenR = true.B, loadSignExt = true.B).generate,
		LW -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Load, ImmType.IType, AluType.add, LSLen.word, wenR = true.B, loadSignExt = true.B).generate,
		LBU -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Load, ImmType.IType, AluType.add, LSLen.byte, wenR = true.B).generate,
		LHU -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Load, ImmType.IType, AluType.add, LSLen.half, wenR = true.B).generate,
		SB -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Store, ImmType.SType, AluType.add, LSLen.byte, wenM = true.B).generate,
		SH -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Store, ImmType.SType, AluType.add, LSLen.half, wenM = true.B, loadSignExt = true.B).generate,
		SW -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.Store, ImmType.SType, AluType.add, LSLen.word, wenM = true.B).generate,
		ADDI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.add, LSLen.word, wenR = true.B).generate,
		SLTI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.slt, LSLen.word, wenR = true.B).generate,
		SLTIU -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.sltu, LSLen.word, wenR = true.B).generate,
		XORI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.xor, LSLen.word, wenR = true.B).generate,
		ORI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.or, LSLen.word, wenR = true.B).generate,
		ANDI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.and, LSLen.word, wenR = true.B).generate,
		SLLI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.sll, LSLen.word, wenR = true.B).generate,
		SRLI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.srl, LSLen.word, wenR = true.B).generate,
		SRAI -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.AluI, ImmType.IType, AluType.sra, LSLen.word, wenR = true.B).generate,
		ADD -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.add, LSLen.word, wenR = true.B).generate,
		SUB -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.sub, LSLen.word, wenR = true.B).generate,
		SLL -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.sll, LSLen.word, wenR = true.B).generate,
		SLT -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.slt, LSLen.word, wenR = true.B).generate,
		SLTU -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.sltu, LSLen.word, wenR = true.B).generate,
		XOR -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.xor, LSLen.word, wenR = true.B).generate,
		SRL -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.srl, LSLen.word, wenR = true.B).generate,
		SRA -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.sra, LSLen.word, wenR = true.B).generate,
		OR -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.or, LSLen.word, wenR = true.B).generate,
		AND -> DecodeBase(SrcFrom.RS1, SrcFrom.RS2, ExType.AluR, ImmType.NType, AluType.and, LSLen.word, wenR = true.B).generate,
		ECALL -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Ecall, ImmType.NType, AluType.add, LSLen.word).generate,
		EBREAK -> DecodeBase(SrcFrom.PC, SrcFrom.Imm, ExType.Ebreak, ImmType.NType, AluType.add, LSLen.word).generate,
		CSRRW -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.CSR, ImmType.IType, CSRType.csrrw, LSLen.word, wenR = true.B).generate,
		CSRRS -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.CSR, ImmType.IType, CSRType.csrrs, LSLen.word, wenR = true.B).generate,
		CSRRC -> DecodeBase(SrcFrom.RS1, SrcFrom.Imm, ExType.CSR, ImmType.IType, CSRType.csrrc, LSLen.word, wenR = true.B).generate,
		CSRRWI -> DecodeBase(SrcFrom.Imm, SrcFrom.Imm, ExType.CSR, ImmType.IType, CSRType.csrrwi, LSLen.word, wenR = true.B).generate,
		CSRRSI -> DecodeBase(SrcFrom.Imm, SrcFrom.Imm, ExType.CSR, ImmType.IType, CSRType.csrrsi, LSLen.word, wenR = true.B).generate,
		CSRRCI -> DecodeBase(SrcFrom.Imm, SrcFrom.Imm, ExType.CSR, ImmType.IType, CSRType.csrrci, LSLen.word, wenR = true.B).generate,
		MRET -> DecodeBase(SrcFrom.Imm, SrcFrom.Imm, ExType.Mret, ImmType.NType, AluType.add, LSLen.word).generate,
		FENCEI -> DecodeBase(SrcFrom.Imm, SrcFrom.Imm, ExType.FENCEI, ImmType.NType, AluType.add, LSLen.word).generate
	)
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
	def NType = "b101".U // not a type

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
	def Ebreak = "b1001".U
	def Ecall = "b1010".U

	def CSR = "b1011".U
	def Mret = "b1100".U

	def FENCEI = "b1101".U

	def apply() = UInt(5.W)
}

object FuType {
	def apply() = UInt(8.W)
	def X = "b00000000".U
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
}

object BrType {
	def beq = "b1010".U
	def bne = "b1011".U
	def blt = "b1100".U
	def bge = "b1101".U
	def bltu = "b1110".U
	def bgeu = "b1111".U
	def jalr = "b10000".U
	def jal = "b10001".U
}

object CSRType {
	def csrrw = "b10010".U
	def csrrs = "b10011".U
	def csrrc = "b10100".U
	def csrrwi = "b10101".U
	def csrrsi = "b10110".U
	def csrrci = "b10111".U
}

object LSLen {
	def byte = "b00".U
	def half = "b01".U
	def word = "b10".U

	def apply() = UInt(2.W)
}

class DecodedInst extends Bundle {
	val inst = new StaticInst
	val pc = UInt(32.W) // pc of the instruction

	val src1From = SrcFrom() // rs1 source
	val src2From = SrcFrom() // rs2 source
	val exType = ExType()
	val immType = ImmType()
	val fuType = FuType()
	val lsLength = LSLen() // length of load/store data
	val wenR = Bool() // write reg enable
	val wenM = Bool() // write memory enable
	val loadSignExt = Bool() // sign extend for load

	val isEbreak = Bool() // is ebreak instruction

	def default: List[UInt] = 
		List(SrcFrom.RS1,SrcFrom.RS2,ExType.AluR,ImmType.NType,AluType.add,LSLen.word,false.B,false.B,false.B)

	def signals = Seq(
		src1From,
		src2From,
		exType,
		immType,
		fuType,
		lsLength,
		wenR,
		wenM,
		loadSignExt
	)

	def decode(table: Array[(BitPat, List[UInt])]) = {
		val decoded: Seq[UInt] = ListLookup(inst.code, default, table)
		signals zip decoded foreach { case (signal, value) => signal := value}
	}
}

class IDOut extends Bundle {
	val decoded = new DecodedInst
	val rdata1 = UInt(32.W) // read data from regfile rs1
	val rdata2 = UInt(32.W) // read data from regfile rs2
}

class DecoderIO extends Bundle {
	val in = Flipped(Decoupled(new IFUOut))
	val dataHazard = Input(Bool())
	val writeback = Flipped(Decoupled(new W2DOut))
	val out = Decoupled(new IDOut)
}

class Decoder extends Module{
	val io = IO(new DecoderIO)
	val regfile = Module(new Regfile())

	val wbIn = io.writeback.bits
	val idOut = io.out.bits

	idOut.decoded.inst := io.in.bits.inst
	idOut.decoded.decode(RV32IDecode.table)

	idOut.decoded.isEbreak := io.in.bits.inst.code === RV32IDecode.EBREAK
	idOut.decoded.pc := io.in.bits.pc

	/**
	  * Regfile
	  */
	regfile.io.wen := wbIn.info.wenR && io.writeback.fire
	regfile.io.waddr := wbIn.info.inst.rd
	regfile.io.wdata := wbIn.regWdata
	regfile.io.raddr1 := idOut.decoded.inst.rs1
	regfile.io.raddr2 := idOut.decoded.inst.rs2

	idOut.rdata1 := regfile.io.rdata1
	idOut.rdata2 := regfile.io.rdata2

	// for multi-cycle cpu
	io.in.ready := io.out.fire || !io.in.valid
	
	io.out.valid := io.in.valid && io.dataHazard === false.B

	io.writeback.ready := true.B
}
