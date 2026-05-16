package Entity.Inamici;

import Entity.Entity;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GamePanel;
import Observer.DamagePopUp;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public abstract class Enemy extends Entity {
    public String name;
    public boolean alive = true;
    public BufferedImage battleBackground;
    public double battleSize = 1;
    public double mapSize = 1;


    // Logica de Bătălie
    public enum EnemyAction { ATTACK, HEAL, DEFEND, SPECIAL, BUFF}
    public EnemyAction nextAction;
    public int intentValue;
    public int attackDamage;

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
            int screenX = worldX - gp.player.worldX + gp.player.screenX;
            int screenY = worldY - gp.player.worldY + gp.player.screenY;
            int drawSize = (int)(gp.tileSize * mapSize);

            if (gp.player.worldX < gp.player.screenX) {
                screenX = worldX;
            } else if (gp.screenWidth - gp.player.screenX > (gp.maxWorldCol * gp.tileSize) - gp.player.worldX) {
                screenX = worldX - (gp.maxWorldCol * gp.tileSize - gp.screenWidth);
            }

            if (gp.player.worldY < gp.player.screenY) {
                screenY = worldY;
            } else if (gp.screenHeight - gp.player.screenY > (gp.maxWorldRow * gp.tileSize) - gp.player.worldY) {
                screenY = worldY - (gp.maxWorldRow * gp.tileSize - gp.screenHeight);
            }

            if (worldX + drawSize > gp.player.worldX - gp.player.screenX &&
                    worldX - drawSize < gp.player.worldX + gp.player.screenX &&
                    worldY + drawSize > gp.player.worldY - gp.player.screenY &&
                    worldY - drawSize < gp.player.worldY + gp.player.screenY) {

                BufferedImage image = switch(direction) {
                    case "up" -> (spriteNum == 1) ? up1 : up2;
                    case "down" -> (spriteNum == 1) ? down1 : down2;
                    case "left" -> (spriteNum == 1) ? left1 : left2;
                    case "right" -> (spriteNum == 1) ? right1 : right2;
                    default -> null;
                };

                if(image != null) {
                    g2.drawImage(image, screenX, screenY, drawSize, drawSize, null);
                } else {
                    g2.setColor(Color.RED);
                    g2.fillRect(screenX + 4, screenY + 4, drawSize - 8, drawSize - 8);
                }
            }
        }
    }

    // În Entity.Inamici.Enemy.java

    public void takeDamage(int amount) {
        int finalDamage = amount;

        // VERIFICARE VULNERABILITATE
        for (StatusEffect s : activeStatuses) {
            if (s.type == StatusEffect.Type.VULNERABLE) {
                // Exemplu: +50% damage (finalDamage * 1.5)
                finalDamage = amount + (amount * s.value / 100);

                // Debug opțional să vezi în consolă
                System.out.println("VULNERABIL! Damage crescut de la " + amount + " la " + finalDamage);
                break;
            }
        }

        if (this.shield < finalDamage) {
            int remaining = finalDamage - shield;
            shield = 0;
            life -= remaining;
            gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "-" + remaining, Color.RED));
        } else {
            shield -= finalDamage;
            gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "-" + finalDamage + " BLOCK", Color.GRAY));
        }

        if (life <= 0) {
            life = 0;
            alive = false;
            checkQuestProgress();
        }
    }

    private void checkQuestProgress() {
        if (this.name != null && this.name.equals("Lup")) {
            var quest2 = gp.questManager.getQuestById(2, true);
            if (quest2 != null && quest2.isReceived && !quest2.isCompleted) {
                gp.questManager.updateQuestProgress(2, true, 1);
                gp.ui.message = "Progres: " + quest2.currentAmount + "/5";
                gp.ui.messageOn = true;
            } else {
                var quest1 = gp.questManager.getQuestById(1, true);
                if (quest1 != null && quest1.isReceived && !quest1.isCompleted) {
                    gp.questManager.updateQuestProgress(1, true, 1);
                    gp.ui.message = "Misiune completată: Tutorial";
                    gp.ui.messageOn = true;
                }
            }
        }
    }

    public void heal(int amount) {
        life = Math.min(life + amount, maxLife);
        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "+" + amount, Color.GREEN));
    }

    public void defend(int amount) {
        shield += amount;
        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "+" + amount, Color.CYAN));
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