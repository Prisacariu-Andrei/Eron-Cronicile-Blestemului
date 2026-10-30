package Observer;

import Entity.Cards.CardCatalog;
import Entity.Items.Card;
import Entity.Player.Player;
import Entity.StatusEffect;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fața unei cărți, la fel peste tot (mână, recompensă, magazin, grimoar), după schița din proiect: cadru maro (accent după raritate),
 * cost în cerc albastru, element sus-dreapta, fereastră de artă, nume, "~ Descriere ~", descriere scurtă, rânduri de statistici cu
 * valorile reale (verde = bonus), combo (auriu când e îndeplinit). Se desenează o singură dată la 300x420 într-un cache și apoi se scalează.
 * Arta vine din res/Cards/&lt;id&gt;.png (220x104, exact fereastra de artă) și se desenează separat, peste fața scalată, cu
 * nearest-neighbor (pixel art ascuțit, fără estompare), iar dacă lipsește se desenează un înlocuitor procedural (în cache).
 * Cartea aleasă/ridicată primește un luciu discret care trece peste artă.
 */
public final class CardFace {
    public static final int W = 300, H = 420;
    /** Raportul înălțime/lățime al cărții (5:7). */
    public static final float ASPECT = 1.4f;

    private static final int ART_X = 40, ART_Y = 76, ART_W = 220, ART_H = 104;
    private static final Color INK = new Color(48, 30, 16), BROWN = new Color(112, 64, 30);
    private static final Map<String, BufferedImage> faces = new HashMap<>();
    private static final Map<String, BufferedImage> arts = new HashMap<>();       // înlocuitoarele procedurale
    private static final Map<String, BufferedImage> pngs = new HashMap<>();        // artele din res/Cards (null = lipsește)
    private static final java.util.regex.Pattern STAT = java.util.regex.Pattern.compile("^\\s*(Pagub|Arsur|Armur|Vindec|Aplic|Sc[aâ]ntei|Aliat|Status|Otrav|Cost)", java.util.regex.Pattern.CASE_INSENSITIVE);

    private CardFace() {}

    /** Desenează cartea în dreptunghiul dat. {@code hot} = combo-ul propriu e îndeplinit acum (rândul de combo se aprinde). */
    public static void draw(Graphics2D g2, Card c, Player p, int x, int y, int w, int h, boolean hot) {
        draw(g2, c, p, x, y, w, h, hot, false);
    }

