package Main;

/**
 * Camera unică a jocului. Toate desenările (tile-uri, Eron, inamici, NPC-uri, ceață) își iau
 * poziția pe ecran de aici, în loc să-și repete fiecare calculul cu marginile hărții.
 *
 * Colțul stânga-sus al ecranului, în coordonate lume, este (x, y). Camera îl urmărește pe Eron
 * și este limitată la dimensiunea REALĂ a hărții curente (TileManager.actualMapCols/Rows).
 */
public class Camera {
    private final GamePanel gp;

    // Citite de thread-ul Swing (desenare), scrise de thread-ul jocului
    private volatile double x, y;

    public Camera(GamePanel gp) {
        this.gp = gp;
    }

    /** Poziția pe care ar trebui s-o aibă camera acum: Eron în centru, fără să iasă din hartă. */
    private double targetX() {
        double wanted = gp.player.worldX + gp.tileSize / 2.0 - gp.screenWidth / 2.0;
        return clamp(wanted, 0, maxX());
    }

    private double targetY() {
        double wanted = gp.player.worldY + gp.tileSize / 2.0 - gp.screenHeight / 2.0;
        return clamp(wanted, 0, maxY());
    }

    private double maxX() {
        return Math.max(0, gp.tileM.actualMapCols * gp.tileSize - gp.screenWidth);
    }

    private double maxY() {
        return Math.max(0, gp.tileM.actualMapRows * gp.tileSize - gp.screenHeight);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Mută camera direct pe țintă (după teleportare, încărcare de hartă etc.). */
    public void snap() {
        x = targetX();
        y = targetY();
    }

    /** Apelată o dată pe tick de update. */
    public void update() {
        snap();
    }

    // --- Coordonate pentru desenare ---

    public int getX() { return (int) Math.round(x); }
    public int getY() { return (int) Math.round(y); }

    public int screenX(int worldX) { return worldX - getX(); }
    public int screenY(int worldY) { return worldY - getY(); }
}
