import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
//
public class Start {
    public static void main(String[] args) throws Throwable {
        hackNatives();

        System.load("C:\\Users\\azeroy\\CLionProjects\\untitled\\cmake-build-debug\\untitled.dll");
        ClassLoader loader = new URLClassLoader(
                new URL[]{
                        //          new File("C:\\Users\\azeroy\\AppData\\Roaming\\.minecraft\\versions\\1.8.9\\1.8.9.jar").toURI().toURL()
                        new File("C:\\Users\\azeroy\\AppData\\Roaming\\.minecraft\\versions\\blazingpack_1.8.8\\blazingpack_1.8.8App.jar").toURI().toURL()
                },
                Start.class.getClassLoader()
        );

        //   Launch.classLoader = new LaunchClassLoader(loader);
        //   Launch.main(args);
        loader.loadClass("net.minecraft.client.main.Main").getMethod("main", String[].class).invoke(null, new Object[]{args});
    }

    private static void hackNatives() {
        String paths = System.getProperty("java.library.path");
        String nativesDir = "C:/Users/azeroy/.gradle/caches/minecraft/net/minecraft/natives/1.8.9";
        if (paths == null || paths.equals("")) {
            paths = nativesDir;
        } else {
            paths = paths + File.pathSeparator + nativesDir;
        }

        System.setProperty("java.library.path", paths);

        try {
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Throwable var3) {
        }

    }
}