    /** Ca mai sus; {@code shimmer} = cartea e aleasă/ridicată: un luciu discret trece peste artă. */
    public static void draw(Graphics2D g2, Card c, Player p, int x, int y, int w, int h, boolean hot, boolean shimmer) {
        Object old = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(face(c, p, hot), x, y, w, h, null);

        // fereastra de artă, la scara cu care s-a desenat fața
        double sx = w / (double) W, sy = h / (double) H;
        int ax = x + (int) Math.round(ART_X * sx), ay = y + (int) Math.round(ART_Y * sy);
        int aw = (int) Math.round(ART_W * sx), ah = (int) Math.round(ART_H * sy);
        BufferedImage png = png(c);
        if (png != null) {
            // la scara 1:1 pixelii cad exact; altfel, tot nearest (marginile pixelilor rămân tăioase)
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(png, ax, ay, aw, ah, null);
            Stroke s = g2.getStroke();
            g2.setStroke(new BasicStroke((float) Math.max(1.5, 3 * sx)));
            g2.setColor(BROWN);
            g2.drawRect(ax, ay, aw, ah);
            g2.setStroke(s);
        }
        if (shimmer) sheen(g2, ax, ay, aw, ah);
        if (old != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
    }

    private static BufferedImage sheenImg;

    /** Banda de lumină (o dată desenată, apoi doar mutată): trece în diagonală peste artă la ~2,6 secunde, cu pauză. */
    private static void sheen(Graphics2D g2, int ax, int ay, int aw, int ah) {
        if (sheenImg == null) {
            sheenImg = new BufferedImage(64, 104, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheenImg.createGraphics();
            g.setPaint(new LinearGradientPaint(0, 0, 64, 0, new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 255, 255, 0), new Color(255, 250, 230, 105), new Color(255, 255, 255, 0)}));
            g.shear(-0.35, 0);   // bandă ușor înclinată
            g.fillRect(24, 0, 64, 104);
            g.dispose();
        }
        float phase = (System.currentTimeMillis() % 2600) / 2600f;
        float travel = phase * 1.7f - 0.35f;
        int bandW = Math.max(10, aw * 2 / 5);
        int bx = ax + Math.round(travel * aw) - bandW / 2;
        Shape oldClip = g2.getClip();
        g2.clipRect(ax, ay, aw, ah);
        Object hint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(sheenImg, bx, ay, bandW, ah, null);
        if (hint != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g2.setClip(oldClip);
    }

    /** La pornire: spune în log ce cărți nu au PNG în res/Cards (pentru ele rămâne înlocuitorul procedural). */
    public static void verifyArt() {
        int missing = 0;
        for (String id : Entity.Cards.CardFactory.allIds()) {
            if (CardFace.class.getResource("/Cards/" + id + ".png") == null) {
                missing++;
                Main.Exception.GameExceptionHandler.getInstance().handleWarning("Cartea " + id + " nu are artă: lipsește res/Cards/" + id + ".png (se folosește înlocuitorul)", null);
            }
        }
        if (missing == 0) System.out.println("Arta cărților: toate cărțile au PNG în res/Cards.");
    }

    /** Fața din cache; se redesenează doar dacă s-a schimbat ceva din ce se vede (valori cu bonusuri, combo, raritate). */
    public static synchronized BufferedImage face(Card c, Player p, boolean hot) {
        String desc = c.getDynamicDescription(p);
        String key = c.id + "|" + c.name + "|" + c.rarity + "|" + c.cost() + "|" + hot + "|" + c.comboHint() + "|" + c.tags + "|" + desc
                + "|" + bonusSig(c, p);
        BufferedImage img = faces.get(key);
        if (img == null) {
            if (faces.size() > 300) faces.clear();
            img = render(c, p, desc, hot);
            faces.put(key, img);
        }
        return img;
    }

    private static String bonusSig(Card c, Player p) {
        return p == null ? "" : p.cardDamage(c) + "," + p.statusPower(StatusEffect.Type.BURN) + "," + p.statusTurns(StatusEffect.Type.BURN) + "," + p.runeSum("cardShield") + "," + p.bonusShield;
    }

    // ---------------------------------------------------------------- randare

    private record Row(String text, Color color, boolean combo) {}

    private static BufferedImage render(Card c, Player p, String desc, boolean hot) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // separă descrierea în rânduri de poveste (flavor) și rânduri de statistici
        String flavor = CardCatalog.flavor(c.id == null ? "" : c.id);
        List<String> stats = new ArrayList<>(), prose = new ArrayList<>();
        if (desc != null) for (String line : desc.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (STAT.matcher(t).find()) stats.add(t); else prose.add(t);
        }
        if (flavor.isEmpty()) flavor = String.join(" ", prose);

        // cadru + pergament
        Color frame = frameColor(c.rarity);
        if ("Epică".equals(c.rarity)) {
            for (int i = 3; i >= 1; i--) {
                g.setColor(new Color(170, 90, 255, 40 + (3 - i) * 25));
                g.setStroke(new BasicStroke(6 + i * 3));
                g.draw(new RoundRectangle2D.Float(8, 8, W - 16, H - 16, 26, 26));
            }
        }
        g.setPaint(new GradientPaint(0, 0, new Color(246, 237, 212), 0, H, new Color(226, 211, 176)));
        g.fill(new RoundRectangle2D.Float(8, 8, W - 16, H - 16, 26, 26));
        g.setColor(frame);
        g.setStroke(new BasicStroke(13));
        g.draw(new RoundRectangle2D.Float(10, 10, W - 20, H - 20, 24, 24));
        g.setColor(INK);
        g.setStroke(new BasicStroke(2));
        g.draw(new RoundRectangle2D.Float(3.5f, 3.5f, W - 7, H - 7, 30, 30));
        g.draw(new RoundRectangle2D.Float(16.5f, 16.5f, W - 33, H - 33, 18, 18));

        // artă
        if (png(c) != null) {
            g.setColor(new Color(30, 22, 16));   // arta PNG se desenează deasupra, în draw()
            g.fillRect(ART_X, ART_Y, ART_W, ART_H);
        } else {
            g.drawImage(art(c, stats), ART_X, ART_Y, null);
        }
        g.setColor(BROWN);
        g.setStroke(new BasicStroke(3));
        g.drawRect(ART_X, ART_Y, ART_W, ART_H);

        // cost
        g.setColor(new Color(24, 52, 168));
        g.fillOval(18, 20, 52, 52);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawOval(18, 20, 52, 52);
        g.setFont(new Font(Font.SERIF, Font.BOLD, 34));
        String cost = String.valueOf(c.cost());
        g.drawString(cost, 44 - g.getFontMetrics().stringWidth(cost) / 2, 58);

        // elemente (dreapta sus)
        g.setFont(new Font(Font.SERIF, Font.BOLD, 23));
        int tx = W - 26;
        List<String> tags = new ArrayList<>(c.tags);
        for (int i = tags.size() - 1; i >= 0; i--) {
            String label = CardCatalog.tagLabel(tags.get(i));
            tx -= g.getFontMetrics().stringWidth(label);
            outlined(g, label, tx, 52, CardCatalog.tagColor(tags.get(i)));
            tx -= 12;
        }

        // nume
        int maxW = W - 52;
        String upper = c.name.toUpperCase();
        List<String> nameLines = fit(g, upper, maxW, Font.BOLD, 28, 18, 1);   // întâi o singură linie, micșorând fontul
        if (nameLines.get(nameLines.size() - 1).endsWith("…") && !upper.endsWith("…")) nameLines = fit(g, upper, maxW, Font.BOLD, 20, 14, 2);
        int ny = nameLines.size() == 1 ? 208 : 200;
        g.setColor(INK);
        for (String l : nameLines) { g.drawString(l, W / 2 - g.getFontMetrics().stringWidth(l) / 2, ny); ny += g.getFontMetrics().getHeight() - 2; }

        // ~ Descriere ~
        int dy = nameLines.size() == 1 ? 230 : 236;
        g.setFont(new Font(Font.SERIF, Font.ITALIC, 15));
        String hd = "Descriere";
        int hw = g.getFontMetrics().stringWidth(hd);
        g.setColor(BROWN);
        g.drawString(hd, W / 2 - hw / 2, dy);
        g.setStroke(new BasicStroke(2));
        g.draw(new QuadCurve2D.Float(30, dy - 4, 50, dy - 12, 72, dy - 4));
        g.draw(new QuadCurve2D.Float(W - 72, dy - 4, W - 50, dy - 12, W - 30, dy - 4));

        // descriere scurtă
        int top = dy + 20;
        List<String> fl = fit(g, flavor, maxW, Font.PLAIN, 18, 13, 2);
        g.setColor(INK);
        for (String l : fl) { g.drawString(l, W / 2 - g.getFontMetrics().stringWidth(l) / 2, top); top += g.getFontMetrics().getHeight() - 1; }

        // statistici + combo, cu fontul micșorat până încap
        List<Row> rows = new ArrayList<>();
        for (String s : stats) rows.add(new Row(normalize(s), statColor(s, c, p), false));
        String combo = c.comboHint();
        String reaction = Entity.Cards.Combos.reactionHint(c);
        int bottom = H - 26;
        int avail = bottom - top;
        int size = 21;
        List<Row> laid = null;
        for (int pass = 0; pass < 2; pass++) {   // întâi cu rândul de reacții (fără să coborâm sub 15), apoi fără el
            int minSize = pass == 0 ? 15 : 12;
            for (size = 21; size >= minSize; size--) {
                g.setFont(new Font(Font.SERIF, Font.BOLD, size));
                FontMetrics fm = g.getFontMetrics();
                laid = new ArrayList<>();
                for (Row r : rows) for (String l : wrap(fm, r.text(), maxW - 4)) laid.add(new Row(l, r.color(), false));
                for (String extra : new String[]{combo == null ? null : "Combo: " + lower(combo), pass == 0 ? reaction : null}) {
                    if (extra == null) continue;
                    for (String l : wrap(fm, extra, maxW - 4)) laid.add(new Row(l, hot ? new Color(176, 98, 0) : new Color(70, 80, 130), true));
                }
                if (laid.size() * (fm.getHeight() - 2) <= avail) break;
            }
            if (size >= minSize) break;
            size = minSize;
        }
        if (laid != null) {
            g.setFont(new Font(Font.SERIF, Font.BOLD, Math.max(size, 12)));
            FontMetrics fm = g.getFontMetrics();
            int lh = fm.getHeight() - 2, y = top + fm.getAscent() - 2;
            for (Row r : laid) {
                if (y > bottom + 6) break;
                if (r.combo() && hot) {
                    g.setColor(new Color(255, 214, 90, 120));
                    g.fillRoundRect(24, y - fm.getAscent() + 1, W - 48, lh + 1, 8, 8);
                }
                g.setColor(r.color());
                g.drawString(r.text(), 30, y);
                y += lh;
            }
        }
        g.dispose();
        return img;
    }

