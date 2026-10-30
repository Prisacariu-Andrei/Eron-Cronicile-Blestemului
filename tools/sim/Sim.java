package Main;

import Entity.Inamici.*;
import Entity.Items.Card;
import Entity.Player.Player;
import java.lang.reflect.*;
import java.util.*;

/**
 * Simulator de balans (nu face parte din joc): un bot joacă luptele cu bossii folosind motorul real
 * (cărți, combo-uri, statusuri, AI-ul inamicilor, aliatul). Tura inamicului e rulată sincron (fără Timer-ele UI).
 */
public class Sim {
    static GamePanel gp;
    static Random rnd = new Random(7);

    // paguba estimată / armura pe carte (pentru alegerea botului); valorile reale le aplică motorul
    static final Map<String, Integer> DMG = new HashMap<>();
    static final Set<String> SHIELD = Set.of("PavazaGlaciara", "PavazaAmurgului", "MantiaNoptii", "ValDeCeata", "RitualulUmbrelor", "ChemareaHaiducilor");
    static {
        String[] kv = {"CadereaStelelor:45", "FulgerDeSange:16", "UraganDeAzur:12", "LanceDeFoc:14", "FulgerInLant:12", "SocTermic:10",
                "ExecutieUmbrita:18", "SabieSiVraja:15", "FurtunaHaiducilor:14", "PrabusireaGerului:14", "TrasnetulVidului:10", "InimaFurtunii:16",
                "ParjolulSferic:10", "RafalaDeBruma:6", "PloaieRece:4", "ScanteiaStrabuna:6", "SuflulPirjolit:10", "ScanteiaDeJar:3", "TortaVie:9",
                "SulitaDeFoc:8", "InfernulMic:9", "ClesteDeUmbra:7", "ArcElectric:4", "Turtura:3", "Rezonanta:5", "ArmoniaElementelor:8", "GeamatulEcoului:8",
                "Dezolare:9", "PovaraAnilor:6", "ManiorulIernii:8", "FluxulStrabun:30", "DeslusireaRunelor:30"};
        for (String s : kv) { String[] p = s.split(":"); DMG.put(p[0], Integer.parseInt(p[1])); }
    }

