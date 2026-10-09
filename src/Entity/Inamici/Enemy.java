package Entity.Inamici;

import Entity.Entity;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GameFeel;
import Main.GamePanel;
import Observer.DamagePopUp;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public abstract class Enemy extends Entity {
    public String name;
    public boolean alive = true;
    public BufferedImage battleBackground;
    public String battleMusicKey;
    public double battleSize = 1;
    public double mapSize = 1;
    public int nextActionValue = 0;

    // Logica de Bătălie
    public enum EnemyAction { ATTACK, HEAL, DEFEND, SPECIAL, BUFF}
    public EnemyAction nextAction;
    public int intentValue;
    public int attackDamage;
    public boolean persistentShield = false;   // scutul nu dispare la sfârșitul turei (Valerius, Malakor)
    public String introTitle, introSubtitle;    // bannerul de la începutul luptei (Pretorienii, fazele lui Malakor)

    // Grup de luptă (enemies.json): membrii suplimentari care luptă alături de acesta când te atingi de el
    public java.util.List<String> packExtras = new java.util.ArrayList<>();
    public int battleCenterX = -1; // pus de BattleUI la desenare, ca să apară popup-urile deasupra inamicului potrivit

    protected int popX() { return battleCenterX > 0 ? battleCenterX : gp.screenWidth - gp.tileSize * 6; }

    /** Inamicii în viață din aceeași luptă (el însuși inclus). */
    public java.util.List<Enemy> teamMates() {
        java.util.List<Enemy> out = new java.util.ArrayList<>();
        for (Enemy e : gp.player.team) if (e.alive) out.add(e);
        if (!out.contains(this) && alive) out.add(this);
        return out;
    }

    // Logică de Mișcare & AI
    protected int startWorldX, startWorldY;
    protected int actionLockCounter = 0;
    protected int wanderRange;
    protected int detectionRange;
    protected boolean isChasing = false;
    public boolean isStatic = false; // Dacă e true, inamicul nu se mișcă pe hartă

    public Enemy(GamePanel gp, int worldX, int worldY) {
        this.gp = gp;
        this.worldX = worldX;
        this.worldY = worldY;

        // Salvăm punctul de spawn pentru patrulă
        this.startWorldX = worldX;
        this.startWorldY = worldY;

        // Inițializăm distanțele bazate pe mărimea tile-ului
        this.wanderRange = gp.tileSize * 2;
        this.detectionRange = gp.tileSize * 5;

        this.direction = "down";
        this.speed = 1; // Viteza de patrulare

        // Hitbox moștenit din Entity
        this.hitBox = new Rectangle(8, 16, 32, 32);
        this.hitBoxDefaultX = hitBox.x;
        this.hitBoxDefaultY = hitBox.y;
    }

    // Metode abstracte obligatorii pentru subclase
    public abstract void getImage();
    public abstract void setNextIntent();

    public void update() {
        if (alive) {
            gp.cChecher.checkPlayer(this);

            if (!isStatic) {
                setAction();
                collisionOn = false;
                gp.cChecher.checkTile(this);

                if (!collisionOn) {
                    switch (direction) {
                        case "up" -> worldY -= speed;
                        case "down" -> worldY += speed;
                        case "left" -> worldX -= speed;
                        case "right" -> worldX += speed;
                    }
                }

                spriteCounter++;
                if (spriteCounter > 12) {
                    spriteNum = (spriteNum == 1) ? 2 : 1;
                    spriteCounter = 0;
                }
            }
        }
    }

    public void setAction() {
        int diffX = Math.abs(worldX - gp.player.worldX);
        int diffY = Math.abs(worldY - gp.player.worldY);
        int distance = (int) Math.sqrt(diffX * diffX + diffY * diffY);

        if (distance < detectionRange) {
            isChasing = true;
            chasePlayer();
        } else {
            if (distance > detectionRange * 1.5) {
                isChasing = false;
            }
            wanderAround();
        }
    }

    private void chasePlayer() {
        speed = 2;
        if (worldX < gp.player.worldX) direction = "right";
        else if (worldX > gp.player.worldX) direction = "left";

        if (Math.abs(worldX - gp.player.worldX) < speed) {
            if (worldY < gp.player.worldY) direction = "down";
            else if (worldY > gp.player.worldY) direction = "up";
        }
    }

    private void wanderAround() {
        speed = 1;
        actionLockCounter++;

        if (actionLockCounter >= 120) {
            Random random = new Random();
            int i = random.nextInt(100) + 1;

            if (i <= 25) direction = "up";
            else if (i <= 50) direction = "down";
            else if (i <= 75) direction = "left";
            else direction = "right";

            actionLockCounter = 0;
        }

        if (worldX > startWorldX + wanderRange) direction = "left";
        if (worldX < startWorldX - wanderRange) direction = "right";
        if (worldY > startWorldY + wanderRange) direction = "up";
        if (worldY < startWorldY - wanderRange) direction = "down";
    }


    public void draw(Graphics2D g2) {
        if (alive) {
            // Leagăn lent pe verticală (mai nervos când te urmărește), ca să nu stea nimic perfect nemișcat
            float bobRate = isChasing ? 2.2f : 1f;
            float bobPhase = (startWorldX * 7 + startWorldY * 13) % (int) GameFeel.IDLE_BOB_PERIOD_TICKS;
            int bob = Math.round((float) Math.sin((gp.fx.ticks * bobRate + bobPhase) * 2 * Math.PI / GameFeel.IDLE_BOB_PERIOD_TICKS)
                    * GameFeel.IDLE_BOB_PX * (isChasing ? 1.5f : 1f));

            int screenX = gp.camera.screenX(worldX);
            int screenY = gp.camera.screenY(worldY) + bob;
            int drawSize = (int)(gp.tileSize * mapSize);

            // Desenăm doar ce se vede pe ecran
            if (screenX + drawSize > 0 && screenX < gp.screenWidth &&
                    screenY + drawSize > 0 && screenY < gp.screenHeight) {

                BufferedImage image = switch(direction) {
                    case "up" -> (spriteNum == 1) ? up1 : up2;
                    case "down" -> (spriteNum == 1) ? down1 : down2;
                    case "left" -> (spriteNum == 1) ? left1 : left2;
                    case "right" -> (spriteNum == 1) ? right1 : right2;
                    default -> null;
                };

                drawPackMembers(g2, screenX, screenY, bob);
                if(image != null) {
                    g2.drawImage(image, screenX, screenY, drawSize, drawSize, null);
                } else {
                    g2.setColor(Color.RED);
                    g2.fillRect(screenX + 4, screenY + 4, drawSize - 8, drawSize - 8);
                }
            }
        }
    }

    private java.util.List<Enemy> packVisuals;

    /**
     * Grupul se vede pe hartă: ceilalți membri (enemies.json -> pack/with) stau în jurul liderului, cu sprite-ul lor, și îl urmează.
     * Sunt doar desen; lupta pornește când te atingi de grup.
     */
    private void drawPackMembers(Graphics2D g2, int screenX, int screenY, int bob) {
        if (packExtras.isEmpty()) return;
        if (packVisuals == null) {
            packVisuals = new java.util.ArrayList<>();
            for (String id : packExtras) {
                Enemy e = EnemyFactory.createEnemy(id, gp, 0, 0);
                if (e != null) packVisuals.add(e);
            }
        }
        double[][] spots = {{0.62, 0.30}, {-0.62, 0.30}, {0.0, -0.50}};
        for (int i = 0; i < packVisuals.size(); i++) {
            Enemy m = packVisuals.get(i);
            BufferedImage img = m.down1 != null ? m.down1 : m.fightStand;
            if (img == null) continue;
            double[] s = spots[i % spots.length];
            int size = (int) (gp.tileSize * Math.max(0.8, m.mapSize));
            int mb = Math.round((float) Math.sin((gp.fx.ticks + i * 23) * 2 * Math.PI / GameFeel.IDLE_BOB_PERIOD_TICKS) * GameFeel.IDLE_BOB_PX);
            g2.drawImage(img, screenX + (int) (s[0] * gp.tileSize), screenY + (int) (s[1] * gp.tileSize) + mb, size, size, null);
        }
    }

    // În Entity.Inamici.Enemy.java

    /** Feedback în luptă când inamicul e lovit: shake, hit-stop, scântei, sunet (reglaje în GameFeel). */
    protected void hitFeedback(int dealt, boolean blocked) {
        if (!(gp.getCurrentState() instanceof GameState.BattleState)) return;
        if (blocked) gp.fx.enemyBlocked();
        else gp.fx.enemyHurt(dealt);
    }

    /** Damage-ul care ajunge de fapt la inamic: Colțul, Vulnerabilitatea lui și Slăbiciunea lui Eron (-25%). */
    protected int incomingDamage(Player player, int amount) {
        int finalDamage = amount;

        if(player.areDinteChilimbar){
            finalDamage += 3;
        }
        // VERIFICARE VULNERABILITATE
        for (StatusEffect s : activeStatuses) {
            if (s.type == StatusEffect.Type.VULNERABLE) {

                // Luăm valoarea de bază a efectului (ex: 50 pentru +50%)
                int procentVulnerabilitate = s.value;

                // Dacă jucătorul a învins-o pe Jean și are Paloșul Legendar, mărim procentul la 75% (adică x1.75 damage)
                if (gp.player.arePalosLegendar && procentVulnerabilitate == 50) {
                    procentVulnerabilitate = 75;
                }

                // Aplicăm noua formulă cu procentul actualizat
                finalDamage = amount + (amount * procentVulnerabilitate / 100);

                // Debug în consolă ca să vezi clar dacă s-a aplicat runa
                System.out.println("VULNERABIL! Damage crescut de la " + amount + " la " + finalDamage +
                        (gp.player.arePalosLegendar ? " (Efect Paloș Legendar Activ!)" : ""));
                break;
            }
        }

        return player.weakened(finalDamage);
    }

    public void takeDamage(Player player, int amount) {
        int finalDamage = incomingDamage(player, amount);

        if (this.shield < finalDamage) {
            int remaining = finalDamage - shield;
            shield = 0;
            life -= remaining;
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "-" + remaining, Color.RED));
            hitFeedback(remaining, false);
        } else {
            shield -= finalDamage;
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "-" + finalDamage + " BLOCK", Color.GRAY));
            hitFeedback(0, true);
        }

        if (life <= 0) {
            life = 0;
            alive = false;
            // Progresul quest-urilor se actualizează în Player.winBattle()
        }
    }

    public void heal(int amount) {
        life = Math.min(life + amount, maxLife);
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "+" + amount, Color.GREEN));
    }

    public void defend(int amount) {
        shield += amount;
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "+" + amount, Color.CYAN));
    }

    public void executeIntent(Player player) {
        if (stunTurns == 0) {
            if (nextAction == EnemyAction.ATTACK) {

                int damageToDeal = intentValue;

                // VERIFICARE SLĂBICIUNE (WEAKNESS)
                for (StatusEffect s : activeStatuses) {
                    if (s.type == StatusEffect.Type.WEAK) {
                        damageToDeal = intentValue / 2; // Reduce damage-ul la jumătate
                        break;
                    }
                }

                player.damagePlayer(damageToDeal);
            } else if (nextAction == EnemyAction.HEAL) {
                this.heal(intentValue);
            } else if (nextAction == EnemyAction.DEFEND) {
                this.defend(intentValue);
            }
            setNextIntent();
        }
    }
}