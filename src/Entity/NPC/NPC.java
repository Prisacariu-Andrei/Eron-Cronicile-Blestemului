package Entity.NPC;

import Entity.Entity;
import Main.GamePanel;
import GameState.*; // Importă pachetul cu stările jocului
import java.awt.*;

public abstract class NPC extends Entity {

    protected GamePanel gp;
    public String name;
    public String[] dialogues = new String[20]; // Rezervăm 20 de linii de dialog
    public int dialogueIndex = 0;
    public boolean isShowingDialogue = false;

    // Indicator pentru a ști dacă NPC-ul este în dialog de introducere
    public boolean isTalking = false;

    public NPC(GamePanel gp, int x, int y) {
        this.gp = gp;
        this.worldX = x;
        this.worldY = y;
        this.direction = "down";
        this.speed = 0; // NPC-urile stau pe loc de regulă

        // Hitbox standard pentru NPC
        hitBox = new Rectangle(8, 16, 48, 48);
    }

    // Metode pe care fiecare NPC le va implementa obligatoriu
    public abstract void getImage();
    public abstract void setDialogue();

    // Metoda apelată când apeși [E] (Modificată pentru State Pattern)
    public void speak() {
        if (dialogues[dialogueIndex] == null) {
            dialogueIndex = 0;
            gp.activeNpcIndex = -1;
            gp.changeState(new PlayState(gp)); // Schimbare prin interfață înapoi la joc
            isTalking = false;
            return;
        }

        gp.changeState(new DialogueState(gp)); // Schimbare prin interfață către ecranul de dialog
        gp.dialogueUI.currentDialogue = dialogues[dialogueIndex];
        gp.dialogueUI.resetCharIndex();

        dialogueIndex++;
    }

    public void draw(Graphics2D g2) {
        // Poziția pe ecran vine din cameră (aceeași pentru tile-uri, Eron, inamici)
        int screenX = gp.camera.screenX(worldX);
        int screenY = gp.camera.screenY(worldY);

        // Verificăm dacă NPC-ul se află în interiorul ferestrei
        if (screenX + gp.tileSize > 0 &&
                screenX < gp.screenWidth &&
                screenY + gp.tileSize > 0 &&
                screenY < gp.screenHeight) {

            g2.drawImage(down1, screenX, screenY, gp.tileSize, gp.tileSize, null);
        }
    }
}