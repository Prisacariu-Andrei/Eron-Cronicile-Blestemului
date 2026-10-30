package Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/** Setările jucătorului (tasta T): volum, scuturatul ecranului, viteza textului. Se salvează în eron_settings.properties, în folderul de date (AppPaths). */
public final class Settings {
    public static float volume = 1.0f;        // 0..1
    public static boolean shake = true;       // scuturatul ecranului la lovituri
    public static int textSpeed = 1;          // 0 = lent, 1 = normal, 2 = rapid
    public static boolean fullscreen = false; // ecran complet fără ramă (F11 / Alt+Enter / setări)

    private static final File FILE = AppPaths.file("eron_settings.properties");

    static { load(); }

    private Settings() {}

    public static void load() {
        if (!FILE.exists()) return;
        try (FileInputStream in = new FileInputStream(FILE)) {
            Properties p = new Properties();
            p.load(in);
            volume = Math.max(0f, Math.min(1f, Float.parseFloat(p.getProperty("volume", "1.0"))));
            shake = Boolean.parseBoolean(p.getProperty("shake", "true"));
            textSpeed = Math.max(0, Math.min(2, Integer.parseInt(p.getProperty("textSpeed", "1"))));
            fullscreen = Boolean.parseBoolean(p.getProperty("fullscreen", "false"));
            Lang.set(p.getProperty("language", "ro"));   // limba e globală, nu pe slot de salvare
        } catch (Exception ignored) { /* fișier stricat: rămân valorile implicite */ }
    }

    public static void save() {
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            Properties p = new Properties();
            p.setProperty("volume", String.valueOf(volume));
            p.setProperty("shake", String.valueOf(shake));
            p.setProperty("textSpeed", String.valueOf(textSpeed));
            p.setProperty("fullscreen", String.valueOf(fullscreen));
            p.setProperty("language", Lang.get());
            p.store(out, "Eron: setari");
        } catch (Exception ignored) { /* setările rămân doar în memorie */ }
    }

    /** Câte tick-uri între două litere în dialoguri (mai mic = mai rapid). */
    public static int dialogueTicksPerChar() { return switch (textSpeed) { case 0 -> 4; case 2 -> 1; default -> 2; }; }

    /** Înmulțitorul vitezei textului din cutscene-uri. */
    public static float cutsceneSpeedFactor() { return switch (textSpeed) { case 0 -> 0.6f; case 2 -> 1.7f; default -> 1f; }; }

    public static String textSpeedLabel() {
        return Lang.t(switch (textSpeed) { case 0 -> "settings.speed.slow"; case 2 -> "settings.speed.fast"; default -> "settings.speed.normal"; });
    }
}
