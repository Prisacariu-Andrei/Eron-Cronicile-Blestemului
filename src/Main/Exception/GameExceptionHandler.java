package Main.Exception;

import Database.DatabaseManager;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import javax.swing.JOptionPane;

public class GameExceptionHandler {

    private static GameExceptionHandler instance;
    private static final String LOG_FILE = "error_log.txt";

    private GameExceptionHandler() {}

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
        System.err.println("[WARNING] " + message + ": " + e.getMessage());
        logToFile("WARNING", message, e);
    }

    /**
     * Tratează o eroare CRITICĂ. Salvează logul, închide baza de date curat
     * și afișază o fereastră pop-up nativă înainte de a opri jocul.
     */
    public void handleCriticalError(String message, java.lang.Exception e) {
        System.err.println("[CRITICAL ERROR] " + message);
        if (e != null) e.printStackTrace();

        // 1. Logăm detaliile în fișierul text
        logToFile("CRITICAL", message, e);

        // 2. Închidem baza de date curat ca să nu corupem fișierul .db
        try {
            DatabaseManager.getInstance().close();
        } catch (java.lang.Exception ex) {
            System.err.println("Nu s-a putut închide DB în timpul crash-ului: " + ex.getMessage());
        }

        // 3. Afișăm o alertă grafică direct pe ecranul utilizatorului
        String dialogMessage = message + "\n\nDetalii: " + (e != null ? e.getMessage() : "Necunoscute")
                + "\n\nJocul se va închide în siguranță. Verifică 'error_log.txt' pentru detalii.";

        JOptionPane.showMessageDialog(null, dialogMessage, "Eroare Critică - ERON", JOptionPane.ERROR_MESSAGE);

        // 4. Oprim procesul în siguranță
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