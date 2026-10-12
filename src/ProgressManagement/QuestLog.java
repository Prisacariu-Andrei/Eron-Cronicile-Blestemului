package ProgressManagement;

import Entity.Cards.CardFactory;
import Main.GamePanel;
import Main.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Misiunile din res/Data/quests.json. Stări: nepornită -> activă -> îndeplinită -> predată.
 * (Sistemul vechi, QuestManager, rămâne pentru misiunile principale/secundare din poveste.)
 */
public class QuestLog {
    public enum State { NOT_STARTED, ACTIVE, COMPLETED, TURNED_IN }

    public static class Objective {
        public String type, target, label, response;
        public int count = 1;
    }

    public static class Def {
        public String id, title, description, giver, turnIn, requires;
        public String offerText, acceptText, activeText, completeText;
        public int minLevel, coins, xp;
        public String card;
        public final List<Objective> objectives = new ArrayList<>();
    }

    public static class Pickup {
        public String id, item, zone;
        public int col, row, rgb = 0xBFE4FF;
    }

    private final GamePanel gp;
    private final Map<String, Def> defs = new LinkedHashMap<>();
    public final List<Pickup> pickups = new ArrayList<>();
    private final Map<String, String> npcNames = new HashMap<>(); // id NPC -> nume afișat

    private final Map<String, State> states = new HashMap<>();
    private final Map<String, int[]> counts = new HashMap<>();
    private final Set<String> collected = new HashSet<>(); // id-urile obiectelor deja culese

    public QuestLog(GamePanel gp) {
        this.gp = gp;
        Map<String, Object> root = Json.map(Json.parseResource("/Data/quests.json"));
        for (Object o : Json.list(root.get("quests"))) {
            Map<String, Object> m = Json.map(o);
            Def d = new Def();
            d.id = Json.str(m.get("id"), null);
            if (d.id == null) continue;
            d.title = Json.str(m.get("title"), d.id);
            d.description = Json.str(m.get("description"), "");
            d.giver = Json.str(m.get("giver"), "");
            d.turnIn = Json.str(m.get("turnIn"), d.giver);
            d.requires = Json.str(m.get("requires"), null);
            d.minLevel = Json.integer(m.get("minLevel"), 1);
            d.offerText = Json.str(m.get("offerText"), "");
            d.acceptText = Json.str(m.get("acceptText"), "");
            d.activeText = Json.str(m.get("activeText"), "");
            d.completeText = Json.str(m.get("completeText"), "");
            Map<String, Object> rw = Json.map(m.get("rewards"));
            d.coins = Json.integer(rw.get("coins"), 0);
            d.xp = Json.integer(rw.get("xp"), 0);
            d.card = Json.str(rw.get("card"), null);
            for (Object ob : Json.list(m.get("objectives"))) {
                Map<String, Object> om = Json.map(ob);
                Objective x = new Objective();
                x.type = Json.str(om.get("type"), "kill");
                x.target = Json.str(om.get("target"), "");
                x.count = Math.max(1, Json.integer(om.get("count"), 1));
                x.label = Json.str(om.get("label"), x.target);
                x.response = Json.str(om.get("response"), null);
                d.objectives.add(x);
            }
            defs.put(d.id, d);
            states.put(d.id, State.NOT_STARTED);
            counts.put(d.id, new int[d.objectives.size()]);
        }
        for (Object o : Json.list(root.get("pickups"))) {
            Map<String, Object> m = Json.map(o);
            Pickup p = new Pickup();
            p.id = Json.str(m.get("id"), null);
            p.item = Json.str(m.get("item"), "");
            p.zone = Json.str(m.get("zone"), "");
            p.col = Json.integer(m.get("col"), 0);
            p.row = Json.integer(m.get("row"), 0);
            try { p.rgb = Integer.decode(Json.str(m.get("color"), "#BFE4FF")); } catch (NumberFormatException ignored) { }
            if (p.id != null) pickups.add(p);
        }
        for (Object o : Json.list(Json.map(Json.parseResource("/Data/npcs.json")).get("npcs"))) {
            Map<String, Object> m = Json.map(o);
            npcNames.put(Json.str(m.get("id"), "?"), Json.str(m.get("name"), "?"));
        }
    }

    /** Joc nou: toate misiunile de la capăt. */
    public void reset() {
        for (String id : defs.keySet()) {
            states.put(id, State.NOT_STARTED);
            counts.put(id, new int[defs.get(id).objectives.size()]);
        }
        collected.clear();
    }

