package pl.memexurer.retproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.internal.StringUtil;

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

        public void copyS38(ByteBuf src, ByteBuf dest) {
            PacketPrepender.writeVarIntToBuffer(dest, 0x38);

            int action = readAndCopyInt(src, dest);
            int entries = readAndCopyInt(src, dest);
            for (int i = 0; i < entries; i++) {
                dest.writeLong(src.readLong()); // skip
                dest.writeLong(src.readLong()); // uuid

                if (action == 0) {
                    subString(src, dest);
                    int properties = readAndCopyInt(src, dest);
                    for (int j = 0; j < properties; j++) {
                        copyString(src, dest); //skip property name
                        copyString(src, dest); //skip property value
                        boolean bool = src.readBoolean();
                        dest.writeBoolean(bool);
                        if (bool) {
                            copyString(src, dest); // skip signature
                        }
                    }

                    readAndCopyInt(src, dest); // ping gamemode
                    readAndCopyInt(src, dest); // skip ping
                    boolean bool = src.readBoolean();
                    dest.writeBoolean(bool);
                    if (bool) {
                        copyString(src, dest);
                    }
                } else if (action == 1 || action == 2) {
                    readAndCopyInt(src, dest);
                } else if (action == 3) {
                    boolean bool = src.readBoolean();
                    dest.writeBoolean(bool);
                    if (bool) {
                        copyString(src, dest);
                    }
                }
            }
        }

        public int readAndCopyInt(ByteBuf src, ByteBuf dst) {
            int read = PacketSplitter.readVarIntFromBuffer(src);
            PacketPrepender.writeVarIntToBuffer(dst, read);
            return read;
        }

        public void subString(ByteBuf src, ByteBuf dst) {
            byte[] displayNameRaw = new byte[PacketSplitter.readVarIntFromBuffer(src)];
            src.readBytes(displayNameRaw);
            String displayName = new String(displayNameRaw);

            if (displayName.length() > 16) {
                displayName = displayName.substring(0, 16);
            }

            displayNameRaw = displayName.getBytes();
            PacketPrepender.writeVarIntToBuffer(dst, displayNameRaw.length);
            dst.writeBytes(displayNameRaw);
        }

        private void copyString(ByteBuf src, ByteBuf dst) {
            int size = PacketSplitter.readVarIntFromBuffer(src);
            PacketPrepender.writeVarIntToBuffer(dst, size);

            byte[] data = new byte[size];
            src.readBytes(data);

            dst.writeBytes(data);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf src) throws Exception {
            ByteBuf originalSrcBuf = src.copy();
            int packetId = PacketSplitter.readVarIntFromBuffer(src);

            System.out.println("Read from proxy: " + packetId);

            ByteBuf dest;
            if (packetId == 0x38) { // player list
                dest = Unpooled.buffer();
                copyS38(src, dest);

              //  dumpHex(originalSrcBuf, dest);
            } else {
                src.resetReaderIndex();
                dest = src.copy();
            }

            if (packetId >= 0x3B && packetId <= 0x3E) { // scoreboard packets
                return;
            }

            ProxyServer.this.packetHandler.accept(dest);
        }
    }

    private void dumpHex(ByteBuf he, ByteBuf he2) {
        // do debugowania pakeitow xdddd
        // to bylo trudne w chuj Nie Pozdrawiam.

        String hes = dumpHex(he);
        String hes2 = dumpHex(he2);
        if(hes.length() > hes2.length()) {
            System.out.println(padLeftZeros(hes2, hes.length()));
            System.out.println(hes);
        } else {
            System.out.println(padLeftZeros(hes, hes2.length()));
            System.out.println(hes2);
        }
    }

    public String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('_');
        }
        sb.append(inputString);

        return sb.toString();
    }

    private String dumpHex(ByteBuf he) {
        byte[] hax = new byte[he.readableBytes()];
        he.getBytes(0, hax);
        return (StringUtil.toHexString(hax));
    }
}
