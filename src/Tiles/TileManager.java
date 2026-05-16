package Tiles;

import Main.GamePanel;
import Main.Exception.GameExceptionHandler;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;

public class TileManager {
    GamePanel gp;
    public Tile[] tile;
    public int[][] mapTileNum;
    public String currentZone = "Eldoria";

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
                break;

            case "Castelul":
                break;
        }
    }

    public void setup(int index, String imageName, boolean collision) {
        try {
            InputStream is = getClass().getResourceAsStream("/Tiles/" + imageName + ".png");
            if (is == null) {
                is = getClass().getResourceAsStream("/Tiles/Common/" + imageName.substring(imageName.lastIndexOf("/") + 1) + ".png");
            }

            if (is != null) {
                tile[index] = new Tile();
                tile[index].image = ImageIO.read(is);
                tile[index].collision = collision;
            } else {
                // Dacă resursa este complet absentă, trimitem un Warning controlat
                GameExceptionHandler.getInstance().handleWarning("Elementul grafic (Tile) din resurse lipsește complet: " + imageName, null);
            }

        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare neprevăzută întâmpinată la citirea imaginii: " + imageName, e);
        }
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

            while (row < gp.maxWorldRow) {
                String line = br.readLine();
                if (line == null) break;

                String[] numbers = line.trim().split("\\s+");

                for (col = 0; col < gp.maxWorldCol && col < numbers.length; col++) {
                    mapTileNum[col][row] = Integer.parseInt(numbers[col]);
                }
                row++;
            }
            br.close();
            System.out.println("✅ SUCCES: Harta " + filePath + " a fost încărcată corect!");

        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleCriticalError("A apărut o eroare structurală la citirea fișierului de configurare a hărții: " + filePath, e);
        }
    }

    public void draw(Graphics2D g2) {
        int worldCol = 0;
        int worldRow = 0;

        while (worldCol < gp.maxWorldCol && worldRow < gp.maxWorldRow) {
            int tileNum = mapTileNum[worldCol][worldRow];

            int worldX = worldCol * gp.tileSize;
            int worldY = worldRow * gp.tileSize;

            int screenX = worldX - gp.player.worldX + gp.player.screenX;
            int screenY = worldY - gp.player.worldY + gp.player.screenY;

            if (gp.player.worldX < gp.player.screenX) {
                screenX = worldX;
            }
            if (gp.player.worldY < gp.player.screenY) {
                screenY = worldY;
            }
            int rightOffset = gp.screenWidth - gp.player.screenX;
            if (rightOffset > (gp.maxWorldCol * gp.tileSize) - gp.player.worldX) {
                screenX = gp.screenWidth - ((gp.maxWorldCol * gp.tileSize) - worldX);
            }
            int bottomOffset = gp.screenHeight - gp.player.screenY;
            if (bottomOffset > (gp.maxWorldRow * gp.tileSize) - gp.player.worldY) {
                screenY = gp.screenHeight - ((gp.maxWorldRow * gp.tileSize) - worldY);
            }

            if (worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                    worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                    worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                    worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {

                if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null) {
                    g2.drawImage(tile[tileNum].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
                }
            } else {
                if (gp.player.worldX < gp.player.screenX ||
                        gp.player.worldY < gp.player.screenY ||
                        rightOffset > (gp.maxWorldCol * gp.tileSize) - gp.player.worldX ||
                        bottomOffset > (gp.maxWorldRow * gp.tileSize) - gp.player.worldY) {

                    if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null) {
                        g2.drawImage(tile[tileNum].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
                    }
                }
            }

            worldCol++;
            if (worldCol == gp.maxWorldCol) {
                worldCol = 0;
                worldRow++;
            }
        }
    }
}