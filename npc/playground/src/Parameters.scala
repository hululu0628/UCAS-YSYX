package cpu

import chisel3._
import chisel3.util._

case class DevicePara(val base: Long, val size: Long)

// need a generater to generate this file
object NPCParameters {
	val sramStart = 0x80000000L
	val sramSize = 0x8000000L
	val mmioStart = 0xa0000000L
	val mmioSize = 0x2000000L
	val mmioNum = 1
	val deviceTab = Map(
		"mrom" -> DevicePara(0x20000000L, 0x1000L),
		"uart" -> DevicePara(0x10000000L,0x1000L),
		"clint" -> DevicePara(0xa0000048L, 0x8L)
	)
}
