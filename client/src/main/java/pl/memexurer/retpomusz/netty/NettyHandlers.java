package pl.memexurer.retpomusz.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
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

    public static boolean handleWrite(Object ctx, Object written) {
        if (isBlacklisted(ctx) == null) {
            return true; // allow blacklisted writes
        }

        if (!stop)
            return true; // allow writes when not connected to any server

        if (!(written instanceof Comparable<?>)) {
            String hex = Integer.toHexString(written.getClass().getName().hashCode());
            return hex.equals("d105ddef"); // only allow sending plugin message packet
        }

        return true; // allow all bytebufs
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
            if (isBlacklisted(self) == null)
                return true;

            addChannel(self, self);

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
                if (method.getReturnType().getName().contains("GameProfile")) {
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

        pipeClient.setReadHandler(buf -> {
            Method writeAndFlush = getObjectMethod(channel.getClass(), "writeAndFlush");

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
