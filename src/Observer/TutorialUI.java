package Observer;

import Entity.Cards.Combos;
import Entity.Items.Card;
import Main.GamePanel;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Indiciile pas cu pas din prima luptă (Fiara din Eldoria) și de la prima recompensă. Un indiciu apare când e momentul lui,
 * arată elementul din ecran la care se referă și așteaptă ca jucătorul să facă acțiunea cerută (sau să apese ENTER la cele
 * informative). Fiecare indiciu apare o singură dată (flag "hint:<id>" în salvare); ESC sare peste toate.
 */
public class TutorialUI {
    public enum Hint { SELECT, PLAY, ESSENCE, INTENT, END_TURN, REACTION, REWARD }

    private static final int MIN_SHOW_TICKS = 30;   // ca un ENTER ținut apăsat să nu închidă indiciile pe loc
    private static final int BATTLE_DELAY_TICKS = 110;   // cărțile intră în mână înainte să apară primul indiciu

    private final GamePanel gp;
    private Hint active = null;
    private boolean battleGuide = false;
    private int shownTicks = 0, battleTicks = 0;
    private int handAtStart, manaAtStart;

    public TutorialUI(GamePanel gp) {
        this.gp = gp;
    }

    private static String flag(Hint h) {
        return switch (h) {
            case SELECT -> "hint:select";
            case PLAY -> "hint:play";
            case ESSENCE -> "hint:essence";
            case INTENT -> "hint:intent";
            case END_TURN -> "hint:end";
            case REACTION -> "hint:reaction";
            case REWARD -> "hint:reward";
        };
    }

    private boolean done(Hint h) { return gp.story.has(flag(h)); }

    private void finish(Hint h) {
        gp.story.set(flag(h));
        if (active == h) active = null;
    }

    /** Sare peste toate indiciile (ESC). */
    private void skipAll() {
        for (Hint h : Hint.values()) gp.story.set(flag(h));
        gp.story.set("hint:skipped");
        active = null;
        battleGuide = false;
    }

    public boolean isActive() { return active != null; }

    // ---------------------------------------------------------------- viața indiciilor

    /** Începutul unei lupte: ghidul e pornit doar în prima luptă, cea cu Fiara Blestemului din Eldoria. */
    public void onBattleStart() {
        active = null;
        battleTicks = 0;
        var e = gp.player.currentEnemy;
        battleGuide = !gp.story.has("hint:skipped") && !gp.story.has("fiara") && "Eldoria".equals(gp.tileM.currentZone)
                && e != null && e.name != null && e.name.startsWith("Lup Slăbit") && !(done(Hint.SELECT) && done(Hint.END_TURN));
    }

    /** Apelată la fiecare tick din BattleState. */
    public void update() {
        tick();
        if (!battleGuide) return;
        battleTicks++;
        if (active != null) {
            if (active == Hint.PLAY && (gp.player.inventory.hand.size() < handAtStart || gp.player.currentMana < manaAtStart)) finish(Hint.PLAY);
            return;
        }
        if (battleTicks < BATTLE_DELAY_TICKS || !gp.player.isPlayerTurn || gp.battleUI.isTransitioning || gp.player.currentEnemy == null) return;
        Hint next = null;
        if (!done(Hint.SELECT)) next = Hint.SELECT;
        else if (!done(Hint.PLAY)) next = Hint.PLAY;
        else if (!done(Hint.ESSENCE)) next = Hint.ESSENCE;
        else if (!done(Hint.INTENT)) next = Hint.INTENT;
        else if (!done(Hint.REACTION) && reactionReady()) next = Hint.REACTION;
        else if (!done(Hint.END_TURN) && done(Hint.INTENT)) next = Hint.END_TURN;
        if (next != null) activate(next);
    }

    /** Cât timp a stat indiciul pe ecran (ticks); numărat în update, nu la desen. */
    public void tick() { if (active != null) shownTicks++; }

    private void activate(Hint h) {
        active = h;
        shownTicks = 0;
        handAtStart = gp.player.inventory.hand.size();
        manaAtStart = gp.player.currentMana;
    }

    /** Vreo carte din mână ar declanșa acum o reacție de elemente (conturul auriu din mână)? */
    private boolean reactionReady() {
        for (Card c : gp.player.inventory.hand) if (Combos.ready(gp.player, c, gp.player.currentEnemy) == 2) return true;
        return false;
    }

    /** Prima recompensă: ce înseamnă monedele, XP-ul și cartea. */
    public void onRewardOpen() {
        active = null;
        battleGuide = false;
        if (!gp.story.has("hint:skipped") && !done(Hint.REWARD)) activate(Hint.REWARD);
    }

    // ---------------------------------------------------------------- tastatura

    private static boolean isSelectKey(int c) {
        return c == KeyEvent.VK_A || c == KeyEvent.VK_D || c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT || (c >= KeyEvent.VK_1 && c <= KeyEvent.VK_8);
    }

