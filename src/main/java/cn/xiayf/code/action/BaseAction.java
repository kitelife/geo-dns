package cn.xiayf.code.action;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpRequest;

class BaseAction {

    Map<String, String> parseURLParam(HttpRequest req) {
        Map<String, String> urlParams = new HashMap<>();
        String[] uriParts = req.getUri().split("\\?");
        if (uriParts.length != 2) {
            return urlParams;
        }

        String[] urlParamParts = uriParts[1].split("&");
        for (String p : urlParamParts) {
            String[] paramKV = p.split("=");
            if (paramKV.length != 2) {
                continue;
            }
            urlParams.put(paramKV[0], paramKV[1]);
        }
        return urlParams;
    }
}