    private static String lower(String s) { return s.length() > 1 ? Character.toLowerCase(s.charAt(0)) + s.substring(1) : s; }

    private static Color frameColor(String rarity) {
        return switch (rarity == null ? "" : rarity) {
            case "Rară" -> new Color(122, 150, 190);
            case "Epică" -> new Color(206, 158, 40);
            default -> new Color(112, 64, 30);
        };
    }

    /** "Paguba 8" -> "Pagubă 8", "Aplică Înghețat (2 ture)" -> "Status: Înghețat 2 ture", etc. */
    private static String normalize(String s) {
        String t = s.trim();
        t = t.replaceFirst("^Paguba:?", "Pagubă").replaceFirst("^Arsura:?", "Arsură").replaceFirst("^Armura:?", "Armură").replaceFirst("^Vindecă", "Vindecare");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^Aplică (.*?)(?: (\\d+))? \\((\\d+) ture\\)(.*)$").matcher(t);
        if (m.find()) t = "Status: " + m.group(1) + (m.group(2) != null ? " " + m.group(2) : "") + " " + m.group(3) + " ture" + m.group(4);
        return t;
    }

    private static Color statColor(String line, Card c, Player p) {
        Color base = new Color(52, 34, 20), green = new Color(26, 128, 40), red = new Color(176, 36, 30);
        if (p == null) return base;
        if (line.startsWith("Pagub")) { int b = p.cardDamage(c) + (p.areDinteChilimbar ? 3 : 0); return b > 0 ? green : b < 0 ? red : base; }
        if (line.startsWith("Arsur")) return p.statusPower(StatusEffect.Type.BURN) + p.statusTurns(StatusEffect.Type.BURN) > 0 ? green : base;
        if (line.startsWith("Armur")) return p.runeSum("cardShield") + p.bonusShield > 0 ? green : base;
        return base;
    }

