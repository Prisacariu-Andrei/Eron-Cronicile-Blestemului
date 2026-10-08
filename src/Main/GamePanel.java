package Main;

import Database.SaveLoadManager;
import Entity.Inamici.Enemy;
import Entity.NPC.NPC;
import Entity.Player.Player;
import Observer.*;
import Tiles.TileManager;
import ProgressManagement.QuestManager;
import GameState.GameState;
import GameState.MenuState;
import GameState.InputNameState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable {

    // SETĂRI ECRAN
    final int originalTileSize = 32;
    final int scale = 2;
    public final int tileSize = originalTileSize * scale;
    public final int screenWidth = 1600;
    public final int screenHeight = 900;

    // SETĂRI WORLD
    public final int maxWorldCol = 200;
    public final int maxWorldRow = 200;

    // STATE-UL JOCULUI (Controlat prin Interfață)
    private GameState currentState;

    // SISTEM
    int FPS = 60;
    public Camera camera = new Camera(this); // înaintea TileManager, care o folosește la încărcarea hărții
    public Effects fx = new Effects(this);   // shake, hit-stop, fade-uri, particule (vezi GameFeel pentru reglaje)
    public TileManager tileM = new TileManager(this);
    public QuestManager questManager = new QuestManager();
    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;
    public CollisionChecker cChecher = new CollisionChecker(this);
    public EventHandler eHandler = new EventHandler(this);
    public AssetSetter aSetter = new AssetSetter(this);
    public GameTimer gameTimer = new GameTimer();
    public SaveLoadManager saveLoadManager = new SaveLoadManager();
    // ENTITĂȚI
    public Player player = new Player(this, keyH);
    public Enemy[] enemy = new Enemy[15];
    public NPC[] npc = new NPC[10];

    // UI & OBSERVERS
    public UI ui = new UI(this);
    public HealthBarUI healthUI = new HealthBarUI();
    public PauseUI pauseUI = new PauseUI(this);
    public InventoryUI inventoryUI = new InventoryUI(this);
    public BattleUI battleUI = new BattleUI(this);
    public RewardUI rewardUI = new RewardUI(this);
    public DeathUI deathUI = new DeathUI(this);
    public DialogueUI dialogueUI = new DialogueUI(this);
    public QuestBookUI questBookUI = new QuestBookUI(this);
    public LeaderboardUI leaderboardUI = new LeaderboardUI(this);
    public CreditsUI creditsUI = new CreditsUI(this);
    public CutsceneUI cutsceneUI = new CutsceneUI(this);
    public ShopUI shopUI = new ShopUI(this);
    public ArrayList<DamagePopUp> popups = new ArrayList<>();

    // OPȚIUNI MENIU
    public int currentMenuOption = 0;
    public int activeNpcIndex = -1;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        player.addObserver(healthUI);

        // Terminarea unei misiuni aduce monede (cât: economy.json -> questRewards)
        questManager.onComplete = (quest, isMain) -> {
            int reward = Economy.get().questReward(quest.id, isMain);
            if (reward > 0) {
                player.addCoins(reward);
                ui.message = "Misiune încheiată: +" + reward + " monede";
                ui.messageOn = true;
            }
        };
    }

    public void setupGame() {
        try {
            Database.DatabaseManager.getInstance();
        } catch (Exception e) {
            System.out.println("⚠️ Eroare la inițializarea bazei de date: " + e.getMessage());
        }

        // Setăm inamicii pentru harta Eldoria
        aSetter.setEnemyForMap("/Maps/Eldoria");

        // APELUL CRUCIAL PENTRU NPC-URI:
        aSetter.setNPC(tileM.currentZone);

        // Pornim jocul direct în starea de Meniu
        // NOTĂ: Schimbarea de stare va porni automat muzica de meniu configurată în changeState!
        changeState(new MenuState(this));
    }

    /**
     * Schimbă starea curentă a jocului asigurând rularea metodelor de exit și enter,
     * și actualizează automat SoundTrack-ul în mod inteligent.
     */
    public void changeState(GameState newState) {
        GameState previous = currentState;
        if (currentState != null) {
            currentState.exit();
        }

        currentState = newState;

        if (currentState != null) {
            currentState.enter();
            fx.onStateChanged(previous, currentState); // fade-uri / flash-uri între scene
            System.out.println("DEBUG: Starea s-a schimbat în: " + newState.getClass().getSimpleName());

            // =========================================================================
            // GESTIUNE AUTOMATĂ AUDIO ÎN FUNCȚIE DE STARE (OPȚIUNEA B)
            // =========================================================================
            String stateClassName = currentState.getClass().getSimpleName();

            switch (stateClassName) {
                case "MenuState":
                    // Înregistrează o cheie "MeniuPrincipal" în SoundManager sau folosește ce piesă vrei pentru ecranul de start
                    SoundManager.getInstance().playMusic("MeniuPrincipal");
                    break;

                case "PlayState":
                    // Când dai "Începe Aventura", piesa de meniu se oprește automat
                    // și pornește muzica dedicată hărții curente (ex: "Eldoria")
                    if (tileM != null && tileM.currentZone != null && !tileM.currentZone.isEmpty()) {
                        SoundManager.getInstance().playMusic(tileM.currentZone);
                    } else {
                        SoundManager.getInstance().playMusic("Eldoria");
                    }
                    break;

                case "BattleState":
                    // Când intri în luptă, se trece la piesa dinamică de combat a zonei respective
                    if (tileM != null && tileM.currentZone != null && !tileM.currentZone.isEmpty()) {
                        SoundManager.getInstance().playMusic(player.currentEnemy.battleMusicKey);
                    } else {
                        SoundManager.getInstance().playMusic("Eldoria");
                    }
                    break;

                case "CutsceneState":
                    // Muzica din Eldoria începe odată cu prologul (sub fade-ul din negru) și continuă în joc
                    SoundManager.getInstance().playMusic("Eldoria");
                    break;

                case "DeathState":
                    // Oprim muzica complet când jucătorul moare pentru un efect dramatic
                    SoundManager.getInstance().stopMusic();
                    break;
            }
            // =========================================================================
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    /**
     * Bucla jocului cu pas fix: logica rulează de FPS ori pe secundă indiferent cât durează desenarea,
     * deci viteza jocului nu depinde de FPS-ul de pe ecran. Între cadre thread-ul doarme, în loc să
     * ocupe un nucleu de procesor la 100% (cum făcea vechea buclă "while(true)" fără pauză).
     */
    @Override
    public void run() {
        final long step = 1_000_000_000L / FPS;
        final long maxLag = step * 5; // dacă rămânem în urmă cu >5 cadre (ex. fereastra a fost mutată), renunțăm la restul
        long previous = System.nanoTime();
        long lag = 0;

        while (gameThread != null) {
            long now = System.nanoTime();
            lag = Math.min(maxLag, lag + (now - previous));
            previous = now;

            boolean updated = false;
            while (lag >= step) {
                update();
                lag -= step;
                updated = true;
            }
            if (updated) repaint();

            // Dormim până aproape de următorul tick (lăsăm ~1 ms marjă pentru precizia somnului)
            long sleep = step - lag - 1_000_000L;
            if (sleep > 0) java.util.concurrent.locks.LockSupport.parkNanos(sleep);
        }
    }

    public void update() {
        long t0 = System.nanoTime();
        if (fx.consumeHitStop()) {
            // Hit-stop: câteva cadre în care totul stă pe loc, ca lovitura să aibă greutate
            DebugStats.onUpdate(System.nanoTime() - t0);
            return;
        }
        if (currentState != null) {
            currentState.update();
        }
        camera.update();
        updatePopups();
        healthUI.update();
        fx.update();
        DebugStats.onUpdate(System.nanoTime() - t0);
    }

    /** Numerele care plutesc dispar singure după ce expiră, în orice stare a jocului (nu doar în luptă). */
    private void updatePopups() {
        for (int i = popups.size() - 1; i >= 0; i--) {
            DamagePopUp p = popups.get(i);
            p.update();
            if (p.lifeSpan <= 0) popups.remove(i);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        long t0 = System.nanoTime();
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        fx.beginFrame(g2); // screen shake
        if (currentState != null) {
            currentState.draw(g2);
        }
        fx.drawFront(g2);
        fx.endFrame(g2);
        fx.drawOverlay(g2); // fade-uri / flash-uri peste tot ecranul

        DebugStats.onRender(System.nanoTime() - t0);
        if (DebugStats.visible) DebugStats.draw(g2, this);

        g2.dispose();
    }
}