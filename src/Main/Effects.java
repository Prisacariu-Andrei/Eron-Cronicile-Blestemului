package Main;

import GameState.*;

import java.awt.*;
import java.util.Random;

/**
 * Punctul central pentru "game feel": screen shake, hit-stop, fade-uri între scene, particule și ambianța fiecărei zone.
 * Restul codului nu face decât să spună ce s-a întâmplat (gp.fx.playerHurt(15)), iar aici se decide cum arată.
 * Valorile de reglaj sunt în GameFeel.
 *
 * Rulează pe pașii fixi de 60 de ticks pe secundă ai jocului, deci nu depinde de FPS.
 */
public class Effects {
    private final GamePanel gp;
    public final ParticleSystem particles = new ParticleSystem();
    private final Random rng = new Random();

    /** Contor de ticks (crește doar când jocul nu e înghețat), pentru animațiile idle. */
    public int ticks;

    // --- screen shake ---
    private float shakeStrength;
    private int shakeLeft, shakeTotal;
    private volatile int shakeX, shakeY;
    private int appliedX, appliedY;

    // --- hit-stop ---
    private int hitStopLeft;

    // --- fade / flash peste tot ecranul ---
    private int fadeLeft, fadeTotal, fadeRgb;
    private float fadeAlpha;

    // --- ambianță ---
    private String ambientZone = "";

    // --- camera de la tick-ul trecut, pentru paralaxă ---
    private int lastCamX, lastCamY;

    public Effects(GamePanel gp) {
        this.gp = gp;
    }

    // =====================================================================================
    // COMENZI (apelate din restul jocului)
    // =====================================================================================

    public void shake(float strength, int tickCount) {
        strength = Math.min(strength, GameFeel.SHAKE_MAX);
        if (strength <= 0 || tickCount <= 0) return;
        if (strength >= currentShake()) { // nu înlocuim un shake mai puternic cu unul mai slab
            shakeStrength = strength;
            shakeTotal = tickCount;
            shakeLeft = tickCount;
        }
    }

    private float currentShake() {
        if (shakeLeft <= 0) return 0;
        float r = (float) shakeLeft / shakeTotal;
        return shakeStrength * r * r;
    }

    /** Îngheață jocul pentru câteva cadre. */
    public void hitStop(int tickCount) {
        hitStopLeft = Math.max(hitStopLeft, tickCount);
    }

    private void hitStopForDamage(int damage) {
        if (damage >= GameFeel.HIT_STOP_BIG_DAMAGE) hitStop(GameFeel.HIT_STOP_BIG_TICKS);
        else if (damage >= GameFeel.HIT_STOP_MIN_DAMAGE) hitStop(GameFeel.HIT_STOP_TICKS);
    }

    /** Acoperă ecranul cu o culoare care dispare treptat (pentru tranziții și flash-uri). */
    public void fade(int rgb, float startAlpha, int tickCount) {
        if (tickCount <= 0 || startAlpha <= 0) return;
        fadeRgb = rgb;
        fadeAlpha = startAlpha;
        fadeTotal = tickCount;
        fadeLeft = tickCount;
    }

    /** Eron a fost lovit în luptă. */
    public void playerHurt(int damage) {
        shake(GameFeel.SHAKE_PLAYER_HIT_BASE + damage * GameFeel.SHAKE_PER_DAMAGE, GameFeel.SHAKE_HIT_TICKS);
        hitStopForDamage(damage);
        sparks(ParticleSystem.FRONT, gp.battleUI.playerCenterX(), gp.battleUI.playerCenterY(),
                GameFeel.HIT_SPARKS + damage / 3, 0xFF4A4A, 7f);
        fade(0xB01020, Math.min(0.40f, 0.12f + damage * 0.01f), 12);
        gp.battleUI.onPlayerHit();
        sound("hurt");
    }