    // =====================================================================================
    // INTEROGĂRI
    // =====================================================================================

    public List<Def> all() { return new ArrayList<>(defs.values()); }

    public State state(String id) { return states.getOrDefault(id, State.NOT_STARTED); }

    public int progress(Def d, int objective) { return counts.get(d.id)[objective]; }

    public String npcName(String npcId) { return npcNames.getOrDefault(npcId, npcId); }

    private boolean available(Def d) {
        return state(d.id) == State.NOT_STARTED && gp.progression.level >= d.minLevel
                && (d.requires == null || state(d.requires) == State.TURNED_IN);
    }

    public Def availableFrom(String npcId) {
        for (Def d : defs.values()) if (d.giver.equals(npcId) && available(d)) return d;
        return null;
    }

    public Def readyFor(String npcId) {
        for (Def d : defs.values()) if (d.turnIn.equals(npcId) && state(d.id) == State.COMPLETED) return d;
        return null;
    }

    public Def activeFrom(String npcId) {
        for (Def d : defs.values()) if (state(d.id) == State.ACTIVE && (d.giver.equals(npcId) || d.turnIn.equals(npcId))) return d;
        return null;
    }

    public boolean hasActiveTalk(String npcId) {
        for (Def d : defs.values()) {
            if (state(d.id) != State.ACTIVE) continue;
            for (int i = 0; i < d.objectives.size(); i++) {
                Objective o = d.objectives.get(i);
                if (o.type.equals("talk") && o.target.equals(npcId) && counts.get(d.id)[i] < o.count) return true;
            }
        }
        return false;
    }

    /** True dacă o misiune activă mai are nevoie de acest obiect (obiectele din lume apar doar atunci). */
    public boolean needsItem(String item) {
        for (Def d : defs.values()) {
            if (state(d.id) != State.ACTIVE) continue;
            for (int i = 0; i < d.objectives.size(); i++) {
                Objective o = d.objectives.get(i);
                if (o.type.equals("collect") && o.target.equals(item) && counts.get(d.id)[i] < o.count) return true;
            }
        }
        return false;
    }

    public boolean isCollected(String pickupId) { return collected.contains(pickupId); }

    // =====================================================================================
    // ACȚIUNI
    // =====================================================================================

    public void accept(Def d) {
        if (state(d.id) != State.NOT_STARTED) return;
        states.put(d.id, State.ACTIVE);
        counts.put(d.id, new int[d.objectives.size()]);
        gp.fx.questSound(false);
        notice("Misiune nouă: " + d.title);
    }

    public void onKill(String enemyName) { advance("kill", enemyName); }

    public void collect(Pickup p) {
        collected.add(p.id);
        advance("collect", p.item);
    }

    /** Jucătorul vorbește cu un NPC. Returnează replica lui dacă a bifat un obiectiv "talk" (poate fi ""), altfel null. */
    public String onTalk(String npcId) {
        String reply = null;
        for (Def d : defs.values()) {
            if (state(d.id) != State.ACTIVE) continue;
            int[] c = counts.get(d.id);
            for (int i = 0; i < d.objectives.size(); i++) {
                Objective o = d.objectives.get(i);
                if (o.type.equals("talk") && o.target.equals(npcId) && c[i] < o.count) {
                    c[i] = o.count;
                    if (reply == null) reply = o.response == null ? "" : o.response;
                    checkComplete(d);
                }
            }
        }
        return reply;
    }

    private void advance(String type, String target) {
        for (Def d : defs.values()) {
            if (state(d.id) != State.ACTIVE) continue;
            int[] c = counts.get(d.id);
            for (int i = 0; i < d.objectives.size(); i++) {
                Objective o = d.objectives.get(i);
                if (o.type.equals(type) && o.target.equals(target) && c[i] < o.count) {
                    c[i]++;
                    if (c[i] < o.count) {
                        notice(o.label + ": " + c[i] + "/" + o.count);
                        gp.fx.questSound(false);
                    }
                }
            }
            checkComplete(d);
        }
    }

    private void checkComplete(Def d) {
        if (state(d.id) != State.ACTIVE) return;
        int[] c = counts.get(d.id);
        for (int i = 0; i < d.objectives.size(); i++) if (c[i] < d.objectives.get(i).count) return;
        states.put(d.id, State.COMPLETED);
        gp.fx.questSound(true);
        notice("Misiune îndeplinită: " + d.title + " - întoarce-te la " + npcName(d.turnIn));
    }

