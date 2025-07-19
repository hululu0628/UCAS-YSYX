# "一生一芯"工程项目

这是"一生一芯"的工程项目.

### 项目初始化
```C
cd /path/to/your/ysyx-workbench
// 拉取子模块
git submodule update --init --recursive
// 设置环境变量并且把am-kernels拉下来
./init.sh ysyxworkbench
./init.sh nemu
./init.sh abstract-machine
./init.sh npc
./init.sh ysyxSoC
./init.sh nvboard
./init.sh am-kernels
// 各个子模块的操作在子模块对应的README中（还没写）