    private static void outlined(Graphics2D g, String s, int x, int y, Color c) {
        g.setColor(new Color(40, 26, 14, 230));
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) g.drawString(s, x + dx, y + dy);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    /** Alege cel mai mare font (între max și min) la care textul încape pe maxLines rânduri; setează fontul pe g și întoarce rândurile. */
    private static List<String> fit(Graphics2D g, String text, int maxW, int style, int max, int min, int maxLines) {
        List<String> lines = null;
        for (int s = max; s >= min; s--) {
            g.setFont(new Font(Font.SERIF, style, s));
            lines = wrap(g.getFontMetrics(), text, maxW);
            if (lines.size() <= maxLines) return lines;
        }
        if (lines.size() > maxLines) {   // tot nu încape: taie și pune "…"
            lines = new ArrayList<>(lines.subList(0, maxLines));
            String last = lines.get(maxLines - 1);
            lines.set(maxLines - 1, last.endsWith("…") ? last : last + "…");
        }
        return lines;
    }

    private static List<String> wrap(FontMetrics fm, String text, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String trial = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(trial) > maxW && cur.length() > 0) { out.add(cur.toString()); cur = new StringBuilder(word); }
            else cur = new StringBuilder(trial);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    // ---------------------------------------------------------------- artă

    /** Înlocuitorul procedural (doar pentru cărțile fără PNG), ținut în cache. */
    private static BufferedImage art(Card c, List<String> stats) {
        String key = c.id == null ? c.name : c.id;
        BufferedImage a = arts.get(key);
        if (a == null) {
            a = placeholder(c, stats);
            arts.put(key, a);
        }
        return a;
    }

