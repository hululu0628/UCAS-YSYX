#include <cpu/decode.h>
#ifdef CONFIG_CACHESIM_TRACE
FILE * cache_fp = NULL;
void init_cachetrace(const char * file)
{
	Log("Initialize cache trace file: %s", file);
	FILE * fp = fopen(file, "wb");
	Assert(fp != NULL, "Can not open \"%s\"", file);
	Log("Open cache trace file \"%s\" successfully", file);
	cache_fp = fp;
}
void trace_pc(Decode * s)
{
	if(cache_fp == NULL)
		return;
	fwrite(&s->pc, sizeof(s->pc), 1, cache_fp);
}
#endif
