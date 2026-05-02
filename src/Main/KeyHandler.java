package Main;

import Entity.Items.Card;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    public boolean upPressed, downPressed, rightPressed, leftPressed,
            dashPressed, interactPressed,
            enterPressed,escPressed,
            tPressed;
    GamePanel gp;
    @Override
    public void keyTyped(KeyEvent keyE) {
    }
    public KeyHandler(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void keyPressed(KeyEvent keyE) {
        int code = keyE.getKeyCode();
        // În KeyHandler.java
        if (gp.gameState == GamePanel.GameState.DEATH) {

            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                gp.deathUI.commandNum--;
                if (gp.deathUI.commandNum < 0) gp.deathUI.commandNum = 1;
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                gp.deathUI.commandNum++;
                if (gp.deathUI.commandNum > 1) gp.deathUI.commandNum = 0;
            }

            if (code == KeyEvent.VK_ENTER) {
                if (gp.deathUI.commandNum == 0) { // Main Menu selectat
                    gp.player.resetPlayer();      // Resetează viața și inventarul
                    gp.gameState = GamePanel.GameState.MENU; // Schimbă starea
                    System.out.println("DEBUG: Trecem la MENU");
                } else if (gp.deathUI.commandNum == 1) { // Quit selectat
                    System.exit(0);
                }
            }
            return; // IMPORTANT: Oprim execuția aici ca să nu citească alte taste
        }
        // 1. GESTIUNE BATTLE (Taste specifice)
        if (gp.gameState == GamePanel.GameState.BATTLE) {
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                if (gp.battleUI.commandNum > 0) gp.battleUI.commandNum--;
            }
            if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
                if (gp.battleUI.commandNum < gp.player.inventory.hand.size() - 1) gp.battleUI.commandNum++;
            }
            if (code == KeyEvent.VK_ENTER) {
                gp.player.playSelectedCard(); // Apelăm direct metoda din Player
                return; // Oprim execuția aici pentru a nu declanșa alte meniuri
            }
            if (code == KeyEvent.VK_T){
                gp.player.endTurn();
                return;
            }
        }
        if (gp.gameState == GamePanel.GameState.REWARD) {
            if (code == KeyEvent.VK_LEFT) {
                gp.rewardUI.commandNum--;
                if (gp.rewardUI.commandNum < 0) gp.rewardUI.commandNum = 2;
            }
            if (code == KeyEvent.VK_RIGHT) {
                gp.rewardUI.commandNum++;
                if (gp.rewardUI.commandNum > 2) gp.rewardUI.commandNum = 0;
            }
            if (code == KeyEvent.VK_ENTER) {
                // Adăugăm cartea aleasă în pachetul permanent al jucătorului
                Card selectedGarde = gp.rewardUI.rewardCards.get(gp.rewardUI.commandNum);
                gp.player.inventory.cards.add(selectedGarde);

                // Revenim la explorarea pe hartă
                gp.gameState = GamePanel.GameState.PLAY;

                // Efect de respingere (să nu mai stăm pe inamic)
                gp.player.worldY -= 10;
            }
            // În KeyHandler.java, metoda keyPressed

        }
        // GESTIUNE INVENTAR (SCROLLING)
        if (gp.gameState == GamePanel.GameState.INVENTORY) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                if (gp.inventoryUI.slotRow > 0) {
                    gp.inventoryUI.slotRow--;
                }
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                // Verificăm să nu depășim numărul total de cărți
                // Presupunem că MAX_VISIBLE_ROWS în InventoryUI este 15
                int maxVisible = 15;
                if (gp.inventoryUI.slotRow < gp.player.inventory.cards.size() - maxVisible) {
                    gp.inventoryUI.slotRow++;
                }
            }
            // Ieșim din metodă pentru aceste taste ca să nu se miște și personajul în spate
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP || code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                return;
            }
        }

        if(code == KeyEvent.VK_W || code == KeyEvent.VK_UP){
            upPressed = true;
        }
        if(code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN){
            downPressed = true;
        }
        if(code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT){
            leftPressed = true;
        }
        if(code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT){
            rightPressed = true;
        }
        if(code == KeyEvent.VK_SPACE){
            dashPressed = true;
        }
        if(code == KeyEvent.VK_E){
            interactPressed = true;
        }
        if(code == KeyEvent.VK_ENTER){
            enterPressed = true;
        }
        if(code == KeyEvent.VK_ESCAPE)
            escPressed = true;
        if(code == KeyEvent.VK_T){
            tPressed = true;
            System.out.println("DEBUG: Tasta T a fost apasata!"); // Verifică dacă apare în consolă
        }
        if (code == KeyEvent.VK_I) {
            if (gp.gameState == GamePanel.GameState.PLAY) {
                gp.gameState = GamePanel.GameState.INVENTORY;
                gp.inventoryUI.slotRow = 0; // Resetăm scroll-ul la fiecare deschidere
            } else if (gp.gameState == GamePanel.GameState.INVENTORY) {
                gp.gameState = GamePanel.GameState.PLAY;
            }
        }
        if (code == KeyEvent.VK_E) {
            if (gp.gameState == GamePanel.GameState.PLAY) {
                gp.player.interactNPC();
            }
            else if (gp.gameState == GamePanel.GameState.DIALOGUE) {

                // 1. Verificăm DACĂ textul se scrie în acest moment
                // Presupunem că dialogueUI este accesibil prin gp.ui.dialogueUI sau direct gp.dialogueUI
                if (gp.dialogueUI.isWriting()) {
                    // Dacă se scrie, sărim peste animație și afișăm tot textul
                    gp.dialogueUI.skipTypewriter();
                }
                else {
                    // Dacă textul este deja afișat complet, trecem la logica de căutare NPC pentru următoarea replică
                    int oldX = gp.player.hitBox.x;
                    int oldY = gp.player.hitBox.y;
                    int oldW = gp.player.hitBox.width;
                    int oldH = gp.player.hitBox.height;

                    gp.player.hitBox.x -= gp.tileSize;
                    gp.player.hitBox.y -= gp.tileSize;
                    gp.player.hitBox.width += gp.tileSize * 2;
                    gp.player.hitBox.height += gp.tileSize * 2;

                    int npcIndex = gp.cChecher.checkEntity(gp.player, gp.npc);

                    gp.player.hitBox.x = oldX;
                    gp.player.hitBox.y = oldY;
                    gp.player.hitBox.width = oldW;
                    gp.player.hitBox.height = oldH;

                    if (npcIndex != 999) {
                        gp.npc[npcIndex].speak();
                    }
                }
            }
        }


    }

    @Override
    public void keyReleased(KeyEvent keyE) {
        int code = keyE.getKeyCode();

        if(code == KeyEvent.VK_W || code == KeyEvent.VK_UP){
            upPressed = false;
        }
        if(code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN){
            downPressed = false;
        }
        if(code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT){
            leftPressed = false;
        }
        if(code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT){
            rightPressed = false;
        }
        if(code == KeyEvent.VK_SPACE){
            dashPressed = false;
        }
        if(code == KeyEvent.VK_E){
            interactPressed = false;
        }
        if(code == KeyEvent.VK_ENTER){
            enterPressed = false;
        }

        if(code == KeyEvent.VK_ESCAPE)
            escPressed = false;

        if(code == KeyEvent.VK_T){
            tPressed = false;
        }
    }
}