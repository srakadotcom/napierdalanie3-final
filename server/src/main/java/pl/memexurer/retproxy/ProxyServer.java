package pl.memexurer.retproxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;

import java.util.function.Consumer;

public class ProxyServer extends NettyServer {
    private final MinecraftServer server;
    private Consumer<ByteBuf> packetHandler;

    public ProxyServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        System.out.println(ch.remoteAddress() + " connected to proxy");
        ch.pipeline().addLast(new Handler());

        server.setPacketHandler(ch::writeAndFlush);
        this.setPacketHandler(server::write);
    }

    private void setPacketHandler(Consumer<ByteBuf> packetHandler) {
        this.packetHandler = packetHandler;
    }

    private class Handler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            int packetId = PacketSplitter.readVarIntFromBuffer(msg);

            System.out.println("Read from proxy: " + packetId);
            if(packetId == 62) {
                return;
            }
            msg.resetReaderIndex();
            ProxyServer.this.packetHandler.accept(msg.copy());
        }
    }
}