    static Object call(Object o, String m, Object... a) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass())
            for (Method x : c.getDeclaredMethods()) if (x.getName().equals(m) && x.getParameterCount() == a.length) { x.setAccessible(true); return x.invoke(o, a); }
        throw new NoSuchMethodException(m);
    }
    static Object get(Object o, String f) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass())
            try { Field x = c.getDeclaredField(f); x.setAccessible(true); return x.get(o); } catch (NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(f);
    }
    static void set(Object o, String f, Object v) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass())
            try { Field x = c.getDeclaredField(f); x.setAccessible(true); x.set(o, v); return; } catch (NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(f);
    }

    static boolean won(Player p) { return p.livingEnemies().isEmpty(); }

    /** Botul joacă o tură: cărțile utile întâi, armură dacă vine o lovitură mare, apoi cea mai bună combinație de paguba. */
    static void playTurn(Player p) throws Exception {
        // abilitățile (fără cost de esență): paguba / armura / slăbiciune / vindecare
        for (var ab : gp.progression.abilities) {
            if (!gp.progression.isUnlocked(ab) || p.abilityWait(ab) != 0) continue;
            boolean heal = ab.effects.stream().anyMatch(e -> "heal".equals(e.get("type")));
            if (heal && p.life > p.maxLife * 0.5) continue;
            p.useAbility(ab);
            if (won(p)) return;
        }
        for (int guard = 0; guard < 12 && !won(p); guard++) {
            List<Card> hand = p.inventory.hand;
            int mana = p.currentMana;
            Enemy foe = p.currentEnemy;
            int incoming = 0;
            for (Enemy e : p.livingEnemies()) if (e.nextAction != null && !e.nextAction.name().equals("DEFEND") && !e.nextAction.name().equals("BUFF")) incoming += e.intentValue;
            int pick = -1;
            // 1) cărți gratuite / esență în plus
            for (int i = 0; i < hand.size(); i++) { String id = hand.get(i).id; if ((id.equals("FluxulStrabun") || id.equals("DeslusireaRunelor")) && hand.get(i).manaCost <= mana) { pick = i; break; } }
            // 2) vindecare când e nevoie
            if (pick < 0 && p.life < p.maxLife * 0.45) for (int i = 0; i < hand.size(); i++) if (hand.get(i).id.equals("SuflulVietii") && hand.get(i).manaCost <= mana) { pick = i; break; }
            // 3) armură dacă lovitura care vine e mare și n-avem destulă
            if (pick < 0 && incoming >= 12 && p.shield < incoming * 0.6) {
                for (int i = 0; i < hand.size(); i++) if (SHIELD.contains(hand.get(i).id) && hand.get(i).manaCost <= mana) { pick = i; break; }
            }
            // 4) cea mai bună combinație de paguba cu esența rămasă: jucăm prima carte din ea
            if (pick < 0) {
                int n = hand.size(), bestVal = 0, bestFirst = -1;
                for (int mask = 1; mask < (1 << n); mask++) {
                    int cost = 0, val = 0, first = -1;
                    for (int i = 0; i < n; i++) if ((mask & (1 << i)) != 0) {
                        Card c = hand.get(i); cost += c.manaCost;
                        val += DMG.getOrDefault(c.id, SHIELD.contains(c.id) ? 2 : 1);
                        if (first < 0 || DMG.getOrDefault(c.id, 0) == 9 /* Dezolare întâi */) first = i;
                    }
                    if (cost <= mana && val > bestVal) { bestVal = val; bestFirst = first; }
                }
                pick = bestFirst;
            }
            if (pick < 0) break;
            int before = hand.size(), manaBefore = p.currentMana;
            gp.battleUI.commandNum = pick;
            p.playSelectedCard();
            if (hand.size() == before && p.currentMana == manaBefore) break;   // n-a putut fi jucată
        }
    }

    /** Tura inamicului, ca în Player.endTurn + finishEnemyTurn, dar fără Timer-e. false = Eron a căzut. */
    static boolean enemyTurn(Player p) throws Exception {
        p.isPlayerTurn = false;
        p.isInvulnerable = false;
        @SuppressWarnings("unchecked") List<Enemy> team = (List<Enemy>) get(p, "team");
        List<Enemy> foes = new ArrayList<>(team);
        if (foes.isEmpty() && p.currentEnemy != null) foes.add(p.currentEnemy);
        for (Enemy f : foes) if (!f.persistentShield && !"Valerius, Cavalerul Neinvins".equals(f.name)) f.shield = 0;
        while (!p.inventory.hand.isEmpty()) p.inventory.discard(p.inventory.hand.removeFirst());
        for (Enemy f : foes) f.updateStatuses();
        p.retarget();
        if (won(p)) return true;
        gp.party.actAll(p, p.currentEnemy);
        p.retarget();
        if (won(p)) return true;
        p.currentMana = p.maxMana;
        for (Enemy e : foes) {
            if (!e.alive) continue;
            e.executeIntent(p);
            if (p.life <= 0) return false;
        }
        // finishEnemyTurn
        gp.battleUI.commandNum = 0;
        p.combo.reset();
        p.updateStatuses();
        p.shield = 0;
        if ((boolean) get(p, "arePlatosaValerius")) p.addShield(5);
        if (p.runeSum("turnShield") > 0) p.addShield(p.runeSum("turnShield"));
        p.isPlayerTurn = true;
        call(p, "tickAbilityCooldowns");
        for (int i = 0; i < 5; i++) p.inventory.drawCard();
        for (Enemy f : team) if (f.stunTurns > 0) f.stunTurns -= 1;
        if (team.isEmpty() && p.currentEnemy != null && p.currentEnemy.stunTurns > 0) p.currentEnemy.stunTurns -= 1;
        p.retarget();
        return p.life > 0;
    }

    record Result(boolean win, int turns, int lifeLeft, int dealt) {}

    static Result fight(String enemy, List<String> extras, List<String> deck) throws Exception {
        Player p = gp.player;
        p.inventory.cards.clear(); p.inventory.hand.clear(); p.inventory.discardPile.clear();
        for (String id : deck) p.inventory.cards.add(Entity.Cards.CardFactory.createCard(id, p));
        Collections.shuffle(p.inventory.cards, rnd);
        p.life = p.maxLife;
        Enemy e = EnemyFactory.createEnemy(enemy, gp, 0, 0);
        // -Dtweak="maxLife=200,attackDamage=9": valori de probă pentru boss (doar în simulare)
        for (String kv : System.getProperty("tweak", "").split(",")) {
            if (!kv.contains("=")) continue;
            String[] t = kv.split("=");
            set(e, t[0].trim(), Integer.parseInt(t[1].trim()));
            if (t[0].trim().equals("maxLife")) e.life = e.maxLife;
        }
        for (String x : extras) e.packExtras.add(x);
        for (var al : gp.party.members) al.life = al.maxLife;
        p.startBattleNow(e);
        @SuppressWarnings("unchecked") List<Enemy> team = (List<Enemy>) get(p, "team");
        int total = 0; for (Enemy t : team) total += t.maxLife + t.shield;
        int turns = 0;
        boolean alive = true;
        while (turns < 40) {
            turns++;
            playTurn(p);
            if (won(p)) break;
            alive = enemyTurn(p);
            if (!alive || won(p)) break;
        }
        boolean win = won(p) && p.life > 0;
        int life = Math.max(0, p.life);
        gp.changeState(new GameState.PlayState(gp));
        p.currentEnemy = null; team.clear();
        return new Result(win, turns, life, total);
    }

    public static void main(String[] a) throws Exception {
        gp = new GamePanel(); gp.setSize(gp.screenWidth, gp.screenHeight);
        gp.setupGame();
        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream("/dev/null"), true));   // fără DEBUG din joc
        java.io.PrintStream out = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err), true, "UTF-8");
        String[] starting = {"ParjolulSferic", "ParjolulSferic", "Dezolare", "PavazaGlaciara", "PavazaGlaciara", "ParjolulSferic", "ParjolulSferic", "SuflulVietii", "PavazaGlaciara", "PavazaGlaciara"};
        var levels = Json.map(Json.map(Json.parseResource("/Data/progression.json")).get("cardLevels"));
        // boss, extra, index zonă (DebugMode), cărți câștigate până acolo
        Object[][] plan = {
            {"Fenris cel Alb", "", 1, 3},
            {"Feldgug", "", 2, 6},
            {"Jean Plesa", "", 3, 9},
            {"Glaciara, Stăpânul Inelului de Gheață", "", 4, 12},
            {"Charon, Străjerul Umbrelor fără Tihnă", "", 5, 14},
            {"Soldat de Obsidian", "Soldat de Obsidian", 6, 16},
            {"Fenris de Obsidian", "Feldgug de Obsidian,Glaciara de Obsidian", 6, 16},
            {"Bătrânul Cronicar", "", 6, 16},
            {"Malakor, Monarhul Amurgului", "", 6, 16},
        };
        int runs = Integer.parseInt(System.getProperty("runs", "200"));
        String only = System.getProperty("only", "");
        out.println("boss|nivel|viațăEron|cărțiÎnPlus|victorii%|tureMedii|viațăRămasă|HPinamic|pagubăPeTură");
        for (Object[] row : plan) {
            String boss = (String) row[0];
            if (!only.isEmpty() && !boss.startsWith(only)) continue;
            DebugMode.skipTo(gp, (int) row[2]);
            for (int i = 0; i < 400 && !(gp.getCurrentState() instanceof GameState.PlayState); i++) gp.update();
            int lvl = gp.progression.level;
            List<String> pool = new ArrayList<>();
            for (var en : levels.entrySet()) if (Json.integer(en.getValue(), 99) <= lvl && !en.getKey().equals("CadereaStelelor")) pool.add(en.getKey());
            int wins = 0, turnSum = 0, lifeSum = 0, dealt = 0;
            for (int r = 0; r < runs; r++) {
                List<String> deck = new ArrayList<>(Arrays.asList(starting));
                for (int k = 0; k < (int) row[3]; k++) deck.add(pool.get(rnd.nextInt(pool.size())));
                List<String> extras = ((String) row[1]).isEmpty() ? List.of() : Arrays.asList(((String) row[1]).split(","));
                Result res = fight(boss, extras, deck);
                if (res.win) { wins++; turnSum += res.turns; lifeSum += res.lifeLeft; }
                dealt = res.dealt;
            }
            double avgT = wins == 0 ? 0 : turnSum / (double) wins;
            out.printf("%s|%d|%d|%d|%.0f|%.1f|%.0f|%d|%.1f%n", boss.split(",")[0], lvl, gp.player.maxLife, (int) row[3], 100.0 * wins / runs,
                    avgT, wins == 0 ? 0 : lifeSum / (double) wins, dealt, avgT == 0 ? 0 : dealt / avgT);
        }
        System.exit(0);
    }
}
