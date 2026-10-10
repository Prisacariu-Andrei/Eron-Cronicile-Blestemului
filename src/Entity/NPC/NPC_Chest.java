package Entity.NPC;

import Entity.Cards.CardFactory;
import Entity.Runes.RuneFactory;
import GameState.PlayState;
import Main.GamePanel;
import Main.Json;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Un cufăr din res/Data/chests.json (se deschide cu F). Conține monede, o rună și/sau o carte. Starea "deschis" e un flag al poveștii
 * (chest_<zonă>_<număr>), deci se salvează odată cu jocul.
 */
public class NPC_Chest extends NPC {
    private final String flag;
    private final Map<String, Object> loot;
    private static final Random RNG = new Random();

    public NPC_Chest(GamePanel gp, int col, int row, String id, Map<String, Object> loot) {
        super(gp, gp.tileSize * col, gp.tileSize * row);
        this.name = "Cufăr";
        this.flag = "chest_" + id;
        this.loot = loot;
    }

    public boolean isOpen() { return gp.story.has(flag); }

    @Override public boolean usesF() { return true; }
    @Override public void getImage() { }
    @Override public void setDialogue() { }

    /** E în loc de F: doar ne spune ce să apăsăm (fără dialog). */
    @Override
    public void speak() {
        gp.activeNpcIndex = -1;
        gp.changeState(new PlayState(gp));
        gp.ui.message = isOpen() ? "Cufărul e gol." : "Apasă [F] ca să deschizi cufărul.";
        gp.ui.messageOn = true;
    }

    @Override
    public void use() {
        if (isOpen()) { gp.ui.message = "Cufărul e gol."; gp.ui.messageOn = true; return; }
        gp.story.set(flag);

        List<String> got = new ArrayList<>();
        List<Object> range = Json.list(loot.get("coins"));
        if (range.size() >= 2) {
            int min = Json.integer(range.get(0), 0), max = Json.integer(range.get(1), min);
            int coins = max <= min ? min : min + RNG.nextInt(max - min + 1);
            gp.player.addCoins(coins);
            got.add("+" + coins + " monede");
        }
        String runeId = Json.str(loot.get("rune"), null);
        if (runeId != null) {
            var rune = RuneFactory.createRune(runeId);
            if (rune != null && gp.player.giveRune(rune)) {
                got.add(rune.name);
            } else {   // o ai deja sau rucsacul e plin: monede în loc
                int c = Json.integer(Json.map(Json.parseResource("/Data/chests.json")).get("runeDuplicateCoins"), 60);
                gp.player.addCoins(c);
                got.add("+" + c + " monede (rună deja deținută)");
            }
        }
        String cardId = Json.str(loot.get("card"), null);
        if (cardId != null) {
            gp.player.inventory.addCardToDeck(cardId, gp.player);
            got.add("carte: " + CardFactory.createCard(cardId, gp.player).name);
        }
        gp.ui.message = "Cufăr: " + String.join(", ", got);
        gp.ui.messageOn = true;
        gp.fx.rewardBurst(gp.camera.screenX(worldX) + gp.tileSize / 2, gp.camera.screenY(worldY));
        gp.fx.questSound(true);
    }

    @Override
    public void draw(Graphics2D g2) {
        int sx = gp.camera.screenX(worldX), sy = gp.camera.screenY(worldY), ts = gp.tileSize;
        if (sx + ts < 0 || sx > gp.screenWidth || sy + ts < 0 || sy > gp.screenHeight) return;
        boolean open = isOpen();
        int bx = sx + ts / 8, by = sy + ts / 3, bw = ts * 3 / 4, bh = ts / 2;

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(bx - 2, by + bh - 6, bw + 4, 14);
        g2.setColor(new Color(112, 70, 36));
        g2.fillRoundRect(bx, by + bh / 3, bw, bh * 2 / 3, 8, 8);                      // corpul
        g2.setColor(new Color(76, 46, 22));
        g2.drawRoundRect(bx, by + bh / 3, bw, bh * 2 / 3, 8, 8);
        if (open) {
            g2.setColor(new Color(30, 18, 10));
            g2.fillRect(bx + 4, by + bh / 3 - 2, bw - 8, 8);                          // interiorul
            g2.setColor(new Color(150, 96, 50));
            g2.fillRoundRect(bx - 1, by - bh / 3, bw + 2, bh / 3, 8, 8);              // capacul ridicat
        } else {
            g2.setColor(new Color(150, 96, 50));
            g2.fillRoundRect(bx - 1, by, bw + 2, bh / 2 + 2, 10, 10);                 // capacul
            g2.setColor(new Color(240, 200, 70));
            g2.fillRect(bx + bw / 2 - 4, by + bh / 3, 8, 12);                          // lacătul
            g2.setColor(new Color(76, 46, 22));
            g2.drawRoundRect(bx - 1, by, bw + 2, bh / 2 + 2, 10, 10);
            // indicația [F] când ești aproape
            int dx = gp.player.worldX - worldX, dy = gp.player.worldY - worldY;
            if (dx * dx + dy * dy < ts * ts * 2.4) {
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
                g2.setColor(new Color(0, 0, 0, 180));
                g2.drawString("[F]", sx + ts / 2 - 11, sy + 6 - Math.round(Math.abs((float) Math.sin(gp.fx.ticks * 0.1)) * 5));
                g2.setColor(new Color(255, 230, 120));
                g2.drawString("[F]", sx + ts / 2 - 12, sy + 4 - Math.round(Math.abs((float) Math.sin(gp.fx.ticks * 0.1)) * 5));
            }
        }
    }
}
