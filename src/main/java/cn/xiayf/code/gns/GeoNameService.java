package cn.xiayf.code.gns;

import java.util.Properties;
import java.util.logging.Logger;

import cn.xiayf.code.gns.action.IndexAction;
import cn.xiayf.code.gns.action.NameServiceAction;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class GeoNameService {

    private static final Logger logger = Logger.getLogger(GeoNameService.class.getName());

    private Properties pps;

    private GeoNameService(Properties pps) {
        this.pps = pps;
    }

    private void start() throws Exception {
        //
        Router r = new Router()
                .register("/", new IndexAction())
                .register("/ns", new NameServiceAction(pps));
        //
        int port = Integer.valueOf(pps.getProperty("service.port", "8808"));

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerInitializer(r))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind("0.0.0.0", port).sync();

            logger.info("Server started: http://0.0.0.0:" + port);

            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args) throws Exception {
        //
        Properties pps = new Properties();
        pps.load(GeoNameService.class.getResourceAsStream("/app.properties"));
        //
        new GeoNameService(pps).start();
    }
}
