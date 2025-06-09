package cpu.mem

import chisel3._
import chisel3.util._
import chisel3.util.random._
import cpu._

abstract class DeviceBase extends AXI4LiteSlaveBase{
	val device: String = "device_base"
}

// mmio arbiter
class MMIO extends Module {
	val io = IO(new Bundle{
		val arbiterIn = new AXI4IO()
	})
	io.arbiterIn.setSlaveDefault()

	val writeEnable = io.arbiterIn.awvalid
	val readEnable = io.arbiterIn.arvalid
	val mmioEnable = writeEnable || readEnable
	val addr = Mux(writeEnable, io.arbiterIn.awaddr, io.arbiterIn.araddr)

	val uart = Module(new UARTImp())
	val clint = Module(new CLINTImp())
	val devicelist = List(uart, clint)
	val deviceVec = VecInit(devicelist.map { d =>
		val dt = NPCParameters.deviceTab(d.device)
		addr >= (dt.base.U) && addr < (dt.base.U + dt.size.U) && mmioEnable
	})
	val deviceVecReg = RegEnable[Vec[Bool]](deviceVec, mmioEnable)
	val deviceFinalVec = Mux(deviceVec.reduce(_ || _), deviceVec, deviceVecReg)


	devicelist.zip(deviceFinalVec).foreach { case(d, b) =>
		when(b) {
			d.io <> io.arbiterIn
		} .otherwise {
			d.io.setMasterDefault()
		}
	}
}
