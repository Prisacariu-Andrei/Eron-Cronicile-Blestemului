package Main;

public class GameTimer {
    private int elapsedSeconds;
    private int fpsCounter;
    private boolean isRunning; // ★ VARIABILĂ NOUĂ: Controlează dacă timpul curge sau nu

    public GameTimer() {
        this.elapsedSeconds = 0;
        this.fpsCounter = 0;
        this.isRunning = false; // Jocul începe cu timer-ul oprit (în meniu)
    }

    /**
     * Metodă apelată în update()-ul principal din GamePanel (rulează de 60 de ori pe secundă).
     */
    public void update() {
        // ★ VERIFICARE: Dacă timer-ul este oprit, ignorăm update-urile!
        if (!isRunning) {
            return;
        }

        fpsCounter++;

        // Dacă jocul rulează la 60 FPS, 60 de update-uri înseamnă exact 1 secundă
        if (fpsCounter >= 60) {
            elapsedSeconds++;
            fpsCounter = 0;
        }
    }

    // ★ METODE NOI PENTRU CONTROLUL TIMPULUI ★

    /**
     * Pornește numărătoarea timpului.
     */
    public void start() {
        this.isRunning = true;
    }

    /**
     * Oprește temporar numărătoarea timpului (util pentru Pauză sau Cutscene).
     */
    public void stop() {
        this.isRunning = false;
    }

    /**
     * Returnează dacă timerul rulează în acest moment.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Returnează timpul total în secunde (folositor pentru salvarea în baza de date).
     */
    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    /**
     * Setează timpul manual (folositor când încarci salvarea din baza de date).
     */
    public void setElapsedSeconds(int seconds) {
        this.elapsedSeconds = seconds;
        this.fpsCounter = 0; // Resetăm și fracțiunea de secundă
    }

    /**
     * Resetează timerul complet (pentru New Game).
     */
    public void reset() {
        this.elapsedSeconds = 0;
        this.fpsCounter = 0;
        this.isRunning = false;
    }

    /**
     * Returnează timpul sub formă de text formatat (ex: "05:14") perfect pentru UI.
     */
    public String getFormattedTime() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}