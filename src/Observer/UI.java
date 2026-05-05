package Observer;

import Main.GamePanel;
import java.awt.*;

public class UI {
    GamePanel gp;
    Graphics2D g2;
    public Font arial_40, arial_80B;

    // Mesaje temporare (ex: "Ai primit o carte!")
    public String message = "";
    public boolean messageOn = false;
    public int messageCounter = 0;

    public UI(GamePanel gp) {
        this.gp = gp;
        arial_40 = new Font("Arial", Font.PLAIN, 40);
        arial_80B = new Font("Arial", Font.BOLD, 80);
    }

    public void draw(Graphics2D g2) {
        this.g2 = g2;
        g2.setFont(arial_40);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // --- LAYER-URI UI ÎN FUNCȚIE DE STATE ---

        if (gp.gameState == GamePanel.GameState.MENU) {
            drawMenuScreen();
        }

        if (gp.gameState == GamePanel.GameState.PLAY) {
            // HUD-ul de bază (Viața)
            gp.healthUI.draw(g2);
            drawManaText(g2);      // Textul pentru Mana de dash
            drawInteractionPrompt();
            drawMessage();
        }

        if (gp.gameState == GamePanel.GameState.PAUSE) {
            gp.pauseUI.draw(g2);
        }

        if (gp.gameState == GamePanel.GameState.DIALOGUE) {
            gp.dialogueUI.draw(g2);
        }

        if (gp.gameState == GamePanel.GameState.INVENTORY) {
            gp.inventoryUI.draw(g2);
        }

        if (gp.gameState == GamePanel.GameState.BATTLE) {
            gp.battleUI.draw(g2);
        }

        if (gp.gameState == GamePanel.GameState.DEATH) {
            gp.deathUI.draw(g2);
        }

        if (gp.gameState == GamePanel.GameState.REWARD) {
            gp.rewardUI.draw(g2);
        }

        if (gp.gameState == GamePanel.GameState.QUEST){
            gp.questBookUI.draw(g2);
        }
    }

    private void drawInteractionPrompt() {
        // Logica de "Senzor" 64x64 pentru tasta E
        int oldX = gp.player.hitBox.x;
        int oldY = gp.player.hitBox.y;
        int oldW = gp.player.hitBox.width;
        int oldH = gp.player.hitBox.height;

        // Mărim hitbox-ul doar pentru detecție
        gp.player.hitBox.x -= gp.tileSize;
        gp.player.hitBox.y -= gp.tileSize;
        gp.player.hitBox.width += gp.tileSize * 2;
        gp.player.hitBox.height += gp.tileSize * 2;

        int npcIndex = gp.cChecher.checkEntity(gp.player, gp.npc);

        // Resetăm hitbox-ul la normal
        gp.player.hitBox.x = oldX;
        gp.player.hitBox.y = oldY;
        gp.player.hitBox.width = oldW;
        gp.player.hitBox.height = oldH;

        if (npcIndex != 999) {
            String text = "[E] TO TALK";
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));

            int x = getXforCenteredText(text);
            int y = gp.player.screenY;

            // Umbră
            g2.setColor(Color.BLACK);
            g2.drawString(text, x+ 2, y/4+2);
            // Text principal
            g2.setColor(Color.YELLOW);
            g2.drawString(text, x, y/4);
        }
    }

    private void drawMenuScreen() {
        // 1. FUNDAL (Asigură-te că asta e prima!)
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // 2. TITLU PRINCIPAL: ERON
        g2.setFont(arial_80B.deriveFont(Font.BOLD, 90f));
        String titleMain = "ERON";
        int x1 = getXforCenteredText(titleMain);
        int y1 = gp.tileSize * 3; // Folosim tileSize pentru a fi siguri de poziție (aprox 192px)

        // Umbră pentru contrast
        g2.setColor(Color.GRAY);
        g2.drawString(titleMain, x1 + 5, y1 + 5);

        // Text principal
        g2.setColor(new Color(255, 215, 0)); // Auriu
        g2.drawString(titleMain, x1, y1);

        // 3. TITLU SECUNDAR: Cronicile Blestemului
        // Verifică dacă arial_40 este încărcat corect în constructor!
        g2.setFont(arial_40.deriveFont(Font.PLAIN, 48f));
        String titleSub = "Cronicile Blestemului";
        int x2 = getXforCenteredText(titleSub);
        int y2 = y1 + 50; // Îl punem la fix 50 pixeli sub primul rând

        g2.setColor(Color.WHITE);
        g2.drawString(titleSub, x2, y2);

        // 4. LINIE DECORATIVĂ (Opțional, ajută la vizualizarea spațiului)
        g2.setColor(new Color(255, 215, 0, 100)); // Auriu transparent
        g2.fillRect(x1-30, y2+20, 320, 2);

        // 5. OPȚIUNI MENIU
        g2.setFont(arial_40.deriveFont(Font.BOLD, 26F));
        String[] menuOptions = {"ÎNCEPE AVENTURA", "CONTINUĂ CĂLĂTORIA", "OPȚIUNI ȘI SECRETE", "PĂRĂSEȘTE TĂRÂMUL"};

        for (int i = 0; i < menuOptions.length; i++) {
            String text = menuOptions[i];
            int y = (gp.tileSize * 6) + (i * 55); // Mutăm meniul mai jos
            int textX = getXforCenteredText(text);

            if (i == gp.currentMenuOption) {
                g2.setColor(Color.YELLOW);
                g2.drawString("> " + text + " <", getXforCenteredText("> " + text + " <"), y);
            } else {
                g2.setColor(Color.WHITE);
                g2.drawString(text, textX, y);
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
        // Setăm fontul și culoarea
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));

        // Verificăm dacă jucătorul are destulă mană pentru dash (30) pentru a schimba culoarea
        // Acesta este un feedback vizual excelent pentru jucător
        if (gp.player.exploreMana < 30) {
            g2.setColor(new Color(200, 200, 200, 150)); // Gri dacă nu poate da dash
        } else {
            g2.setColor(new Color(0, 204, 255)); // Albastru deschis (Cyan) dacă poate
        }

        // Convertim float-ul în int pentru a nu afișa zecimale plictisitoare (ex: 75.23 -> 75)
        String manaText = "DASH: " + (int)gp.player.exploreMana + "%";

        // Poziționăm textul. Dacă vrei să fie lângă viață:
        // Presupunând că bara de viață e la x=32, y=32
        int x = gp.tileSize / 2;
        int y = (gp.tileSize / 2) + 45; // Poziționat sub bara de viață

        // Desenăm un mic "glow" sau umbră pentru lizibilitate
        g2.setColor(Color.BLACK);
        g2.drawString(manaText, x + 2, y + 2); // Umbra

        // Desenăm textul principal (revenim la culoarea setată anterior)
        if (gp.player.exploreMana < 30) g2.setColor(Color.GRAY);
        else g2.setColor(new Color(0, 204, 255));

        g2.drawString(manaText, x, y);
    }
}