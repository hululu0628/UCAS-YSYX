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
		"sram" -> DevicePara(0x0f000000L, 0x2000L),
		"flash" -> DevicePara(0x30000000L, 0x10000000L),
		"sdram" -> DevicePara(0xa0000000L, 0x8000000L),
		"clint" -> DevicePara(0x02000000L, 0x10000L)
	)
	val debugEnable = true
	val perfEnable = true
	val simHalt = true

	val cache = new {
		val enableICache = true
		val addrWidth = 32
		val taglen = 26
		val indexlen = 4
		val offsetlen = 2
		val blockSize = 8 << offsetlen
		val blockNum = 1 << indexlen
	}
}
