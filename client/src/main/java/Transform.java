import com.mojang.authlib.GameProfile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import pl.memexurer.retpomusz.netty.NettyHandlers;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.Base64;
import java.util.UUID;

public class Transform {

    private static final String[] TRANSFORM_BLACKLIST = new String[]{
            "net.minecraft.launchwrapper",
            "joptsimple",
            "org",
            "java",
            "com.sun"
    };
    private static final Printer printer = new Textifier();
    private static final TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);

    static {
        try {
            System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
            System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.exit(0);
        }
    }

    public static byte[] transform(ClassLoader loader,
                                   String name,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain,
                                   byte[] contents) {
        name = name.replace("/", ".");
        for (String blacklistedName : TRANSFORM_BLACKLIST) {
            if (name.startsWith(blacklistedName))
                return contents;
        }

        String coolerName = name;

        boolean modified = false;
        boolean isAbstractChannel = false;
        boolean isUnpooled = false;

        try {
            ClassReader reader = new ClassReader(contents);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.EXPAND_FRAMES);

            for (MethodNode methodNode : node.methods) { // replace blazingpack names
                for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
                    if (insnNode instanceof LdcInsnNode) {
                        Object cst = ((LdcInsnNode) insnNode).cst;
                        if (!(cst instanceof String))
                            continue;

                        String cstStrnig = (String) cst;
                        if (cstStrnig.contains("Blazing")) {
                            ((LdcInsnNode) insnNode).cst = cstStrnig.replace("Blazing", "Spierdalaj");
                            modified = true;
                        } else if (cstStrnig.contains("promise.channel does not match: %s (expected: %s)")) {
                            System.out.println("Found abstract channel handler context");
                            isAbstractChannel = true;
                        } else if(cstStrnig.equals( "The total length of the specified buffers is too big.")) {
                            System.out.println("Found unpooled class");
                            isUnpooled = true;
                        }
                    }
                }
            }

            for(MethodNode methodNode: node.methods) {
                boolean found = false, found2 = false;
                for(AbstractInsnNode insnNode: methodNode.instructions.toArray()) {
                    if(insnNode instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if(methodInsnNode.owner.equals("java/util/UUID") && methodInsnNode.name.equals("fromString")) {
                            found = true;
                        }
                        if(methodInsnNode.desc.equals("(Ljava/util/UUID;Ljava/lang/String;)V") && methodInsnNode.name.equals("<init>")) {
                            found2 = true;
                        }
                    }
                }

                if(found && found2 && node.methods.size() == 9) {
                    NettyHandlers.s02PacketLoginSuccess = coolerName;
                }
            }

            if (isAbstractChannel) { // public writeAndFlush(Ljava/lang/Object;Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelFuture;
                for (MethodNode methodNode : node.methods) {
                    if (methodNode.name.equals("write") && methodNode.desc.contains("(Ljava/lang/Object;ZL")) {
                        System.out.println("Found channel write");
                        InsnList insnList = new InsnList();

                        insnList.add(new LabelNode());
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pl/memexurer/retpomusz/netty/NettyHandlers", "handleWrite", "(Ljava/lang/Object;)Z", false));
                        insnList.add(new JumpInsnNode(Opcodes.IFNE, findFirstLabel(methodNode.instructions)));
                        insnList.add(new LabelNode());
                        insnList.add(new InsnNode(Opcodes.RETURN));
                        methodNode.instructions.insert(insnList);
                        modified = true;
                    } else if (methodNode.name.equals("invokeChannelRead")) {
                        System.out.println("Found channel read");
                        InsnList insnList = new InsnList();
                        insnList.add(new LabelNode());
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pl/memexurer/retpomusz/netty/NettyHandlers", "handleRead", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
                        insnList.add(new JumpInsnNode(Opcodes.IFNE, findFirstLabel(methodNode.instructions)));
                        insnList.add(new LabelNode());
                        insnList.add(new InsnNode(Opcodes.RETURN));
                        methodNode.instructions.insert(insnList);

                        modified = true;
                    } else if(methodNode.name.equals("invokeClose")) {
                        System.out.println("Found channel close");
                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pl/memexurer/retpomusz/netty/NettyHandlers", "handleClose", "(Ljava/lang/Object;)V", false));
                        methodNode.instructions.insert(insnList);

                        modified = true;
                    }
                }
            }

            for (MethodNode methodNode : node.methods) { // add channel active hook
                if (methodNode.name.equals("channelActive") && !Modifier.isAbstract(methodNode.access)) {
                    System.out.println("Channel active hooked");

                    InsnList insnList = new InsnList();
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pl/memexurer/retpomusz/netty/NettyHandlers", "addChannel", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
                    methodNode.instructions.insert(insnList);

                    modified = true;
                }
            }

            if (modified) {
                System.out.println("Transforming " + coolerName);
                // System.out.println("Modifying");
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                byte[] bytes = writer.toByteArray();
                if(isUnpooled)
                    Files.write(new File(coolerName.hashCode() + ".class").toPath(), bytes);
                return bytes;
            }
        } catch (Exception ex) {
            if (modified) {
                System.out.println("Exception on modify:");
                ex.printStackTrace();
            }
        }

        return contents;
    }

    private static LabelNode findFirstLabel(InsnList list) {
        for(AbstractInsnNode node: list.toArray())
            if(node instanceof LabelNode)
                return (LabelNode) node;
        return null;
    }

    public static String print(AbstractInsnNode insnNode) {
        if (insnNode == null) return "null";


        insnNode.accept(methodPrinter);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString().trim();
    }

}
