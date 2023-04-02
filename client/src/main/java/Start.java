import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class Start {
    public static void main(String[] args) throws Throwable {
        File bp = new File(System.getProperty("user.home") + "\\AppData\\Roaming\\.minecraft\\versions\\blazingpack_1.8.8\\blazingpack_1.8.8App.jar");
        if (!bp.exists()) {
            System.out.println("Nie znaleziono blazingpack_1.8.8App.jar w " + bp.getAbsolutePath());
            System.out.println("Zrob cos zeby tam sie znalazl blazingpack.");
            return;
        }

        File rogal = new File(System.getProperty("user.dir"), "rogal.dll");
        if (!rogal.exists()) {
            System.out.println("Nie znaleziono rogala dll. (" + rogal.getAbsolutePath() + ")");
            return;
        }

        System.load(rogal.getAbsolutePath());

        ClassLoader loader = new URLClassLoader(
                new URL[]{
                        bp.toURI().toURL()
                },
                Start.class.getClassLoader()
        );

        loader.loadClass("net.minecraft.client.main.Main").getMethod("main", String[].class).invoke(null, new Object[]{args});
    }
}
