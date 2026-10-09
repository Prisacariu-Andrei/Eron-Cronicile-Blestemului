package Observer;

import Main.GamePanel;
import Entity.Items.Card;
import Entity.Items.Rune;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class InventoryUI {
    GamePanel gp;
    public int slotRow = 0;
    public int runeCursor = 0;
    private String message = "";
    private Color messageColor = Color.WHITE;
    private int messageTicks = 0;
    private final int MAX_VISIBLE_ROWS = 15; // Definit ca variabilă de clasă pentru a fi accesibilă peste tot

    public InventoryUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        if(gp.player == null) return;

        // 1. CALCULĂM DIMENSIUNILE (Prima dată)
        int x = gp.tileSize * 2;
        int y = gp.tileSize;
        int width = gp.screenWidth - (gp.tileSize * 4);
        int height = gp.screenHeight - (gp.tileSize * 2);

        // 2. DESENĂM FUNDALUL
        g2.setColor(new Color(20, 20, 30, 240));
        g2.fillRoundRect(x, y, width, height, 35, 35);

        g2.setColor(new Color(255, 215, 0, 200));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, width, height, 35, 35);

        // 3. TITLU ȘI LINIE
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 36F));
        g2.drawString("GRIMOARUL LUI ERON", x + gp.tileSize, y + gp.tileSize);
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(x + gp.tileSize, y + gp.tileSize + 10, x + width - gp.tileSize, y + gp.tileSize + 10);

        // 4. SECȚIUNE RUNE
        drawRuneSection(g2, x, y);

        // 5. SECȚIUNE CĂRȚI
        ArrayList<Card> sortedCards = new ArrayList<>(gp.player.inventory.cards);
        sortedCards.sort(Comparator.comparing(card -> card.name));

        int startX = x + width / 2;
        int startY = y + gp.tileSize * 3;

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
        g2.setColor(new Color(255, 100, 100));
        g2.drawString("PACHETUL DE LUPTĂ:", startX, y + gp.tileSize * 2 + 20);

        g2.setFont(g2.getFont().deriveFont(18F));
        for (int i = 0; i < MAX_VISIBLE_ROWS; i++) {
            int cardIndex = i + slotRow;
            if (cardIndex < sortedCards.size()) {
                Card c = sortedCards.get(cardIndex);
                int currentY = startY + (i * 30);

                g2.setColor(new Color(100, 200, 255));
                g2.drawString("[" + c.manaCost + "]", startX, currentY);
                g2.setColor(Color.WHITE);
                g2.drawString(c.name, startX + 40, currentY);
            }
        }

        // 6. INDICATOR SCROLL
        if (sortedCards.size() > MAX_VISIBLE_ROWS) {
            drawScrollIndicator(g2, startX + (width / 2) - 60, startY, sortedCards.size());
        }

        // OBIECTE CHEIE: artefactele primite de la boss-i (gates.json)
        drawKeyItems(g2, startX, y + height - gp.tileSize * 2 + 4);

        // 7. INFO SUBSOL
        g2.setFont(g2.getFont().deriveFont(14F));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Cărți: " + sortedCards.size() + " | Rune: " + gp.player.inventory.runes.size() + "/" + Entity.Runes.RuneFactory.bagSize(), x + gp.tileSize, y + height - 30);
        g2.drawString("[A/D] alege rună   [ENTER] echipează/dezechipează   [W/S] derulează pachetul   [I] închide", x + width - 640, y + height - 30);
        if (messageTicks > 0) {
            messageTicks--;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
            g2.setColor(messageColor);
            g2.drawString(message, x + gp.tileSize, y + height - 62);
        }
    }

    private void drawRuneSection(Graphics2D g2, int x, int y) {
        var inv = gp.player.inventory;
        int left = x + gp.tileSize, slots = inv.runeSlots();
        clampCursor();

        // --- sloturile echipate ---
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
        g2.setColor(Color.CYAN);
        g2.drawString("RUNE ECHIPATE  " + inv.equippedCount() + "/" + slots, left, y + gp.tileSize * 2 + 20);
        int sy = y + gp.tileSize * 2 + 40, box = 84, gap = 18;
        java.util.List<Rune> equipped = new ArrayList<>();
        for (Rune r : inv.runes) if (r.isEquipped) equipped.add(r);
        for (int i = 0; i < slots; i++) {
            int bx = left + i * (box + gap);
            g2.setColor(new Color(34, 34, 52));
            g2.fillRoundRect(bx, sy, box, box, 18, 18);
            Rune r = i < equipped.size() ? equipped.get(i) : null;
            boolean sel = r != null && r == selectedRune();
            g2.setColor(sel ? Color.YELLOW : new Color(110, 110, 140));
            g2.setStroke(sel ? new BasicStroke(3) : new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f));
            if (r != null) g2.setStroke(new BasicStroke(sel ? 3 : 2));
            g2.drawRoundRect(bx, sy, box, box, 18, 18);
            g2.setStroke(new BasicStroke(1));
            if (r != null) RuneIcon.draw(g2, r, bx + 6, sy + 6, box - 12, true);
        }

        // --- rucsacul de rune ---
        int by = sy + box + 44;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("RUNELE TALE  " + inv.runes.size() + "/" + Entity.Runes.RuneFactory.bagSize(), left, by);
        if (inv.runes.isEmpty()) {
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 18F));
            g2.setColor(Color.GRAY);
            g2.drawString("Nicio rună încă. Învinge inamici sau caută un negustor.", left, by + 36);
            return;
        }
        int rows = 6, rowH = 36, first = Math.max(0, Math.min(runeCursor - rows / 2, inv.runes.size() - rows));
        for (int i = first; i < Math.min(inv.runes.size(), first + rows); i++) {
            Rune r = inv.runes.get(i);
            int ry = by + 12 + (i - first) * rowH;
            boolean sel = i == runeCursor;
            g2.setColor(sel ? new Color(70, 62, 105, 235) : new Color(36, 34, 52, 200));
            g2.fillRoundRect(left, ry, 560, rowH - 4, 12, 12);
            if (sel) { g2.setColor(Color.YELLOW); g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(left, ry, 560, rowH - 4, 12, 12); g2.setStroke(new BasicStroke(1)); }
            RuneIcon.draw(g2, r, left + 4, ry + 1, rowH - 6, false);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 19F));
            g2.setColor(RuneIcon.rarityColor(r.rarity));
            g2.drawString(r.name, left + rowH + 10, ry + 23);
            if (r.isEquipped) {
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 15F));
                g2.setColor(new Color(120, 230, 140));
                g2.drawString("ECHIPATĂ", left + 560 - 90, ry + 22);
            }
        }

        // --- tooltip (inspectare) ---
        Rune r = selectedRune();
        int ty = by + 12 + rows * rowH + 8, th = y + gp.screenHeight - gp.tileSize * 2 - 84 - ty;
        g2.setColor(new Color(14, 14, 24, 240));
        g2.fillRoundRect(left, ty, 560, th, 16, 16);
        g2.setColor(RuneIcon.rarityColor(r.rarity));
        g2.drawRoundRect(left, ty, 560, th, 16, 16);
        RuneIcon.draw(g2, r, left + 10, ty + 10, 56, true);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
        g2.drawString(r.name + "  (" + r.rarity + ")", left + 80, ty + 32);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 17F));
        g2.setColor(new Color(120, 230, 140));
        g2.drawString(r.buffText(), left + 80, ty + 56);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16F));
        g2.setColor(new Color(205, 205, 220));
        int ly = ty + 82;
        for (String line : wrap(g2, r.description, 530)) { g2.drawString(line, left + 14, ly); ly += 21; }
        g2.setColor(new Color(150, 150, 170));
        g2.drawString(r.bound ? "Nu poate fi vândută." : "Valoare de vânzare: " + Main.Economy.get().sellPriceOf(r.price) + " monede (la negustor)", left + 14, ty + th - 12);
    }

    private static java.util.List<String> wrap(Graphics2D g2, String text, int width) {
        java.util.List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() > 0 && g2.getFontMetrics().stringWidth(cur + " " + w) > width) { lines.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private void clampCursor() {
        int n = gp.player.inventory.runes.size();
        runeCursor = n == 0 ? 0 : Math.max(0, Math.min(runeCursor, n - 1));
    }

    public Rune selectedRune() {
        var runes = gp.player.inventory.runes;
        return runes.isEmpty() ? null : runes.get(Math.max(0, Math.min(runeCursor, runes.size() - 1)));
    }

    /** A/D (sau ←/→) mută cursorul prin rune. */
    public void moveRuneCursor(int dir) {
        int n = gp.player.inventory.runes.size();
        if (n == 0) return;
        runeCursor = Math.floorMod(runeCursor + dir, n);
        Main.SoundManager.getInstance().playSE("click");
    }

    /** ENTER: echipează sau dezechipează runa aleasă. */
    public void toggleSelectedRune() {
        Rune r = selectedRune();
        if (r == null) return;
        boolean wasEquipped = r.isEquipped;
        String msg = gp.player.toggleRune(r);
        boolean ok = r.isEquipped != wasEquipped;
        message = msg;
        messageColor = ok ? new Color(120, 230, 140) : new Color(255, 110, 110);
        messageTicks = 120;
        Main.SoundManager.getInstance().playSE(ok ? (r.isEquipped ? "ability" : "click") : "deny");
        if (ok && r.isEquipped) gp.fx.burst(2, 330, 360, 14, r.color, 5f, 30, 4f, 0.1f);
    }

    private void drawKeyItems(Graphics2D g2, int x, int y) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("OBIECTE CHEIE:", x, y);
        var items = gp.story.keyItems();
        g2.setFont(g2.getFont().deriveFont(15F));
        if (items.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Niciun artefact încă. Învinge boss-ii fiecărei zone.", x, y + 26);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            var it = items.get(i);
            int ly = y + 12 + i * 22;
            int cx = x + 8, cy = ly + 2;
            g2.setColor(new Color(it.color));
            g2.fillPolygon(new int[]{cx, cx + 8, cx, cx - 8}, new int[]{cy - 9, cy, cy + 9, cy}, 4);
            g2.setColor(Color.WHITE);
            g2.drawString(it.name, x + 24, ly + 6);
            g2.setColor(new Color(165, 165, 185));
            g2.drawString("- " + it.purpose, x + 24 + g2.getFontMetrics().stringWidth(it.name) + 8, ly + 6);
        }
    }

    private void drawScrollIndicator(Graphics2D g2, int scrollX, int startY, int totalCards) {
        int barHeight = MAX_VISIBLE_ROWS * 30;
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(scrollX, startY - 20, 5, barHeight);

        int knobHeight = Math.max(20, (int)((double)MAX_VISIBLE_ROWS / totalCards * barHeight));
        int knobY = startY - 20 + (slotRow * barHeight / totalCards);

        g2.setColor(Color.YELLOW);
        g2.fillRect(scrollX - 2, knobY, 9, knobHeight);
    }
}