package Observer;

import Main.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CutsceneUI {
    GamePanel gp;

    // Paginile Prologului (din documentația jocului)
    private final String[][] pages = {
            {"PROLOGUL",
                    "Nemurirea s-a scurs... Lumea s-a schimbat; o simți în adierea rece a magiei ce nu mai aduce viață, " +
                    "ci înlănțuie sufletele într-o nemurire fără tihnă."},
            {"PROLOGUL",
                    "Într-un tărâm unde descântecul s-a împletit fatal cu blestemul, tu ești Eron, ultimul păstrător " +
                    "al flăcării de demult. Un războinic vrăjitor prin ale cărui vene curge puterea timpurilor uitate."},
            {"PROLOGUL",
                    "Scopul tău este unul singur: să pătrunzi în inima întunericului și să dezlegi blestemul " +
                    "Regelui Vrăjitor, cel care a furat Inelul Eternității pentru a rescrie realitatea după voia sa perfidă."},
            {"ELDORIA - ECOUL SPERANȚEI",
                    "Te trezești în satul Eldoria, un ultim refugiu unde timpul pare să fi încremenit sub o mantie de ceață. " +
                    "Bătrânul Cronicar te așteaptă. Vorbește cu el (E) pentru a-ți primi prima misiune."}
    };

    private int pageIndex = 0;
    private int charIndex = 0;   // Câte caractere din pagina curentă sunt afișate (efect typewriter)
    private int frameCounter = 0;
    private int fadeAlpha = 255; // Fade-in la începutul fiecărei pagini

    public CutsceneUI(GamePanel gp) {
        this.gp = gp;
    }

    public void reset() {
        pageIndex = 0;
        startPage();
    }

    private void startPage() {
        charIndex = 0;
        frameCounter = 0;
        fadeAlpha = 255;
    }

    public void update() {
        if (fadeAlpha > 0) fadeAlpha = Math.max(0, fadeAlpha - 8);

        // Un caracter nou la fiecare 2 cadre
        frameCounter++;
        if (isWriting() && frameCounter % 2 == 0) charIndex++;
    }

    public boolean isWriting() {
        return charIndex < pages[pageIndex][1].length();
    }

    public void skipTypewriter() {
        charIndex = pages[pageIndex][1].length();
    }

    /** Trece la pagina următoare. Returnează false dacă nu mai sunt pagini. */
    public boolean nextPage() {
        if (pageIndex >= pages.length - 1) return false;
        pageIndex++;
        startPage();
        return true;
    }

    public void draw(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // Titlul paginii
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 36F));
        g2.setColor(new Color(212, 175, 55));
        String title = pages[pageIndex][0];
        g2.drawString(title, getXforCenteredText(title, g2), gp.screenHeight / 3);

        // Textul paginii, afișat treptat și împărțit pe rânduri
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 26F));
        g2.setColor(Color.WHITE);
        String visible = pages[pageIndex][1].substring(0, charIndex);
        int y = gp.screenHeight / 3 + 80;
        for (String line : wrap(visible, g2, gp.screenWidth - 400)) {
            g2.drawString(line, 200, y);
            y += 40;
        }

        // Indicații
        g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 16F));
        g2.setColor(Color.GRAY);
        String hint = isWriting() ? "ENTER - afișează tot" : "ENTER - continuă";
        g2.drawString(hint, gp.screenWidth - 260, gp.screenHeight - 40);
        g2.drawString("ESC - sari peste prolog", 40, gp.screenHeight - 40);
        g2.drawString((pageIndex + 1) + " / " + pages.length, gp.screenWidth / 2 - 15, gp.screenHeight - 40);

        if (fadeAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, fadeAlpha));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
    }

    private List<String> wrap(String text, Graphics2D g2, int maxWidth) {
        List<String> lines = new ArrayList<>();
        FontMetrics fm = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }
}
