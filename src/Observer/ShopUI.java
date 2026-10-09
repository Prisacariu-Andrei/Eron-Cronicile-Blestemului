package Observer;

import Entity.Cards.CardFactory;
import Entity.Items.Card;
import Entity.Items.Rune;
import Entity.Runes.RuneFactory;
import Main.Easing;
import Main.Economy;
import Main.GameFeel;
import Main.GamePanel;
import Main.SoundManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Magazinul: două file, "Cumpără" (marfa negustorului) și "Vinde" (cărțile din pachetul lui Eron).
 * Prețurile vin din res/Data/economy.json (Economy); marfa fiecărui negustor din res/Data/npcs.json.
 * Controale (în ShopState): A/D schimbă fila, W/S aleg, ENTER cumpără/vinde, ESC iese.
 */
public class ShopUI {
    private final GamePanel gp;

    private String merchant = "";
    private final List<String> stock = new ArrayList<>();       // cheile din CardFactory
    private final Map<String, Card> preview = new LinkedHashMap<>(); // cheie -> carte (pentru nume, cost, descriere)
    private final Map<String, Rune> runePreview = new LinkedHashMap<>(); // "rune:id" -> rună (marfa de tip rună)

    private int tab = 0;                    // 0 = cumpără, 1 = vinde
    private final int[] selected = new int[2];
    private String message = "";
    private Color messageColor = Color.WHITE;
    private int messageTicks = 0, shakeTicks = 0, openTicks = 0;
    private float coinsShown = 0;           // numărătoare care "curge" spre valoarea reală

    private static final int VISIBLE_ROWS = 7;
    private static final int ROW_H = 66;
    private static final int MESSAGE_TICKS = 110;

    public ShopUI(GamePanel gp) {
        this.gp = gp;
    }

    public void open(String merchantName, List<String> stockKeys) {
        merchant = merchantName;
        stock.clear();
        preview.clear();
        runePreview.clear();
        for (String key : stockKeys) {
            try {
                if (key.startsWith("rune:")) {
                    Rune r = RuneFactory.createRune(key.substring(5));
                    if (r != null) { stock.add(key); runePreview.put(key, r); }
                    continue;
                }
                Card c = CardFactory.createCard(key, gp.player);
                if (c != null) { stock.add(key); preview.put(key, c); }
            } catch (Exception ignored) { /* cheie necunoscută: nu apare în magazin */ }
        }
        tab = 0;
        selected[0] = selected[1] = 0;
        message = "";
        messageTicks = shakeTicks = openTicks = 0;
        coinsShown = gp.player.coins;
    }

    public void update() {
        openTicks++;
        if (messageTicks > 0) messageTicks--;
        if (shakeTicks > 0) shakeTicks--;
        coinsShown += (gp.player.coins - coinsShown) * 0.2f;
        if (Math.abs(gp.player.coins - coinsShown) < 0.5f) coinsShown = gp.player.coins;
    }

    // =====================================================================================
    // INTRĂRI
    // =====================================================================================

    public void switchTab(int dir) {
        int t = Math.floorMod(tab + dir, 2);
        if (t != tab) { tab = t; click(); }
    }

    public void move(int dir) {
        int n = tab == 0 ? stock.size() : sellEntries().size();
        if (n == 0) return;
        selected[tab] = Math.floorMod(selected[tab] + dir, n);
        click();
    }

    /** ENTER: cumpără sau vinde elementul ales. */
    public void confirm() {
        if (tab == 0) buy(); else sell();
    }

    private void buy() {
        if (stock.isEmpty()) return;
        String key = stock.get(Math.min(selected[0], stock.size() - 1));
        if (runePreview.containsKey(key)) { buyRune(key); return; }
        int price = Economy.get().buyPrice(key);
        if (gp.player.coins < price) {
            say("Nu ai destule monede! Îți mai trebuie " + (price - gp.player.coins) + ".", new Color(255, 90, 90));
            deny();
            return;
        }
        gp.player.coins -= price;
        gp.player.inventory.addCardToDeck(key, gp.player);
        say("Ai cumpărat " + preview.get(key).name + " (-" + price + " monede)", new Color(120, 230, 140));
        coinSound();
    }

