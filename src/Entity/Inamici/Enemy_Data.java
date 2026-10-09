package Entity.Inamici;

import Entity.Player.Player;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Un inamic definit în res/Data/enemies.json (nume, sprite, viață, damage, rol). Rolurile fac echipele mai interesante:
 * brute atacă și uneori se apără, healer vindecă/protejează aliații, mage lovește cu magie și vindecă un aliat rănit,
 * guard ridică scuturi pe aliați. Se creează prin EnemyFactory (id = numele din JSON).
 */
public class Enemy_Data extends Enemy {
    private final String role;
    private final int healValue, shieldValue, baseDamage;
    private final Random rng = new Random();

    public Enemy_Data(GamePanel gp, int x, int y, Map<String, Object> d) {
        super(gp, x, y);
        name = Json.str(d.get("id"), "?");
        role = Json.str(d.get("role"), "brute");
        maxLife = Json.integer(d.get("hp"), 40);
        life = maxLife;
        baseDamage = Json.integer(d.get("dmg"), 8);
        attackDamage = baseDamage;
        healValue = Json.integer(d.get("heal"), 12);
        shieldValue = Json.integer(d.get("shield"), 8);
        mapSize = Json.number(d.get("mapSize"), 1.0);
        battleSize = Json.number(d.get("battleSize"), 1.0);
        battleMusicKey = Json.str(d.get("music"), "InamiciEldoria");
        if (Json.bool(d.get("passive"), false)) isStatic = true; // luptă doar dacă te atingi tu de el

        int px = (int) (gp.tileSize * mapSize);
        hitBox = new Rectangle(8, 16, Math.max(16, px - 16), Math.max(16, px - 16));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;

        Color tint = parseTint(Json.str(d.get("tint"), ""));
        BufferedImage map = tinted(setup(Json.str(d.get("map"), "/Enemy/Wolf")), tint);
        BufferedImage fight = tinted(setup(Json.str(d.get("fight"), Json.str(d.get("map"), "/Enemy/Wolf"))), tint);
        up1 = up2 = down1 = down2 = left1 = left2 = right1 = right2 = map;
        fightStand = fight;
        setNextIntent();
    }

    @Override public void getImage() { /* sprite-urile se încarcă în constructor, din JSON */ }

    @Override
    public void setNextIntent() {
        int r = rng.nextInt(100);
        Enemy hurt = mostHurt(false);
        double hurtFrac = hurt == null ? 1.0 : (double) hurt.life / hurt.maxLife;

        switch (role) {
            case "healer" -> {
                if (hurt != null && hurtFrac < 0.65 && r < 70) heal();
                else if (r < 55) protect();
                else attack();
            }
            case "mage" -> {
                if (hurt != null && hurtFrac < 0.5 && r < 55) heal();
                else attack();
            }
            case "guard" -> {
                if (r < 60) protect(); else attack();
            }
            default -> { // brute
                if (r < 78) attack();
                else { nextAction = EnemyAction.DEFEND; intentValue = shieldValue; }
            }
        }
    }

    private void attack() { nextAction = EnemyAction.ATTACK; intentValue = baseDamage + rng.nextInt(4); }
    private void heal() { nextAction = EnemyAction.HEAL; intentValue = healValue; }
    private void protect() { nextAction = EnemyAction.DEFEND; intentValue = shieldValue; }

    /** Aliatul cu cea mai mică viață (în procente). includeSelf = dacă el însuși poate fi ales. Null dacă nu e rănit nimeni. */
    private Enemy mostHurt(boolean includeSelf) {
        Enemy best = null;
        double bestFrac = 1.0;
        for (Enemy e : teamMates()) {
            if (!includeSelf && e == this) continue;
            double f = (double) e.life / e.maxLife;
            if (f < bestFrac) { bestFrac = f; best = e; }
        }
        if (best == null && includeSelf && life < maxLife) best = this;
        return best;
    }

    /** Pe cine protejează: aliatul cu cea mai puțină viață, altfel pe sine. */
    private Enemy protectTarget() {
        Enemy weakest = null;
        double bestFrac = 2.0;
        for (Enemy e : teamMates()) {
            if (e == this) continue;
            double f = (double) (e.life + e.shield) / e.maxLife;
            if (f < bestFrac) { bestFrac = f; weakest = e; }
        }
        return weakest != null && rng.nextInt(100) < 70 ? weakest : this;
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        switch (nextAction) {
            case ATTACK -> { super.executeIntent(player); return; } // damage (cu Slăbiciune), apoi alege altă intenție
            case HEAL -> {
                Enemy t = mostHurt(true);
                if (t != null) {
                    t.heal(intentValue);
                    if (t != this) gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, name + " vindecă", new Color(120, 230, 140)));
                }
            }
            case DEFEND -> {
                Enemy t = "brute".equals(role) ? this : protectTarget();
                t.defend(intentValue);
                if (t != this) gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, name + " protejează", new Color(120, 190, 255)));
            }
            default -> { }
        }
        setNextIntent();
    }

    private static Color parseTint(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        try { return new Color(Integer.parseInt(hex.replace("#", ""), 16)); } catch (NumberFormatException e) { return null; }
    }

    /** O copie a sprite-ului înmulțită cu culoarea (păstrează transparența). */
    private static BufferedImage tinted(BufferedImage src, Color tint) {
        if (src == null || tint == null) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) { out.setRGB(x, y, 0); continue; }
                int r = ((p >> 16) & 0xFF) * tint.getRed() / 255;
                int g = ((p >> 8) & 0xFF) * tint.getGreen() / 255;
                int b = (p & 0xFF) * tint.getBlue() / 255;
                // un strop de culoare pură ca să nu iasă prea întunecat
                r = Math.min(255, (r * 3 + tint.getRed()) / 4 + 10);
                g = Math.min(255, (g * 3 + tint.getGreen()) / 4 + 10);
                b = Math.min(255, (b * 3 + tint.getBlue()) / 4 + 10);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }
}
