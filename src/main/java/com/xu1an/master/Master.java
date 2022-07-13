package com.xu1an.master;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xu1an.common.KeyValue;
import com.xu1an.config.Config;
import com.xu1an.master.handler.RpcResponseMessageHandler;
import com.xu1an.master.session.Session;
import com.xu1an.master.session.SessionFactory;
import com.xu1an.message.RequestTaskMessage;
import com.xu1an.message.RpcRequestMessage;
import com.xu1an.message.RpcResponseMessage;
import com.xu1an.mrapp.IMapReduce;
import com.xu1an.mrapp.WordCount;
import com.xu1an.protocol.MessageCodecSharable;
import com.xu1an.protocol.ProcotolFrameDecoder;
import com.xu1an.protocol.SequenceIdGenerator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Xu1Aan
 * @Date: 2022/07/12/18:11
 * @Description:
 */
@Slf4j
public class Master {
    public static void main(String[] args) throws InterruptedException {

        MapReduceServer();
    }


    private static void MapReduceServer(){
        // 建立服务
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();
        CountDownLatch WAIT = new CountDownLatch(1);
        // 获得机器数量
        Semaphore semaphore = new Semaphore(Config.getMapNums());
        List<Channel> channelStatusList = new CopyOnWriteArrayList<>();
        Semaphore semaphoreMapReduce = new Semaphore(1);
        AtomicInteger count = new AtomicInteger(0);

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss,worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ProcotolFrameDecoder());
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);
                    ch.pipeline().addLast(RPC_HANDLER);
                    ch.pipeline().addLast("handler",new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            //log.debug("msg: {}", msg);
                            if((msg instanceof RequestTaskMessage)) {
                                log.debug("RequestTaskMessage");
                                RequestTaskMessage message = (RequestTaskMessage) msg;
                                Session session = SessionFactory.getSession();
                                session.bind(ctx.channel(),message.getWorkerName());

                                List<Channel> channels = session.getAllChannel();
                                channelStatusList.add(ctx.channel());
                                log.debug("{}",channels.size());
                                if (channels.size() == Config.getMapNums()) {
                                    WAIT.countDown();
                                }
                            }
                            if ((msg instanceof RpcResponseMessage)) {
                                log.debug("RpcResponseMessage");
                                channelStatusList.add(ctx.channel());
                                semaphore.release();
                            }
                        }

                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            if (count.getAndIncrement() == 0) {
                                new Thread(() -> {
                                    try {
                                        log.debug("wait");
                                        WAIT.await();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    log.debug("Start");
                                    // 1. 判断文件路径是否有文件
                                    String filePath = "./data";
                                    File file = new File(filePath);
                                    File[] files = file.listFiles();
                                    if (files == null) {
                                        log.error("the file path error. no file in dir");
                                        return;
                                    }

                                    log.debug("Map start");
                                    List<KeyValue> intermediate = new CopyOnWriteArrayList<>();
                                    List<Thread> threadSet = new ArrayList<>();
                                    for (File f : files) {
                                        String content = readAll(f);
                                        Thread thread = new Thread(() -> {
                                            try {
                                                semaphore.acquire();
                                                Channel channel = channelStatusList.remove(0);
                                                IMapReduce mapreduce = getProxyService(IMapReduce.class, channel);
                                                Gson gson = new Gson();
                                                Type type = new TypeToken<List<KeyValue>>() {
                                                }.getType();
                                                List<KeyValue> studentInfoList = gson.fromJson(mapreduce.map(content).toString(), type);
                                                // 打印
                                                intermediate.addAll(studentInfoList);
                                                // System.out.println(intermediate.get(1).getKey());
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        thread.start();
                                        threadSet.add(thread);
                                    }

                                    for (Thread thread : threadSet) {
                                        try {
                                            thread.join();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    // 3. 对List集合（相当于缓存区）排序，将数据按Key进行排序
                                    intermediate.sort(Comparator.comparing(KeyValue::getKey));

                                    String outputName = "mr-out1.txt";
                                    File outputFile = new File(outputName);
                                    log.debug("the output will in :[{}]", outputName);

                                    log.debug("Reduce start");
                                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
                                        for (int i = 0; i < intermediate.size(); ) {
                                            int j = i + 1;
                                            String keyI = intermediate.get(i).getKey();

                                            while (j < intermediate.size() && (intermediate.get(j).getKey().equals(keyI))) {
                                                j++;
                                            }
                                            List<String> values = new ArrayList<>(j - i);
                                            for (int k = i; k < j; k++) {
                                                values.add(intermediate.get(k).getValue());
                                            }

                                            IMapReduce mapReduce = new WordCount();
                                            String output = mapReduce.reduce(values);

                                            bw.write(keyI + " " + output + System.lineSeparator());
                                            i = j;
                                        }
                                        log.debug("Reduce finished");

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }).start();

                            }
                        }
                    });
                }
            });

            Channel channel = serverBootstrap.bind(8080).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e){
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    private static <T> T getProxyService(Class<T> serviceClass,Channel channel) {
        ClassLoader loader = serviceClass.getClassLoader();
        Class<?>[] interfaces = new Class[]{serviceClass};
        //                                                            sayHello  "张三"
        Object o = Proxy.newProxyInstance(loader, interfaces, (proxy, method, args) -> {
            // 1. 将方法调用转换为 消息对象
            int sequenceId = SequenceIdGenerator.nextId();
            RpcRequestMessage msg = new RpcRequestMessage(
                    sequenceId,
                    serviceClass.getName(),
                    method.getName(),
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args
            );
            // 2. 将消息对象发送出去
            channel.writeAndFlush(msg);

            // 3. 准备一个空 Promise 对象，来接收结果             指定 promise 对象异步接收结果线程
            DefaultPromise<Object> promise = new DefaultPromise<>(channel.eventLoop());
            RpcResponseMessageHandler.PROMISES.put(sequenceId, promise);

            // 4. 等待 promise 结果
            promise.await();
            if(promise.isSuccess()) {
                // 调用正常
                return promise.getNow();
            } else {
                // 调用失败
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) o;
    }

    private static String readAll(final File file) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }


}
