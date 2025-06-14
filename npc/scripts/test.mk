NPC_TEST = $(NPC_HOME)/test
test-build:
	make -C $(NPC_TEST) $(filter-out test-build, $(MAKECMDGOALS))