    private void sell() {
        List<String[]> entries = sellEntries();
        if (entries.isEmpty()) { say("Nu ai nicio carte de vândut.", new Color(255, 200, 90)); deny(); return; }
        String name = entries.get(Math.min(selected[1], entries.size() - 1))[0];
        if (entries.get(Math.min(selected[1], entries.size() - 1))[2].equals("rune")) { sellRune(name); return; }

        int min = Economy.get().minDeckSize();
        if (gp.player.inventory.cards.size() <= min) {
            say("Trebuie să păstrezi cel puțin " + min + " cărți în pachet!", new Color(255, 90, 90));
            deny();
            return;
        }
        int price = Economy.get().sellPriceForCardName(name, gp.player);
        for (int i = 0; i < gp.player.inventory.cards.size(); i++) {
            if (gp.player.inventory.cards.get(i).name.equals(name)) {
                gp.player.inventory.cards.remove(i);
                break;
            }
        }
        gp.player.coins += price;
        say("Ai vândut " + name + " (+" + price + " monede)", new Color(255, 215, 90));
        coinSound();
        int left = sellEntries().size();
        if (selected[1] >= left) selected[1] = Math.max(0, left - 1);
    }

    private void buyRune(String key) {
        Rune r = runePreview.get(key);
        if (gp.player.inventory.ownsRune(r.id)) { say("Ai deja " + r.name + ".", new Color(255, 200, 90)); deny(); return; }
        if (gp.player.inventory.runeBagFull()) { say("Rucsacul de rune este plin!", new Color(255, 90, 90)); deny(); return; }
        if (gp.player.coins < r.price) {
            say("Nu ai destule monede! Îți mai trebuie " + (r.price - gp.player.coins) + ".", new Color(255, 90, 90));
            deny();
            return;
        }
        gp.player.coins -= r.price;
        Rune bought = RuneFactory.createRune(r.id);
        gp.player.giveRune(bought); // se echipează singură dacă are loc
        say("Ai cumpărat " + r.name + (bought.isEquipped ? " (echipată)" : "") + " (-" + r.price + " monede)", new Color(120, 230, 140));
        coinSound();
    }

    private void sellRune(String name) {
        Rune r = null;
        for (Rune x : gp.player.inventory.runes) if (x.name.equals(name)) { r = x; break; }
        if (r == null) return;
        int price = Economy.get().sellPriceOf(r.price);
        gp.player.inventory.runes.remove(r);
        gp.player.refreshRunes(); // dacă era echipată, își pierde bonusul
        gp.player.coins += price;
        say("Ai vândut " + name + " (+" + price + " monede)", new Color(255, 215, 90));
        coinSound();
        int left = sellEntries().size();
        if (selected[1] >= left) selected[1] = Math.max(0, left - 1);
    }

    private void say(String text, Color color) {
        message = text;
        messageColor = color;
        messageTicks = MESSAGE_TICKS;
    }

    private void deny() {
        shakeTicks = 14;
        if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("deny");
    }

    private void coinSound() {
        if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("coin");
        gp.fx.burst(2, 800, 330, 12, 0xFFD36B, 5f, 34, 5f, 0.1f);
    }

    private void click() {
        if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("click");
    }

