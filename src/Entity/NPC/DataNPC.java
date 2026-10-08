package Entity.NPC;

import GameState.PlayState;
import GameState.ShopState;
import Main.GamePanel;
import Main.Json;
import Main.Exception.GameExceptionHandler;
import ProgressManagement.QuestLog;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Un NPC descris în res/Data/npcs.json, nu în cod. Tipuri: "simple", "dialogue", "merchant", "quest"
 * (vezi câmpul "_help" din fișier). Pentru un NPC nou nu scrii nicio clasă: adaugi o intrare în JSON.
 *
 * Orice NPC poate oferi, primi sau preda misiuni din quests.json (prin id-ul lui). Când are o misiune de oferit,
 * de predat sau în desfășurare, aceasta are prioritate față de dialogul lui obișnuit.
 */
public class DataNPC extends NPC {
    private record Choice(String text, String next, String action) {}
    private record Node(String text, List<Choice> choices, String next, String action) {}

    private static final Random RNG = new Random();

    // moduri de conversație
    private static final int NORMAL = 0, QUEST_OFFER = 1, QUEST_TURN_IN = 2, QUEST_INFO = 3;

    private final String id, type, spritePath;
    private final Color tint;

    // simple
    private final List<String> lines = new ArrayList<>();
    private final boolean randomLines;
    private int lineIndex = 0;

    // dialogue / merchant / quest
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final String startNode;
    private String currentNode;

    // merchant
    private final List<String> stock = new ArrayList<>();

    private boolean talking = false;
    private int mode = NORMAL;
    private QuestLog.Def pendingQuest;

    public DataNPC(GamePanel gp, int x, int y, Map<String, Object> def) {
        super(gp, x, y);
        this.id = Json.str(def.get("id"), "npc");
        this.name = Json.str(def.get("name"), "???");
        this.type = Json.str(def.get("type"), "simple");
        this.spritePath = Json.str(def.get("sprite"), "/Player/Eron_down1.png");
        this.tint = parseColor(Json.str(def.get("tint"), null));

        for (Object o : Json.list(def.get("lines"))) lines.add(String.valueOf(o));
        this.randomLines = "random".equals(Json.str(def.get("lineMode"), "cycle"));

        for (Map.Entry<String, Object> e : Json.map(def.get("nodes")).entrySet()) {
            Map<String, Object> n = Json.map(e.getValue());
            List<Choice> choices = new ArrayList<>();
            for (Object c : Json.list(n.get("choices"))) {
                Map<String, Object> cm = Json.map(c);
                choices.add(new Choice(Json.str(cm.get("text"), "..."), Json.str(cm.get("next"), null), Json.str(cm.get("action"), null)));
            }
            nodes.put(e.getKey(), new Node(Json.str(n.get("text"), "..."), choices, Json.str(n.get("next"), null), Json.str(n.get("action"), null)));
        }
        this.startNode = Json.str(def.get("start"), null);

        for (Object o : Json.list(Json.map(def.get("shop")).get("stock"))) stock.add(String.valueOf(o));

        getImage();
    }

    /** Creează toți NPC-urile din fișier care aparțin zonei și îi pune în primele locuri libere de după "firstSlot". */
    public static void spawnForZone(GamePanel gp, String zone, int firstSlot) {
        Object root = Json.parseResource("/Data/npcs.json");
        int slot = firstSlot;
        for (Object o : Json.list(Json.map(root).get("npcs"))) {
            Map<String, Object> def = Json.map(o);
            if (!zone.equals(Json.str(def.get("zone"), ""))) continue;
            while (slot < gp.npc.length && gp.npc[slot] != null) slot++;
            if (slot >= gp.npc.length) {
                GameExceptionHandler.getInstance().handleWarning("Prea mulți NPC în zona " + zone + ": nu mai e loc în gp.npc", null);
                return;
            }
            gp.npc[slot] = new DataNPC(gp, gp.tileSize * Json.integer(def.get("col"), 0), gp.tileSize * Json.integer(def.get("row"), 0), def);
        }
    }

