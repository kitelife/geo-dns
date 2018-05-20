package cn.xiayf.code;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.gson.Gson;

import cn.xiayf.code.action.Action;
import cn.xiayf.code.action.RespBody;
import cn.xiayf.code.exception.BadReqException;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class Router {
    private static final Logger logger = Logger.getLogger(Router.class.getName());

    private static Gson gson = new Gson();

    private ConcurrentHashMap<String, Action> routeMap;

    public Router() {
        routeMap = new ConcurrentHashMap<>();
    }

    public FullHttpResponse dispatch(ChannelHandlerContext ctx, HttpRequest req) {
        try {
            String path = req.getUri().split("\\?")[0];
            if (routeMap.containsKey(path)) {
                return this.respOK(ctx, routeMap.get(path).handle(ctx, req));
            }
        } catch (BadReqException e) {
            return this.respBadReq(ctx, e);
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
            return this.respInternalError(ctx, e);
        }
        return this.respNotFound(ctx);
    }

    public Router register(String path, Action action) {
        routeMap.put(path, action);
        return this;
    }

    private FullHttpResponse respOK(ChannelHandlerContext ctx, RespBody rb) {
        return new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(gson.toJson(rb), CharsetUtil.UTF_8)
        );
    }

    private FullHttpResponse respNotFound(ChannelHandlerContext ctx) {
        RespBody rb = new RespBody(HttpResponseStatus.NOT_FOUND.code(), "未找到目标资源");
        return new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND,
                Unpooled.copiedBuffer(gson.toJson(rb), CharsetUtil.UTF_8)
        );
    }

    private FullHttpResponse respInternalError(ChannelHandlerContext ctx, Exception e) {
        RespBody rb = new RespBody(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                "系统错误, " + e.getMessage());
        return new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Unpooled.copiedBuffer(gson.toJson(rb), CharsetUtil.UTF_8)
        );
    }

    private FullHttpResponse respBadReq(ChannelHandlerContext ctx, Exception e) {
        RespBody rb = new RespBody(HttpResponseStatus.BAD_REQUEST.code(),
                "请求有误，" + e.getMessage());
        return new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST,
                Unpooled.copiedBuffer(gson.toJson(rb), CharsetUtil.UTF_8)
        );
    }
}
