package cpu

import chisel3._
import chisel3.util._

object StageConnectSingle {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T]) = {
		next <> curr
	}
}

object StageConnectMulti {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T]) = {
		next.valid := curr.valid
		curr.ready := next.ready
		next.bits := RegEnable(curr.bits, curr.fire)
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
