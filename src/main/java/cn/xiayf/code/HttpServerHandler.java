package cn.xiayf.code;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

@Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private Router router;

    HttpServerHandler(Router router) {
        this.router = router;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
        //
        FullHttpResponse resp = router.dispatch(ctx, req);
        //
        resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, resp.content().readableBytes());
        //
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
}
