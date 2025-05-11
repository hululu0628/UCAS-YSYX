#***************************************************************************************
# Copyright (c) 2014-2024 Zihao Yu, Nanjing University
#
# NEMU is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
#          http://license.coscl.org.cn/MulanPSL2
#
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
#
# See the Mulan PSL v2 for more details.
#**************************************************************************************/

DIRS-y += $(CSRC)/src/device/io

DEVICE-$(CONFIG_HAS_TIMER) += $(CSRC)/src/device/timer.c
DEVICE-$(CONFIG_HAS_SERIAL) += $(CSRC)/src/device/serial.c
DEVICE-$(CONFIG_HAS_KEYBOARD) += $(CSRC)/src/device/keyboard.c
DEVICE-$(CONFIG_HAS_VGA) += $(CSRC)/src/device/vga.c
DEVICE-$(CONFIG_HAS_AUDIO) += $(CSRC)/src/device/audio.c
DEVICE-$(CONFIG_HAS_DISK) += $(CSRC)/src/device/disk.c
DEVICE-$(CONFIG_HAS_SDCARD) += $(CSRC)/src/device/sdcard.c

CSRCS-$(CONFIG_DEVICE) += $(DEVICE-y) $(CSRC)/src/device/device.c $(CSRC)/src/device/alarm.c $(CSRC)/src/device/intr.c

ifdef CONFIG_DEVICE
LIBS += $(shell sdl2-config --libs)
endif