    /** Predare: primești monede, XP și cartea; misiunea devine "predată". Returnează rezumatul recompenselor. */
    public String turnIn(Def d) {
        if (state(d.id) != State.COMPLETED) return "";
        states.put(d.id, State.TURNED_IN);
        notice("Misiune predată: " + d.title);
        if (d.coins > 0) gp.player.addCoins(d.coins);
        if (d.card != null) gp.player.inventory.addCardToDeck(d.card, gp.player);
        gp.progression.addXp(d.xp); // la urmă: un eventual mesaj de nivel nou îl înlocuiește pe cel de mai sus
        gp.fx.questSound(true);
        return rewardText(d);
    }

    /** Admin (panoul F9): îndeplinește și predă misiunea cu tot cu recompense, oricare i-ar fi starea. */
    public void adminSkip(Def d) {
        if (state(d.id) == State.TURNED_IN) return;
        if (state(d.id) == State.NOT_STARTED) states.put(d.id, State.ACTIVE);
        int[] c = counts.get(d.id);
        for (int i = 0; i < c.length; i++) c[i] = d.objectives.get(i).count;
        states.put(d.id, State.COMPLETED);
        turnIn(d);
    }

    /** Admin: următoarea misiune neterminată (în ordinea din quests.json), sau null dacă au fost toate predate. */
    public Def adminNext() {
        for (Def d : defs.values()) if (state(d.id) != State.TURNED_IN) return d;
        return null;
    }

    public String rewardText(Def d) {
        List<String> parts = new ArrayList<>();
        if (d.coins > 0) parts.add(d.coins + " monede");
        if (d.xp > 0) parts.add(d.xp + " XP");
        if (d.card != null) parts.add("cartea " + cardName(d.card));
        return parts.isEmpty() ? "" : "Recompense: " + String.join(", ", parts);
    }

    private String cardName(String key) {
        try {
            var c = CardFactory.createCard(key, gp.player);
            return c != null ? c.name : key;
        } catch (Exception e) {
            return key;
        }
    }

    /** O linie pe obiectiv, cu progresul: "Învinge lupi în Bârlog: 1/3". */
    public List<String> progressLines(Def d) {
        List<String> out = new ArrayList<>();
        int[] c = counts.get(d.id);
        for (int i = 0; i < d.objectives.size(); i++) {
            Objective o = d.objectives.get(i);
            out.add(o.label + ": " + Math.min(c[i], o.count) + "/" + o.count);
        }
        return out;
    }

    public String progressText(Def d) { return String.join("\n", progressLines(d)); }

    private void notice(String text) {
        gp.ui.message = text;
        gp.ui.messageOn = true;
        gp.ui.messageCounter = 0;
    }

    // =====================================================================================
    // SALVARE
    // =====================================================================================

    /** "id:stare:c1,c2;id2:stare:c1|pickup1,pickup2" (stare = indexul în enum). */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Def d : defs.values()) {
            State s = state(d.id);
            if (s == State.NOT_STARTED) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(d.id).append(':').append(s.ordinal()).append(':');
            int[] c = counts.get(d.id);
            for (int i = 0; i < c.length; i++) sb.append(i > 0 ? "," : "").append(c[i]);
        }
        sb.append('|').append(String.join(",", collected));
        return sb.toString();
    }

    public void deserialize(String data) {
        reset();
        if (data == null || data.isBlank()) return;
        String[] halves = data.split("\\|", -1);
        for (String q : halves[0].split(";")) {
            String[] p = q.split(":", -1);
            Def d = p.length >= 3 ? defs.get(p[0]) : null;
            if (d == null) continue;
            try {
                states.put(d.id, State.values()[Integer.parseInt(p[1])]);
                String[] nums = p[2].isEmpty() ? new String[0] : p[2].split(",");
                int[] c = counts.get(d.id);
                for (int i = 0; i < c.length && i < nums.length; i++) c[i] = Integer.parseInt(nums[i]);
            } catch (RuntimeException e) {
                states.put(d.id, State.NOT_STARTED); // date stricate: misiunea o ia de la capăt
            }
        }
        if (halves.length > 1 && !halves[1].isEmpty()) for (String id : halves[1].split(",")) collected.add(id);
    }
}
