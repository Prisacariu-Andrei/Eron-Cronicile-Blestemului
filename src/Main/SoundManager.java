package Main;

import Main.Exception.GameExceptionHandler;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class SoundManager {

    private static SoundManager instance;

    // Clip-ul curent pentru muzica de fundal (rulare în loop)
    private Clip backgroundMusic;
    private String currentMusicPath = "";

    // Map-uri pentru a stoca rapid căile către fișierele noastre
    private final Map<String, URL> musicFiles = new HashMap<>();
    private final Map<String, URL> seFiles = new HashMap<>();

    private SoundManager() {
        loadSounds();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Înregistrăm toate fișierele audio din proiect.
     */
    private void loadSounds() {
        try {
            // --- 1. SOUNDTRACK-URI ZONE ---
            registerMusic("Eldoria", "/sound/music/eldoria.wav");
            registerMusic("Barlogul", "/sound/music/eldoria.wav");
            registerMusic("Abisul", "/sound/music/eldoria.wav");
            registerMusic("Citadela", "/sound/music/eldoria.wav");
            registerMusic("Tundra", "/sound/music/eldoria.wav");
            registerMusic("Podul", "/sound/music/eldoria.wav");
            registerMusic("Castelul", "/sound/music/eldoria.wav");

            // --- 2. SOUNDTRACK-URI INAMICI ---
            registerMusic("InamiciEldoria", "/sound/music/inamic_eldoria.wav");
            registerMusic("InamiciBarlog", "/sound/music/inamic_barlog.wav");
            registerMusic("InamiciAbisul", "/sound/music/inamic_abisul.wav");
            registerMusic("InamiciCitadela", "/sound/music/inamic_citadela.wav");
            registerMusic("InamiciTundra", "/sound/music/inamic_tundra.wav");
            registerMusic("InamiciPodul", "/sound/music/inamic_podul.wav");
            registerMusic("InamiciCastelull", "/sound/music/inamic_castelul.wav");

            // --- 3. EFECTE SONORE (SE) ---
            registerSE("click", "/sound/se/click.wav");
            registerSE("hit", "/sound/se/hit.wav");

        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la încărcarea fișierelor audio de bază.", e);
        }
    }

    private void registerMusic(String key, String path) {
        URL url = getClass().getResource(path);
        if (url != null) musicFiles.put(key, url);
        else System.err.println("⚠️ [SOUND] Muzica nu a fost găsită: " + path);
    }

    private void registerSE(String key, String path) {
        URL url = getClass().getResource(path);
        if (url != null) seFiles.put(key, url);
        else System.err.println("⚠️ [SOUND] Efectul SE nu a fost găsit: " + path);
    }

    // =========================================================================
    // LOGICĂ PENTRU MUZICĂ DE FUNDAL (SoundTracks)
    // =========================================================================

    /**
     * Pornește o melodie în loop. Dacă melodia rulează deja, nu o reia de la zero.
     */
    public void playMusic(String key) {
        if (currentMusicPath.equals(key)) return;

        stopMusic();

        URL url = musicFiles.get(key);
        if (url == null) {
            System.err.println("⚠️ [SOUND] Nu există muzică mapată pentru cheia: " + key);
            return;
        }

        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(ais);

            // --- FORȚARE VOLUM MAXIM ---
            if (backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(gainControl.getMaximum()); // Forțează volumul maxim din Java
            }

            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
            currentMusicPath = key;
            System.out.println("✅ [SOUND] Se redă muzica de fundal pentru: " + key);
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut reda muzica: " + key, e);
        }
    }

    /**
     * Redă un efect sonor scurt o singură dată.
     */
    public void playSE(String key) {
        URL url = seFiles.get(key);
        if (url == null) return;

        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip seClip = AudioSystem.getClip();
            seClip.open(ais);

            // --- FORȚARE VOLUM MAXIM ---
            if (seClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) seClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(gainControl.getMaximum());
            }

            seClip.start();
            System.out.println("🔊 [SOUND] Efect redat: " + key);

            seClip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    seClip.close();
                }
            });
        } catch (Exception e) {
            System.err.println("Eroare redare SE: " + e.getMessage());
        }
    }

    /**
     * Oprește instantaneu muzica de fundal curentă.
     */
    public void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
            backgroundMusic.close();
        }
        currentMusicPath = "";
    }
}