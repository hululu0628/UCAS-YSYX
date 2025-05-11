#pragma once
#include <common.h>
#include <fstream>

extern std::ofstream log_stream;

extern bool log_enable();

#define log_write(x) do { \
	if(log_enable()) \
		log_stream << x << std::endl; \
} while(0)

#define _Log(x, color) do { \
	log_write(x); \
	stdout_write(ANSI_FG_##color << x << ANSI_NONE); \
} while(0)

#define Log(x) do {_Log("[" << __FILE__ << ":" << __LINE__ << " " << __func__ << "] " << x, BLUE);} while(0)
#define Log_Warn(x) do {_Log("[" << __FILE__ << ":" << __LINE__ << " " << __func__ << "] " << x, YELLOW);} while(0)
#define Log_Error(x) do {_Log("[" << __FILE__ << ":" << __LINE__ << " " << __func__ << "] " << x, RED);} while(0)

#define Assert(cond, x) do { \
	if(!(cond)) { \
		Log_Error(x); \
		assert(0); \
	} \
} while(0)
