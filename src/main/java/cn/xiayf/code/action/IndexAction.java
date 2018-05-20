package cn.xiayf.code.action;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class IndexAction implements Action {

    public RespBody handle(ChannelHandlerContext ctx, HttpRequest req) {
        return new RespBody(HttpResponseStatus.OK.code(), "欢迎访问智能域名服务！");
    }
}
