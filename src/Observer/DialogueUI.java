package Observer;

import Main.Easing;
import Main.GameFeel;
import Main.GamePanel;
import Main.SoundManager;
import java.awt.*;

public class DialogueUI {
    GamePanel gp;
    public String currentDialogue = "";
    private int charIndex = 0;
    private int counter = 0;
    private int openTicks = 0; // cât timp a trecut de când s-a deschis dialogul (pentru animația de intrare)

    private static final int OPEN_TICKS = 12;

    public DialogueUI(GamePanel gp) {
        this.gp = gp;
    }

    /** Apelată când începe un dialog nou: fereastra intră lin. */
    public void onOpen() {
        openTicks = 0;
    }

    /**
     * Un tick: apar literele una câte una. Înainte asta se făcea în draw(), deci viteza textului depindea de FPS.
     */
    public void update() {
        if (openTicks < OPEN_TICKS) openTicks++;

        if (charIndex < currentDialogue.length()) {
            counter++;
            if (counter >= 2) { // Viteza: cu cât e numărul mai mare, cu atât e mai lent (2-3 e ideal)
                char c = currentDialogue.charAt(charIndex);
                charIndex++;
                counter = 0;
                if (GameFeel.SOUND_FEEDBACK && !Character.isWhitespace(c) && charIndex % GameFeel.TYPEWRITER_BLIP_EVERY_CHARS == 0) {
                    SoundManager.getInstance().playSE("blip");
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        // SETĂRI FEREASTRĂ
        int x = gp.tileSize * 3;
        int y = gp.tileSize ;
        int width = gp.screenWidth - (gp.tileSize * 6);
        int height = gp.tileSize * 4;

        // Intrare: fereastra coboară puțin și se face vizibilă (easing), nu apare brusc
        float open = Easing.outCubic(openTicks / (float) OPEN_TICKS);
        int slide = Math.round((1f - open) * -30);
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, open)));
        y += slide;

        drawSubWindow(x, y, width, height, g2);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 24F));
        g2.setColor(Color.WHITE);

        int Y=y;
        String textToDisplay = currentDialogue.substring(0, Math.min(charIndex, currentDialogue.length()));
        for (String line : textToDisplay.split("\n")) {
            g2.drawString(line, x +gp.tileSize/2, Y+gp.tileSize);
            Y += 40;
        }

        // "Apasă [E]" pulsează ușor când tot textul e afișat, ca să se vadă că jocul așteaptă
        g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 16F));
        if (!isWriting()) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(gp.fx.ticks * 0.12);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, open * pulse)));
        }
        g2.drawString("Apasă [E] pentru a continua...", x + width - gp.tileSize * 4, y + height - gp.tileSize/2);
        g2.setComposite(oldComposite);
    }

    private void drawSubWindow(int x, int y, int width, int height, Graphics2D g2) {
        Color c = new Color(0, 0, 0, 210); // Negru transparent
        g2.setColor(c);
        g2.fillRoundRect(x, y, width, height, 35, 35);

        c = new Color(255, 255, 255); // Contur alb
        g2.setColor(c);
        g2.setStroke(new BasicStroke(5));
        g2.drawRoundRect(x + 5, y + 5, width - 10, height - 10, 25, 25);
    }

    public void resetCharIndex() {
        this.charIndex = 0;   // Resetăm poziția la prima literă
        this.counter = 0;     // Resetăm și cronometrul de viteză
    }

    // În DialogueUI
    public void skipTypewriter() {
        charIndex = currentDialogue.length();
    }

    public boolean isWriting() {
        return charIndex < currentDialogue.length();
    }
}
