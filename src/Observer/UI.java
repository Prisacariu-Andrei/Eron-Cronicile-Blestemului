package Observer;

import Main.Exception.GameExceptionHandler;
import Main.GamePanel;
import GameState.*; // Importă toate stările din pachetul nou
import java.awt.*;
import java.util.Objects;

public class UI {
    GamePanel gp;
    Graphics2D g2;
    public Font arial_40, arial_80B;
    public java.awt.image.BufferedImage menuBackground;


    // --- ADAUGĂ LA VARIABILELE GLOBALE DIN UI.java ---
    public FogOfWar fogOfWar;

    // --- ADAUGĂ ÎN CONSTRUCTORUL UI(GamePanel gp) ---

    // Proprietate folosită de KeyHandler pentru selecția opțiunilor din meniuri (Start, Death, etc.)
    public int commandNum = 0;

    // Mesaje temporare (ex: "Ai primit o carte!")
    public String message = "";
    public boolean messageOn = false;
    public int messageCounter = 0;

    public UI(GamePanel gp) {
        this.gp = gp;
        arial_40 = new Font("Arial", Font.PLAIN, 40);
        arial_80B = new Font("Arial", Font.BOLD, 80);
        this.fogOfWar = new FogOfWar(gp, gp.tileSize*8);

        // ★ OPTIMIZARE: Încărcăm imaginea o SINGURĂ dată la inițializarea UI-ului, nu în loop-ul de draw!
        try {
            menuBackground = javax.imageio.ImageIO.read(java.util.Objects.requireNonNull(
                    getClass().getResourceAsStream("/Backgrounds/menu_bg1.png")));
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut încărca textura pentru Meniul Principal (/Backgrounds/menu_bg1.png)", e);
        }
    }

    public void draw(Graphics2D g2) {
        this.g2 = g2;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
        g2.setColor(Color.WHITE);
        g2.drawString("Timp: " + gp.gameTimer.getFormattedTime(), (int)(gp.screenWidth*0.9), (int)(gp.screenHeight*0.05));

        g2.setFont(arial_40);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Preluăm starea curentă prin interfață
        GameState state = gp.getCurrentState();

        // --- LAYER-URI UI ÎN FUNCȚIE DE CLASA STĂRII CURENTE ---
        if (state instanceof MenuState) {
            drawMenuScreen();
        }
        if (state instanceof PlayState) {
            if (Objects.equals(gp.tileM.currentZone, "Abisul")) {
                fogOfWar.draw(g2,-5);
            }
            if (Objects.equals(gp.tileM.currentZone, "Tundra")){
                fogOfWar.draw(g2,235);
            }
            gp.healthUI.draw(g2);
            drawManaText(g2);
            drawInteractionPrompt();
            drawMessage();

        }
        if (state instanceof PauseState) {
            gp.pauseUI.draw(g2);
        }
        if (state instanceof DialogueState) {
            gp.dialogueUI.draw(g2);
        }
        if (state instanceof InventoryState) {
            gp.inventoryUI.draw(g2);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
            g2.setColor(Color.WHITE);
            g2.drawString("Timp: " + gp.gameTimer.getFormattedTime(), gp.tileSize * 11, gp.tileSize);
        }
        if (state instanceof BattleState) {
            gp.battleUI.draw(g2);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
            g2.setColor(Color.WHITE);
            g2.drawString("Timp: " + gp.gameTimer.getFormattedTime(), gp.tileSize * 11, gp.tileSize);
        }
        if (state instanceof DeathState) {
            gp.deathUI.draw(g2);
        }
        if (state instanceof RewardState) {
            gp.rewardUI.draw(g2);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
            g2.setColor(Color.WHITE);
            g2.drawString("Timp: " + gp.gameTimer.getFormattedTime(), gp.tileSize * 11, gp.tileSize);
        }
        if (state instanceof QuestGameState) {
            gp.questBookUI.draw(g2);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
            g2.setColor(Color.WHITE);
            g2.drawString("Timp: " + gp.gameTimer.getFormattedTime(), gp.tileSize * 11, gp.tileSize);
        }
        if (state instanceof LeaderboardState) {
            gp.leaderboardUI.draw(g2);
        }
    }

