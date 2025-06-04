package cpu

import chisel3._
import chisel3.util._

// need a generater to generate this file
object Parameters {
	val sramStart = 0x80000000
	val sramSize = 0x8000000
	val mmioStart = 0xa0000000
	val mmioSize = 0x2000000
}
