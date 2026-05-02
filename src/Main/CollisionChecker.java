package Main;

import Entity.Entity;
import Entity.Inamici.Enemy;

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
                entityTopRow=(entityTopWorldY-entity.speed)/gp.tileSize;
                tileNum1= gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                tileNum2= gp.tileM.mapTileNum[entityRightCol][entityTopRow];
                if (gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision ||
                        gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) {
                    entity.collisionOn = true;
                }
                break;
            case "down":
                entityBottomRow=(entityBottomWorldY+entity.speed)/gp.tileSize;
                tileNum1= gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                tileNum2= gp.tileM.mapTileNum[entityRightCol][entityBottomRow];
                if (gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision ||
                        gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) {
                    entity.collisionOn = true;
                }
                break;
            case "left":
                entityLeftCol=(entityLeftWorldX-entity.speed)/gp.tileSize;
                tileNum1= gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                tileNum2= gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                if (gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision ||
                        gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) {
                    entity.collisionOn = true;
                }
                break;
            case "right":
                entityRightCol=(entityRightWorldX+entity.speed)/gp.tileSize;
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
        int index = 999;
        for (int i = 0; i < target.length; i++) {
            if (target[i] != null) {
                // Hitbox-ul entității (Eron)
                entity.hitBox.x = entity.worldX + entity.hitBox.x;
                entity.hitBox.y = entity.worldY + entity.hitBox.y;
                // Hitbox-ul țintei (NPC)
                target[i].hitBox.x = target[i].worldX + target[i].hitBox.x;
                target[i].hitBox.y = target[i].worldY + target[i].hitBox.y;

                if (entity.hitBox.intersects(target[i].hitBox)) {
                    if (target[i] != entity) index = i;
                }
                // Resetăm coordonatele hitbox-ului la valorile lor relative
                entity.hitBox.x = 8; // sau cât ai tu setat în Entity/Player
                entity.hitBox.y = 16;
                target[i].hitBox.x = 8;
                target[i].hitBox.y = 16;
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
        // Vrem să detectăm dacă jucătorul este la o distanță de un tileSize
        int activationRange = gp.tileSize;

        // Calculăm distanța dintre centrul inamicului și centrul jucătorului
        int centerX = entity.worldX + (gp.tileSize / 2);
        int centerY = entity.worldY + (gp.tileSize / 2);
        int playerCenterX = gp.player.worldX + (gp.tileSize / 2);
        int playerCenterY = gp.player.worldY + (gp.tileSize / 2);

        double distance = Math.sqrt(Math.pow(centerX - playerCenterX, 2) + Math.pow(centerY - playerCenterY, 2));

        // --- LOGICĂ DECLANȘARE LUPTĂ ---
        if (distance <= activationRange && entity instanceof Enemy e) {
            if (e.alive && gp.gameState != GamePanel.GameState.BATTLE) {
                gp.player.currentEnemy = e;
                gp.gameState = GamePanel.GameState.BATTLE;
                gp.player.startBattle(); // Inițializează cărțile, mana, etc.
            }
        }

        // --- LOGICĂ COLIZIUNE FIZICĂ (să nu se suprapună sprite-urile) ---
        // Verificăm direcția pentru a opri inamicul să intre în player
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