    /** Scutul a oprit o lovitură: un tremur mic și un sunet metalic, fără scântei roșii. */
    public void blocked() {
        shake(2.2f, 8);
        sparks(ParticleSystem.FRONT, gp.battleUI.playerCenterX() + 30, gp.battleUI.playerCenterY(), 7, 0x9FD4FF, 5f);
        sound("block");
    }

    /** Eron a lovit inamicul. */
    public void enemyHurt(int damage) {
        shake(GameFeel.SHAKE_ENEMY_HIT_BASE + damage * 0.08f, 10);
        hitStopForDamage(damage);
        sparks(ParticleSystem.FRONT, gp.battleUI.enemyCenterX(), gp.battleUI.enemyCenterY(),
                GameFeel.HIT_SPARKS + damage / 3, 0xFFD36B, 8f);
        gp.battleUI.onEnemyHit();
        sound("hit");
    }

    /** O reacție sau un combo de cărți: scuturat scurt, explozie de particule în culoarea elementului, un clinchet. */
    public void reaction(int rgb, int x, int y, boolean main) {
        if (main) {
            shake(GameFeel.SHAKE_REACTION, GameFeel.SHAKE_REACTION_TICKS);
            sound("ability");
        }
        burst(ParticleSystem.FRONT, x, y, GameFeel.REACTION_PARTICLES, rgb, 7f, 40, 6f, -0.02f);
        sparks(ParticleSystem.FRONT, x, y, 8, 0xFFFFFF, 6f);
    }

    /** Inamicul a oprit lovitura cu scutul. */

    public void enemyBlocked() {
        shake(1.8f, 7);
        sparks(ParticleSystem.FRONT, gp.battleUI.enemyCenterX() - 30, gp.battleUI.enemyCenterY(), 7, 0x9FD4FF, 5f);
        sound("block");
    }

    /** Un boss își schimbă faza. */
    public void phaseChange() {
        shake(GameFeel.SHAKE_PHASE_CHANGE, GameFeel.SHAKE_PHASE_TICKS);
        hitStop(GameFeel.HIT_STOP_BIG_TICKS);
        fade(0xFFFFFF, 0.8f, 30);
        burst(ParticleSystem.FRONT, gp.battleUI.enemyCenterX(), gp.battleUI.enemyCenterY(), 40, 0xC58BFF, 9f, 50, 7f, 0.05f);
        sound("phase");
    }

    /** Eron a călcat pe spike-uri pe hartă. */
    public void mapHurt(int damage) {
        shake(GameFeel.SHAKE_SPIKE, 14);
        hitStopForDamage(damage);
        float px = gp.player.worldX + gp.tileSize / 2f, py = gp.player.worldY + gp.tileSize * 0.7f;
        burst(ParticleSystem.WORLD, px, py, 14, 0xE04848, 4f, 26, 5f, 0.12f);
        fade(0xB01020, 0.25f, 12);
        sound("hurt");
    }

    /** Eron s-a vindecat pe hartă (focul de tabără). */
    public void healSparkle() {
        float px = gp.player.worldX + gp.tileSize / 2f, py = gp.player.worldY + gp.tileSize * 0.6f;
        for (int i = 0; i < 22; i++) {
            particles.spawn(ParticleSystem.WORLD, ParticleSystem.GLOW,
                    px + (rng.nextFloat() - 0.5f) * 40, py + (rng.nextFloat() - 0.5f) * 20,
                    (rng.nextFloat() - 0.5f) * 0.6f, -0.5f - rng.nextFloat() * 1.1f, -0.01f, 0.98f,
                    40 + rng.nextInt(30), 3f, 2f, 0.9f, i % 3 == 0 ? 0xE8FFB0 : 0x6BFF9A);
        }
        sound("heal");
    }

    /** Acțiune refuzată (nu ai destulă esență / nivel): sunet scurt și jos. */
    public void denied() {
        sound("deny");
    }

