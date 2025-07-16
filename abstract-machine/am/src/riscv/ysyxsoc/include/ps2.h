#ifndef __PS2_H__
#define __PS2_H__

#include "amdev.h"
#include <am.h>

#define PS2_BASE	0x10011000L

typedef struct {
	uint32_t amcode;
	uint32_t ps2code;
} ps2_map_t;

extern ps2_map_t ps2lut[];

#endif
