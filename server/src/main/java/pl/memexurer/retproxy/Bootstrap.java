package pl.memexurer.retproxy;

public class Bootstrap {

    public static void main(String[] args) {
        MinecraftServer minecraftServer = new MinecraftServer();
        ProxyServer proxyServer = new ProxyServer(minecraftServer);

        new Thread(() -> proxyServer.bind(21377)).start();
        new Thread(() -> minecraftServer.bind(25565)).start();
    }
}
