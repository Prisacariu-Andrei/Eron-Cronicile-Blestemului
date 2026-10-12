package Tiles;

import Main.DebugStats;
import Main.GamePanel;
import Main.Exception.GameExceptionHandler;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class TileManager {
    GamePanel gp;
    public Tile[] tile;
    public int[][] mapTileNum;
    public String currentZone = "Eldoria";

    // Dimensiunea reală a hărții încărcate (în tile-uri), citită din fișier. Camera se oprește la ea.
    public int actualMapCols = 0;
    public int actualMapRows = 0;

    public TileManager(GamePanel gp) {
        this.gp = gp;
        tile = new Tile[50];
        mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];

        loadZoneTiles("Eldoria");
        loadMap("/Maps/Eldoria");
    }

    public void loadZoneTiles(String zoneName) {
        this.currentZone = zoneName;
        tile = new Tile[50];
        switch (zoneName) {
            case "Eldoria":
                setup(0, zoneName+"/grass", false);
                setup(1, zoneName+"/Rock", true);
                setup(2, zoneName+"/water", true);
                setup(3, zoneName+"/Tree", true);
                setup(4, zoneName+"/pathHorizontal", false);
                setup(5, zoneName+"/pathVertical", false);
                setup(6, zoneName+"/pathHorizontal-Up", false);
                setup(7, zoneName+"/pathHorizontal-Down", false);
                setup(8, zoneName+"/pathUp-Horizontal", false);
                setup(9, zoneName+"/pathDown-Horizontal", false);
                setup(10, zoneName+"/Intersection3Up", false);
                setup(11, zoneName+"/Intersection3Right", false);
                setup(12, zoneName+"/Intersection3Left", false);
                setup(13, zoneName+"/Intersection3Down", false);
                setup(14, zoneName+"/Intersection4", false);

                for (int i = 1; i <= 12; i++) {
                    setup(14 + i, zoneName+"/H" + i, true);
                    setup(26 + i, zoneName+"/B" + i, true);
                }

                setup(39, zoneName+"/bridge", false);
                setup(40, zoneName+"/healZone", false);
                setup(41, zoneName+"/damageZone", false);
                setup(42, zoneName+"/pathVertical", false);
                setup(43, zoneName+"/RockEntrance", false);
                setup(44, zoneName+"/pathHorizontal", false);
                setup(45, zoneName+"/pathHorizontal", false);
                setup(46, zoneName+"/RockEntrance", false);
                break;

            case "Barlogul":
                setup(0, zoneName+"/Dirt", false);
                setup(1, zoneName+"/Dirt1", false);
                setup(2, zoneName+"/Dirt2", false);
                setup(3, zoneName+"/Dirt3", false);
                setup(4, zoneName+"/Tree", true);
                setup(5, zoneName+"/water", true);
                break;

            case "Abisul":
                setup(0, zoneName+"/Dirt3", false);
                setup(1, zoneName+"/Mount", true);
                setup(2, zoneName+"/RockEntrance", false);
                break;

            case "Citadela":
                setup(0 , zoneName+"/grass", false);
                setup(1 , zoneName+"/water", true);
                setup(2 , zoneName+"/pathHorizontal", false);
                setup(3 , zoneName+"/pathVertical", false);
                setup(4 , zoneName+"/pathHorizontal-Up", false);
                setup(5 , zoneName+"/pathDown-Horizontal", false);
                setup(6 , zoneName+"/Tree", true);
                setup(7 , zoneName+"/wall", true);
                setup(8 , zoneName+"/floor", false);
                setup(9 , zoneName+"/table", true);
                setup(10, zoneName+"/tableWithDrinks", true);
                setup(11, zoneName+"/tableBroken", true);
                setup(12, zoneName+"/Stairs", false);
                setup(13, zoneName+"/window", true);
                setup(14, zoneName+"/bridge", false);
                setup(15, zoneName+"/pathHorizontal", false);
                setup(16, zoneName+"/chair", true);
                setup(17, zoneName+"/sky", true);
                break;

            case "Tundra":
                setup(0 ,zoneName+"/Tree",true);
                setup(1 ,zoneName+"/Snow",false);
                setup(2 ,zoneName+"/LeverR",false);
                setup(3 ,zoneName+"/LeverL",false);
                setup(4 ,zoneName+"/Snow",false);
                break;

            case "Podul":
                setup(0 ,zoneName+"/Abis",true);
                setup(1 ,zoneName+"/U1",true);
                setup(2 ,zoneName+"/grass",false);
                setup(3 ,zoneName+"/D1",true);
                setup(4 ,zoneName+"/L",true);
                setup(5 ,zoneName+"/ColtSTG1",true);
                setup(6 ,zoneName+"/ColtSTG2",true);
                setup(7 ,zoneName+"/PodStart2",true);
                setup(8 ,zoneName+"/PodStart1",false);
                setup(9 ,zoneName+"/PodStart3",true);
                setup(10,zoneName+"/PodMijl2",true);
                setup(11,zoneName+"/PodMijl1",false);
                setup(12,zoneName+"/PodFinal2",true);
                setup(13,zoneName+"/PodFinal1",false);
                setup(14,zoneName+"/PodFinal3",true);
                setup(15,zoneName+"/CastleGrass",false);
                setup(16,zoneName+"/ColtDRT1",true);
                setup(17,zoneName+"/ColtDRT2",true);
                setup(18,zoneName+"/R",true);
                setup(19,zoneName+"/U2",true);
                setup(20,zoneName+"/D2",true);

                setup(21 ,zoneName+"/grass",false);
                setup(22,zoneName+"/CastleGrass",false);
                break;

            case "Castelul":
                setup(0,zoneName+"/CastleGrass",false);
                setup(1,zoneName+"/T1",true);
                setup(2,zoneName+"/T2",true);
                setup(3,zoneName+"/T3",true);
                setup(4,zoneName+"/T4",true);
                setup(5,zoneName+"/T5",true);
                setup(6,zoneName+"/T6",true);
                setup(7,zoneName+"/T7",true);
                setup(8,zoneName+"/T8",true);
                setup(9,zoneName+"/W1",true);
                setup(10,zoneName+"/W2",true);
                setup(11,zoneName+"/W3",true);
                setup(12,zoneName+"/W4",true);
                setup(13,zoneName+"/W5",true);
                setup(14,zoneName+"/W6",true);
                setup(15,zoneName+"/L",true);
                setup(16,zoneName+"/R",true);
                setup(17,zoneName+"/path",false);
                setup(18,zoneName+"/R1",true);
                setup(19,zoneName+"/R2",true);
                setup(20,zoneName+"/R3",true);
                setup(21,zoneName+"/R4",true);
                setup(22,zoneName+"/R5",true);
                setup(23,zoneName+"/R6",true);
                setup(24,zoneName+"/R7",true);
                setup(25,zoneName+"/R8",true);
                setup(26 ,zoneName+"/Abis",false);


                break;
        }
        CaveTiles.install(tile, zoneName, gp.tileSize);   // decorul procedural al Abisului și al Bârlogului (torțe, tron, oase...)
    }

    public void setup(int index, String imageName, boolean collision) {
        try {
            InputStream is = getClass().getResourceAsStream("/Tiles/" + imageName + ".png");
            if (is == null) {
                is = getClass().getResourceAsStream("/Tiles/Common/" + imageName.substring(imageName.lastIndexOf("/") + 1) + ".png");
            }

            if (is == null) { // unele dale ale Castelului se împart cu Podul
                is = getClass().getResourceAsStream("/Tiles/Podul/" + imageName.substring(imageName.lastIndexOf("/") + 1) + ".png");
            }

            if (is != null) {
                tile[index] = new Tile();
                tile[index].image = scaleToTile(ImageIO.read(is));
                tile[index].collision = collision;
            } else {
                // Dacă resursa este complet absentă, trimitem un Warning controlat și desenăm o dală de umplutură,
                // ca zona să fie jucabilă (și vizibilă) până apare desenul real
                GameExceptionHandler.getInstance().handleWarning("Elementul grafic (Tile) din resurse lipsește complet: " + imageName + " (dală provizorie)", null);
                tile[index] = new Tile();
                tile[index].image = placeholder(imageName, collision);
                tile[index].collision = collision;
            }

        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare neprevăzută întâmpinată la citirea imaginii: " + imageName, e);
        }
    }

    /** Dală provizorie, în stil obsidian: piatră cu rosturi pentru dalele cu coliziune, lespezi pentru podea. Culoarea variază după nume. */
    private BufferedImage placeholder(String imageName, boolean collision) {
        int ts = gp.tileSize;
        BufferedImage img = new BufferedImage(ts, ts, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int h = Math.abs(imageName.hashCode());
        int shade = (h % 5) * 5;
        Color base = collision ? new Color(46 + shade, 40 + shade, 66 + shade) : new Color(88 + shade, 84 + shade, 100 + shade);
        g.setColor(base);
        g.fillRect(0, 0, ts, ts);
        g.setColor(base.darker());
        if (collision) {   // cărămizi
            for (int row = 0; row < 4; row++) {
                int y = row * ts / 4;
                g.drawLine(0, y, ts, y);
                int off = (row % 2) * ts / 4;
                for (int x = off; x < ts; x += ts / 2) g.drawLine(x, y, x, y + ts / 4);
            }
        } else {           // lespezi
            g.drawRect(0, 0, ts - 1, ts - 1);
            g.drawLine(ts / 2, 0, ts / 2, ts);
            g.drawLine(0, ts / 2, ts, ts / 2);
        }
        g.dispose();
        return img;
    }

    /**
     * Mărește imaginea la dimensiunea unui tile o singură dată
, la încărcare (cu aceeași interpolare "nearest"
     * pe care o folosea desenarea), ca să nu fie scalată din nou la fiecare cadru pentru fiecare tile.
     */
    private BufferedImage scaleToTile(BufferedImage src) {
        if (src == null || (src.getWidth() == gp.tileSize && src.getHeight() == gp.tileSize)) return src;
        BufferedImage scaled = new BufferedImage(gp.tileSize, gp.tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, gp.tileSize, gp.tileSize, null);
        g.dispose();
        return scaled;
    }

    public void loadMap(String filePath) {
        try {
            if (!filePath.startsWith("/")) {
                filePath = "/" + filePath;
            }

            System.out.println("DEBUG: Încerc să încarc harta de la: " + filePath);
            InputStream is = getClass().getResourceAsStream(filePath);

            if (is == null) {
                // Eroare critică - nu putem juca pe o hartă lipsă
                GameExceptionHandler.getInstance().handleCriticalError(
                        "Fisierul text al hărții lipsește din directorul res/Maps: " + filePath, null);
                return;
            }

            mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int col;
            int row = 0;
            int maxCols = 0;
            int lastFilledRow = -1;

            // ÎNLOCUIEȘTE BUCLA TA ACTUALĂ CU ACEASTA:
            while (row < gp.maxWorldRow) {
                String line = br.readLine();
                if (line == null) break;

                // Eliminăm spațiile de la capete și împărțim prin orice secvență de spații albe
                String[] numbers = line.trim().split("\\s+");

                // Reținem dimensiunea reală a hărții (rândurile goale de la final nu contează)
                if (!line.isBlank()) {
                    lastFilledRow = row;
                    maxCols = Math.max(maxCols, Math.min(numbers.length, gp.maxWorldCol));
                }

                for (col = 0; col < gp.maxWorldCol && col < numbers.length; col++) {
                    try {
                        // Trim() curăță orice spațiu accidental din jurul cifrei
                        String token = numbers[col].trim();
                        if (!token.isEmpty()) {
                            mapTileNum[col][row] = Integer.parseInt(token);
                        }
                    } catch (NumberFormatException e) {
                        // Dacă dă eroare, punem un tile "sigur" (ex: 0) și printăm unde e buba
                        System.err.println("Eroare de formatare la Rândul " + row + ", Coloana " + col + ". Valoare: '" + numbers[col] + "'");
                        mapTileNum[col][row] = 0;
                    }
                }
                row++;
            }
            br.close();
            actualMapCols = maxCols;
            actualMapRows = lastFilledRow + 1;
            // harta nouă: camera se mută direct, fără deplasare animată (nu există încă la prima încărcare)
            if (gp.camera != null && gp.player != null) gp.camera.snap();
            System.out.println("✅ SUCCES: Harta " + filePath + " (" + actualMapCols + "x" + actualMapRows + ") a fost încărcată corect!");

        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleCriticalError("A apărut o eroare structurală la citirea fișierului de configurare a hărții: " + filePath, e);
        }
    }

    /**
     * Desenează doar tile-urile care încap pe ecran (în jur de 375), nu toată grila de 200x200.
     * Poziția pe ecran vine din Camera, deci arată la fel și când camera este oprită la marginea hărții.
     */
    public void draw(Graphics2D g2) {
        int camX = gp.camera.getX();
        int camY = gp.camera.getY();

        int startCol = Math.max(0, camX / gp.tileSize);
        int endCol = Math.min(gp.maxWorldCol - 1, (camX + gp.screenWidth) / gp.tileSize);
        int startRow = Math.max(0, camY / gp.tileSize);
        int endRow = Math.min(gp.maxWorldRow - 1, (camY + gp.screenHeight) / gp.tileSize);

        int drawn = 0; // pentru overlay-ul F3
        for (int worldRow = startRow; worldRow <= endRow; worldRow++) {
            int screenY = worldRow * gp.tileSize - camY;
            for (int worldCol = startCol; worldCol <= endCol; worldCol++) {
                int tileNum = mapTileNum[worldCol][worldRow];
                if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null) {
                    // Imaginile sunt deja la dimensiunea unui tile (vezi scaleToTile), deci copiere simplă, fără scalare
                    g2.drawImage(tile[tileNum].image, worldCol * gp.tileSize - camX, screenY, null);
                    drawn++;
                }
            }
        }
        DebugStats.tilesDrawn = drawn;
    }
}
