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
		val arbiterIn = new AXI4LiteIO()
	})
	val uart = Module(new UARTImp())
	val devicelist = List(uart)
	val deviceVec = devicelist.map { d =>
		NPCParameters.deviceTab(d.device)
	}
}
