#include "common.h"
#include "debug.h"
#include "utils.h"

std::ofstream log_stream;

bool log_enable()
{
	return MUXDEF(CONFIG_TRACE, (excuted_inst_num >= CONFIG_TRACE_START) &&
         (excuted_inst_num <= CONFIG_TRACE_END), false);
}

void init_log(const char *log_file)
{
	if(log_file)
	{
		log_stream.open(log_file, std::ios::out | std::ios::trunc);
		if(!log_stream.is_open())
		{
			stdout_write(ANSI_FG_RED << "Error: Cannot open log file at " << log_file << ANSI_NONE);
			exit(1);
		}
		Log("Log file opened at " << log_file);
	} else
		stdout_write("Running without log file");
}
