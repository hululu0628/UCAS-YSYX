#include <am.h>
#include <sys/cdefs.h>
#include "ysyxsoc.h"

#define KEYDOWN_MASK 0x8000

ps2_map_t ps2lut[] = {
	{AM_KEY_ESCAPE, 0x76},
	{AM_KEY_F1, 0x05},
	{AM_KEY_F2, 0x06},
	{AM_KEY_F3, 0x04},
	{AM_KEY_F4, 0x0c},
	{AM_KEY_F5, 0x03},
	{AM_KEY_F6, 0x0b},
	{AM_KEY_F7, 0x83},
	{AM_KEY_F8, 0x0a},
	{AM_KEY_F9, 0x01},
	{AM_KEY_F10, 0x09},
	{AM_KEY_F11, 0x78},
	{AM_KEY_F12, 0x07},
	{AM_KEY_GRAVE, 0x0E},
	{AM_KEY_1, 0x16},
	{AM_KEY_2, 0x1E},
	{AM_KEY_3, 0x26},
	{AM_KEY_4, 0x25},
	{AM_KEY_5, 0x2E},
	{AM_KEY_6, 0x36},
	{AM_KEY_7, 0x3D},
	{AM_KEY_8, 0x3E},
	{AM_KEY_9, 0x46},
	{AM_KEY_0, 0x45},
	{AM_KEY_MINUS, 0x4E},
	{AM_KEY_EQUALS, 0x55},
	{AM_KEY_BACKSPACE, 0x66},
	{AM_KEY_TAB, 0x0D},
	{AM_KEY_Q, 0x15},
	{AM_KEY_W, 0x1D},
	{AM_KEY_E, 0x24},
	{AM_KEY_R, 0x2D},
	{AM_KEY_T, 0x2C},
	{AM_KEY_Y, 0x35},
	{AM_KEY_U, 0x3C},
	{AM_KEY_I, 0x43},
	{AM_KEY_O, 0x44},
	{AM_KEY_P, 0x4D},
	{AM_KEY_LEFTBRACKET, 0x54},
	{AM_KEY_RIGHTBRACKET, 0x5B},
	{AM_KEY_BACKSLASH, 0x5D},
	{AM_KEY_CAPSLOCK, 0x58},
	{AM_KEY_A, 0x1C},
	{AM_KEY_S, 0x1B},
	{AM_KEY_D, 0x23},
	{AM_KEY_F, 0x2B},
	{AM_KEY_G, 0x34},
	{AM_KEY_H, 0x33},
	{AM_KEY_J, 0x3B},
	{AM_KEY_K, 0x42},
	{AM_KEY_L, 0x4B},
	{AM_KEY_SEMICOLON, 0x4C},
	{AM_KEY_APOSTROPHE, 0x52},
	{AM_KEY_RETURN, 0x5A},
	{AM_KEY_LSHIFT, 0x12},
	{AM_KEY_Z, 0x1A},
	{AM_KEY_X, 0x22},
	{AM_KEY_C, 0x21},
	{AM_KEY_V, 0x2A},
	{AM_KEY_B, 0x32},
	{AM_KEY_N, 0x31},
	{AM_KEY_M, 0x3A},
	{AM_KEY_COMMA, 0x41},
	{AM_KEY_PERIOD, 0x49},
	{AM_KEY_SLASH, 0x4A},
	{AM_KEY_RSHIFT, 0x59},
	{AM_KEY_LCTRL, 0x14},
	{AM_KEY_LALT, 0x11},
	{AM_KEY_SPACE, 0x29},
	{AM_KEY_RALT, 0xE011},
	{AM_KEY_RCTRL, 0xE014},
	{AM_KEY_UP, 0xE075},
	{AM_KEY_DOWN, 0xE072},
	{AM_KEY_LEFT, 0xE06B},
	{AM_KEY_RIGHT, 0xE074},
	{AM_KEY_INSERT, 0xE070},
	{AM_KEY_DELETE, 0xE071},
	{AM_KEY_HOME, 0xE06C},
	{AM_KEY_END, 0xE069},
	{AM_KEY_PAGEUP, 0xE07D},
	{AM_KEY_PAGEDOWN, 0xE07A},
};

uint32_t lookup_ps2_key(uint32_t key)
{
	uint32_t res = 0;
	for(int i = 0; i < AM_KEY_PAGEDOWN - 1; i++)
	{
		if(ps2lut[i].ps2code == key)
		{
			res = ps2lut[i].amcode;
			break;
		}
	}
	return res;
}

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
	uint32_t key = inb(PS2_BASE + 0x0);

	if(key == 0xF0)
		kbd->keydown = false;
	else
		kbd->keydown = true;
	key = inb(PS2_BASE + 0x0);
	if(key == 0xE0)
		key = (key << 8) | inb(PS2_BASE + 0x0);
	kbd->keycode = lookup_ps2_key(key);
}
