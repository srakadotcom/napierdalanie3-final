package pl.memexurer.retproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;

import java.util.UUID;
import java.util.function.Consumer;

public class MinecraftServer extends NettyServer {
    private static final AttributeKey<Boolean> PROXYING = AttributeKey.newInstance("proxying");
    private static final AttributeKey<Integer> STATE = AttributeKey.newInstance("state");
    private Consumer<ByteBuf> packetHandler;
    private Channel proxyChannel;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new Handler());
    }

    public void setPacketHandler(Consumer<ByteBuf> packetHandler) {
        this.packetHandler = packetHandler;
    }

    public void write(ByteBuf packet) {
        this.proxyChannel.writeAndFlush(packet);
    }

    private class Handler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            if (ctx.channel().hasAttr(PROXYING)) {
                packetHandler.accept(msg.copy());
            } else {
                int packetId = PacketSplitter.readVarIntFromBuffer(msg);

                if (!ctx.channel().hasAttr(STATE)) {
                    if (packetId == 0) {
                        PacketSplitter.readVarIntFromBuffer(msg);
                        msg.skipBytes(PacketSplitter.readVarIntFromBuffer(msg));
                        msg.readUnsignedShort();
                        int nextState = PacketSplitter.readVarIntFromBuffer(msg);
                        ctx.channel().attr(STATE).set(nextState);
                    } else throw new IllegalArgumentException("Unknown packet!");
                } else {
                    int state = ctx.channel().attr(STATE).get();
                    if(state == 1 && packetId == 0) { // motd request packet
                        String desc = "Plonacy dupson\nGraj za darma";
                        String json = "{\"version\":{\"name\":\"name\",\"protocol\":47},\"players\":{\"max\":0,\"online\":0},\"description\":\"" + desc + "\"}";

                        byte[] bytes = json.getBytes();
                        ByteBuf packet = Unpooled.buffer();
                        PacketPrepender.writeVarIntToBuffer(packet, 0); // packet id
                        PacketPrepender.writeVarIntToBuffer(packet, bytes.length); // buffer
                        packet.writeBytes(bytes);
                        ctx.channel().writeAndFlush(packet);
                    } else if(state == 2) {
                        if(packetId == 0) {
                            { // login success packet
                                ByteBuf packet = Unpooled.buffer();
                                PacketPrepender.writeVarIntToBuffer(packet, 2); // packet id

                                //uuid:
                                byte[] uuid = UUID.randomUUID().toString().getBytes();
                                PacketPrepender.writeVarIntToBuffer(packet, uuid.length);
                                packet.writeBytes(uuid);

                                //name:
                                byte[] playerName = ":3".getBytes();
                                PacketPrepender.writeVarIntToBuffer(packet, playerName.length);
                                packet.writeBytes(playerName);

                                ctx.channel().writeAndFlush(packet);
                            }

                            MinecraftServer.this.proxyChannel = ctx.channel();
                            ctx.channel().attr(PROXYING).set(true);
                            System.out.println("Enabling proxy mode for " + ctx.channel().remoteAddress());
                        }
                    }
                }
            }
        }
    }
}
