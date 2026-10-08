package Observer;

import Main.Easing;
import Main.Exception.GameExceptionHandler;
import Main.GameFeel;
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
                gp.fx.drawAmbient(g2); // ninsoarea cade prin ceață, nu sub ea
            }
            gp.healthUI.draw(g2);
            drawManaText(g2);
            drawCoins(g2);
            drawInteractionPrompt();
            drawMessage();

        }
        if (state instanceof PauseState) {
            gp.pauseUI.draw(g2);
        }
        if (state instanceof DialogueState) {
            gp.dialogueUI.draw(g2);
        }
        if (state instanceof ShopState) {
            gp.shopUI.draw(g2);
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
            String npcName = gp.npc[npcIndex].name;
            String text = "[E] – talk" + (npcName != null ? "  (" + npcName + ")" : "");
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));

            // Deasupra lui Eron, oriunde este el pe ecran (lângă marginile hărții nu mai e în centru)
            int x = gp.camera.screenX(gp.player.worldX) + gp.tileSize / 2 - g2.getFontMetrics().stringWidth(text) / 2;
            int y = gp.camera.screenY(gp.player.worldY) - 15 // Reparat din screenY/4 (care trimitea textul pe tavan)
                    - Math.round((float) Math.abs(Math.sin(gp.fx.ticks * 0.1)) * 5); // saltă ușor ca să atragă atenția

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

        gp.fx.drawAmbient(g2); // luminițe care plutesc în spatele meniului

        // 2. OPȚIUNI MENIU
        g2.setFont(arial_40.deriveFont(Font.BOLD, 26F));
        String[] menuOptions = {"ÎNCEPE AVENTURA", "CONTINUĂ CĂLĂTORIA", "TOP SCORURI", "OPȚIUNI ȘI SECRETE", "PĂRĂSEȘTE TĂRÂMUL"};

        // În interiorul metodei drawMenuScreen din UI.java:
        for (int i = 0; i < menuOptions.length; i++) {
            // Folosește direct variabila commandNum a clasei UI
            int y = (int)(gp.screenHeight * 0.45) + (i * 55);
            if (i == this.commandNum) {
                // Opțiunea aleasă se leagănă ușor și pulsează, ca meniul să nu fie un ecran înghețat
                float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.12);
                int sway = Math.round((float) Math.sin(gp.fx.ticks * 0.15) * 3);
                g2.setColor(new Color(255, 255, 90 + Math.round(120 * (1f - pulse))));
                g2.drawString("> " + menuOptions[i] + " <", getXforCenteredText("> " + menuOptions[i] + " <") + sway, y);
            } else {
                g2.setColor(Color.WHITE);
                g2.drawString(menuOptions[i], getXforCenteredText(menuOptions[i]), y);
            }
        }
    }

    private static final int MESSAGE_TICKS = 120;

    // Contorul de monede din HUD: numărul "curge" spre valoarea reală și sare când primești monede
    public int coinPulse = 0;
    private float coinsShown = -1;

    private void drawCoins(Graphics2D g2) {
        if (coinsShown < 0) coinsShown = gp.player.coins;
        float scale = 1f + 0.25f * Easing.outQuad(coinPulse / 18f);
        int size = Math.round(24 * scale);
        int x = gp.tileSize / 2, y = (gp.tileSize / 2) + 58;
        ShopUI.drawCoin(g2, x, y, size);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F * scale));
        String text = String.valueOf(Math.round(coinsShown));
        g2.setColor(Color.BLACK);
        g2.drawString(text, x + size + 10, y + size - 3);
        g2.setColor(new Color(255, 221, 100));
        g2.drawString(text, x + size + 8, y + size - 5);

        drawLevel(g2, x, y + size + 12);
    }

    /** Nivelul și bara de XP sub monede. */
    private void drawLevel(Graphics2D g2, int x, int y) {
        var pg = gp.progression;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 17F));
        String lvl = "Nv. " + pg.level;
        g2.setColor(Color.BLACK);
        g2.drawString(lvl, x + 1, y + 15);
        g2.setColor(new Color(200, 170, 255));
        g2.drawString(lvl, x, y + 14);

        int barX = x + 62, barY = y + 3, barW = 150, barH = 11;
        float fill = pg.xpToNext() == 0 ? 1f : Easing.clamp01(pg.xp / (float) pg.xpToNext());
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
        g2.setColor(new Color(140, 90, 230));
        g2.fillRoundRect(barX, barY, Math.round(barW * fill), barH, 8, 8);
        g2.setColor(new Color(235, 225, 255));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(barX, barY, barW, barH, 8, 8);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11F));
        String xpText = pg.xpToNext() == 0 ? "MAX" : pg.xp + "/" + pg.xpToNext();
        g2.setColor(Color.WHITE);
        g2.drawString(xpText, barX + barW / 2 - g2.getFontMetrics().stringWidth(xpText) / 2, barY + 9);
    }

    /** Un tick: mesajul din HUD expiră după 120 de ticks (2 secunde), indiferent de FPS-ul desenării. */
    public void update() {
        if (coinPulse > 0) coinPulse--;
        if (coinsShown >= 0) {
            coinsShown += (gp.player.coins - coinsShown) * 0.2f;
            if (Math.abs(gp.player.coins - coinsShown) < 0.5f) coinsShown = gp.player.coins;
        }
        if (messageOn) {
            messageCounter++;
            if (messageCounter > MESSAGE_TICKS) {
                messageCounter = 0;
                messageOn = false;
            }
        }
    }

    private void drawMessage() {
        if (messageOn) {
            // Intră și iese lin: alunecă din stânga și se estompează, în loc să apară și să dispară brusc
            float in = Easing.clamp01(messageCounter / (float) GameFeel.MESSAGE_FADE_TICKS);
            float out = Easing.clamp01((MESSAGE_TICKS - messageCounter) / (float) GameFeel.MESSAGE_FADE_TICKS);
            float alpha = Easing.outCubic(Math.min(in, out));
            int slide = Math.round((1f - Easing.outCubic(in)) * -28);
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, alpha)));
            g2.setFont(g2.getFont().deriveFont(20F));
            g2.drawString(message, gp.tileSize / 2 + slide, gp.tileSize * 5);
            g2.setComposite(old);
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