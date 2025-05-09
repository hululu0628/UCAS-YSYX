#include "common.h"
#include "debug.h"

std::ofstream log_stream;

void init_log(const char *log_file)
{
	if(log_file)
	{
		log_stream.open(log_file, std::ios::out | std::ios::trunc);
		if(!log_stream.is_open())
		{
			std::cerr << "Error: Cannot open log file at " << log_file << std::endl;
			exit(1);
		}
		std::cout << "Log file opened at " << log_file << std::endl;
		log_stream << "Log File Initialized" << std::endl;
	} else
		std::cout << "Running without log file" << std::endl;
}
