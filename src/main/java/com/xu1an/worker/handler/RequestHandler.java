package com.xu1an.worker.handler;

import com.xu1an.message.RequestTaskMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Xu1Aan
 * @Date: 2022/07/13/14:29
 * @Description:
 */
@ChannelHandler.Sharable
public class RequestHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        RequestTaskMessage taskMessage = new RequestTaskMessage();
        taskMessage.setStatus(1);
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientPort = String.valueOf(insocket.getPort());
        taskMessage.setWorkerName(clientPort);
        ctx.writeAndFlush(taskMessage);
    }
}