    /** res/Cards/&lt;id&gt;.png așa cum e (fără scalare), sau null dacă nu există. */
    private static BufferedImage png(Card c) {
        String id = c.id == null ? c.name : c.id;
        if (pngs.containsKey(id)) return pngs.get(id);
        BufferedImage out = null;
        try (var in = CardFace.class.getResourceAsStream("/Cards/" + id + ".png")) {
            if (in != null) out = ImageIO.read(in);
        } catch (Exception ignored) { }
        pngs.put(id, out);
        return out;
    }

    private static BufferedImage placeholder(Card c, List<String> stats) {
        BufferedImage img = new BufferedImage(ART_W, ART_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        String tag = c.tags.isEmpty() ? "" : c.tags.iterator().next();
        boolean dmg = false, shield = false, heal = false;
        for (String s : stats) { dmg |= s.startsWith("Pagub"); shield |= s.startsWith("Armur"); heal |= s.startsWith("Vindec"); }
        String id = c.id == null ? "" : c.id;
        boolean ally = id.contains("Haiduc") || id.equals("SabieSiVraja");

        Color c1, c2;
        switch (tag) {
            case "FOC" -> { c1 = new Color(60, 16, 10); c2 = new Color(214, 92, 28); }
            case "GHEATA" -> { c1 = new Color(30, 70, 120); c2 = new Color(190, 235, 255); }
            case "FULGER" -> { c1 = new Color(10, 12, 40); c2 = new Color(60, 54, 120); }
            case "UMBRA" -> { c1 = new Color(14, 6, 28); c2 = new Color(92, 48, 140); }
            default -> { c1 = new Color(40, 54, 60); c2 = new Color(140, 156, 150); }
        }
        g.setPaint(new GradientPaint(0, 0, c1, 0, ART_H, c2));
        g.fillRect(0, 0, ART_W, ART_H);
        Color accent = tag.isEmpty() ? new Color(220, 210, 160) : CardCatalog.tagColor(tag);

        if (ally) {
            g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(200, 205, 215));
            g.drawLine(70, 90, 150, 14);                                  // sabia
            g.setColor(new Color(150, 100, 50));
            g.drawLine(150, 90, 70, 14);                                  // sulița
            g.setColor(new Color(220, 220, 230));
            g.fillPolygon(new int[]{64, 78, 70}, new int[]{20, 20, 6}, 3);
            g.setColor(new Color(120, 120, 130));
            g.drawLine(60, 76, 84, 84);
        } else if (shield || heal && !dmg) {
            if (heal && !shield) {                                          // frunză / picătură verde
                Path2D leaf = new Path2D.Float();
                leaf.moveTo(110, 12); leaf.curveTo(160, 30, 160, 80, 110, 94); leaf.curveTo(60, 80, 60, 30, 110, 12);
                g.setPaint(new GradientPaint(80, 12, new Color(150, 235, 120), 140, 94, new Color(30, 130, 60)));
                g.fill(leaf);
                g.setColor(new Color(20, 90, 40));
                g.setStroke(new BasicStroke(3));
                g.drawLine(110, 20, 110, 92);
            } else {                                                        // scut în culoarea elementului
                Path2D sh = new Path2D.Float();
                sh.moveTo(110, 8); sh.lineTo(150, 20); sh.lineTo(146, 60); sh.curveTo(142, 82, 124, 92, 110, 98);
                sh.curveTo(96, 92, 78, 82, 74, 60); sh.lineTo(70, 20); sh.closePath();
                g.setPaint(new GradientPaint(70, 8, accent.brighter(), 150, 98, accent.darker().darker()));
                g.fill(sh);
                g.setColor(new Color(255, 255, 255, 190));
                g.setStroke(new BasicStroke(3));
                g.draw(sh);
                g.drawLine(110, 16, 110, 90);
            }
        } else {
            switch (tag) {
                case "FOC" -> comet(g);
                case "GHEATA" -> snow(g);
                case "FULGER" -> bolt(g, accent);
                case "UMBRA" -> moon(g);
                default -> star(g, accent);
            }
        }
        if (c.applies.stream().anyMatch(m -> "BURN".equals(m.get("status"))) || stats.stream().anyMatch(s -> s.startsWith("Arsur"))) embers(g);
        g.dispose();
        return img;
    }

