package cpu

import chisel3._
import chisel3.util._

object StageConnect {
	def apply[T <: Data](curr: DecoupledIO[T], next: DecoupledIO[T]) = {
		val arch = "single"
		if(arch == "single") {
			curr.ready := next.ready
			next.valid := curr.valid
			next.bits := curr.bits 
		}
	}
}
