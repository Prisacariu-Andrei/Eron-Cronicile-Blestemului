package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Map;

/**
 * Malakor, Monarhul Amurgului: bătălia finală în trei faze (cifrele sunt în res/Data/castle.json, secțiunea "malakor").
 *  Faza I   - Egida Monarhului: Pecetea de Obsidian (un scut persistent care absoarbe daunele și se reface puțin); loviturile lui te Slăbesc.
 *  Faza II  - Furia Umbrei Frânte: scutul a crăpat; loviturile lui te fac Fragil (primești daune mult mai mari).
 *  Faza III - Eclipsa Finală: aproape învins, își varsă blestemul: atacul crește cu fiecare tură.
 * Fiecare schimbare de fază are banner cu text, scuturat, flash și o nouă imagine.
 */
public class Enemy_Malakor extends Enemy {

    public int currentPhase = 1; // 1 = Egida Monarhului, 2 = Furia Umbrei Frânte, 3 = Eclipsa Finală

    private final Map<String, Object> cfg;
    private int phase3Turns = 0;

    public Enemy_Malakor(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.cfg = Json.map(Json.map(Json.parseResource("/Data/castle.json")).get("malakor"));
        this.name = "Malakor, Monarhul Amurgului";
        Map<String, Object> p1 = phase(1);
        this.maxLife = Json.integer(p1.get("life"), 300);
        this.life = maxLife;
        this.shield = Json.integer(p1.get("shield"), 180);   // Pecetea de Obsidian
        this.persistentShield = true;
        this.attackDamage = Json.integer(p1.get("attack"), 14);
        this.mapSize = 1.4;
        this.introTitle = Json.str(p1.get("title"), "FAZA I");
        this.introSubtitle = Json.str(p1.get("subtitle"), "");

        hitBox = new Rectangle(0, 0, (int) (gp.tileSize * mapSize), (int) (gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;
        this.battleMusicKey = "InamiciCastelull";

        getImage();
        setNextIntent();
    }

    private Map<String, Object> phase(int n) { return Json.map(cfg.get("phase" + n)); }

    @Override
    public void getImage() {
        up1 = up2 = down1 = down2 = left1 = left2 = right1 = right2 = setup("/Enemy/MalakorPhaseOne");
        battleBackground = setup("/Backgrounds/Malakor_bg");
        fightStand = switch (currentPhase) {
            case 2 -> setup("/Enemy/MalakorPhaseTwo");
            case 3 -> setup("/Enemy/MalakorPhaseThree");
            default -> setup("/Enemy/MalakorPhaseOne");
        };
    }

    @Override
    public void setNextIntent() {
        nextAction = EnemyAction.ATTACK;
        intentValue = switch (currentPhase) {
            case 2 -> Json.integer(phase(2).get("attack"), 20);
            case 3 -> Json.integer(phase(3).get("attack"), 12) + phase3Turns * Json.integer(phase(3).get("scalePerTurn"), 5);
            default -> Json.integer(phase(1).get("attack"), 14);
        };
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        switch (currentPhase) {
            case 1 -> {   // lovitură precisă de energie întunecată: Slăbiciune
                player.damagePlayer(intentValue);
                player.applyStatus(StatusEffect.Type.WEAK, Json.integer(phase(1).get("weakTurns"), 2), 25);
                int regen = Json.integer(phase(1).get("shieldRegen"), 0);
                if (regen > 0 && shield > 0) {
                    shield += regen;
                    gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "Pecetea se refac +" + regen, new Color(170, 120, 255)));
                }
            }
            case 2 -> {   // Fragilitate: daune mult mai mari asupra lui Eron
                player.damagePlayer(intentValue);
                player.applyStatus(StatusEffect.Type.VULNERABLE, Json.integer(phase(2).get("fragilityTurns"), 2), Json.integer(phase(2).get("fragilityPercent"), 100));
            }
            case 3 -> {   // Scalare: mai tare în fiecare tură
                player.damagePlayer(intentValue);
                phase3Turns++;
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "Atacul crește!", new Color(255, 120, 60)));
            }
            default -> { }
        }
        setNextIntent();
    }

    private void changePhase(int next) {
        currentPhase = next;
        Map<String, Object> ph = phase(next);
        switch (next) {
            case 2 -> {
                name = "Malakor, Furia Umbrei Frânte";
                maxLife = Json.integer(ph.get("life"), 300);
                life = maxLife;
                shield = 0;
                mapSize = 1.5;
            }
            case 3 -> {
                name = "Malakor, Eclipsa Finală";
                maxLife = Json.integer(ph.get("life"), 220);
                life = maxLife;
                shield = 0;
                phase3Turns = 0;
                mapSize = 2;
            }
            default -> { }
        }
        stunTurns = 0;
        getImage();
        setNextIntent();
        gp.battleUI.announcePhase(Json.str(ph.get("title"), "FAZĂ NOUĂ"), Json.str(ph.get("subtitle"), ""), next == 2 ? Color.ORANGE : new Color(255, 80, 80));
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, Json.str(ph.get("title"), ""), next == 2 ? Color.ORANGE : Color.RED));
        gp.fx.phaseChange();
        endTurnSafely();   // Eron primește o mână nouă, fără ca inamicul să joace în faza nouă
    }

    @Override
    public void takeDamage(Player player, int amount) {
        int dmg = incomingDamage(player, amount);

        if (shield > 0) {   // Pecetea de Obsidian (faza I) absoarbe lovitura
            int absorbed = Math.min(shield, dmg);
            shield -= absorbed;
            dmg -= absorbed;
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "-" + absorbed + " PECETE", new Color(170, 140, 255)));
            hitFeedback(0, true);
            if (currentPhase == 1 && shield <= 0) {   // scutul crapă: începe faza II
                changePhase(2);
                return;
            }
            if (dmg <= 0) return;
        }

        life -= dmg;
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "-" + dmg, Color.RED));
        hitFeedback(dmg, false);

        if (life <= 0) {
            if (currentPhase == 1) {          // (fără scut, dar cu viața la zero: tot faza II)
                changePhase(2);
            } else if (currentPhase == 2) {
                changePhase(3);
            } else {                          // doar în faza III moare de tot
                name = "Malakor";
                life = 0;
                alive = false;
            }
        }
    }

    /** Curăță mâna și începe tura lui Eron din nou, pe noua fază (fără tura inamicului între ele). */
    private void endTurnSafely() {
        if (gp.player == null) return;
        gp.player.isPlayerTurn = false;
        while (!gp.player.inventory.hand.isEmpty()) gp.player.inventory.discard(gp.player.inventory.hand.removeFirst());
        gp.player.shield = 0;
        gp.player.currentMana = gp.player.maxMana;
        gp.player.isPlayerTurn = true;
        for (int i = 0; i < 5; i++) gp.player.inventory.drawCard();
        gp.battleUI.commandNum = 0;
    }
}
