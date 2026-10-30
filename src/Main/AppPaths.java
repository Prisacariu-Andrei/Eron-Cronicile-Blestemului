package Main;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Folderul cu datele jucătorului: %APPDATA%\Eron pe Windows (altfel ~/.eron). Salvările, setările și logul trăiesc aici,
 * nu lângă joc, ca să meargă și când jocul stă într-un folder fără drept de scriere. Se creează la prima folosire.
 * Clasa nu depinde de alte clase din joc (e folosită și de logger).
 */
public final class AppPaths {
    private static File dir;

    private AppPaths() {}

    public static synchronized File dir() {
        if (dir == null) {
            String appData = System.getenv("APPDATA");
            File d = appData != null && !appData.isBlank()
                    ? new File(appData, "Eron")
                    : new File(System.getProperty("user.home"), ".eron");
            //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
            dir = d;
        }
        return dir;
    }

    /**
     * Fișierul cu numele dat din folderul de date. Dacă nu există încă, dar există unul cu același nume în folderul de lucru
     * (salvări/setări vechi, de dinainte de mutare), îl copiază o singură dată.
     */
    public static File file(String name) {
        File target = new File(dir(), name);
        if (!target.exists()) {
            File old = new File(name);
            if (old.isFile()) {
                try { Files.copy(old.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES); } catch (Exception ignored) { /* se pornește de la zero */ }
            }
        }
        return target;
    }
}