    @Override
    public void getImage() {
        try {
            BufferedImage img = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(spritePath)));
            down1 = tint == null ? img : tinted(img, tint);
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut încărca sprite-ul NPC-ului " + id + ": " + spritePath, e);
        }
    }

    @Override public void setDialogue() { /* replicile vin din JSON */ }

    /** Ce semn apare deasupra capului (vezi NPC.draw): ! misiune de oferit, ? gata de predat / în desfășurare. */
    @Override
    public int markerKind() {
        QuestLog ql = gp.questLog;
        if (ql.readyFor(id) != null) return MARKER_READY;
        if (ql.availableFrom(id) != null) return MARKER_AVAILABLE;
        if (ql.hasActiveTalk(id)) return MARKER_TALK;
        if (ql.activeFrom(id) != null) return MARKER_ACTIVE;
        return MARKER_NONE;
    }

    // =====================================================================================
    // CONVERSAȚIE
    // =====================================================================================

    /** Apelată la fiecare apăsare de E în dialog: începe conversația sau trece mai departe. */
    @Override
    public void speak() {
        if (!talking) {
            talking = true;
            gp.dialogueUI.speaker = name;
            mode = NORMAL;
            pendingQuest = null;
            QuestLog ql = gp.questLog;

            String reply = ql.onTalk(id); // vorbitul cu acest NPC poate bifa un obiectiv al unei misiuni active
            QuestLog.Def ready = ql.readyFor(id);
            if (ready != null) {
                mode = QUEST_TURN_IN;
                pendingQuest = ready;
                String rewards = ql.rewardText(ready);
                showText(ready.completeText + (rewards.isEmpty() ? "" : "\n" + rewards), null);
                return;
            }
            if (reply != null && !reply.isEmpty()) {
                mode = QUEST_INFO;
                showText(reply, null);
                return;
            }
            QuestLog.Def offer = ql.availableFrom(id);
            if (offer != null) {
                mode = QUEST_OFFER;
                pendingQuest = offer;
                showText(offer.offerText + "\n" + ql.rewardText(offer), List.of("Primesc misiunea.", "Poate mai târziu."));
                return;
            }
            QuestLog.Def active = ql.activeFrom(id);
            if (active != null) {
                mode = QUEST_INFO;
                showText(active.activeText + "\n" + ql.progressText(active), null);
                return;
            }

            // dialogul obișnuit
            if ("simple".equals(type)) {
                showText(nextLine(), null);
            } else if (startNode == null || !nodes.containsKey(startNode)) {
                end();
            } else {
                goTo(startNode);
            }
            return;
        }

        // Textul curent a fost citit și nu are opțiuni: predăm misiunea / continuăm / încheiem
        if (mode == QUEST_TURN_IN) {
            gp.questLog.turnIn(pendingQuest);
            gp.fx.rewardBurst(gp.camera.screenX(worldX) + gp.tileSize / 2, gp.camera.screenY(worldY));
            end();
            return;
        }
        if (mode != NORMAL) { end(); return; }

        Node n = currentNode == null ? null : nodes.get(currentNode);
        if (n != null && n.next() != null && nodes.containsKey(n.next())) goTo(n.next());
        else end();
    }

    /** Jucătorul a ales o opțiune (indexul din lista curentă). */
    @Override
    public void choose(int index) {
        if (mode == QUEST_OFFER) {
            if (index == 0 && pendingQuest != null) {
                gp.questLog.accept(pendingQuest);
                mode = QUEST_INFO; // după replica de acceptare, E încheie conversația
                showText(pendingQuest.acceptText, null);
            } else {
                end();
            }
            return;
        }

        Node n = currentNode == null ? null : nodes.get(currentNode);
        if (n == null || index < 0 || index >= n.choices().size()) return;
        Choice c = n.choices().get(index);
        boolean opensShop = "shop".equals(c.action());
        if (c.action() != null && !opensShop) runAction(c.action());
        if (opensShop) { end(); openShop(); return; }
        if (c.next() != null && nodes.containsKey(c.next())) goTo(c.next());
        else end();
    }

    private void goTo(String nodeId) {
        currentNode = nodeId;
        Node n = nodes.get(nodeId);
        List<String> labels = new ArrayList<>();
        for (Choice c : n.choices()) labels.add(c.text());
        showText(n.text(), labels);
        if (n.action() != null) runAction(n.action());
    }

    private void showText(String text, List<String> choiceLabels) {
        gp.dialogueUI.currentDialogue = fill(text);
        gp.dialogueUI.resetCharIndex();
        if (choiceLabels == null || choiceLabels.isEmpty()) gp.dialogueUI.clearChoices();
        else gp.dialogueUI.setChoices(choiceLabels);
    }

    private String nextLine() {
        if (lines.isEmpty()) return "...";
        if (randomLines) return lines.get(RNG.nextInt(lines.size()));
        String l = lines.get(lineIndex % lines.size());
        lineIndex++;
        return l;
    }

    private String fill(String text) {
        return text.replace("{name}", String.valueOf(gp.player.name)).replace("{coins}", String.valueOf(gp.player.coins));
    }

    private void end() {
        talking = false;
        mode = NORMAL;
        pendingQuest = null;
        currentNode = null;
        gp.dialogueUI.clearChoices();
        gp.dialogueUI.speaker = null;
        gp.activeNpcIndex = -1;
        gp.changeState(new PlayState(gp));
    }

    private void openShop() {
        gp.changeState(new ShopState(gp, name, stock));
    }

    /** Acțiuni din JSON: "heal:N", "coins:N". ("shop" se tratează la alegerea opțiunii.) */
    private void runAction(String action) {
        String[] p = action.split(":");
        int arg = p.length > 1 ? parseInt(p[1]) : 0;
        switch (p[0]) {
            case "heal" -> {
                gp.player.life = Math.min(gp.player.maxLife, gp.player.life + arg);
                gp.player.notifyObservers();
                gp.fx.healSparkle();
            }
            case "coins" -> gp.player.addCoins(arg);
            default -> GameExceptionHandler.getInstance().handleWarning("Acțiune necunoscută în npcs.json: " + action, null);
        }
    }

    // =====================================================================================
    // ajutoare
    // =====================================================================================

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static Color parseColor(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) return null;
        try { return Color.decode(hex); } catch (NumberFormatException e) { return null; }
    }

    /** Copie a sprite-ului colorată cu "tint", păstrând luminile și umbrele desenului. */
    private static BufferedImage tinted(BufferedImage src, Color tint) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int a = argb >>> 24;
                int or = (argb >> 16) & 255, og = (argb >> 8) & 255, ob = argb & 255;
                // Sprite-ul e foarte închis, deci ridicăm luminozitatea ca tenta să se vadă
                double luma = 0.30 * or + 0.59 * og + 0.11 * ob;
                double k = (60 + 0.7 * luma) / 190.0;
                int r = (int) Math.min(255, tint.getRed() * k * 0.8 + or * 0.2);
                int g = (int) Math.min(255, tint.getGreen() * k * 0.8 + og * 0.2);
                int b = (int) Math.min(255, tint.getBlue() * k * 0.8 + ob * 0.2);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }
}
