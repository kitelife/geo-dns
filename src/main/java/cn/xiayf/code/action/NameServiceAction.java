package cn.xiayf.code.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import cn.xiayf.code.exception.BadReqException;

public class NameServiceAction extends BaseAction implements Action {

    private static final Logger logger = Logger.getLogger(NameServiceAction.class.getName());

    private Properties pps;

    private static final Boolean lock = true;

    private static ExecutorService es;
    private static Cache<String, Set<String>> globalCache;

    public NameServiceAction(Properties pps) {
        this.pps = pps;
        //
        synchronized(lock) {
            if (es == null) {
                es = Executors.newFixedThreadPool(Integer.valueOf(
                        pps.getProperty("dns.pool.thread.num", "50")));
            }
            if (globalCache == null) {
                globalCache = CacheBuilder.newBuilder()
                        .maximumSize(Integer.valueOf(pps.getProperty("dns.cache.num", "10000")))
                        .expireAfterWrite(Integer.valueOf(
                                pps.getProperty("dns.cache.life.second", "3600")), TimeUnit.SECONDS)
                        .build();
            }
        }
    }

    public RespBody handle(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
        Map<String, String> urlParams = this.parseURLParam(req);
        if (!urlParams.containsKey("domain")) {
            throw new BadReqException("缺少请求参数domain");
        }
        String domain = urlParams.get("domain");
        //
        Set<String> ips;

        ips = globalCache.getIfPresent(domain);
        if (ips == null) {
            ips = resolveDomain(domain, loadDNSServers(),
                    Integer.valueOf(pps.getProperty("dns.timeout")),
                    Integer.valueOf(pps.getProperty("dns.retry.times")));
            globalCache.put(domain, ips);
        }
        return new RespBody(HttpResponseStatus.OK.code(), "请求成功", ips);
    }

    private static Set<String> resolveDomain(String domain, List<String> dnsServers, int timeout, int retryCount) {
        Set<String> ips = new HashSet<>();
        List<Future<List<String>>> futures = new ArrayList<>();
        for (String dnsServer : dnsServers) {
            Future<List<String>> futureResult = es.submit(() -> {
                try {
                    return getDNSRecs(domain, dnsServer, new String[] {"A"}, timeout, retryCount);
                } catch (NamingException e) {
                    logger.warning(String.format("%s, %s", e.getMessage(), dnsServer));
                }
                return new ArrayList<>();
            });
            futures.add(futureResult);
        }

        for (Future<List<String>> f : futures) {
            try {
                ips.addAll(f.get(6, TimeUnit.SECONDS));
            } catch (Exception e){
                logger.warning(e.getMessage());
            }
        }

        return ips;
    }

    private static ArrayList<String> getDNSRecs(String domain, String provider, String[] types, int timeout,
                                               int retryCount) throws NamingException {

        ArrayList<String> results = new ArrayList<>(15);

        Hashtable<String, String> env = new Hashtable<>();

        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        //设置域名服务器
        env.put(Context.PROVIDER_URL, "dns://" + provider);

        // 连接时间
        env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(timeout));

        // 连接次数
        env.put("com.sun.jndi.dns.timeout.retries", String.valueOf(retryCount));

        long startTime = System.currentTimeMillis();

        DirContext ictx = new InitialDirContext(env);
        Attributes attrs = ictx.getAttributes(domain, types);

        for (Enumeration e = attrs.getAll(); e.hasMoreElements(); ) {
            Attribute a = (Attribute) e.nextElement();
            int size = a.size();
            for (int i = 0; i < size; i++) {
                results.add((String) a.get(i));
            }
        }
        logger.info(String.format("Provider: %s, domain: %s, Elapsed: %s", provider, domain,
                System.currentTimeMillis()-startTime));
        return results;
    }

    private List<String> loadDNSServers() {
        List<String> dnsServers = new ArrayList<>();

        Set<String> pNames = pps.stringPropertyNames();
        pNames.stream()
                .filter(name -> name.startsWith("dns.servers."))
                .forEach(name -> dnsServers.addAll(
                        Arrays.asList(pps.getProperty(name).split(","))
                        ));
        return dnsServers;
    }
}
