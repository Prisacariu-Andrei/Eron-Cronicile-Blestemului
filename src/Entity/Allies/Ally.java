package Entity.Allies;

import Main.Json;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Un aliat din echipa lui Eron. Definiția (viață, abilități, replici) vine din res/Data/allies.json. */
public class Ally {
    public static class Ability {
        public final String name, when, type, status;
        public final int value, statusTurns, statusValue;

        Ability(Map<String, Object> d) {
            name = Json.str(d.get("name"), "?");
            when = Json.str(d.get("when"), "always");
            Map<String, Object> e = Json.map(d.get("effect"));
            type = Json.str(e.get("type"), "damage");
            value = Json.integer(e.get("value"), 0);
            status = Json.str(e.get("status"), "");
            statusTurns = Json.integer(e.get("statusTurns"), 1);
            statusValue = Json.integer(e.get("statusValue"), 0);
        }
    }

    public static class Line {
        public final String speaker, text;
        Line(Map<String, Object> d) { speaker = Json.str(d.get("speaker"), ""); text = Json.str(d.get("text"), ""); }
    }

    public final String id, name, sprite, recruitFrom;
    public final int color, maxLife;
    public final List<Ability> abilities = new ArrayList<>();
    public final List<Line> scene = new ArrayList<>();

    public int life, shield;
    public String lastAction = "";   // ce a făcut în ultima tură (se vede în interfața de luptă)
    private BufferedImage image;

    Ally(Map<String, Object> d) {
        id = Json.str(d.get("id"), "");
        name = Json.str(d.get("name"), id);
        sprite = Json.str(d.get("sprite"), "/Enemy/JeanPlesa.png");
        recruitFrom = Json.str(d.get("recruitFrom"), "");
        int c;
        try { c = Integer.parseInt(Json.str(d.get("color"), "#FFFFFF").replace("#", ""), 16); } catch (NumberFormatException e) { c = 0xFFFFFF; }
        color = c;
        maxLife = Json.integer(d.get("maxLife"), 50);
        life = maxLife;
        for (Object o : Json.list(d.get("abilities"))) abilities.add(new Ability(Json.map(o)));
        for (Object o : Json.list(d.get("scene"))) scene.add(new Line(Json.map(o)));
    }

    public boolean alive() { return life > 0; }

    public BufferedImage image() {
        if (image == null) {
            try { image = ImageIO.read(Ally.class.getResourceAsStream(sprite)); } catch (Exception ignored) { /* fără sprite: UI-ul desenează doar panoul */ }
        }
        return image;
    }
}
