package Main;

import Entity.Inamici.Enemy;
import Entity.NPC.NPC;
import Entity.Player.CardDatabase;
import Entity.Player.Player;
import Observer.*;
import Tiles.TileManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable {

    // SETĂRI ECRAN
    final int originalTileSize = 32;
    final int scale = 2;
    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 20;
    public final int maxScreenRow = 14;
    public final int screenWidth = maxScreenCol * tileSize;
    public final int screenHeight = maxScreenRow * tileSize;

    // SETĂRI WORLD
    public final int maxWorldCol = 200;
    public final int maxWorldRow = 200;

    // STATE-UL JOCULUI
    public enum GameState { MENU, PLAY, PAUSE, INVENTORY, BATTLE, DEATH, REWARD, DIALOGUE }
    public GameState gameState = GameState.MENU;

    // SISTEM
    int FPS = 60;
    public TileManager tileM = new TileManager(this);
    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;
    public CollisionChecker cChecher = new CollisionChecker(this);
    public EventHandler eHandler = new EventHandler(this);
    public AssetSetter aSetter = new AssetSetter(this);

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
    public ArrayList<DamagePopUp> popups = new ArrayList<>();

    // OPȚIUNI MENIU (Păstrate aici pentru simplitate)
    public int currentMenuOption = 0;
    private final String[] menuOptions = {"ÎNCEPE AVENTURA", "CONTINUĂ CĂLĂTORIA", "OPȚIUNI", "PĂRĂSEȘTE TĂRÂMUL"};

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        CardDatabase.init();
        player.addObserver(healthUI);
    }

    public void setupGame() {
        aSetter.setEnemyForMap("/Maps/Eldoria");
        gameState = GameState.MENU;
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

    public void update() {
        if (gameState == GameState.MENU) {
            updateMenu();
        } else if (gameState == GameState.PLAY) {
            player.update();
            eHandler.checkEvent();
            for (int i = 0; i < enemy.length; i++) {
                if (enemy[i] != null) {
                    if (enemy[i].alive) {
                        enemy[i].update();
                    } else {
                        // Opțional: scoate inamicul din listă dacă e mort definitiv
                         enemy[i] = null;
                    }
                }
            }
            if (keyH.escPressed) {
                gameState = GameState.PAUSE;
                keyH.escPressed = false;
            }

        } else if (gameState == GameState.PAUSE) {
            updatePause();

        } else if (gameState == GameState.BATTLE || gameState == GameState.REWARD) {
            player.update();
            for (int i = 0; i < popups.size(); i++) {
                popups.get(i).update();
                if (popups.get(i).lifeSpan <= 0) popups.remove(i--);
            }

            // Asigură-te că verificăm dacă inamicul a murit pentru a termina bătălia corect
            if (gameState == GameState.BATTLE && player.currentEnemy != null && !player.currentEnemy.alive) {
                player.winBattle();
            }
        }
    }

    private void updateMenu() {
        if (keyH.upPressed) {
            currentMenuOption = (currentMenuOption <= 0) ? menuOptions.length - 1 : currentMenuOption - 1;
            keyH.upPressed = false;
        }
        if (keyH.downPressed) {
            currentMenuOption = (currentMenuOption >= menuOptions.length - 1) ? 0 : currentMenuOption + 1;
            keyH.downPressed = false;
        }
        if (keyH.enterPressed) {
            if (currentMenuOption == 0) gameState = GameState.PLAY;
            if (currentMenuOption == 3) System.exit(0);
            keyH.enterPressed = false;
        }
    }

    private void updatePause() {
        if (keyH.escPressed) {
            gameState = GameState.PLAY;
            keyH.escPressed = false;
        }
        // Folosim direct selectorul din PauseUI dacă ai implementat acolo comanda
        if (keyH.upPressed) {
            pauseUI.commandNum = (pauseUI.commandNum <= 0) ? 3 : pauseUI.commandNum - 1;
            keyH.upPressed = false;
        }
        if (keyH.downPressed) {
            pauseUI.commandNum = (pauseUI.commandNum >= 3) ? 0 : pauseUI.commandNum + 1;
            keyH.downPressed = false;
        }
        if (keyH.enterPressed) {
            if (pauseUI.commandNum == 0) {
                gameState = GameState.PLAY;
                aSetter.setEnemyForMap("/Maps/Eldoria.txt"); // Încarcă inamicii de start
                aSetter.setNPC(this.tileM.currentZone); // incarca NPC-urile
            }
            if (pauseUI.commandNum == 3) gameState = GameState.MENU;
            keyH.enterPressed = false;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // 1. DESENEAZĂ MENIUL PRINCIPAL
        if (gameState == GameState.MENU) {
            ui.draw(g2);
        }

        // 2. DESENEAZĂ LUMEA + UI-UL DE BAZĂ + PAUZA
        else if (gameState == GameState.PLAY || gameState == GameState.PAUSE || gameState == GameState.DIALOGUE) {
            tileM.draw(g2);
            for (NPC n : npc) if (n != null) n.draw(g2);
            for (Enemy e : enemy) if (e != null) e.draw(g2);
            player.draw(g2);

            // Desenăm HUD-ul (Viață, Dash Mana)
            ui.draw(g2);
        }

        // 3. DESENEAZĂ ALTE STATE-URI (Luptă etc)
        else {
            ui.draw(g2);
        }

        g2.dispose();
    }
}