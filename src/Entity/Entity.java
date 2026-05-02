package Entity;

import Entity.Player.Player;
import Observer.DamagePopUp;
import Main.GamePanel;
import Entity.Inamici.Enemy;

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
    public String direction = "down"; // Setează o direcție default ca să nu ai erori

    public Rectangle hitBox = new Rectangle(0, 0, 48, 48); // Hitbox default

    // --- ADAUGĂ ACESTE LINII ---

    // Coordonatele de bază ale hitbox-ului (necesare pentru CollisionChecker)
    public int hitBoxDefaultX, hitBoxDefaultY;

    // Pentru logica inamicilor (unde se spawnează)
    public int startWorldX, startWorldY;

    // Controlul acțiunilor (ca să nu schimbe direcția la fiecare frame)
    public int actionLockCounter = 0;

    // State-ul entității
    public boolean collisionOn = false; // Folosește numele acesta dacă vrei să fii compatibil cu checkPlayer
    // ---------------------------

    public int spriteCounter = 0;
    public int spriteNum = 1; // De obicei plecăm de la 1

    // Statistici
    public String name = " ";
    public int maxLife = 100;
    public int life = 100;
    public int shield = 0;
    public int damage; // Adaugă și damage-ul de bază al entității
    public int stunTurns = 0;
    public int weaknessTurns = 0;
    public int burnTurns = 0;
    public int voltCount = 0;
    // În clasa Entity (mama lui Player și Enemy)
    public ArrayList<StatusEffect> activeStatuses = new ArrayList<>();

    public void applyStatus(StatusEffect.Type type, int duration, int value) {
        // Verificăm dacă are deja statusul, dacă da, îi reînnoim durata
        for (StatusEffect s : activeStatuses) {
            if (s.type == type) {
                s.duration = Math.max(s.duration, duration);
                s.value = Math.max(s.value, value);
                return;
            }
        }
        activeStatuses.add(new StatusEffect(type, duration, value));
    }

    // În clasa Entity.java
    public void updateStatuses() {
        Iterator<StatusEffect> it = activeStatuses.iterator();
        while (it.hasNext()) {
            StatusEffect s = it.next();

            switch (s.type) {
                case BURN:
                    this.life -= s.value;
                    // Popup Portocaliu pentru foc
                    addStatusPopup("-" + s.value + " BURN", new Color(255, 100, 0));
                    break;
                case POISON:
                    this.life -= s.value;
                    // Popup Verde pentru otravă
                    addStatusPopup("-" + s.value + " POISON", new Color(0, 200, 50));
                    break;
            }

            if (this.life <= 0) {
                this.life = 0;
                if (this instanceof Enemy) {
                    ((Enemy) this).alive = false;
                }
                // Dacă e jucătorul, poți declanșa Game Over aici
            }

            s.duration--;
            if (s.duration <= 0) it.remove();
        }
    }

    private void addStatusPopup(String text, Color color) {
        // Verificare de siguranță: dacă gp nu a fost setat, oprim metoda să nu dea crash
        if (gp == null) {
            System.out.println("DEBUG: gp este NULL în Entity pentru " + this.name);
            return;
        }

        int px, py;

        if (this instanceof Player) {
            px = gp.tileSize * 3;
        } else {
            // Am ajustat puțin X-ul ca să nu iasă din ecran inamicul
            px = gp.screenWidth - gp.tileSize * 6;
        }
        py = gp.tileSize * 3;

        // Verificăm și dacă lista de popups există
        if (gp.popups != null) {
            gp.popups.add(new DamagePopUp(px, py, text, color));
        }
        System.out.println("DEBUG: Adăugăm popup: " + text + " la " + px + "," + py);
    }

    protected BufferedImage setup(String imagePath) {
        try {
            return javax.imageio.ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(imagePath + ".png")));
        } catch (Exception e) {
            System.out.println("Eroare la incarcarea imaginii: " + imagePath);
            return null;
        }
    }
}