    private static void comet(Graphics2D g) {
        Path2D trail = new Path2D.Float();
        trail.moveTo(20, 8); trail.curveTo(80, 12, 130, 40, 160, 62); trail.lineTo(150, 88); trail.curveTo(110, 70, 60, 36, 20, 8);
        g.setPaint(new GradientPaint(20, 8, new Color(255, 120, 20, 0), 160, 70, new Color(255, 170, 40, 230)));
        g.fill(trail);
        g.setPaint(new RadialGradientPaint(160, 66, 34, new float[]{0f, 0.55f, 1f}, new Color[]{new Color(255, 245, 190), new Color(255, 120, 30), new Color(150, 20, 10)}));
        g.fillOval(126, 32, 68, 68);
        g.setColor(new Color(40, 10, 0));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(126, 32, 68, 68);
    }

    private static void snow(Graphics2D g) {
        g.setColor(new Color(235, 250, 255));
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 6; i++) {
            AffineTransform t = g.getTransform();
            g.rotate(Math.toRadians(i * 60), 110, 52);
            g.drawLine(110, 52, 110, 8);
            g.drawLine(110, 22, 99, 12); g.drawLine(110, 22, 121, 12);
            g.drawLine(110, 36, 102, 29); g.drawLine(110, 36, 118, 29);
            g.setTransform(t);
        }
        g.setColor(new Color(220, 245, 255, 190));
        g.fillPolygon(new int[]{20, 30, 40}, new int[]{96, 62, 96}, 3);
        g.fillPolygon(new int[]{176, 188, 200}, new int[]{96, 56, 96}, 3);
    }

    private static void bolt(Graphics2D g, Color accent) {
        Polygon b = new Polygon(new int[]{128, 84, 108, 70, 138, 112, 150}, new int[]{4, 54, 54, 100, 44, 44, 4}, 7);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
        g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(b);
        g.setColor(accent);
        g.fill(b);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.draw(b);
    }

    private static void moon(Graphics2D g) {
        g.setColor(new Color(60, 30, 100, 150));
        for (int i = 0; i < 4; i++) g.fillOval(10 + i * 50, 70 + (i % 2) * 8, 90, 36);   // ceață
        Area moon = new Area(new Ellipse2D.Float(70, 12, 76, 76));
        moon.subtract(new Area(new Ellipse2D.Float(94, 6, 70, 70)));
        g.setPaint(new GradientPaint(70, 12, new Color(235, 215, 255), 140, 88, new Color(140, 90, 210)));
        g.fill(moon);
        g.setColor(new Color(235, 215, 255, 200));
        g.fillOval(160, 24, 5, 5); g.fillOval(40, 30, 4, 4); g.fillOval(182, 60, 3, 3);
    }

    private static void star(Graphics2D g, Color accent) {
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
        g.fillOval(60, 6, 100, 100);
        g.setColor(accent);
        g.setStroke(new BasicStroke(4));
        g.drawOval(60, 6, 100, 100);
        Path2D st = new Path2D.Float();
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? 36 : 15;
            double px = 110 + Math.cos(ang) * r, py = 56 + Math.sin(ang) * r;
            if (i == 0) st.moveTo(px, py); else st.lineTo(px, py);
        }
        st.closePath();
        g.fill(st);
    }

    private static void embers(Graphics2D g) {
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 16; i++) {
            g.setColor(new Color(255, 170 + r.nextInt(70), 40, 150 + r.nextInt(100)));
            int s = 2 + r.nextInt(4);
            g.fillOval(r.nextInt(ART_W - 8), r.nextInt(ART_H - 8), s, s);
        }
    }
}
