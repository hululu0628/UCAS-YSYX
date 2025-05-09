#include <common.h>
#include "mem.h"
#include <fstream>
#include <getopt.h>

NPC_state npc_state = { .state = NPC_STOP};

void sdb_set_batch_mode();

static char *log_file = nullptr;
static char *img_file = nullptr;
static char *diff_file = nullptr;


void init_mem();
void init_log(const char *log_file);
void init_monitor();
void init_difftest(const char *ref_so_file, long img_size, int port);

static int parse_args(int argc, char **argv)
{
	assert(argc > 0);
	const struct option table[] = {
	{"batch"    , no_argument      , nullptr, 'b'},
	{"log"      , required_argument, nullptr, 'l'},
	{"diff"     , required_argument, nullptr, 'd' },
	{"help"     , no_argument      , nullptr, 'h'},
	{0          , 0                , nullptr,  0 },
	};
	int o;
  	while ( (o = getopt_long(argc, argv, "-bhl:d:", table, nullptr)) != -1) 
	{
    		switch (o) 
		{
			case 'b': sdb_set_batch_mode(); break;
      			case 'l': log_file = optarg; break;
			case 'd': diff_file = optarg; break;
      			case 1: img_file = optarg; return 0;
      			default:
				std::cout << "Usage: " << argv[0] << " [OPTION...] IMAGE [args]" << std::endl;
				std::cout << "Options:" << std::endl;
				std::cout << "\t-b, --batch		run in batch mode" << std::endl;
				std::cout << "\t-l, --log=FILE		output log to FILE" << std::endl;
				std::cout << "\t-d, --diff=FILE		difftest-so with FILE" << std::endl;
				std::cout << "\t-h, --help		show this help message" << std::endl;
        			exit(0);
    		}
  	}
  	return 0;
}

word_t load_image(const char *img_file)
{
	if(img_file == nullptr)
	{
		std::cerr << "Error: No image file specified" << std::endl;
		exit(1);
	}
	std::ifstream input;
	input.open(img_file, std::ios::in);
	if(!input.is_open())
	{
		std::cerr << "Error: Cannot open image file at " << img_file << std::endl;
		exit(1);
	}
	input.seekg(0, input.end);
	word_t length = input.tellg();
	input.seekg(0, input.beg);
	std::cout << "Loading image file: " << img_file << std::endl;
	std::cout << "Image file size: 0x" << std::hex << length << std::endl;
	assert(length < PMEM_SIZE);
	input.read((char *)guest_to_host(PMEM_START), length);
	input.close();

	return length;
}

void init_sim(int argc, char **argv)
{
	parse_args(argc, argv);
	init_log(log_file);
	init_mem();
	word_t img_size = load_image(img_file);
	init_monitor();
	init_difftest(diff_file, img_size, 1234);
}
