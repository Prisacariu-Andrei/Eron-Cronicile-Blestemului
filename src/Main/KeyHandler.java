package Main;

import Database.SaveLoadManager;
import Entity.Items.Card;
import GameState.*; // Importă toate stările noi
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    public boolean upPressed, downPressed, rightPressed, leftPressed,
            dashPressed, interactPressed,
            enterPressed, escPressed,
            tPressed, iPressed;
    GamePanel gp;

    @Override
    public void keyTyped(KeyEvent e) {
        GameState state = gp.getCurrentState();
        // Verificăm dacă state nu este null înainte de a-l folosi
        if (state instanceof InputNameState) {
            char c = e.getKeyChar();
            // Permitem doar caractere vizibile și Backspace
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                ((InputNameState)state).addChar(c);
            } else if (c == KeyEvent.VK_BACK_SPACE) {
                ((InputNameState)state).removeChar();
            }
        }
    }

    public KeyHandler(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void keyPressed(KeyEvent keyE) {
        int code = keyE.getKeyCode();
        GameState state = gp.getCurrentState();

        if (code == KeyEvent.VK_F3) {
            DebugStats.visible = !DebugStats.visible; // FPS / performanță
            return;
        }

        if (code == KeyEvent.VK_ESCAPE) {
            if (state instanceof PlayState) {
                System.out.println("DEBUG: Jocul a trecut în PAUZĂ.");
                SoundManager.getInstance().playSE("click"); // <--- ADĂUGAT: Pune efect de click!
                gp.changeState(new PauseState(gp));
                return;
            } else if (state instanceof PauseState) {
                System.out.println("DEBUG: Jocul a revenit din PAUZĂ.");
                SoundManager.getInstance().playSE("click"); // <--- ADĂUGAT: Pune efect de click!
                gp.changeState(new PlayState(gp));
                return;
            } else if (state instanceof LeaderboardState) {
                gp.changeState(new MenuState(gp));
                return;
            } else if (state instanceof CreditsState) {
                gp.changeState(new MenuState(gp));
                return;
            } else if (state instanceof CutsceneState) {
                ((CutsceneState) state).skip();
                return;
            } else if (state instanceof ShopState) {
                SoundManager.getInstance().playSE("click");
                gp.changeState(new PlayState(gp));
                return;
            } else if (state instanceof RewardState) { // <--- ADAUGĂ ACEST BLOC NOU!
                System.out.println("DEBUG: Jucătorul a refuzat recompensa.");
                gp.rewardUI.commandNum = 0;
                gp.leaveReward();
                return;
            }
        }

        // Dialog cu opțiuni de răspuns (NPC-uri din npcs.json): W/S aleg, E sau ENTER confirmă
        if (state instanceof DialogueState && gp.dialogueUI.hasChoices() && !gp.dialogueUI.isWriting()) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) gp.dialogueUI.moveChoice(-1);
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) gp.dialogueUI.moveChoice(1);
            if ((code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER) && gp.activeNpcIndex != -1 && gp.npc[gp.activeNpcIndex] != null) {
                gp.npc[gp.activeNpcIndex].choose(gp.dialogueUI.choice);
            }
            return;
        }

        // 0. GESTIUNE MENIU PRINCIPAL
        if (state instanceof MenuState) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                gp.ui.commandNum--;
                if (gp.ui.commandNum < 0) gp.ui.commandNum = 4; // Ajustat la 4 pentru că ai 5 opțiuni în UI!
                SoundManager.getInstance().playSE("click");
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                gp.ui.commandNum++;
                if (gp.ui.commandNum > 4) gp.ui.commandNum = 0;
                SoundManager.getInstance().playSE("click");
            }
            if (code == KeyEvent.VK_ENTER) {
                enterPressed = false;

                if (gp.ui.commandNum == 0) gp.changeState(new InputNameState(gp,1));
                else if (gp.ui.commandNum == 1) {gp. changeState(new PlayState(gp));}
                else if (gp.ui.commandNum == 2) {gp.changeState(new LeaderboardState(gp));}
                else if (gp.ui.commandNum == 4) {
                    // MODIFICAT: Închidem baza de date curat înainte de a închide procesul ★★★
                    Database.DatabaseManager.getInstance().close();
                    System.exit(0);
                }
            }
            return;
        }

        // 1. GESTIUNE DEATH SCREEN
        if (state instanceof DeathState) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                gp.deathUI.commandNum--;
                if (gp.deathUI.commandNum < 0) gp.deathUI.commandNum = 1;
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                gp.deathUI.commandNum++;
                if (gp.deathUI.commandNum > 1) gp.deathUI.commandNum = 0;
            }
            if (code == KeyEvent.VK_ENTER) {
                enterPressed = false;
                if (gp.deathUI.commandNum == 0) {
                    gp.player.resetPlayer();
                    gp.changeState(new MenuState(gp));
                } else if (gp.deathUI.commandNum == 1) {
                    // MODIFICAT: Închidem baza de date curat și aici ★★★
                    Database.DatabaseManager.getInstance().close();
                    System.exit(0);
                }
            }
            return;
        }

        // 2. GESTIUNE BATTLE
        if (state instanceof BattleState) {
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                if (gp.battleUI.commandNum > 0) { gp.battleUI.commandNum--; SoundManager.getInstance().playSE("click"); }
            }
            if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
                if (gp.battleUI.commandNum < gp.player.inventory.hand.size() - 1) { gp.battleUI.commandNum++; SoundManager.getInstance().playSE("click"); }
            }
            if (code == KeyEvent.VK_ENTER) {
                gp.player.playSelectedCard();
                return;
            }
            if (code == KeyEvent.VK_T) {
                gp.player.endTurn();
                return;
            }
            // Abilități (progression.json): tastele 1-4
            if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                gp.player.useAbility(code - KeyEvent.VK_1);
                return;
            }
        }

        // 3. GESTIUNE REWARD SCREEN
        if (state instanceof RewardState) {
            if (code == KeyEvent.VK_LEFT) {
                gp.rewardUI.commandNum--;
                if (gp.rewardUI.commandNum < 0) gp.rewardUI.commandNum = 2;
                SoundManager.getInstance().playSE("click");
            }
            if (code == KeyEvent.VK_RIGHT) {
                gp.rewardUI.commandNum++;
                if (gp.rewardUI.commandNum > 2) gp.rewardUI.commandNum = 0;
                SoundManager.getInstance().playSE("click");
            }
            if (code == KeyEvent.VK_ENTER) {
                Card selectedGarde = gp.rewardUI.rewardCards.get(gp.rewardUI.commandNum);
                gp.fx.rewardBurst(gp.rewardUI.selectedCenterX(), gp.rewardUI.selectedCenterY());
                gp.player.inventory.cards.add(selectedGarde);
                gp.leaveReward();
                gp.player.worldY -= 10;
            }
        }

        // 4. GESTIUNE INVENTAR
        if (state instanceof InventoryState) {
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
            // Runele: A/D aleg, ENTER echipează sau dezechipează (detaliile se văd în tooltip)
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)  { gp.inventoryUI.moveRuneCursor(-1); return; }
            if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) { gp.inventoryUI.moveRuneCursor(1); return; }
            if (code == KeyEvent.VK_ENTER) { gp.inventoryUI.toggleSelectedRune(); return; }
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP || code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                return;
            }
        }

        // 5. GESTIUNE DIALOG ȘI JURNAL DE QUEST-URI
        if (code == KeyEvent.VK_J) {
            System.out.println("DEBUG: Tasta J a fost apasată.");

            if (state instanceof PlayState || state instanceof DialogueState) {
                gp.questBookUI.isOpen = true;
                gp.changeState(new QuestGameState(gp));
                return;
            } else if (state instanceof QuestGameState) {
                gp.questBookUI.isOpen = false;
                gp.changeState(new PlayState(gp));
                return;
            }
        }

        // Comutare tab-uri când jurnalul este deschis
        if (state instanceof QuestGameState) {
            if (code == KeyEvent.VK_Q) {
                System.out.println("DEBUG: Tasta Q a fost apasată în meniul Quest.");
                gp.questBookUI.switchTab();
            }
            return;
        }

        // 6. INTERACȚIUNE ȘI GESTIUNE DIALOG
        if (code == KeyEvent.VK_E) {
            if (state instanceof PlayState) {
                // Jucătorul încearcă să pornească o discuție (detectează distanța/coliziunea în metodă)
                gp.player.interactNPC();
            } else if (state instanceof DialogueState) {

                if (gp.dialogueUI.isWriting()) {
                    gp.dialogueUI.skipTypewriter();
                } else {
                    // Verificăm ce NPC este cel cu care vorbim în loc să apelăm speak() pe toți simultan
                    // Presupunând că în gp.player salvezi indexul NPC-ului curent interacționat (ex: gp.player.npcIndex)
                    int npcIndex = gp.activeNpcIndex;

                    if (npcIndex != -1 && gp.npc[npcIndex] != null) {
                        gp.npc[npcIndex].speak();
                    } else {
                        // Fallback de siguranță: dacă nu știi exact indexul, închide dialogul
                        gp.changeState(new PlayState(gp));
                    }
                }
            }
        }

        // 7. DEPLASĂRI GENERALE PE HARTĂ
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
        if (code == KeyEvent.VK_T) {
            tPressed = true;
            System.out.println("DEBUG: Tasta T a fost apasata!");
        }

        // Deschidere inventar
        if (code == KeyEvent.VK_I) {
            iPressed = true;
            if (state instanceof PlayState) {
                System.out.println("DEBUG: Deschidem Inventarul.");
                gp.changeState(new InventoryState(gp));
                gp.inventoryUI.slotRow = 0;
            } else if (state instanceof InventoryState) {
                System.out.println("DEBUG: Închidem Inventarul.");
                gp.changeState(new PlayState(gp));
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
        if (code == KeyEvent.VK_I){
            iPressed = false;
        }
    }
}