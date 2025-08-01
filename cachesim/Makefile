BUILD_DIR = $(YSYX_HOME)/cachesim/build
SRC_DIR += $(YSYX_HOME)/cachesim/src
INC_DIR += $(YSYX_HOME)/cachesim/include $(YSYX_HOME)/cachesim/config/include/generated
INC_PATHS = $(addprefix -I, $(INC_DIR))

CC = gcc
CFLAGS = -Wall -O2

SRCS = $(wildcard $(SRC_DIR)/*.c)

ARGS ?= 

menuconfig:
	make -C config menuconfig

build:
	@mkdir -p $(BUILD_DIR)
	@$(CC) $(INC_PATHS) $(SRCS) -o $(BUILD_DIR)/cachesim $(CFLAGS)

run: build
	$(BUILD_DIR)/cachesim $(ARGS)
