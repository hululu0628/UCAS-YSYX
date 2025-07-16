#include <am.h>
#include "ysyxsoc.h"

void __am_gpu_config(AM_GPU_CONFIG_T *cfg)
{
	cfg->present = true;
	cfg->has_accel = false;
	cfg->width = 640;
	cfg->height = 480;
	cfg->vmemsz = cfg->width * cfg->height * sizeof(uint32_t);
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *fb)
{
	uint32_t * pixels = (uint32_t *)fb->pixels;
	uint32_t pixel;
	int x = fb->x, y = fb->y;
	int w = fb->w, h = fb->h;
	int width = 640; // line width

	for(int i = 0; i < h; i++)
	{
		for(int j = 0; j < w; j++)
		{
			pixel = pixels[w * i + j];
			outl(VGA_BASE + (y + i) * width * sizeof(uint32_t) + (x + j) * sizeof(uint32_t), pixel);
		}
	}
}
