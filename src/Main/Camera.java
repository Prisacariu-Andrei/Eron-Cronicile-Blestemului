package Main;

/**
 * Camera unică a jocului. Toate desenările (tile-uri, Eron, inamici, NPC-uri, ceață) își iau
 * poziția pe ecran de aici, în loc să-și repete fiecare calculul cu marginile hărții.
 *
 * Colțul stânga-sus al ecranului, în coordonate lume, este (x, y). Camera îl urmărește pe Eron
 * cu o mică întârziere (lerp) și se uită puțin înainte pe direcția de mers, iar mereu este limitată
 * la dimensiunea REALĂ a hărții curente (TileManager.actualMapCols/Rows). Reglaje: GameFeel.
 */
public class Camera {
    private final GamePanel gp;

    // Citite de thread-ul Swing (desenare), scrise de thread-ul jocului
    private volatile double x, y;

    // Cât se uită camera înainte (pixeli), netezit ca să nu salte când Eron își schimbă direcția
    private double lookX, lookY;

    public Camera(GamePanel gp) {
        this.gp = gp;
    }

    /** Unde ar sta camera dacă Eron ar fi exact în centru (plus privirea înainte), fără să iasă din hartă. */
    private double targetX() {
        double wanted = gp.player.worldX + gp.tileSize / 2.0 - gp.screenWidth / 2.0 + lookX;
        return clamp(wanted, 0, maxX());
    }

    private double targetY() {
        double wanted = gp.player.worldY + gp.tileSize / 2.0 - gp.screenHeight / 2.0 + lookY;
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

    /** Mută camera direct pe țintă (după teleportare, încărcare de hartă etc.), fără deplasare animată. */
    public void snap() {
        lookX = 0;
        lookY = 0;
        x = targetX();
        y = targetY();
    }

    /** Apelată o dată pe tick de update. */
    public void update() {
        // Privirea înainte: în direcția în care merge Eron, proporțional cu cât de repede merge
        double wantLookX = 0, wantLookY = 0;
        if (gp.getCurrentState() instanceof GameState.PlayState) {
            float v = gp.player.moveVelocity * GameFeel.CAMERA_LOOK_AHEAD_PX;
            switch (gp.player.direction) {
                case "left" -> wantLookX = -v;
                case "right" -> wantLookX = v;
                case "up" -> wantLookY = -v;
                case "down" -> wantLookY = v;
            }
        }
        lookX += (wantLookX - lookX) * 0.07;
        lookY += (wantLookY - lookY) * 0.07;

        double tx = targetX(), ty = targetY();
        if (Math.abs(tx - x) > GameFeel.CAMERA_SNAP_DISTANCE || Math.abs(ty - y) > GameFeel.CAMERA_SNAP_DISTANCE) {
            snap(); // teleportare sau hartă nouă
            return;
        }
        // Se apropie de țintă cu o fracțiune pe tick: rapid când e departe, lin când ajunge
        x += (tx - x) * GameFeel.CAMERA_FOLLOW;
        y += (ty - y) * GameFeel.CAMERA_FOLLOW;
    }

    // --- Coordonate pentru desenare ---

    public int getX() { return (int) Math.round(x); }
    public int getY() { return (int) Math.round(y); }

    public int screenX(int worldX) { return worldX - getX(); }
    public int screenY(int worldY) { return worldY - getY(); }
}
