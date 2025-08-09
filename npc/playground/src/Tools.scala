package cpu

import chisel3._
import chisel3.util._

import cpu.decode._

object StageConnectSingle {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T]) = {
		next <> curr
	}
}

object StageConnectMulti {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T]) = {
		next.valid := curr.valid
		curr.ready := next.ready
		next.bits := RegEnable(curr.bits, 0.U.asTypeOf(curr.bits), curr.fire)
	}
}

object StageConnectPipe {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T], flush: Bool) = {
		val pipeReg = RegInit(0.U.asTypeOf(curr.bits))
		val pipeOutValid = RegInit(false.B)

		when(curr.fire) {
			pipeReg := curr.bits
			pipeOutValid := true.B
		}.elsewhen(next.ready || flush) {
			pipeOutValid := false.B
		}
		curr.ready := next.ready
		next.valid := pipeOutValid
		next.bits := pipeReg
	}
}

class StateMachine(direct: String) extends Module {
	val io = IO(new Bundle {
		val valid = Input(Bool())
		val ready = Input(Bool())
		val state = Output(UInt(2.W))
	})
	val s_idle :: s_waitvalid :: s_waitready :: Nil = Enum(3)
	val state = RegInit(s_idle)
	if(direct == "master"){
		val state = RegInit(s_idle)
		state := MuxLookup(state, s_idle)(Seq(
			s_idle -> Mux(io.ready && !io.valid, s_waitvalid, s_idle),
			s_waitvalid -> Mux(io.valid, s_idle, s_waitvalid)
		))
	} else if(direct == "slave") {
		state := MuxLookup(state, s_idle)(Seq(
			s_idle -> Mux(io.valid && !io.ready, s_waitready, s_idle),
			s_waitready -> Mux(io.ready, s_idle, s_waitready)
		))
	} else {
		assert(false.B, "Error: StateMachine direct is invalid")
	}
	io.state := state

	def getstate = io.state
	def isState(s: UInt) = io.state === s
}

trait StageConflict {
	def conflict(rs: UInt, rd: UInt): Bool = {
		(rd =/= 0.U) && (rs === rd)
	}
	def conflictWithStage(rs1: UInt, rs2: UInt, rd: UInt, wen: Bool): Bool = {
		(conflict(rs1, rd) || conflict(rs2, rd)) && wen
	}
	def isRAW(rs1: UInt, rs2: UInt,
		  ex_rd: UInt, ex_wen: Bool, ex_valid: Bool,
		  mem_rd: UInt, mem_wen: Bool, mem_valid: Bool,
		  wb_rd: UInt, wb_wen: Bool, wb_valid: Bool): Bool = {
		((conflictWithStage(rs1, rs2, ex_rd, ex_wen) && ex_valid) ||
		(conflictWithStage(rs1, rs2, mem_rd, mem_wen) && mem_valid) ||
		(conflictWithStage(rs1, rs2, wb_rd, wb_wen) && wb_valid))
	}

	def isBrHazard(id_type: UInt, id_valid: Bool,
		       ex_type: UInt, ex_valid: Bool,
		       mem_type: UInt, mem_valid: Bool): Bool = {
		(((id_type === ExType.Branch || id_type === ExType.Ecall || id_type === ExType.Ebreak) && id_valid) ||
		((ex_type === ExType.Branch || ex_type === ExType.Ecall || ex_type === ExType.Ebreak) && ex_valid) ||
		((mem_type === ExType.Branch || mem_type === ExType.Ecall || mem_type === ExType.Ebreak) && mem_valid))
	}
}
