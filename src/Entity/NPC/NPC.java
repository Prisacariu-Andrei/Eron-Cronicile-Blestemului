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

    /** Jucătorul a ales o opțiune dintr-un dialog cu variante (doar NPC-urile din date, vezi DataNPC, o folosesc). */
    public void choose(int index) {}

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
        // Leagăn lent pe verticală: personajele nu stau perfect nemișcate
        float bobPhase = (worldX * 7 + worldY * 13) % (int) Main.GameFeel.IDLE_BOB_PERIOD_TICKS;
        int bob = Math.round((float) Math.sin((gp.fx.ticks + bobPhase) * 2 * Math.PI / Main.GameFeel.IDLE_BOB_PERIOD_TICKS)
                * Main.GameFeel.IDLE_BOB_PX);
        int screenX = gp.camera.screenX(worldX);
        int screenY = gp.camera.screenY(worldY) + bob;

        // Verificăm dacă NPC-ul se află în interiorul ferestrei
        if (screenX + gp.tileSize > 0 &&
                screenX < gp.screenWidth &&
                screenY + gp.tileSize > 0 &&
                screenY < gp.screenHeight) {

            g2.drawImage(down1, screenX, screenY, gp.tileSize, gp.tileSize, null);
            drawMarker(g2, screenX + gp.tileSize / 2, screenY - 6);
        }
    }

    // Semne de misiune deasupra capului: ! = are o misiune de oferit, ? = gata de predat / în desfășurare
    public static final int MARKER_NONE = 0, MARKER_AVAILABLE = 1, MARKER_READY = 2, MARKER_ACTIVE = 3, MARKER_TALK = 4;

    /** Ce semn să apară deasupra acestui NPC (DataNPC îl calculează din starea misiunilor). */
    public int markerKind() { return MARKER_NONE; }

    private void drawMarker(Graphics2D g2, int cx, int bottomY) {
        int kind = markerKind();
        if (kind == MARKER_NONE) return;
        String sign = (kind == MARKER_AVAILABLE || kind == MARKER_TALK) ? "!" : "?";
        Color color = switch (kind) {
            case MARKER_AVAILABLE, MARKER_READY -> new Color(255, 221, 60);   // galben: acțiune pentru tine
            case MARKER_TALK -> new Color(120, 220, 255);                      // albastru: vorbește cu el
            default -> new Color(165, 165, 175);                               // gri: misiune în desfășurare
        };
        float hop = kind == MARKER_ACTIVE ? 0f : Math.abs((float) Math.sin(gp.fx.ticks * 0.12)) * 7f;
        Font old = g2.getFont();
        g2.setFont(old.deriveFont(Font.BOLD, 34F));
        int x = cx - g2.getFontMetrics().stringWidth(sign) / 2;
        int y = bottomY - Math.round(hop);
        g2.setColor(Color.BLACK);
        g2.drawString(sign, x + 2, y + 2);
        g2.drawString(sign, x - 1, y + 2);
        g2.setColor(color);
        g2.drawString(sign, x, y);
        g2.setFont(old);
    }
}