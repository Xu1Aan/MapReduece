# Java 从零实现MapReduece
> 感谢Star, 项目等其他问题交流可发[邮件](mailto:toxuan1998@qq.com)

**项目简介:star2:**

本项目为Java从零实现MapReduce中的wordcount案例，应用到的技术有Netty框架。基于Netty实现了简单的wordcount案例。相较于单点MapReduece，分布式需要考虑更多的并发操作，以及不同节点之间的通信协议等因素。通过Netty提供的良好的异步编程、事件驱动能力，简化了网络应用（分布式应用）的编程开发过程。

**项目说明:star:**

项目由两部分构成：master、worker

master主要负责任务管理，worker负责执行任务

**项目所遇到的难点：**

为不同worker分配任务时并发操作的编写时，最为重要的是，需要维护一个节点（worker）的状态链表。表明哪些worker当前可用；同时需要使用信号量保证多段代码不被并发调用。其次，需要基于Netty控制粘包、半包问题，本项目的做法时自定义了一个协议；网络传输需要序列化，在此使用gson序列化。

<img src=".\picture\流程图.png" style="zoom:20%;" />

详细步骤：

1. master监听节点连接数量，当到达预设值时，开始执行mapreduce，
2. 由于没有分布式文件系统（如HDFS等），所以有master去读取文件数据，通过RPC调用不同worker执行Splitting和Mapping操作。
3. 最后master节点负责shuffle和reduce操作（这里可以改一下）。

**启动:star:**

`application.propertoes`配置项说明：

```properties
serializer.algorithm=Json //选择序列化格式
netty.maxFrameLength=131072 //设置netty最大帧长度
com.xu1an.mrapp.IMapReduce=com.xu1an.mrapp.WordCount //设置MapReduce的实现类
map.nums=2 //设置map数量
reduce.nums=1
```

master启动地址：`com.xu1an.master.Master`

worker启动地址: `com.xu1an.worker.Worker`

:high_brightness:在上述配置下，需要启动两个worker。在idea中需要对worker配置（configurations）设置：

​	选择Modify -> Allow multiple instances。

结果位于mr-out1.txt中

**last**

目前准备找实习，各位大佬有内推可[联系](mailto:toxuan1998@qq.com)我
