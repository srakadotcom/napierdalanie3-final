package pl.memexurer.retproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class PacketSplitter extends ByteToMessageDecoder {
    protected void decode(ChannelHandlerContext p_decode_1_, ByteBuf p_decode_2_, List<Object> p_decode_3_) throws Exception {
        p_decode_2_.markReaderIndex();
        byte[] abyte = new byte[3];

        for (int i = 0; i < abyte.length; ++i) {
            if (!p_decode_2_.isReadable()) {
                p_decode_2_.resetReaderIndex();
                return;
            }

            abyte[i] = p_decode_2_.readByte();

            if (abyte[i] >= 0) {
                ByteBuf buf = Unpooled.wrappedBuffer(abyte);

                try {
                    int j = readVarIntFromBuffer(buf);

                    if (p_decode_2_.readableBytes() >= j) {
                        p_decode_3_.add(p_decode_2_.readBytes(j));
                        return;
                    }

                    p_decode_2_.resetReaderIndex();
                } finally {
                    buf.release();
                }

                return;
            }
        }

        throw new CorruptedFrameException("length wider than 21-bit");
    }

    public static int readVarIntFromBuffer(ByteBuf buf)
    {
        int i = 0;
        int j = 0;

        while (true)
        {
            byte b0 = buf.readByte();
            i |= (b0 & 127) << j++ * 7;

            if (j > 5)
            {
                throw new RuntimeException("VarInt too big");
            }

            if ((b0 & 128) != 128)
            {
                break;
            }
        }

        return i;
    }
}
