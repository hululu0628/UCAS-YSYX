package cpu

import chisel3._
import chisel3.util._

class PerfCntBaseIO extends Bundle {
	val enable = Input(Bool())
	val disable = Input(Bool())
	val cond = Input(Bool())
}

class PerfCntPrint(name: String, width: Int) extends BlackBox with HasBlackBoxInline {
	val io = IO(new Bundle{
		val cnt = Input(UInt(width.W))
	})
	override val desiredName = s"PerfCntPrint_$name"
	if(NPCParameters.perfEnable) {
	setInline(
		s"PerfCntPrint_$name.sv",
		s"""
		|module PerfCntPrint_$name (
		|	input [$width - 1:0] cnt
		|);
		|	final begin
		|		$$display("\\033[34m[Performance counter %-15s]: %d\\033[0m", "$name", cnt);
		|	end
		|endmodule
		""".stripMargin
	)
	} else {
		setInline(
			s"PerfCntPrint_$name.sv",
			s"""
			|module PerfCntPrint_$name (
			|	input [$width - 1:0] cnt
			|);
			|endmodule
			""".stripMargin
		)
	}
}

class PerfCnt(name: String, width: Int) extends Module {
	val io = IO(new PerfCntBaseIO)
	override val desiredName = s"PerfCnt_$name"
	if(NPCParameters.perfEnable) {
		val s_idle :: s_counting :: Nil = Enum(2)
		val state = RegInit(s_idle)
		when(io.enable) {
			state := s_counting
		} .elsewhen(io.disable) {
			state := s_idle
		}

		val cnt = RegInit(0.U(width.W))
		when(io.cond && state === s_counting) {
			cnt := cnt + 1.U
		}
		val print = Module(new PerfCntPrint(name, width))
		print.io.cnt := cnt
	} else {
		val print = Module(new PerfCntPrint(name, width))
		print.io.cnt := 0.U
	}
}

object PerfCnt {
	def apply(name: String, cond: Bool, width: Int, enable: Bool = true.B, disable: Bool = false.B): PerfCnt = {
		val cnt = Module(new PerfCnt(name, width))
		cnt.io.enable := enable
		cnt.io.disable := disable
		cnt.io.cond := cond
		cnt
	}
}
