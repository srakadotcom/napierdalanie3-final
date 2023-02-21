package pl.memexurer.retpomusz.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.util.function.Consumer;

public class ProxyPipeClient {
    public static AttributeKey<Boolean> PIPE = AttributeKey.valueOf("pipe");
    private Consumer<ByteBuf> readHandler;
    private Channel channel;

    public void connect(String host, int port) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new PacketSplitter(), new PacketPrepender(), new SimpleChannelInboundHandler<ByteBuf>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                            ctx.channel().attr(PIPE).set(true);
                            if(readHandler != null) {
                                readHandler.accept(msg);
                            } else {
                                System.out.println("read handler is null, but it shouldn't be!");
                            }
                        }
                    });
                }
            });

            ChannelFuture f = b.connect(host, port).awaitUninterruptibly();
            this.channel = f.channel();
            f.channel().closeFuture().awaitUninterruptibly();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void setReadHandler(Consumer<ByteBuf> readHandler) {
        this.readHandler = readHandler;
    }

    public void write(ByteBuf buf) {
        if(channel != null) {
            channel.writeAndFlush(buf);
        } else {
            System.out.println("Tried to write, but channel is null!");
        }
    }
}
