package com.myway.server;

import com.myway.service.MyWebSite;
import com.myway.service.SkyWalking;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author wenzhihuai
 * @since 2018/9/4 22:53
 */
@Slf4j
public class HttpServerHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            String host = request.headers().get("host");
            String prefix = host.split("\\.")[0];
            String content;
            switch (prefix) {
                case "sky":
                    SkyWalking skyWalking = new SkyWalking();
                    content = skyWalking.getPage(request.uri());
                    break;
                default:
                    MyWebSite myWebSite = new MyWebSite();
                    content = myWebSite.getPage(request.uri());
            }
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1, OK, Unpooled.wrappedBuffer(content != null ? content
                    .getBytes() : new byte[0]));
            response.headers().set(CONTENT_TYPE, "image/png;charset=UTF-8");
            response.headers().set(CONTENT_LENGTH,
                    response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(response);
            ctx.flush();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.error(cause.toString());
        ctx.close();
    }
}
