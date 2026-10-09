package Main;

import Entity.Entity;
import Entity.Inamici.Enemy;
import GameState.BattleState; // Import nou pentru verificare

import java.awt.*;

public class CollisionChecker {
    GamePanel gp;

    public CollisionChecker(GamePanel gp){
        this.gp=gp;
    }
    public void checkTile(Entity entity){

        int entityLeftWorldX = entity.worldX + entity.hitBox.x;
        int entityRightWorldX = entity.worldX + entity.hitBox.x + entity.hitBox.width;
        int entityTopWorldY = entity.worldY + entity.hitBox.y;
        int entityBottomWorldY = entity.worldY + entity.hitBox.y + entity.hitBox.height;

        int entityLeftCol= entityLeftWorldX/gp.tileSize;
        int entityRightCol= entityRightWorldX/gp.tileSize;
        int entityTopRow= entityTopWorldY/gp.tileSize;
        int entityBottomRow= entityBottomWorldY/gp.tileSize;

        if (entityLeftCol < 0) entityLeftCol = 0;
        if (entityRightCol >= gp.maxWorldCol) entityRightCol = gp.maxWorldCol - 1;
        if (entityTopRow < 0) entityTopRow = 0;
        if (entityBottomRow >= gp.maxWorldRow) entityBottomRow = gp.maxWorldRow - 1;

        int tileNum1, tileNum2;

        switch(entity.direction){
            case "up":
                entityTopRow=Math.max(0, (entityTopWorldY-entity.speed)/gp.tileSize);
                tileNum1= gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                tileNum2= gp.tileM.mapTileNum[entityRightCol][entityTopRow];
                if (gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision ||
                        gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) {
                    entity.collisionOn = true;
                }
                break;
            case "down":
                entityBottomRow=Math.min(gp.maxWorldRow - 1, (entityBottomWorldY+entity.speed)/gp.tileSize); // fără ieșire din matrice la limita hărții
                tileNum1= gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                tileNum2= gp.tileM.mapTileNum[entityRightCol][entityBottomRow];
                if (gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision ||
                        gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) {
                    entity.collisionOn = true;
                }
                break;
            case "left":
                entityLeftCol=Math.max(0, (entityLeftWorldX-entity.speed)/gp.tileSize);
                tileNum1= gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                tileNum2= gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                if (gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision ||
                        gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) {
                    entity.collisionOn = true;
                }
                break;
            case "right":
                entityRightCol=Math.min(gp.maxWorldCol - 1, (entityRightWorldX+entity.speed)/gp.tileSize); // fără ieșire din matrice la limita hărții
                tileNum1= gp.tileM.mapTileNum[entityRightCol][entityTopRow];
                tileNum2= gp.tileM.mapTileNum[entityRightCol][entityBottomRow];
                if (gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision ||
                        gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) {
                    entity.collisionOn = true;
                }
                break;
        }

    }
    public int checkEntity(Entity entity, Entity[] target) {
        int index = 999; // <--- Pornim de la 999 (fără NPC găsit) în loc de 0!
        for (int i = 0; i < target.length; i++) {
            if (target[i] != null) {
                // Salvăm valorile relative originale ca să nu le pierdem
                int entityOldX = entity.hitBox.x;
                int entityOldY = entity.hitBox.y;
                int targetOldX = target[i].hitBox.x;
                int targetOldY = target[i].hitBox.y;

                // Hitbox-ul entității (Eron) transpus în coordonate globale pe hartă
                entity.hitBox.x = entity.worldX + entity.hitBox.x;
                entity.hitBox.y = entity.worldY + entity.hitBox.y;

                // Hitbox-ul țintei (NPC) transpus în coordonate globale pe hartă
                target[i].hitBox.x = target[i].worldX + target[i].hitBox.x;
                target[i].hitBox.y = target[i].worldY + target[i].hitBox.y;

                if (entity.hitBox.intersects(target[i].hitBox)) {
                    if (target[i] != entity) {
                        index = i; // Salvăm indexul NPC-ului lovit
                    }
                }

                // Resetăm coordonatele la valorile lor relative corecte din obiect
                entity.hitBox.x = entityOldX;
                entity.hitBox.y = entityOldY;
                target[i].hitBox.x = targetOldX;
                target[i].hitBox.y = targetOldY;
            }
        }
        return index;
    }
    public void checkPlayer(Entity entity) {
        // 1. Calculăm poziția hitbox-ului pentru COLIZIUNE (fizică)
        int entityHitBoxWorldX = entity.worldX + entity.hitBox.x;
        int entityHitBoxWorldY = entity.worldY + entity.hitBox.y;
        int playerHitBoxWorldX = gp.player.worldX + gp.player.hitBox.x;
        int playerHitBoxWorldY = gp.player.worldY + gp.player.hitBox.y;

        // 2. Creăm o zonă de detectare mai mare (Trigger Zone) pentru BĂTĂLIE
        int activationRange = gp.tileSize;

        // Calculăm distanța dintre centrul inamicului și centrul jucătorului
        int centerX = entity.worldX + (gp.tileSize / 2);
        int centerY = entity.worldY + (gp.tileSize / 2);
        int playerCenterX = gp.player.worldX + (gp.tileSize / 2);
        int playerCenterY = gp.player.worldY + (gp.tileSize / 2);

        double distance = Math.sqrt(Math.pow(centerX - playerCenterX, 2) + Math.pow(centerY - playerCenterY, 2));

        // --- LOGICĂ DECLANȘARE LUPTĂ (Modificată pentru State Pattern) ---
        if (distance <= activationRange && entity instanceof Enemy e) {
            if (e.alive && !(gp.getCurrentState() instanceof BattleState)) {
                gp.player.beginBattle(e); // lupta cu inamicul și grupul lui
            }
        }

        // --- LOGICĂ COLIZIUNE FIZICĂ ---
        switch(entity.direction) {
            case "up": entityHitBoxWorldY -= entity.speed; break;
            case "down": entityHitBoxWorldY += entity.speed; break;
            case "left": entityHitBoxWorldX -= entity.speed; break;
            case "right": entityHitBoxWorldX += entity.speed; break;
        }

        Rectangle predictedHitBox = new Rectangle(entityHitBoxWorldX, entityHitBoxWorldY, entity.hitBox.width, entity.hitBox.height);
        Rectangle playerHitBox = new Rectangle(playerHitBoxWorldX, playerHitBoxWorldY, gp.player.hitBox.width, gp.player.hitBox.height);

        if(predictedHitBox.intersects(playerHitBox)) {
            entity.collisionOn = true;
        }
    }
}