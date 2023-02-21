package pl.memexurer.retpomusz.netty;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class NettyHandlers {
    private static final ProxyPipeClient pipeClient;
    public static String s02PacketLoginSuccess;
    private static boolean stop = false;
    private static Method nameMethod;

    static {
        pipeClient = new ProxyPipeClient();
        new Thread(() -> pipeClient.connect("127.0.0.1", 21377)).start();
    }

    public static void printInheritance(Class<?> clazz, int inheritance) {
        if (true)
            return;

        String indentation = IntStream.range(0, inheritance).mapToObj(i -> "---").collect(Collectors.joining());
        System.out.println(indentation + clazz.getName());
        for (Method method : clazz.getMethods()) {
            System.out.println(indentation + method.getName() + "(" +
                    Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(", ")) +
                    ")");
        }
        if (!clazz.getSuperclass().getName().equals("java.lang.Object"))
            printInheritance(clazz.getSuperclass(), inheritance + 1);
    }

    public static boolean handleWrite(Object written) {
        if(!stop)
            return true;

        // d105ddef@63c74480[ null public=MC|SUZI_GSXR_1000_K7, null public= ! byte ] void float 0 strictfp transient public try float new 3 do throw protected transient new synchronized static short break 9 const $ ] class protected while ' final default { new short throws % = import 6 super default short 0 abstract 6 " long | new(ridx: 0, widx: 8, cap: 8/8), null public=<null>]
        if (!(written instanceof Comparable<?>)) {
            String hex = Integer.toHexString(written.getClass().getName().hashCode());
            if(!hex.equals("d105ddef"))
                return false;
            try {
                System.out.println(ReflectionToStringBuilder.toString(written).replace(
                        written.getClass().getName(),
                        Integer.toHexString(written.getClass().getName().hashCode())
                ));
            } catch (Throwable throwable) {
                ;
            }
        }
        return true;
    }

    private static String getContextName(Object context) {
        try {
            return (String) getNameMethod(context.getClass()).invoke(context);
        } catch (Throwable e) {
            return "";
        }
    }

    private static Method getNameMethod(Class<?> contextClass) {
        if (nameMethod != null)
            return nameMethod;

        for (Method method : contextClass.getMethods())
            if (method.getReturnType() == String.class) {
                nameMethod = method;
                method.setAccessible(true);
                return nameMethod;
            }

        throw new RuntimeException("Could not find name method! bsda adsdasdas");
    }

    public static boolean handleRead(Object self, Object read) throws RuntimeException {
        String handlerName = getContextName(self);


        if (stop) {
            if(isBlacklisted(self) == null)
                return true;

            if (handlerName.equals("YDWZIptvTwr5AN5zwvfF")) { // TgM9dnjnTWoLe8IPlEDg ddd
                byte[] bytes = new byte[(int) getObject("readableBytes", read)];
                try {
                    read.getClass().getMethod("readBytes", byte[].class).invoke(read, new Object[]{bytes});
                    getObject("resetReaderIndex", read);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }

                ByteBuf unpooled = Unpooled.buffer(bytes.length);
                unpooled.writeBytes(bytes);
                pipeClient.write(unpooled);

                return true; // this should be false i think
            }

            return true;
        }

        try {
            if (read instanceof Comparable<?>)
                return true;

            for (Method method : read.getClass().getMethods()) {
                if (method.getReturnType() == GameProfile.class) {
                    stop = true;
                    break;
                }
            }
        } catch (Throwable ex) {
            System.out.println("Failed to make a paket dump!s");
            ex.printStackTrace();
        }
        return true;
    }

    public static void handleClose(Object ctx) {
        if(isBlacklisted(ctx) == null) {
            return;
        }

        //todo do naprawienia: po rozlaczeniu z serwera nie mozna sie polaczyc na nowo
        //bo stop jest wtedy na true i to blokuje wszystkie nowe pakiety
        //odkomentowanie ponizszego kodu nic nie da bo ten check isBlacklitsed nie dziala!
        //pomocy kurwa!!!!!!!!!!

     //   stop = false;
     //   pipeClient.setReadHandler(null);
    }

    private static Method getObjectMethod(Class<?> contextClass, String name) {
        for (Method method : contextClass.getMethods())
            if (method.getName().equals(name)) {
                method.setAccessible(true);
                return method;
            }

        throw new RuntimeException("Could not find a method with specified name!");
    }

    private static Object getObject(String name, Object context) {
        try {
            return getObjectMethod(context.getClass(), name).invoke(context);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object isBlacklisted(Object ctx) {
        try {
            Object channel = getObject("channel", ctx);
            int port = ((InetSocketAddress) getObject("remoteAddress", channel)).getPort();
            if (port == 21377 || port == 21234) { //port proxy; port api bp
                return null;
            } else {
                return channel;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public static void addChannel(Object self, Object ctx) {
        Object channel = isBlacklisted(ctx);
        if (channel == null)
            return;

        Method writeAndFlush = getObjectMethod(channel.getClass(), "writeAndFlush");
        Class<?> byteBufClass = writeAndFlush.getParameterTypes()[0];
        pipeClient.setReadHandler(buf -> {
            Object allocator = getObject("alloc", channel);
            Object byteBuf;
            try {
                byteBuf = allocator.getClass().getMethod("buffer", int.class).invoke(allocator, buf.readableBytes());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);

            try {
                byteBuf.getClass().getMethod("writeBytes", byte[].class).invoke(byteBuf, new Object[]{bytes});
                writeAndFlush.invoke(channel, byteBuf);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