    /** Cărțile din pachet, grupate pe nume: [nume, număr]. */
    private List<String[]> sellEntries() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Card c : gp.player.inventory.cards) counts.merge(c.name, 1, Integer::sum);
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) out.add(new String[]{e.getKey(), String.valueOf(e.getValue()), "card"});
        for (Rune r : gp.player.inventory.runes) if (!r.bound) out.add(new String[]{r.name, "1", "rune"});
        return out;
    }

    // =====================================================================================
    // DESENARE
    // =====================================================================================

    public void draw(Graphics2D g2) {
        int W = gp.screenWidth, H = gp.screenHeight;
        float open = Easing.outCubic(openTicks / 12f);

        g2.setColor(new Color(0, 0, 0, Math.round(170 * open)));
        g2.fillRect(0, 0, W, H);

        int px = 250, py = 70 + Math.round((1f - open) * 40), pw = W - 500, ph = H - 140;
        if (shakeTicks > 0) px += Math.round((float) Math.sin(shakeTicks * 2.4) * shakeTicks * 0.8f); // "nu" cu capul
        Composite oldC = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, open)));

        g2.setColor(new Color(22, 20, 34, 245));
        g2.fillRoundRect(px, py, pw, ph, 28, 28);
        g2.setColor(new Color(212, 175, 55));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, pw, ph, 28, 28);

        // Antet: numele negustorului și monedele
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 30F));
        g2.setColor(new Color(212, 175, 55));
        g2.drawString(merchant, px + 34, py + 52);
        drawCoin(g2, px + pw - 190, py + 28, 26);
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28F));
        g2.drawString(String.valueOf(Math.round(coinsShown)), px + pw - 156, py + 52);

        // File
        drawTab(g2, "CUMPĂRĂ", px + 34, py + 80, tab == 0);
        drawTab(g2, "VINDE", px + 214, py + 80, tab == 1);

        // Listă
        int listY = py + 140;
        if (tab == 0) drawBuyRows(g2, px + 34, listY, pw - 68);
        else drawSellRows(g2, px + 34, listY, pw - 68);

        // Mesaj de feedback
        if (messageTicks > 0) {
            float a = Easing.clamp01(messageTicks / 14f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, a * open)));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24F));
            g2.setColor(messageColor);
            g2.drawString(message, px + pw / 2 - g2.getFontMetrics().stringWidth(message) / 2, py + ph - 62);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, open)));
        }

        // Indicații
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 17F));
        g2.setColor(new Color(170, 170, 185));
        String hint = "[A/D] schimbă fila    [W/S] alege    [ENTER] " + (tab == 0 ? "cumpără" : "vinde") + "    [ESC] ieși";
        g2.drawString(hint, px + pw / 2 - g2.getFontMetrics().stringWidth(hint) / 2, py + ph - 24);

        g2.setComposite(oldC);
    }

    private void drawTab(Graphics2D g2, String label, int x, int y, boolean active) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
        g2.setColor(active ? Color.YELLOW : new Color(130, 130, 150));
        g2.drawString(label, x, y + 22);
        if (active) {
            int w = g2.getFontMetrics().stringWidth(label);
            g2.fillRect(x, y + 30, w, 3);
        }
    }

    private void drawBuyRows(Graphics2D g2, int x, int y, int w) {
        if (stock.isEmpty()) { emptyText(g2, x, y, "Negustorul nu are nimic de vânzare."); return; }
        int first = scrollStart(selected[0], stock.size());
        for (int i = first; i < Math.min(stock.size(), first + VISIBLE_ROWS); i++) {
            String key = stock.get(i);
            if (runePreview.containsKey(key)) {
                Rune r = runePreview.get(key);
                boolean owned = gp.player.inventory.ownsRune(r.id);
                drawRow(g2, x, y + (i - first) * ROW_H, w, i == selected[0], r.name, -1, r.buffText() + " - " + firstLine(r.description),
                        owned ? "Deținută" : "Cumpără  " + r.price, !owned && gp.player.coins >= r.price, r);
                continue;
            }
            Card c = preview.get(key);
            int price = Economy.get().buyPrice(key);
            drawRow(g2, x, y + (i - first) * ROW_H, w, i == selected[0], c.name, c.manaCost, firstLine(c.getDynamicDescription(gp.player)),
                    "Cumpără  " + price, gp.player.coins >= price, null);
        }
        drawScrollHint(g2, x + w + 10, y, first, stock.size());
    }

    private void drawSellRows(Graphics2D g2, int x, int y, int w) {
        List<String[]> entries = sellEntries();
        if (entries.isEmpty()) { emptyText(g2, x, y, "Pachetul tău este gol."); return; }
        int first = scrollStart(selected[1], entries.size());
        boolean canSell = gp.player.inventory.cards.size() > Economy.get().minDeckSize();
        for (int i = first; i < Math.min(entries.size(), first + VISIBLE_ROWS); i++) {
            String name = entries.get(i)[0];
            if (entries.get(i)[2].equals("rune")) {
                Rune r = null;
                for (Rune q : gp.player.inventory.runes) if (q.name.equals(name)) { r = q; break; }
                if (r != null) drawRow(g2, x, y + (i - first) * ROW_H, w, i == selected[1], name + (r.isEquipped ? "  (echipată)" : ""), -1,
                        r.buffText() + " - " + firstLine(r.description), "Vinde  +" + Economy.get().sellPriceOf(r.price), true, r);
                continue;
            }
            int count = Integer.parseInt(entries.get(i)[1]);
            Card sample = null;
            for (Card c : gp.player.inventory.cards) if (c.name.equals(name)) { sample = c; break; }
            int price = Economy.get().sellPriceForCardName(name, gp.player);
            drawRow(g2, x, y + (i - first) * ROW_H, w, i == selected[1], count > 1 ? name + "  x" + count : name,
                    sample == null ? 0 : sample.manaCost, sample == null ? "" : firstLine(sample.getDynamicDescription(gp.player)),
                    "Vinde  +" + price, canSell, null);
        }
        drawScrollHint(g2, x + w + 10, y, first, entries.size());
    }

    private void drawRow(Graphics2D g2, int x, int y, int w, boolean sel, String name, int mana, String desc, String button, boolean enabled, Rune rune) {
        g2.setColor(sel ? new Color(60, 55, 90, 235) : new Color(36, 34, 52, 220));
        g2.fillRoundRect(x, y, w, ROW_H - 8, 16, 16);
        if (sel) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.15);
            g2.setColor(new Color(255, 215, 0, 170 + Math.round(70 * pulse)));
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(x, y, w, ROW_H - 8, 16, 16);
        }
        // cost în mană
        if (rune != null) RuneIcon.draw(g2, rune, x + 10, y + 7, 44, sel);
        else {
            g2.setColor(new Color(41, 128, 185));
            g2.fillOval(x + 14, y + 12, 34, 34);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
            String m = String.valueOf(mana);
            g2.drawString(m, x + 31 - g2.getFontMetrics().stringWidth(m) / 2, y + 36);
        }
        // nume + descriere
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 23F));
        g2.setColor(Color.WHITE);
        g2.drawString(name, x + 66, y + 28);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16F));
        g2.setColor(new Color(185, 185, 200));
        g2.drawString(desc, x + 66, y + 48);
        // buton
        int bw = 190, bh = 40, bx = x + w - bw - 14, by = y + 9;
        g2.setColor(enabled ? new Color(46, 150, 80) : new Color(110, 60, 60));
        if (sel && enabled) g2.setColor(new Color(60, 190, 100));
        g2.fillRoundRect(bx, by, bw, bh, 12, 12);
        g2.setColor(enabled ? Color.WHITE : new Color(210, 170, 170));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
        g2.drawString(button, bx + 16, by + 27);
        drawCoin(g2, bx + bw - 36, by + 8, 24);
    }

    private void emptyText(Graphics2D g2, int x, int y, String text) {
        g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 22F));
        g2.setColor(new Color(160, 160, 175));
        g2.drawString(text, x + 10, y + 40);
    }

    private void drawScrollHint(Graphics2D g2, int x, int y, int first, int total) {
        if (total <= VISIBLE_ROWS) return;
        g2.setColor(new Color(120, 120, 140));
        g2.fillRoundRect(x, y, 6, VISIBLE_ROWS * ROW_H - 8, 4, 4);
        g2.setColor(new Color(212, 175, 55));
        int barH = Math.max(24, (VISIBLE_ROWS * ROW_H - 8) * VISIBLE_ROWS / total);
        int barY = y + (VISIBLE_ROWS * ROW_H - 8 - barH) * first / Math.max(1, total - VISIBLE_ROWS);
        g2.fillRoundRect(x, barY, 6, barH, 4, 4);
    }

    private int scrollStart(int sel, int total) {
        return Math.max(0, Math.min(sel - VISIBLE_ROWS / 2, total - VISIBLE_ROWS));
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        String line = s.split("\n")[0];
        return line.length() > 64 ? line.substring(0, 61) + "..." : line;
    }

    /** Monedă desenată în cod: disc auriu cu margine și o strălucire. */
    public static void drawCoin(Graphics2D g2, int x, int y, int size) {
        g2.setColor(new Color(176, 124, 20));
        g2.fillOval(x, y, size, size);
        g2.setColor(new Color(250, 205, 70));
        g2.fillOval(x + 2, y + 2, size - 4, size - 4);
        g2.setColor(new Color(255, 240, 160));
        g2.fillOval(x + size / 4, y + size / 5, size / 4, size / 5);
        g2.setColor(new Color(176, 124, 20));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, size * 0.55f));
        String s = "$";
        g2.drawString(s, x + size / 2 - g2.getFontMetrics().stringWidth(s) / 2, y + size * 3 / 4);
    }
}
