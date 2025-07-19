# NPC

### chisel环境的配置
如果是vscode，建议用metals，对于ysyx这样的小型项目还是友好的，需要注意的有：
- build.mill不被metals识别，使用metals建议改成build.sc
- metals版本更新对旧版本的Scala不兼容

如果是IDEA，可以用24年的版本，25年的版本貌似对mill构建脚本不是很能识别

### 结构说明
- `vsrc`：Verilog源代码，自动生成
- `csrc`: 仿真代码，基本功能和nemu一样（copy来的）
- `playground`: 处理器核的chisel代码
- `test`：一些测试程序，主要是测试SoC用的
- `config`: menuconfig配置文件
- `constr`：nvboard引脚约束

### 编译及运行
使用nvboard时请指定`PLATFORM=nvboard`, 编译时使用`make nvbuild`，且一定要在menuconfig中配置启用nvboard，不使用的时候也要取消勾选。

编译测试程序使用`make test-build`，和其他测试程序集一样，也可以用`ALL=xxx`指定要编译的测试程序。

`make verilog`生成verilog文件，`make build`生成verilator仿真程序，`make run`运行仿真程序，使用流程和可配置参数与NEMU没差，所以自行查阅各种Makefile。
