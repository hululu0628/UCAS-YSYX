#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <stdint.h>
#include "../../src/monitor/sdb/sdb.h"

static char another_buf[64];

char expr_test_buf[65535];
void expr_test_main(int start_example, int n)
{
	//assert(argc == 2);

	int i = 0;
	FILE *fp = fopen("./tools/gen-expr/output.txt", "r");
	assert(fp != NULL);

	FILE *fp2 = fopen("./tools/gen-expr/result.txt", "w");
	assert(fp2 != NULL);


	bool success;
	uint32_t num, result;
	for(i = 0; i < start_example - 1 + n; i++)
	{
		fscanf(fp, "%s ",another_buf);
		num = atoi(another_buf);
		fgets(expr_test_buf, 65535, fp);
		if(i < start_example - 1)
			continue;
		expr_test_buf[strlen(expr_test_buf) - 1] = '\0';
		success = true;
		printf("%c\n",expr_test_buf[0]);
		result = expr(expr_test_buf, &success);
		printf("%u, %u\n",num, result);
		if(num != result)
		{
			printf("Test failed at example %d!\n", i + 1);
			fprintf(fp2, "gen: %u nemu: %u; Test failed at example %d!\n", 
					num, result, i + 1);
		}
		else
			printf("Test passed at example %d!\n", i + 1);	
	}
	fclose(fp);
	fclose(fp2);

	printf("Test finished!\n");
	while(1);
}