    /** Apelată de KeyHandler înaintea oricărei alte reguli. true = tasta e consumată (indiciul așteaptă altceva). */
    public boolean onKey(int code) {
        if (active == null) return false;
        if (code == KeyEvent.VK_ESCAPE) { skipAll(); return true; }
        switch (active) {
            case SELECT -> {
                if (isSelectKey(code)) { finish(Hint.SELECT); return false; }
                return true;
            }
            case PLAY -> { return !(isSelectKey(code) || code == KeyEvent.VK_ENTER); }   // joacă (detectat în update)
            case END_TURN -> {
                if (code == KeyEvent.VK_T) { finish(Hint.END_TURN); return false; }
                return false;   // până apasă T poate continua să joace cărți
            }
            default -> {   // informative: ENTER sau SPACE închide
                if ((code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) && shownTicks >= MIN_SHOW_TICKS) finish(active);
                return true;
            }
        }
    }

    // ---------------------------------------------------------------- desen

    private static String title(Hint h) {
        return switch (h) {
            case SELECT -> "Alege o carte";
            case PLAY -> "Joacă cartea";
            case ESSENCE -> "Esența";
            case INTENT -> "Intenția inamicului";
            case END_TURN -> "Termină tura";
            case REACTION -> "Reacție de elemente!";
            case REWARD -> "Recompensa ta";
        };
    }

    private static String text(Hint h) {
        return switch (h) {
            case SELECT -> "Apasă 1–8 sau A / D ca să alegi o carte din mână. Cartea aleasă se ridică, ca s-o poți citi.";
            case PLAY -> "Apasă ENTER ca să joci cartea aleasă. Costul ei (numărul din cercul albastru) se scade din esență.";
            case ESSENCE -> "Cercul albastru e esența ta: ai 3 pe tură. Fiecare carte costă esență, iar la începutul turei următoare ea se reface.";
            case INTENT -> "Semnul de deasupra inamicului arată ce face în tura lui: roșu = lovitură (cu cât damage), albastru = apărare, verde = vindecare.";
            case END_TURN -> "Când ai terminat, apasă T. Inamicul își joacă mutarea, apoi primești din nou esență și cărți noi.";
            case REACTION -> "O carte cu contur auriu declanșează o reacție, de pildă Foc pe un inamic Înghețat = Topire, cu damage în plus. Încearcă-o!";
            case REWARD -> "Ai primit monede (pentru magazin) și XP (pentru nivel), iar acum alegi 1 din 3 cărți pentru pachet: A / D alege, ENTER confirmă.";
        };
    }

    private Rectangle target(Hint h) {
        return switch (h) {
            case SELECT, PLAY, REACTION -> gp.battleUI.handBounds();
            case ESSENCE -> gp.battleUI.manaBounds();
            case INTENT -> gp.battleUI.intentBounds();
            case END_TURN -> gp.battleUI.endTurnBounds();
            case REWARD -> new Rectangle(gp.screenWidth / 2 - 240, 135, 480, 56);   // rândul cu monede și XP
        };
    }

    public void draw(Graphics2D g2) {
        if (active == null) return;
        int W = gp.screenWidth, H = gp.screenHeight;

        // elementul din ecran la care se referă, cu contur auriu care pulsează
        Rectangle r = target(active);
        float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.14);
        if (r != null) {
            int grow = Math.round(3 * pulse);
            g2.setColor(new Color(255, 214, 90, 40 + Math.round(40 * pulse)));
            g2.fillRoundRect(r.x - grow, r.y - grow, r.width + 2 * grow, r.height + 2 * grow, 18, 18);
            g2.setStroke(new BasicStroke(4f));
            g2.setColor(new Color(255, 214, 90, 170 + Math.round(70 * pulse)));
            g2.drawRoundRect(r.x - grow, r.y - grow, r.width + 2 * grow, r.height + 2 * grow, 18, 18);
            g2.setStroke(new BasicStroke(1f));
        }

        // panoul cu textul
        Font base = g2.getFont();
        int pw = Math.min(980, (int) (W * 0.66));
        g2.setFont(base.deriveFont(Font.PLAIN, 22F));
        List<String> lines = wrap(g2, text(active), pw - 56);
        int ph = 56 + lines.size() * 30 + 40;
        int px = W / 2 - pw / 2;
        int py = active == Hint.REWARD ? H - ph - 24 : Math.round(H * 0.16f);
        g2.setColor(new Color(10, 10, 22, active == Hint.REWARD ? 255 : 232));   // la recompensă acoperă complet rândul de instrucțiuni de jos
        g2.fillRoundRect(px, py, pw, ph, 24, 24);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(212, 175, 55));
        g2.drawRoundRect(px, py, pw, ph, 24, 24);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(base.deriveFont(Font.BOLD, 26F));
        g2.setColor(new Color(255, 214, 90));
        g2.drawString(title(active), px + 28, py + 40);
        g2.setFont(base.deriveFont(Font.PLAIN, 22F));
        g2.setColor(Color.WHITE);
        int ty = py + 76;
        for (String l : lines) { g2.drawString(l, px + 28, ty); ty += 30; }

        g2.setFont(base.deriveFont(Font.ITALIC, 15F));
        g2.setColor(new Color(170, 170, 190));
        boolean info = active == Hint.ESSENCE || active == Hint.INTENT || active == Hint.REACTION || active == Hint.REWARD;
        String foot = (info ? "ENTER – am înțeles" : "Fă acțiunea ca să continui") + "     ESC – sari peste ghid";
        g2.drawString(foot, px + pw - 28 - g2.getFontMetrics().stringWidth(foot), py + ph - 14);
        g2.setFont(base);
    }

    private static List<String> wrap(Graphics2D g2, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() > 0 && g2.getFontMetrics().stringWidth(cur + " " + w) > width) { lines.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
