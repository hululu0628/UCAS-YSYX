# "一生一芯"工程项目

这是"一生一芯"的工程项目.

### 接入ysyxSoC
将CPU.scala中的BlackBox模块设置为ysyxCPU，将Top.scala中的顶层模块设置为Top（Elaborate当然也要改动），将Makefile的V_FILE_GEN值设为Top
