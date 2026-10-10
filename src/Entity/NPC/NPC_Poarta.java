package Entity.NPC;

import GameState.DialogueState;
import GameState.PlayState;
import Main.GamePanel;

import java.awt.*;

/**
 * Poarta de Obsidian din coridorul Castelului. Închisă, nu lasă pe nimeni să treacă; cu Paloșul Legendar (flag "jean")
 * se dărâmă (dialog + scuturat + scântei) și drumul spre sala tronului e liber. Starea se ține în flag-ul "poarta".
 */
public class NPC_Poarta extends NPC {
    private static final String FLAG = "poarta";
    private final int widthTiles;
    private boolean breaking = false;

    public NPC_Poarta(GamePanel gp, int col, int row, int widthTiles) {
        super(gp, gp.tileSize * col, gp.tileSize * row);
        this.name = "Poarta de Obsidian";
        this.widthTiles = widthTiles;
        hitBox = new Rectangle(0, 0, gp.tileSize * widthTiles, gp.tileSize);
        if (gp.story.has(FLAG)) vanish();   // salvare încărcată după ce poarta căzuse
    }

    private void vanish() {
        worldX = -100000;
        worldY = -100000;
    }

    private boolean isGone() { return worldX < -50000; }

    @Override public boolean usesF() { return true; }
    @Override public void use() {
        if (isGone() || isShowingDialogue) return;
        gp.activeNpcIndex = java.util.Arrays.asList(gp.npc).indexOf(this);
        speak();
    }
    @Override public void getImage() { }

    @Override public void setDialogue() { }

    @Override
    public void speak() {
        if (isShowingDialogue) {            // al doilea E: închide dialogul (și, dacă s-a spart, poarta dispare)
            isShowingDialogue = false;
            gp.activeNpcIndex = -1;
            gp.changeState(new PlayState(gp));
            if (breaking) { breaking = false; vanish(); }
            return;
        }
        isShowingDialogue = true;
        gp.changeState(new DialogueState(gp));
        dialogueIndex = 0;

        if (gp.story.has("jean")) {
            gp.story.set(FLAG);
            breaking = true;
            gp.dialogueUI.currentDialogue = "Ridici Paloșul Legendar în fața porții. Obsidianul geme, apoi se crapă în lumină.\nPorțile Castelului se prăbușesc: calea spre sala tronului e liberă!";
            gp.fx.phaseChange();
            gp.fx.burst(2, gp.camera.screenX(worldX) + gp.tileSize * widthTiles / 2, gp.camera.screenY(worldY) + gp.tileSize / 2, 40, 0xC58BFF, 9f, 50, 7f, 0.05f);
        } else {
            gp.dialogueUI.currentDialogue = "O poartă de obsidian, groasă cât un zid, pecetluită cu runele lui Malakor. Niciun descântec nu o clintește.\nDoar un Paloș legendar, o lamă cinstită, ar putea s-o dărâme.";
        }
        gp.dialogueUI.resetCharIndex();
    }

    @Override
    public void draw(Graphics2D g2) {
        if (isGone()) return;
        int sx = gp.camera.screenX(worldX), sy = gp.camera.screenY(worldY);
        int w = gp.tileSize * widthTiles, h = gp.tileSize;
        if (sx + w < 0 || sx > gp.screenWidth || sy + h < 0 || sy > gp.screenHeight) return;

        float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.06);
        g2.setColor(new Color(24, 20, 38));
        g2.fillRect(sx, sy, w, h);
        g2.setColor(new Color(60, 52, 88));
        for (int i = 0; i < widthTiles; i++) {                       // două canaturi cu rune
            int px = sx + i * gp.tileSize;
            g2.fillRoundRect(px + 4, sy + 3, gp.tileSize - 8, h - 6, 10, 10);
            g2.setColor(new Color(160 + Math.round(60 * pulse), 90, 220, 200));
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(px + gp.tileSize / 2, sy + 12, px + gp.tileSize / 2, sy + h - 12);
            g2.drawOval(px + gp.tileSize / 2 - 7, sy + h / 2 - 7, 14, 14);
            g2.setColor(new Color(60, 52, 88));
        }
        g2.setColor(new Color(120, 90, 190));
        g2.drawRect(sx, sy, w - 1, h - 1);
    }
}