    /** Nivel nou: explozie aurie în jurul lui Eron, flash cald, un mic tremur și sunet. */
    public void levelUp() {
        if (gp.getCurrentState() instanceof BattleState) {
            burst(ParticleSystem.FRONT, gp.battleUI.playerCenterX(), gp.battleUI.playerCenterY(), 48, 0xFFE27A, 9f, 64, 7f, 0.04f);
        } else {
            burst(ParticleSystem.WORLD, gp.player.worldX + gp.tileSize / 2f, gp.player.worldY + gp.tileSize / 3f, 48, 0xFFE27A, 7f, 64, 7f, 0.03f);
        }
        fade(0xFFF2B0, 0.55f, 26);
        shake(4f, 14);
        sound("levelup");
    }

    /** O abilitate folosită în luptă: scântei și un val de culoarea ei din jurul lui Eron (și pe inamic, dacă îl lovește). */
    public void abilityCast(int rgb, boolean onEnemy) {
        int px = gp.battleUI.playerCenterX(), py = gp.battleUI.playerCenterY();
        sparks(ParticleSystem.FRONT, px, py, 10, rgb, 6f);
        burst(ParticleSystem.FRONT, px, py, 16, rgb, 4f, 38, 6f, -0.02f);
        if (onEnemy) sparks(ParticleSystem.FRONT, gp.battleUI.enemyCenterX(), gp.battleUI.enemyCenterY(), 10, rgb, 6f);
        sound("ability");
    }

    /** Abilitatea unui aliat: scântei la aliat și, dacă lovește, la inamic. */
    public void allyCast(int rgb, int ax, int ay, boolean onEnemy) {
        sparks(ParticleSystem.FRONT, ax, ay, 10, rgb, 6f);
        burst(ParticleSystem.FRONT, ax, ay, 14, rgb, 4f, 36, 6f, -0.02f);
        if (onEnemy) sparks(ParticleSystem.FRONT, gp.battleUI.enemyCenterX(), gp.battleUI.enemyCenterY(), 10, rgb, 6f);
        sound("ability");
    }

    public void allyHurt(int ax, int ay) {
        burst(ParticleSystem.FRONT, ax, ay, 12, 0xE04848, 4f, 26, 5f, 0.12f);
        sound("hit");
    }

    /** Sunet pentru evenimente de misiune: ușor la progres, plin la îndeplinire/predare. */
    public void questSound(boolean big) {
        sound(big ? "reward" : "coin");
    }

    /** Un obiect de misiune a fost cules. */
    public void pickupBurst(float wx, float wy, int rgb) {
        burst(ParticleSystem.WORLD, wx, wy, 16, rgb, 3.5f, 32, 5f, -0.03f);
    }

    /** Scânteie care urcă dintr-un obiect de cules, ca să se observe de departe. */
    public void pickupGlint(float wx, float wy, int rgb) {
        particles.spawn(ParticleSystem.WORLD, ParticleSystem.GLOW, wx + (rng.nextFloat() - 0.5f) * 30, wy + 8,
                (rng.nextFloat() - 0.5f) * 0.3f, -0.4f - rng.nextFloat() * 0.4f, -0.005f, 0.99f, 44, 3f, 1f, 0.9f, rgb);
    }

    /** Explozie aurie (recompensă aleasă, runa primită). */
    public void rewardBurst(int screenX, int screenY) {
        burst(ParticleSystem.FRONT, screenX, screenY, GameFeel.REWARD_BURST, 0xFFD36B, 7f, 46, 6f, 0.08f);
        sound("reward");
    }

    /** Un nor de praf în urma lui Eron. */
    public void dust(float worldX, float worldY, float dirX, float dirY, int n) {
        if (!GameFeel.DUST_ENABLED) return;
        int rgb = dustColor(gp.tileM.currentZone);
        for (int i = 0; i < n; i++) {
            particles.spawn(ParticleSystem.WORLD, ParticleSystem.CIRCLE,
                    worldX + (rng.nextFloat() - 0.5f) * 14, worldY + (rng.nextFloat() - 0.5f) * 6,
                    -dirX * (0.4f + rng.nextFloat() * 0.7f) + (rng.nextFloat() - 0.5f) * 0.6f,
                    -dirY * (0.4f + rng.nextFloat() * 0.7f) - 0.25f - rng.nextFloat() * 0.35f,
                    0f, 0.93f, 20 + rng.nextInt(14), 5f, 14f, 0.62f, rgb);
        }
    }

