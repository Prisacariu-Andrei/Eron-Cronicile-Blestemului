package ProgressManagement;

import Entity.Inamici.EnemyFactory;
import GameState.CutsceneState;
import GameState.GameState;
import GameState.PlayState;
import Main.GamePanel;
import Main.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Scenele poveștii, din res/Data/story.json (formatul e explicat în câmpul "_help" al fișierului).
 * Fiecare scenă se joacă o singură dată: flag-ul "scene:<id>" intră în salvare împreună cu restul progresului.
 *
 * Declanșatoarele se pun în coadă cu {@link #queue(String)} (de ex. de StoryProgress când un boss cade) și se joacă
 * din PlayState ({@link #playPending()}), ca scena să nu înceapă în mijlocul unei lupte sau al unui dialog.
 * Scenele de dinaintea unui boss se joacă direct, cu {@link #playNow}.
 */
public class StoryScenes {
    public record Move(String actor, double col, double row, int ticks, String dir, Boolean show, List<double[]> via, double hop, boolean free) {}
    public record Line(String speaker, String text, List<String> effects, double[] cam, List<Move> moves) {}
    public record Actor(String id, String sprite, String spriteSet, String tint, double col, double row, boolean visible) {}

    public static class Scene {
        public final String id, trigger, title;
        public final List<String> hideNpcs = new ArrayList<>(), onEnd = new ArrayList<>();
        public final List<Actor> actors = new ArrayList<>();
        public final List<Line> lines = new ArrayList<>();
        public final double[] eron, cam;   // null = nu se schimbă
        public final String mapZone;       // zona în care se joacă scena (null = cea curentă)
        public final int mapCol, mapRow;

        Scene(Map<String, Object> d) {
            id = Json.str(d.get("id"), "scena");
            trigger = Json.str(d.get("trigger"), "");
            title = Json.str(d.get("title"), "");
            for (Object o : Json.list(d.get("hideNpcs"))) hideNpcs.add(String.valueOf(o));
            for (Object o : Json.list(d.get("onEnd"))) onEnd.add(String.valueOf(o));
            Map<String, Object> map = Json.map(d.get("map"));
            mapZone = Json.str(map.get("zone"), null);
            mapCol = Json.integer(map.get("col"), 0);
            mapRow = Json.integer(map.get("row"), 0);
            eron = numbers(d.get("eron"));
            cam = numbers(d.get("cam"));
            for (Object o : Json.list(d.get("actors"))) {
                Map<String, Object> a = Json.map(o);
                actors.add(new Actor(Json.str(a.get("id"), "?"), Json.str(a.get("sprite"), "/Player/Eron_down1.png"), Json.str(a.get("spriteSet"), null), Json.str(a.get("tint"), null),
                        Json.number(a.get("col"), 0), Json.number(a.get("row"), 0), Json.bool(a.get("visible"), true)));
            }
            for (Object o : Json.list(d.get("lines"))) {
                Map<String, Object> l = Json.map(o);
                List<String> fx = new ArrayList<>();
                Object e = l.get("effect");
                if (e instanceof List<?>) for (Object x : (List<?>) e) fx.add(String.valueOf(x));
                else if (e instanceof String) fx.add((String) e);
                List<Move> moves = new ArrayList<>();
                for (Object mo : Json.list(l.get("moves"))) {
                    Map<String, Object> m = Json.map(mo);
                    moves.add(new Move(Json.str(m.get("actor"), "eron"), Json.number(m.get("col"), 0), Json.number(m.get("row"), 0),
                            Math.max(1, Json.integer(m.get("ticks"), 1)), Json.str(m.get("dir"), null),
                            m.get("show") instanceof Boolean ? (Boolean) m.get("show") : null, points(m.get("via")), Json.number(m.get("hop"), 0), Json.bool(m.get("free"), false)));
                }
                lines.add(new Line(Json.str(l.get("speaker"), ""), Json.str(l.get("text"), ""), fx, numbers(l.get("cam")), moves));
            }
        }

        private static List<double[]> points(Object o) {
            List<double[]> out = new ArrayList<>();
            for (Object pt : Json.list(o)) { double[] d = numbers(pt); if (d != null && d.length >= 2) out.add(d); }
            return out;
        }

        private static double[] numbers(Object o) {
            List<Object> l = Json.list(o);
            if (l.isEmpty()) return null;
            double[] out = new double[l.size()];
            for (int i = 0; i < out.length; i++) out[i] = Json.number(l.get(i), 0);
            return out;
        }
    }

    private final GamePanel gp;
    private final List<Scene> scenes = new ArrayList<>();
    private final List<String> pending = new ArrayList<>();

    public StoryScenes(GamePanel gp) {
        this.gp = gp;
        for (Object o : Json.list(Json.map(Json.parseResource("/Data/story.json")).get("scenes"))) scenes.add(new Scene(Json.map(o)));
    }

    private static String key(Scene s) { return "scene:" + s.id; }

    private Scene unseen(String trigger) {
        for (Scene s : scenes) if (s.trigger.equals(trigger) && !gp.story.has(key(s))) return s;
        return null;
    }

    /** Declanșatorul s-a petrecut: scena lui (dacă există și nu s-a văzut) se joacă de la PlayState. */
    public void queue(String trigger) {
        if (unseen(trigger) != null && !pending.contains(trigger)) pending.add(trigger);
    }

    public void clearPending() { pending.clear(); }

    /** Apelată din PlayState.update: pornește prima scenă din coadă. Returnează true dacă starea s-a schimbat. */
    public boolean playPending() {
        while (!pending.isEmpty()) {
            Scene s = unseen(pending.remove(0));
            if (s != null) { start(s, null); return true; }
        }
        return false;
    }

    /** Joacă acum scena declanșatorului, apoi starea dată (ex. introducerea luptei). false = nu e nicio scenă nouă. */
    public boolean playNow(String trigger, Supplier<GameState> then) {
        Scene s = unseen(trigger);
        if (s == null) return false;
        start(s, then);
        return true;
    }

    /** Starea pentru scena declanșatorului, sau fallback dacă nu există / s-a văzut deja (ex. începutul jocului). */
    public GameState stateFor(String trigger, Supplier<GameState> fallback) {
        Scene s = unseen(trigger);
        if (s == null) return fallback.get();
        gp.story.set(key(s));
        return new CutsceneState(gp, s, null);
    }

    /** Ca mai sus, dar după scenă urmează "then" (ex. finalul, apoi epilogul). */
    public GameState stateFor(String trigger, Supplier<GameState> fallback, Supplier<GameState> then) {
        Scene s = unseen(trigger);
        if (s == null) return fallback.get();
        gp.story.set(key(s));
        return new CutsceneState(gp, s, then);
    }

    /** Un obiect/semn cu care s-a interacționat (trigger "inspect:<id>"). true = scena a pornit. */
    public boolean inspect(String id) { return playNow("inspect:" + id, null); }

    private void start(Scene s, Supplier<GameState> then) {
        gp.story.set(key(s));
        gp.changeState(new CutsceneState(gp, s, then));
    }

    /** La sfârșitul scenei: 'quest:<id>', 'battle:<inamic>', 'set:<flag>', 'coins:<N>'. */
    public void runOnEnd(Scene s) {
        for (String action : s.onEnd) {
            int sep = action.indexOf(':');
            String cmd = sep < 0 ? action : action.substring(0, sep), arg = sep < 0 ? "" : action.substring(sep + 1);
            switch (cmd) {
                case "quest" -> {
                    try { gp.questManager.receiveQuest(Integer.parseInt(arg.trim()), true); } catch (NumberFormatException ignored) { }
                }
                case "credits" -> { /* îl tratează CutsceneState: după scenă urmează genericul */ }
                case "set" -> gp.story.set(arg);
                case "coins" -> {
                    try { gp.player.addCoins(Integer.parseInt(arg.trim())); } catch (NumberFormatException ignored) { }
                }
                case "battle" -> {
                    var foe = EnemyFactory.createEnemy(arg, gp, gp.player.worldX, gp.player.worldY);
                    if (foe != null) gp.player.beginBattle(foe);
                }
                default -> Main.Exception.GameExceptionHandler.getInstance().handleWarning("Acțiune necunoscută în story.json: " + action, null);
            }
        }
    }

    /** Scena e încheiată: starea dată sau jocul. */
    public GameState after(Supplier<GameState> then) { return then != null ? then.get() : new PlayState(gp); }
}
