package ysyx

import chisel3._
import chisel3.util._

class bitrev extends BlackBox {
  val io = IO(Flipped(new SPIIO(1)))
}

// Tx_NEG: set 0, Rx_NEG: set 1
// LSB: set 1 (sent the lower bit of TX first)
/**
  * It's a piece of **
  * notice that the cnt register is not init actually, it has init value 0 only in simulation
  * the reason is ss cannot is set before the first posedge of sck
  * when rising edge of sck comes, ss is already set 0
  * which makes cnt not initialized
  */
class bitrevChisel extends RawModule { // we do not need clock and reset
	val io = IO(Flipped(new SPIIO(1)))
	val cntOut = Wire(UInt(5.W))
	io.miso := true.B
	withClockAndReset(io.sck.asClock, io.ss.asBool) {
		val cnt = RegInit(0.U(5.W))
		when(!io.ss) {
			when(cntOut === 16.U) {
				cnt := 1.U
			}
			.otherwise {
				cnt := cnt + 1.U
			}
		}
		cntOut := cnt
	}
	withClock((!io.sck).asClock) {
		val data = Reg(UInt(8.W))
		when(!io.ss) {
			when(cntOut < 9.U) {
				data := Cat(io.mosi, data(7, 1))
			} otherwise {
				io.miso := data(7)
				data := Cat(data(6, 0), data(7))
			}
		}
	}
}