    private void drawInteractionPrompt() {
        int oldX = gp.player.hitBox.x;
        int oldY = gp.player.hitBox.y;
        int oldW = gp.player.hitBox.width;
        int oldH = gp.player.hitBox.height;

        gp.player.hitBox.x -= gp.tileSize;
        gp.player.hitBox.y -= gp.tileSize;
        gp.player.hitBox.width += gp.tileSize * 2;
        gp.player.hitBox.height += gp.tileSize * 2;

        int npcIndex = gp.cChecher.checkEntity(gp.player, gp.npc);

        gp.player.hitBox.x = oldX;
        gp.player.hitBox.y = oldY;
        gp.player.hitBox.width = oldW;
        gp.player.hitBox.height = oldH;

        if (npcIndex != 999) {
            String text = "[E] TO TALK";
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));

            int x = getXforCenteredText(text);
            int y = gp.player.screenY - 15; // Reparat din screenY/4 (care trimitea textul pe tavan)

            g2.setColor(Color.BLACK);
            g2.drawString(text, x + 2, y + 2);
            g2.setColor(Color.YELLOW);
            g2.drawString(text, x, y);
        }
    }

    private void drawMenuScreen() {
        //1. RANDARE FUNDAL
        if (menuBackground != null) {
            g2.drawImage(menuBackground, -80, 0, gp.screenWidth+80, gp.screenHeight, null);

        } else {
            // Fallback în caz că imaginea nu s-a putut încărca, să nu rămână ecranul gol
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }

        // 2. OPȚIUNI MENIU
        g2.setFont(arial_40.deriveFont(Font.BOLD, 26F));
        String[] menuOptions = {"ÎNCEPE AVENTURA", "CONTINUĂ CĂLĂTORIA", "TOP SCORURI", "OPȚIUNI ȘI SECRETE", "PĂRĂSEȘTE TĂRÂMUL"};

        // În interiorul metodei drawMenuScreen din UI.java:
        for (int i = 0; i < menuOptions.length; i++) {
            // Folosește direct variabila commandNum a clasei UI
            int y = (int)(gp.screenHeight * 0.45) + (i * 55);
            if (i == this.commandNum) {
                g2.setColor(Color.YELLOW);
                g2.drawString("> " + menuOptions[i] + " <", getXforCenteredText("> " + menuOptions[i] + " <"), y);
            } else {
                g2.setColor(Color.WHITE);
                g2.drawString(menuOptions[i], getXforCenteredText(menuOptions[i]), y);
            }
        }
    }

    private void drawMessage() {
        if (messageOn) {
            g2.setFont(g2.getFont().deriveFont(20F));
            g2.drawString(message, gp.tileSize / 2, gp.tileSize * 5);
            messageCounter++;
            if (messageCounter > 120) {
                messageCounter = 0;
                messageOn = false;
            }
        }
    }

    public int getXforCenteredText(String text) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }

    public void drawManaText(Graphics2D g2) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));

        if (gp.player.exploreMana < 30) {
            g2.setColor(new Color(200, 200, 200, 150));
        } else {
            g2.setColor(new Color(0, 204, 255));
        }

        String manaText = "DASH: " + (int)gp.player.exploreMana + "%";

        int x = gp.tileSize / 2;
        int y = (gp.tileSize / 2) + 45;

        g2.setColor(Color.BLACK);
        g2.drawString(manaText, x + 2, y + 2);

        if (gp.player.exploreMana < 30) g2.setColor(Color.GRAY);
        else g2.setColor(new Color(0, 204, 255));

        g2.drawString(manaText, x, y);
    }

    public long getPlayTime() {
        if (gp.gameTimer != null) {
            return gp.gameTimer.getElapsedSeconds();
        }
        return 0;
    }
}