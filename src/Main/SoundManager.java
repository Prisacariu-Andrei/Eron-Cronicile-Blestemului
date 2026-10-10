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
            registerMusic("Barlogul", "/sound/music/barlog.wav");
            registerMusic("Abisul", "/sound/music/abisul.wav");
            registerMusic("Citadela", "/sound/music/citadela.wav");
            registerMusic("Tundra", "/sound/music/tundra.wav");
            registerMusic("Podul", "/sound/music/podul.wav");
            registerMusic("Castelul", "/sound/music/castel.wav");
            registerMusic("MeniuPrincipal", "/sound/music/meniu.wav");

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
                gainControl.setValue(gainFor(gainControl)); // volumul maxim al sistemului, înmulțit cu setarea jucătorului (T)
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
    /** Volumul jucătorului (Settings.volume, 0..1) în decibeli: 0 dB la 100%, tot mai jos cu cât scazi. */
    private static float volumeDb() {
        float v = Settings.volume;
        return v <= 0.001f ? -80f : (float) (20 * Math.log10(v));
    }

    private static float gainFor(FloatControl c) {
        return Math.max(c.getMinimum(), Math.min(c.getMaximum(), c.getMaximum() + volumeDb()));
    }

    /** Aplică imediat volumul din setări muzicii care rulează (efectele îl iau la următoarea redare). */
    public void applyVolume() {
        if (backgroundMusic != null && backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gc = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
            gc.setValue(gainFor(gc));
        }
    }

    public void playSE(String key) {

        URL url = seFiles.get(key);
        if (url == null) {
            // Nu există fișier pentru acest efect: folosim varianta creată în cod (vezi mai jos)
            playSynth(key);
            return;
        }

        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip seClip = AudioSystem.getClip();
            seClip.open(ais);

            // --- FORȚARE VOLUM MAXIM ---
            if (seClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) seClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(gainFor(gainControl));
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

    // =========================================================================
    // EFECTE SONORE CREATE ÎN COD
    // Proiectul nu are fișiere de efecte (res/sound/se), așa că sunetele de feedback (pași, lovituri, bipuri
    // de text etc.) sunt generate din unde simple. Dacă adaugi un fișier "res/sound/se/<nume>.wav" și îl
    // înregistrezi în loadSounds(), el are prioritate. Volumul: GameFeel.SE_VOLUME.
    // =========================================================================

    private static final float SYNTH_RATE = 22050f;
    private final Map<String, Clip> synthClips = new HashMap<>();
    private final Map<String, Integer> synthVariant = new HashMap<>();
    private boolean synthUnavailable = false; // fără placă de sunet / linii audio: rămânem fără sunet, fără erori

    private interface Wave { double at(double t, double u); } // t = secunde, u = progres 0..1

    private synchronized void playSynth(String key) {
        if (synthUnavailable || !GameFeel.SOUND_FEEDBACK) return;

        // Câteva sunete au variante, ca să nu sune mereu identic (ex. bipul literelor)
        String clipKey = key;
        if (key.equals("blip") || key.equals("step")) {
            int v = synthVariant.merge(key, 1, Integer::sum) % 3;
            clipKey = key + v;
        }

        try {
            Clip clip = synthClips.get(clipKey);
            if (clip == null) {
                short[] samples = synthesize(clipKey);
                if (samples == null) return; // cheie necunoscută
                byte[] bytes = new byte[samples.length * 2];
                for (int i = 0; i < samples.length; i++) {
                    bytes[i * 2] = (byte) (samples[i] & 0xFF);
                    bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
                }
                javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(SYNTH_RATE, 16, 1, true, false);
                clip = AudioSystem.getClip();
                clip.open(fmt, bytes, 0, bytes.length);
                synthClips.put(clipKey, clip);
            }
            clip.stop();
            clip.setFramePosition(0);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gc.setValue(Math.max(gc.getMinimum(), Math.min(gc.getMaximum(), volumeDb())));
            }
            clip.start();
        } catch (Exception | LinkageError e) {
            synthUnavailable = true; // o singură încercare, apoi renunțăm în liniște
            System.err.println("⚠️ [SOUND] Efectele create în cod nu pot fi redate: " + e.getMessage());
        }
    }

    /** Generează un efect (PCM 16 biți mono). Returnează null dacă nu cunoaștem numele. */
    public short[] synthesize(String key) {
        java.util.Random rnd = new java.util.Random(key.hashCode()); // același zgomot de fiecare dată
        double[] lp = {0}; // stare pentru zgomotul filtrat
        switch (key) {
            case "click":
                return render(0.035, 0.5, (t, u) -> sine(1500 + 500 * u, t) * decay(u, 7));
            case "blip0": case "blip1": case "blip2": {
                double f = 640 + 110 * (key.charAt(4) - '0');
                return render(0.04, 0.32, (t, u) -> triangle(f, t) * decay(u, 4));
            }
            case "step0": case "step1": case "step2": {
                double f = 80 + 14 * (key.charAt(4) - '0');
                return render(0.07, 0.34, (t, u) -> {
                    lp[0] += 0.35 * ((rnd.nextDouble() * 2 - 1) - lp[0]);
                    return (lp[0] * 0.7 + sine(f, t)) * decay(u, 9);
                });
            }
            case "dash":
                return render(0.24, 0.42, (t, u) -> {
                    lp[0] += (0.06 + 0.4 * Math.sin(Math.PI * u)) * ((rnd.nextDouble() * 2 - 1) - lp[0]);
                    return (lp[0] * 1.4 + 0.25 * sweepSine(260, 900, 0.24, t)) * Math.sin(Math.PI * Math.pow(u, 0.6));
                });
            case "hit":
                return render(0.17, 0.62, (t, u) -> {
                    lp[0] += 0.5 * ((rnd.nextDouble() * 2 - 1) - lp[0]);
                    return (lp[0] * 0.6 * decay(u, 12) + sweepSine(160, 55, 0.17, t) * decay(u, 5));
                });
            case "hurt":
                return render(0.28, 0.7, (t, u) -> {
                    lp[0] += 0.3 * ((rnd.nextDouble() * 2 - 1) - lp[0]);
                    double body = sweepSine(120, 42, 0.28, t) * decay(u, 4);
                    return Math.tanh(1.8 * (body + lp[0] * 0.5 * decay(u, 10)));
                });
            case "block":
                return render(0.15, 0.5, (t, u) -> (sine(880, t) + 0.7 * sine(1330, t) + 0.4 * sine(2110, t)) * decay(u, 9) * 0.45
                        + (rnd.nextDouble() * 2 - 1) * 0.25 * decay(u, 40));
            case "levelup": // cinci note care urcă, cu o strălucire la final
                return render(0.9, 0.45, (t, u) -> arpeggio(new double[]{392.0, 523.25, 659.25, 783.99, 1046.5}, 0.12, t, u)
                        + 0.25 * sine(1568, t) * Math.max(0, (t - 0.55)) * Math.exp(-5 * (t - 0.55)) * 4);
            case "ability": // un fâșâit scurt care urcă, cu un clinchet
                return render(0.26, 0.5, (t, u) -> sweepSine(380, 1300, 0.26, t) * Math.sin(Math.PI * u) * 0.7
                        + 0.3 * sine(1760, t) * Math.exp(-9 * Math.max(0, t - 0.1)) * (t > 0.1 ? 1 : 0));
            case "coin": // două note scurte și strălucitoare, ca un clinchet de monedă
                return render(0.22, 0.45, (t, u) -> {
                    double f = t < 0.07 ? 1318.5 : 1760.0;
                    double local = t < 0.07 ? t : t - 0.07;
                    return (sine(f, t) + 0.35 * sine(f * 2.01, t)) * Math.min(1, local / 0.004) * Math.exp(-14 * local);
                });
            case "deny": // un bâzâit scurt și jos: "nu se poate"
                return render(0.2, 0.5, (t, u) -> (Math.signum(sine(110, t)) * 0.5 + sine(82, t) * 0.5) * (1 - u) * (t % 0.1 < 0.07 ? 1 : 0.2));
            case "heal":
                return render(0.62, 0.42, (t, u) -> arpeggio(new double[]{523.25, 659.25, 783.99}, 0.16, t, u));
            case "reward":
                return render(0.75, 0.42, (t, u) -> arpeggio(new double[]{523.25, 659.25, 783.99, 1046.5}, 0.14, t, u));
            case "battle":
                return render(0.55, 0.5, (t, u) -> {
                    double trem = 0.75 + 0.25 * Math.sin(2 * Math.PI * 22 * t);
                    return (sweepSine(1000, 110, 0.55, t) * 0.8 + sine(55, t) * 0.5) * trem * Math.sin(Math.PI * Math.pow(u, 0.5)) * (1 - u * 0.4);
                });
            case "phase":
                return render(0.95, 0.6, (t, u) -> {
                    lp[0] += 0.08 * ((rnd.nextDouble() * 2 - 1) - lp[0]);
                    return (sine(52, t) * 0.8 + sweepSine(90, 700, 0.95, t) * 0.45 * u + lp[0] * 1.3) * Math.sin(Math.PI * u);
                });
            default:
                return null;
        }
    }

    private static short[] render(double seconds, double gain, Wave wave) {
        int n = (int) (seconds * SYNTH_RATE);
        short[] out = new short[n];
        int fade = (int) (0.004 * SYNTH_RATE); // 4 ms de fade la final, ca să nu pocnească
        for (int i = 0; i < n; i++) {
            double t = i / (double) SYNTH_RATE, u = i / (double) n;
            double v = wave.at(t, u) * gain * GameFeel.SE_VOLUME;
            if (i > n - fade) v *= (n - i) / (double) fade;
            out[i] = (short) (Math.max(-1, Math.min(1, v)) * 32767);
        }
        return out;
    }

    private static double sine(double f, double t) { return Math.sin(2 * Math.PI * f * t); }
    private static double triangle(double f, double t) { double p = (f * t) % 1.0; return 4 * Math.abs(p - 0.5) - 1; }
    private static double decay(double u, double speed) { return Math.exp(-speed * u); }

    /** Sinus a cărui frecvență alunecă de la f0 la f1 pe durata "dur" (faza e integrată corect). */
    private static double sweepSine(double f0, double f1, double dur, double t) {
        return Math.sin(2 * Math.PI * (f0 * t + (f1 - f0) * t * t / (2 * dur)));
    }

    /** Note una după alta, fiecare cu atac moale și stingere. */
    private static double arpeggio(double[] notes, double noteLen, double t, double u) {
        int idx = Math.min(notes.length - 1, (int) (t / noteLen));
        double local = t - idx * noteLen;
        double env = Math.min(1, local / 0.012) * Math.exp(-3.2 * local / noteLen) * (idx == notes.length - 1 ? 1 : 0.85);
        return (sine(notes[idx], t) + 0.3 * sine(notes[idx] * 2, t)) * env * (1 - u * 0.35);
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