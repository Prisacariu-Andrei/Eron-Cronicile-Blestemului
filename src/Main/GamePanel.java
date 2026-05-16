package Main;

import Entity.Inamici.Enemy;
import Entity.NPC.NPC;
import Entity.Player.Player;
import Observer.*;
import Tiles.TileManager;
import ProgressManagement.QuestManager;
import GameState.GameState; // Importă interfața nouă
import GameState.MenuState; // Importă starea inițială de meniu

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

    // STATE-UL JOCULUI (Acum controlat prin Interfață)
    private GameState currentState;

    // SISTEM
    int FPS = 60;
    public TileManager tileM = new TileManager(this);
    public QuestManager questManager = new QuestManager();
    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;
    public CollisionChecker cChecher = new CollisionChecker(this);
    public EventHandler eHandler = new EventHandler(this);
    public AssetSetter aSetter = new AssetSetter(this);
    public GameTimer gameTimer = new GameTimer();

    // ENTITĂȚI
    public Player player = new Player(this, keyH);
    public Enemy[] enemy = new Enemy[10];
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
    public ArrayList<DamagePopUp> popups = new ArrayList<>();

    // OPȚIUNI MENIU (Păstrate aici pentru simplitate)
    public int currentMenuOption = 0;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        player.addObserver(healthUI);
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
        changeState(new MenuState(this));
    }

    /**
     * Schimbă starea curentă a jocului asigurând rularea metodelor de exit și enter.
     */
    public void changeState(GameState newState) {
        if (currentState != null) {
            currentState.exit();
        }
        currentState = newState;
        if (currentState != null) {
            currentState.enter();
        }
    }

    /**
     * Permite altor clase (ex: UI sau KeyHandler) să verifice starea activă.
     */
    public GameState getCurrentState() {
        return currentState;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = (double) 1000000000 / FPS;
        double delta = 0;
        double lastTime = System.nanoTime();

        while (gameThread != null) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    // UPDATE delegat complet către starea curentă
    public void update() {
        if (currentState != null) {
            currentState.update();
        }
    }

    // DRAW delegat complet către starea curentă
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (currentState != null) {
            currentState.draw(g2);
        }

        g2.dispose();
    }
}