    /** Dâră luminoasă în urma dash-ului. */
    public void dashTrail(float x, float y, float dirX, float dirY) {
        for (int i = 0; i < 3; i++) {
            particles.spawn(ParticleSystem.WORLD, ParticleSystem.STREAK,
                    x + (rng.nextFloat() - 0.5f) * 22, y + (rng.nextFloat() - 0.5f) * 34,
                    -dirX * (4f + rng.nextFloat() * 4f), -dirY * (4f + rng.nextFloat() * 4f),
                    0f, 0.92f, 12 + rng.nextInt(8), 3f, 1f, 0.95f, 0x9BD7FF);
        }
        particles.spawn(ParticleSystem.WORLD, ParticleSystem.GLOW, x, y + (rng.nextFloat() - 0.5f) * 20,
                -dirX * 1.2f, -dirY * 1.2f, 0f, 0.9f, 16, 4f, 2f, 0.9f, 0xCFEFFF);
    }

    private static int dustColor(String zone) {
        return switch (zone) {
            case "Eldoria" -> 0xA8A07A;
            case "Barlogul" -> 0xB89A78;
            case "Abisul" -> 0x8C8F96;
            case "Citadela" -> 0xC2A27A;
            case "Tundra" -> 0xF2F8FF;
            case "Podul" -> 0x9C93B0;
            case "Castelul" -> 0x7A5560;
            default -> 0xAAAAAA;
        };
    }

