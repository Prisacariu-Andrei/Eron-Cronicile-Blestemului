package Entity;

import Entity.Player.Player;
import Observer.DamagePopUp;
import Main.GamePanel;
import Entity.Inamici.Enemy;
import Main.Exception.GameExceptionHandler;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class Entity {
    public GamePanel gp;

    public int worldX, worldY;
    public int speed;

    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2, fightStand;
    public String direction = "down";

    public Rectangle hitBox = new Rectangle(0, 0, 48, 48);

    public int hitBoxDefaultX, hitBoxDefaultY;
    public boolean collisionOn = false;

    public int spriteCounter = 0;
    public int spriteNum = 1;

    // Statistici
    public String name = " ";
    public int maxLife = 100;
    public int life = 100;
    public int shield = 0;
    public int damage;
    public int stunTurns = 0;
    public int weaknessTurns = 0;
    public int bonusShield = 0;
    public int burnTurns = 0;

    public ArrayList<StatusEffect> activeStatuses = new ArrayList<>();

    public void applyStatus(StatusEffect.Type type, int duration, int value) {
        for (StatusEffect s : activeStatuses) {
            if (s.type == type) {
                s.duration = Math.max(s.duration, duration);
                s.value = Math.max(s.value, value);
                return;
            }
        }
        activeStatuses.add(new StatusEffect(type, duration, value));
    }

    public void updateStatuses() {
        Iterator<StatusEffect> it = activeStatuses.iterator();
        while (it.hasNext()) {
            StatusEffect s = it.next();

            switch (s.type) {
                case BURN:
                    this.life -= s.value;
                    addStatusPopup("-" + s.value + " BURN", new Color(255, 100, 0));
                    break;
                case POISON:
                    this.life -= s.value;
                    addStatusPopup("-" + s.value + " POISON", new Color(0, 200, 50));
                    break;
            }

            if (this.life <= 0) {
                this.life = 0;
                if (this instanceof Enemy) {
                    ((Enemy) this).alive = false;
                }
            }

            s.duration--;
            if (s.duration <= 0) it.remove();
        }
    }

    private void addStatusPopup(String text, Color color) {
        if (gp == null) {
            return;
        }

        int px, py;

        if (this instanceof Player) {
            px = gp.tileSize * 3;
        } else {
            px = gp.screenWidth - gp.tileSize * 6;
        }
        py = gp.tileSize * 3;

        if (gp.popups != null) {
            gp.popups.add(new DamagePopUp(px, py, text, color));
        }
    }

    protected BufferedImage setup(String imagePath) {
        try {
            return javax.imageio.ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(imagePath + ".png")));
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la încărcarea imaginii prin metoda setup: " + imagePath, e);
            return null;
        }
    }

    protected BufferedImage flipImage(BufferedImage image) {
        if (image == null) return null;

        int w = image.getWidth();
        int h = image.getHeight();

        BufferedImage flipped = new BufferedImage(w, h, image.getType());
        java.awt.Graphics2D g2d = flipped.createGraphics();
        g2d.drawImage(image, 0, 0, w, h, w, 0, 0, h, null);
        g2d.dispose();

        return flipped;
    }
}