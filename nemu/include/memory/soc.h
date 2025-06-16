#ifndef __SOC_H__
#define __SOC_H__
#include <common.h>

#define MAX_SOC_DEVICES 16

typedef uint8_t privlege_t;
#define PRIV_R 0x1 // read
#define PRIV_W 0x2 // write
#define PRIV_X 0x4 // execute

extern int soc_device_num;

typedef struct soc_device_t {
	char name[16];
	paddr_t base;  // base address of the device
	word_t size; // size of the device
	uint8_t * mem;
	privlege_t priv;
} soc_device_t;

extern soc_device_t soc_devices[MAX_SOC_DEVICES]; // 16 devices max

extern int get_soc_index(paddr_t addr);

#endif