    /** Scântei care țâșnesc în toate direcțiile, cu dâră. */
    public void sparks(int layer, float x, float y, int n, int rgb, float speed) {
        for (int i = 0; i < n; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            float sp = speed * (0.35f + rng.nextFloat() * 0.65f);
            particles.spawn(layer, ParticleSystem.STREAK, x, y,
                    (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp - 1.5f, 0.28f, 0.93f,
                    16 + rng.nextInt(14), 2f, 1f, 1f, rgb);
        }
    }

    /** Explozie de cercuri care cad încet. */
    public void burst(int layer, float x, float y, int n, int rgb, float speed, int life, float size, float gravity) {
        for (int i = 0; i < n; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            float sp = speed * (0.25f + rng.nextFloat() * 0.75f);
            particles.spawn(layer, i % 2 == 0 ? ParticleSystem.GLOW : ParticleSystem.CIRCLE, x, y,
                    (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp, gravity, 0.92f,
                    life / 2 + rng.nextInt(life / 2 + 1), size, size * 0.3f, 0.95f, rgb);
        }
    }

    private void sound(String key) {
        if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE(key);
    }

    // =====================================================================================
    // SCHIMBĂRI DE STARE
    // =====================================================================================

    private static boolean isOverlay(GameState s) {
        return s instanceof PauseState || s instanceof InventoryState || s instanceof DialogueState
                || s instanceof QuestGameState || s instanceof ShopState || s instanceof DebugState;
    }

    /** Apelată de GamePanel la fiecare changeState. Decide ce tranziție vizuală se potrivește. */
    public void onStateChanged(GameState from, GameState to) {
        if (to == null) return;
        if (to instanceof BattleState) {
            fade(0x6A3FB5, 0.95f, GameFeel.FADE_BATTLE_START_TICKS);   // flash "mistic" violet
            shake(GameFeel.SHAKE_BATTLE_START, GameFeel.SHAKE_BATTLE_START_TICKS);
            particles.clearLayer(ParticleSystem.WORLD);
            sound("battle");
        } else if (to instanceof DeathState) {
            shake(GameFeel.SHAKE_DEATH, 30);
            fade(0x8B0000, 0.8f, 50);
        } else if (from instanceof BattleState) {
            fade(0x000000, 0.9f, GameFeel.FADE_BATTLE_END_TICKS);
            particles.clearLayer(ParticleSystem.FRONT);
        } else if (from == to) {
            fade(0x000000, 1f, GameFeel.FADE_STATE_TICKS);              // schimbare de hartă
            particles.clear();
        } else if (!isOverlay(from) && !isOverlay(to)) {
            fade(0x000000, 1f, GameFeel.FADE_STATE_TICKS);              // meniu, cutscene, joc etc.
        }
    }

    // =====================================================================================
    // UPDATE (un tick)
    // =====================================================================================

    /** True cât timp jocul e înghețat de un hit-stop. GamePanel sare peste tot restul update-ului. */
    public boolean consumeHitStop() {
        if (hitStopLeft > 0) {
            hitStopLeft--;
            return true;
        }
        return false;
    }

    public void update() {
        ticks++;

        if (shakeLeft > 0) {
            shakeLeft--;
            float amp = currentShake();
            shakeX = Math.round((rng.nextFloat() * 2 - 1) * amp);
            shakeY = Math.round((rng.nextFloat() * 2 - 1) * amp);
        } else {
            shakeX = 0;
            shakeY = 0;
        }

        if (fadeLeft > 0) fadeLeft--;

        int camX = gp.camera.getX(), camY = gp.camera.getY();
        float dx = camX - lastCamX, dy = camY - lastCamY;
        if (Math.abs(dx) > 400 || Math.abs(dy) > 400) { dx = 0; dy = 0; } // teleportare, nu mișcare
        lastCamX = camX;
        lastCamY = camY;

        updateAmbient();
        particles.update(dx, dy);
    }

    // =====================================================================================
    // DESENARE
    // =====================================================================================

    public void beginFrame(Graphics2D g2) {
        appliedX = shakeX;
        appliedY = shakeY;
        if (appliedX != 0 || appliedY != 0) g2.translate(appliedX, appliedY);
    }

    public void endFrame(Graphics2D g2) {
        if (appliedX != 0 || appliedY != 0) g2.translate(-appliedX, -appliedY);
    }

    public void drawWorld(Graphics2D g2) {
        gp.pickups.draw(g2); // obiectele de misiune stau sub particule
        particles.draw(g2, ParticleSystem.WORLD, gp.camera.getX(), gp.camera.getY(), gp.screenWidth, gp.screenHeight);
    }

    public void drawAmbient(Graphics2D g2) {
        particles.draw(g2, ParticleSystem.AMBIENT, 0, 0, gp.screenWidth, gp.screenHeight);
    }

    /**
     * În Tundra ceața e aproape albă și ar acoperi fulgii, așa că ei se desenează PESTE ceață (UI.draw o face).
     * În celelalte zone (ex. Abisul) ambianța stă sub ceață, deci se vede doar în lumina lui Eron.
     */
    public boolean ambientAboveFog() {
        return "Tundra".equals(gp.tileM.currentZone);
    }

    public void drawFront(Graphics2D g2) {
        particles.draw(g2, ParticleSystem.FRONT, 0, 0, gp.screenWidth, gp.screenHeight);
    }

    public void drawOverlay(Graphics2D g2) {
        if (fadeLeft <= 0) return;
        float t = 1f - (float) fadeLeft / fadeTotal;
        float a = fadeAlpha * (1f - Easing.outCubic(t));
        if (a <= 0.004f) return;
        g2.setColor(new Color((fadeRgb >> 16) & 255, (fadeRgb >> 8) & 255, fadeRgb & 255, Math.min(255, (int) (a * 255))));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }

    /** Oprește tot ce e în desfășurare (început de joc nou, sfârșit de cutscene). */
    public void clearAll() {
        particles.clear();
        shakeLeft = 0;
        shakeX = 0;
        shakeY = 0;
        hitStopLeft = 0;
        fadeLeft = 0;
        ambientZone = "";
    }

    // =====================================================================================
    // AMBIANȚĂ: ceva care se mișcă mereu în fundal, diferit în fiecare zonă
    // =====================================================================================

    private void updateAmbient() {
        if (!GameFeel.AMBIENT_ENABLED || GameFeel.AMBIENT_DENSITY <= 0) return;

        GameState s = gp.getCurrentState();
        boolean worldVisible = s instanceof PlayState || isOverlay(s) || s instanceof CutsceneState;
        boolean menu = s instanceof MenuState;
        if (!worldVisible && !menu) return;

        String zone = menu ? "Menu" : gp.tileM.currentZone;
        boolean newZone = !zone.equals(ambientZone);
        if (newZone) {
            ambientZone = zone;
            particles.clearLayer(ParticleSystem.AMBIENT);
        }

        int target = Math.round(ambientTarget(zone) * GameFeel.AMBIENT_DENSITY);
        int have = particles.countLayer(ParticleSystem.AMBIENT);
        // La intrarea într-o zonă umplem ecranul de la început; apoi adăugăm câteva pe tick, de la margini
        int perTick = newZone ? target : 2;
        for (int k = 0; k < perTick && have < target; k++, have++) spawnAmbient(zone, newZone);
    }

    private static int ambientTarget(String zone) {
        return switch (zone) {
            case "Eldoria" -> 46;
            case "Barlogul" -> 26;
            case "Abisul" -> 44;
            case "Citadela" -> 26;
            case "Tundra" -> 150;
            case "Podul" -> 34;
            case "Castelul" -> 48;
            case "Menu" -> 50;
            default -> 0;
        };
    }

    private float rand(float min, float max) { return min + rng.nextFloat() * (max - min); }

    private int pick(int... options) { return options[rng.nextInt(options.length)]; }

    private void spawnAmbient(String zone, boolean scatter) {
        float W = gp.screenWidth, H = gp.screenHeight;
        ParticleSystem p = particles;
        int i;
        switch (zone) {
            case "Eldoria" -> {
                if (rng.nextInt(3) == 0) { // polen
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.CIRCLE, rand(0, W), rand(0, H),
                            rand(-0.2f, 0.35f), rand(-0.25f, 0.1f), 0, 1f, 420 + rng.nextInt(300), 2.2f, 2f, 0.5f, 0xFFF6D0);
                } else { // licurici
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.GLOW, rand(0, W), rand(0, H),
                            rand(-0.2f, 0.2f), rand(-0.15f, 0.1f), 0, 1f, 360 + rng.nextInt(360), 4.4f, 3.4f, 1f, pick(0xFFE9A0, 0xFFD36B, 0xCFFF9A));
                }
                p.setWobble(i, 0.25f, 0.04f);
                p.setDepth(i, rand(0.5f, 1f));
            }
            case "Barlogul" -> { // frunze duse de vânt
                i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.SQUARE, scatter ? rand(0, W) : rand(-60, W * 0.4f), scatter ? rand(0, H) : rand(-30, 0),
                        rand(0.5f, 1.5f), rand(0.5f, 1.2f), 0, 1f, 420 + rng.nextInt(300), 5f, 4f, 0.85f, pick(0xC8782E, 0x9A5B25, 0xA8A03A, 0xB4552B));
                p.setWobble(i, 0.45f, 0.07f);
                p.setDepth(i, rand(0.6f, 1f));
            }
            case "Abisul" -> { // praf în lumina torței, rar câte o scânteie
                if (rng.nextInt(7) == 0) {
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.GLOW, rand(0, W), scatter ? rand(0, H) : H + 10,
                            rand(-0.15f, 0.15f), rand(-0.7f, -0.25f), 0, 1f, 260 + rng.nextInt(200), 3.8f, 2.4f, 1f, 0xFF8A3D);
                } else {
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.CIRCLE, rand(0, W), scatter ? rand(0, H) : H + 10,
                            rand(-0.1f, 0.1f), rand(-0.35f, -0.08f), 0, 1f, 420 + rng.nextInt(400), 3.4f, 3f, 0.85f, 0xE6EAF0);
                }
                p.setWobble(i, 0.2f, 0.05f);
                p.setDepth(i, rand(0.5f, 1f));
            }
            case "Citadela" -> { // scântei de la focurile hanului
                i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.GLOW, rand(0, W), scatter ? rand(0, H) : H + 10,
                        rand(-0.2f, 0.25f), rand(-1.1f, -0.45f), 0, 1f, 170 + rng.nextInt(140), 3.2f, 1.5f, 0.9f, pick(0xFF9A3C, 0xFFC25A, 0xFF6A2B));
                p.setWobble(i, 0.3f, 0.09f);
                p.setDepth(i, rand(0.6f, 1f));
            }
            case "Tundra" -> { // ninsoare purtată de vânt
                if (rng.nextInt(12) == 0) {
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.GLOW, rand(0, W), rand(0, H),
                            0, 0, 0, 1f, 40 + rng.nextInt(40), 2.5f, 1f, 0.95f, 0x8FB8E8);
                } else {
                    float size = rand(3f, 7.5f);
                    boolean fromTop = scatter || rng.nextInt(3) != 0;
                    // fulgi albăstrui-pal: pe ceața albă din Tundra ar fi invizibili dacă ar fi albi
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.CIRCLE,
                            fromTop ? rand(0, W) : rand(-60, 0), fromTop ? (scatter ? rand(0, H) : rand(-30, 0)) : rand(0, H * 0.6f),
                            rand(0.9f, 2.2f), rand(1.0f, 2.4f) * (size / 3.5f + 0.4f), 0, 1f, 350 + rng.nextInt(250), size, size, 0.95f, pick(0xB4C8E6, 0xA3BCE0, 0xC4D4EC));
                }
                p.setWobble(i, 0.25f, 0.06f);
                p.setDepth(i, rand(0.5f, 1f));
            }
            case "Podul" -> { // cenușă și umbre care se ridică din hău
                if (rng.nextInt(4) == 0) {
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.GLOW, rand(0, W), scatter ? rand(0, H) : H + 10,
                            rand(-0.2f, 0.2f), rand(-0.6f, -0.2f), 0, 1f, 280 + rng.nextInt(240), 3f, 2f, 0.9f, 0x7C5CD6);
                } else {
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.CIRCLE, rand(0, W), scatter ? rand(0, H) : H + 10,
                            rand(-0.25f, 0.25f), rand(-0.6f, -0.2f), 0, 1f, 360 + rng.nextInt(300), 2.6f, 2f, 0.5f, 0x9A8FB5);
                }
                p.setWobble(i, 0.3f, 0.05f);
                p.setDepth(i, rand(0.5f, 1f));
            }
            case "Menu" -> { // luminițe aurii care se ridică încet în spatele meniului
                i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.GLOW, rand(0, W), scatter ? rand(0, H) : H + 10,
                        rand(-0.15f, 0.15f), rand(-0.6f, -0.2f), 0, 1f, 300 + rng.nextInt(300), 4.2f, 3f, 1f, pick(0xFFD36B, 0xFFB347, 0xFFF0B0));
                p.setWobble(i, 0.3f, 0.05f);
                p.setDepth(i, 0f);
            }
            case "Castelul" -> { // jar roșu ridicându-se
                if (rng.nextInt(3) == 0) {
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.CIRCLE, rand(0, W), scatter ? rand(0, H) : -10,
                            rand(-0.2f, 0.2f), rand(0.15f, 0.5f), 0, 1f, 400 + rng.nextInt(300), 2.6f, 2.2f, 0.5f, 0x666070);
                } else {
                    i = p.spawn(ParticleSystem.AMBIENT, ParticleSystem.GLOW, rand(0, W), scatter ? rand(0, H) : H + 10,
                            rand(-0.2f, 0.25f), rand(-0.9f, -0.35f), 0, 1f, 260 + rng.nextInt(200), 3f, 1.6f, 0.9f, pick(0xC03030, 0xE04A2A, 0xA02050));
                }
                p.setWobble(i, 0.3f, 0.07f);
                p.setDepth(i, rand(0.6f, 1f));
            }
            default -> { }
        }
    }
}
