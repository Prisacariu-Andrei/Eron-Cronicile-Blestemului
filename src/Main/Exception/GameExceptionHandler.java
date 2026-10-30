package Main.Exception;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class GameExceptionHandler {

    private static GameExceptionHandler instance;
    private static final String LOG_FILE = new java.io.File(Main.AppPaths.dir(), "error_log.txt").getPath();   // folderul de date, nu cel al jocului
    private final java.util.Set<String> seenWarnings = new java.util.HashSet<>();

    private GameExceptionHandler() {
        // Logul pornește gol la fiecare rulare, ca să nu crească la nesfârșit.
        try (FileWriter fw = new FileWriter(LOG_FILE, false)) { /* golit */ } catch (IOException ignored) { }
    }

    public static GameExceptionHandler getInstance() {
        if (instance == null) {
            instance = new GameExceptionHandler();
        }
        return instance;
    }

    /**
     * Tratează o eroare non-critică (jocul poate continua, dar logăm problema).
     */
    public void handleWarning(String message, java.lang.Exception e) {
        // Fiecare avertisment distinct se raportează o singură dată pe rulare: altfel o resursă lipsă
        // (de ex. o imagine) scrie același stack trace pe disc la fiecare inamic creat și încetinește jocul.
        synchronized (seenWarnings) {
            if (!seenWarnings.add(message)) return;
        }
        System.err.println("[WARNING] " + message + (e != null ? ": " + e.getMessage() : ""));
        logToFile("WARNING", message, e);
    }

    /**
     * Tratează o eroare CRITICĂ. Salvează logul, închide baza de date curat
     * și afișază o fereastră pop-up nativă înainte de a opri jocul.
     */
    public void handleCriticalError(String message, Exception e) {
        // 1. Printează IMEDIAT eroarea direct în consolă, ca să știm exact ce s-a întâmplat
        System.err.println("[CRITICAL ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }

        // 2. SPARGEM RECURSIVITATEA: Dacă problema e de la SQLite, nu o mai salvăm în DB!
        if (e instanceof org.sqlite.SQLiteException || (message != null && (message.contains("Database") || message.contains("DatabaseManager")))) {
            System.err.println("⚠️ Alertă: Eroarea provine de la baza de date. Salvarea în DB a fost anulată pentru a preveni StackOverflow.");

            // Opțional: Aici poți scrie eroarea într-un fișier text simplu (txt) local, nu în DB

            // Închidem aplicația controlat, fără crash infinit
            System.exit(1);
            return;
        }

        // 3. Doar pentru alte tipuri de erori (mecanici, hărți) încercăm salvarea în DB
        try {
            // ex: DatabaseManager.getInstance().saveLog(message);
        } catch (Exception ex) {
            System.err.println("Nu s-a putut scrie logul de eroare în baza de date: " + ex.getMessage());
        }

        // Închidem jocul deoarece eroarea este catalogată drept Critică
        System.exit(1);
    }

    /**
     * Scrie detaliile erorii într-un fișier text local (error_log.txt).
     */
    private void logToFile(String level, String message, java.lang.Exception e) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {

            pw.println("=========================================================================");
            pw.println("[" + java.time.LocalDateTime.now() + "] [" + level + "]");
            pw.println("Mesaj: " + message);
            if (e != null) {
                pw.println("Tip Excepție: " + e.getClass().getName());
                pw.println("Detalii Excepție: " + e.getMessage());
                pw.println("Stack Trace:");
                e.printStackTrace(pw);
            }
            pw.println("=========================================================================\n");
        } catch (IOException ioException) {
            System.err.println("Nu s-a putut scrie în fișierul de loguri: " + ioException.getMessage());
        }
    }
}