#include <assert.h>
#include <stdio.h>

// 简单的把ASCII艺术画转换成C语言数组的工具
// 现在可以为nemu添加各种图画了!
int main(int argc, char *argv[])
{
	assert(argc == 2);

	FILE *fp = fopen(argv[1], "r");
	assert(fp != NULL);

	char c;
	int i = 0;
	while((c = fgetc(fp)) != EOF)
	{
		printf("0x%x,", c);
		i++;
		if (i % 10 == 0)
			printf("\n");
	}
	printf("\'\\0\'\n");
	fclose(fp);

	return 0;
}