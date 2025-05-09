#pragma once
#include <common.h>
#include <fstream>

extern bool log_enable;
extern std::ofstream log_stream;
#define log_write(x) do { \
	extern std::ofstream log_stream; \
	extern bool log_enable; \
	log_stream << x << std::endl; \
} while(0)

#define _Log(x) do { \
	log_write(x); \
	stdout_write(x); \
} while(0)

#define Log(x) do {_Log("[" << __FILE__ << ":" << __LINE__ << " " << __func__ << "] " << x);} while(0)
