package Entity.NPC;

import Entity.Entity;
import Main.GamePanel;
import java.awt.*;

public abstract class NPC extends Entity {

    protected GamePanel gp;
    public String name;
    public String[] dialogues = new String[20]; // Rezervăm 20 de linii de dialog
    protected int dialogueIndex = 0;

    public NPC(GamePanel gp, int x, int y) {
        this.gp = gp;
        this.worldX = x;
        this.worldY = y;
        this.direction = "down";
        this.speed = 0; // NPC-urile stau pe loc de regulă

        // Hitbox standard pentru NPC (ajustează dacă e nevoie)
        hitBox = new Rectangle(8, 16, 48, 48);
    }

    // Metode pe care fiecare NPC le va implementa obligatoriu
    public abstract void getImage();
    public abstract void setDialogue();

    // Metoda apelată când apeși [E]
    // În NPC.java
    public void speak() {
        if (dialogues[dialogueIndex] == null) {
            dialogueIndex = 0;
            gp.gameState = GamePanel.GameState.PLAY;
            return;
        }

        gp.dialogueUI.currentDialogue = dialogues[dialogueIndex];
        gp.dialogueUI.resetCharIndex(); // <--- RESETĂM AICI

        dialogueIndex++;
    }


    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        // Randăm doar dacă e vizibil pe ecran
        if (worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {

            g2.drawImage(down1, screenX, screenY, gp.tileSize, gp.tileSize, null);
        }
    }
}