package Entity.NPC;

import Entity.Entity;
import Entity.SpriteSet;
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

    // --- Aspect și mișcare (setul de 8 cadre din res/NPC, vezi SpriteSet) ---
    /** Cele 8 cadre; null = sprite-ul simplu (down1, eventual cu tentă). */
    public SpriteSet spriteSet;
    /** Încotro privește când stă pe loc. */
    public String homeDirection = "down";
    /** Cât de departe (în tile-uri) rătăcește de locul lui; 0 = stă pe loc. */
    public int wanderRadius = 0;
    public boolean walking = false;

    private final int homeCol, homeRow;
    private String moveDir = "down";
    private boolean talkFacing = false;
    private int faceBackTimer = 0, idleTimer = 120, stepAcc = 0, targetX, targetY, animTick = 0;
    private static final java.util.Random RNG = new java.util.Random();
    private static final String[] DIRS = {"down", "up", "left", "right"};
    /** Tile-urile pe care rătăcesc sătenii: iarbă și poteci (nu case, apă, copaci, zone de vindecare sau pasaje). */
    private static boolean wanderTile(int id) { return id == 0 || (id >= 4 && id <= 14); }

    public NPC(GamePanel gp, int x, int y) {
        this.gp = gp;
        this.worldX = x;
        this.worldY = y;
        this.homeCol = x / gp.tileSize;
        this.homeRow = y / gp.tileSize;
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

    /** Obiectele cu care se interacționează cu F (cufere, porți) în loc de E. */
    public boolean usesF() { return false; }

    /** Ce se întâmplă când apeși F lângă obiect. */
    public void use() { }

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

    /** Apelată la fiecare tick cât timp se joacă pe hartă: se întoarce spre Eron / înapoi și rătăcește. */
    public void update() {
        boolean inTalk = gp.activeNpcIndex >= 0 && gp.activeNpcIndex < gp.npc.length && gp.npc[gp.activeNpcIndex] == this;
        if (talkFacing) {
            if (inTalk) return;
            talkFacing = false;
            faceBackTimer = 40;   // o scurtă pauză înainte să se întoarcă la locul lui
        }
        if (faceBackTimer > 0) {
            if (--faceBackTimer == 0) direction = walking ? moveDir : homeDirection;
            return;
        }
        if (wanderRadius > 0 && spriteSet != null) wander();
    }

    /** Eron începe o conversație: NPC-ul se întoarce cu fața la el. */
    public void faceTowards(Entity e) {
        talkFacing = true;
        direction = SpriteSet.facing(worldX + gp.tileSize / 2.0, worldY + gp.tileSize / 2.0, e.worldX + gp.tileSize / 2.0, e.worldY + gp.tileSize / 2.0);
    }

    private void wander() {
        int ts = gp.tileSize;
        if (walking) {
            if (++stepAcc % 2 == 0) {   // un pixel la două ticks: pas lent, de sătean
                int nx = worldX + Integer.signum(targetX - worldX), ny = worldY + Integer.signum(targetY - worldY);
                if (!touchesPlayer(nx, ny)) { worldX = nx; worldY = ny; animTick++; }
            }
            if (worldX == targetX && worldY == targetY) {
                walking = false;
                idleTimer = 150 + RNG.nextInt(330);
            }
            return;
        }
        if (--idleTimer > 0) return;
        int col = worldX / ts, row = worldY / ts;
        int first = RNG.nextInt(4);
        for (int k = 0; k < 4; k++) {
            String d = DIRS[(first + k) % 4];
            int tc = col + (d.equals("left") ? -1 : d.equals("right") ? 1 : 0), tr = row + (d.equals("up") ? -1 : d.equals("down") ? 1 : 0);
            if (Math.abs(tc - homeCol) > wanderRadius || Math.abs(tr - homeRow) > wanderRadius) continue;
            if (!canStandOn(tc, tr)) continue;
            direction = d;
            moveDir = d;
            targetX = tc * ts;
            targetY = tr * ts;
            walking = true;
            stepAcc = 0;
            return;
        }
        idleTimer = 120;
    }

    /** Tile liber: de mers, fără Eron, alți NPC, inamici. */
    private boolean canStandOn(int col, int row) {
        if (col < 0 || row < 0 || col >= gp.maxWorldCol || row >= gp.maxWorldRow) return false;
        if (!"Eldoria".equals(gp.tileM.currentZone)) return false;
        int id = gp.tileM.mapTileNum[col][row];
        if (!wanderTile(id) || gp.tileM.tile[id] == null || gp.tileM.tile[id].collision) return false;
        int ts = gp.tileSize;
        if (touchesPlayer(col * ts, row * ts)) return false;
        for (NPC n : gp.npc) {
            if (n == null || n == this) continue;
            int nc = (n.worldX + ts / 2) / ts, nr = (n.worldY + ts / 2) / ts;
            if (Math.abs(nc - col) + Math.abs(nr - row) == 0) return false;
            if (n.walking && Math.abs(n.targetX / ts - col) + Math.abs(n.targetY / ts - row) == 0) return false;
        }
        for (var e : gp.enemy) if (e != null && Math.abs((e.worldX + ts / 2) / ts - col) + Math.abs((e.worldY + ts / 2) / ts - row) == 0) return false;
        return true;
    }

    /** Ar atinge NPC-ul, la poziția dată, dreptunghiul lui Eron (puțin mărit)? */
    private boolean touchesPlayer(int x, int y) {
        Rectangle me = new Rectangle(x + hitBox.x, y + hitBox.y, hitBox.width, hitBox.height);
        var p = gp.player;
        Rectangle pr = new Rectangle(p.worldX + p.hitBox.x - 4, p.worldY + p.hitBox.y - 4, p.hitBox.width + 8, p.hitBox.height + 8);
        return me.intersects(pr);
    }

    public void draw(Graphics2D g2) {
        // Poziția pe ecran vine din cameră (aceeași pentru tile-uri, Eron, inamici)
        // Leagăn lent pe verticală: personajele nu stau perfect nemișcate
        float bobPhase = (worldX * 7 + worldY * 13) % (int) Main.GameFeel.IDLE_BOB_PERIOD_TICKS;
        int bob = walking ? 0 : Math.round((float) Math.sin((gp.fx.ticks + bobPhase) * 2 * Math.PI / Main.GameFeel.IDLE_BOB_PERIOD_TICKS)
                * Main.GameFeel.IDLE_BOB_PX);
        int screenX = gp.camera.screenX(worldX);
        int screenY = gp.camera.screenY(worldY) + bob;

        // Verificăm dacă NPC-ul se află în interiorul ferestrei
        if (screenX + gp.tileSize > 0 &&
                screenX < gp.screenWidth &&
                screenY + gp.tileSize > 0 &&
                screenY < gp.screenHeight) {

            java.awt.image.BufferedImage img = spriteSet != null ? spriteSet.frame(direction, walking, animTick * 2) : down1;
            if (img != null) {
                Object oldHint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                if (spriteSet != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // pixel art: fără estompare
                g2.drawImage(img, screenX, screenY, gp.tileSize, gp.tileSize, null);
                if (oldHint != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            }
            drawMarker(g2, screenX + gp.tileSize / 2, screenY - 6);
        }
    }

    // Semne de misiune deasupra capului: ! = are o misiune de oferit, ? = gata de predat / în desfășurare
    public static final int MARKER_NONE = 0, MARKER_AVAILABLE = 1, MARKER_READY = 2, MARKER_ACTIVE = 3, MARKER_TALK = 4;

    /** Ce semn să apară deasupra acestui NPC (DataNPC îl calculează din starea misiunilor). */
    public int markerKind() { return MARKER_NONE; }

    /** CutsceneUI îl ridică cât desenează NPC-urile: în scene nu apar semnele de misiune. */
    public static boolean markersHidden = false;

    private void drawMarker(Graphics2D g2, int cx, int bottomY) {
        if (markersHidden) return;
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