package Main;

import Entity.Items.Card;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    public boolean upPressed, downPressed, rightPressed, leftPressed,
            dashPressed, interactPressed,
            enterPressed, escPressed,
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

        // 1. GESTIUNE DEATH SCREEN
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
                    gp.player.resetPlayer();
                    gp.gameState = GamePanel.GameState.MENU;
                } else if (gp.deathUI.commandNum == 1) {
                    System.exit(0);
                }
            }
            return;
        }

        // 2. GESTIUNE BATTLE
        if (gp.gameState == GamePanel.GameState.BATTLE) {
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                if (gp.battleUI.commandNum > 0) gp.battleUI.commandNum--;
            }
            if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
                if (gp.battleUI.commandNum < gp.player.inventory.hand.size() - 1) gp.battleUI.commandNum++;
            }
            if (code == KeyEvent.VK_ENTER) {
                gp.player.playSelectedCard();
                return;
            }
            if (code == KeyEvent.VK_T) {
                gp.player.endTurn();
                return;
            }
        }

        // 3. GESTIUNE REWARD SCREEN
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
                Card selectedGarde = gp.rewardUI.rewardCards.get(gp.rewardUI.commandNum);
                gp.player.inventory.cards.add(selectedGarde);
                gp.gameState = GamePanel.GameState.PLAY;
                gp.player.worldY -= 10;
            }
        }

        // 4. GESTIUNE INVENTAR
        if (gp.gameState == GamePanel.GameState.INVENTORY) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                if (gp.inventoryUI.slotRow > 0) {
                    gp.inventoryUI.slotRow--;
                }
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                int maxVisible = 15;
                if (gp.inventoryUI.slotRow < gp.player.inventory.cards.size() - maxVisible) {
                    gp.inventoryUI.slotRow++;
                }
            }
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP || code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                return;
            }
        }

        // 5. GESTIUNE DIALOG ȘI JURNAL DE QUEST-URI (Tastele J și Q sunt active în timpul dialogului și în joc)
        if (code == KeyEvent.VK_J) {
            System.out.println("DEBUG: Tasta J a fost apasată.");

            if (gp.gameState == GamePanel.GameState.PLAY || gp.gameState == GamePanel.GameState.DIALOGUE) {
                gp.questBookUI.isOpen = true;
                gp.gameState = GamePanel.GameState.QUEST;
                return; // Oprește execuția pentru a nu intra în alte stări
            } else if (gp.gameState == GamePanel.GameState.QUEST) {
                gp.questBookUI.isOpen = false;
                gp.gameState = GamePanel.GameState.PLAY;
                return;
            }
        }

        // Comutare tab-uri când jurnalul este deschis
        if (gp.gameState == GamePanel.GameState.QUEST) {
            if (code == KeyEvent.VK_Q) {
                System.out.println("DEBUG: Tasta Q a fost apasată în meniul Quest.");
                gp.questBookUI.switchTab();
            }
            return; // Împiedică jucătorul să se miște în fundal când citește quest-urile
        }

        // 6. INTERACȚIUNE ȘI GESTIUNE DIALOG
        if (code == KeyEvent.VK_E) {
            if (gp.gameState == GamePanel.GameState.PLAY) {
                gp.player.interactNPC();
            } else if (gp.gameState == GamePanel.GameState.DIALOGUE) {

                if (gp.dialogueUI.isWriting()) {
                    gp.dialogueUI.skipTypewriter();
                } else {
                    // Dacă mai sunt NPC-uri care vorbesc, le lăsăm să își finalizeze logica
                    for (int i = 0; i < gp.npc.length; i++) {
                        if (gp.npc[i] != null) {
                            // Când se apasă tasta în timp ce suntem în dialog, avansăm sau ieșim
                            gp.npc[i].speak();

                            // Ieșim manual din starea de dialog dacă s-a terminat
                            if (gp.npc[i].dialogueIndex == 0) {
                                gp.gameState = GamePanel.GameState.PLAY;
                                return;
                            }
                        }
                    }
                }
            }
        }

        // 6. DEPLASĂRI GENERALE PE HARTĂ
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            upPressed = true;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = true;
        }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (code == KeyEvent.VK_SPACE) {
            dashPressed = true;
        }
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = true;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escPressed = true;
        }
        if (code == KeyEvent.VK_T) {
            tPressed = true;
            System.out.println("DEBUG: Tasta T a fost apasata!");
        }

        // Deschidere inventar
        if (code == KeyEvent.VK_I) {
            if (gp.gameState == GamePanel.GameState.PLAY) {
                gp.gameState = GamePanel.GameState.INVENTORY;
                gp.inventoryUI.slotRow = 0;
            } else if (gp.gameState == GamePanel.GameState.INVENTORY) {
                gp.gameState = GamePanel.GameState.PLAY;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent keyE) {
        int code = keyE.getKeyCode();

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            upPressed = false;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = false;
        }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (code == KeyEvent.VK_SPACE) {
            dashPressed = false;
        }
        if (code == KeyEvent.VK_E) {
            interactPressed = false;
        }
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = false;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escPressed = false;
        }
        if (code == KeyEvent.VK_T) {
            tPressed = false;
        }
    }
}