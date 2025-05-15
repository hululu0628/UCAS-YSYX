package cpu

import chisel3._
import chisel3.util._

object StageConnect {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T]) = {
		val arch = "single"
		if(arch == "single") { next <> curr }
		else if(arch == "multi") { next <> RegEnable(curr, curr.fire) }
	}
}

object StageConnectMulti {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T]) = {
		val arch = "multi"
		if(arch == "single") { next <> curr }
		else if(arch == "multi") {
			next.valid := curr.valid
			curr.ready := next.ready
			next.bits := RegEnable(curr.bits, curr.fire)
		}
	}
}
