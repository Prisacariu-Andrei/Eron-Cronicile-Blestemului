package Observer;

import Main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;

public class FogOfWar {

    private final GamePanel gp;
    private final int circleSize; // Diametrul cercului de lumină

    public FogOfWar(GamePanel gp, int circleSize) {
        this.gp = gp;
        this.circleSize = circleSize;
    }

    public void draw(Graphics2D g2, int colour) {
        // 1. Creăm imaginea tampon (ARGB)
        BufferedImage fogImage = new BufferedImage(gp.screenWidth, gp.screenHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2Fog = (Graphics2D) fogImage.getGraphics();

        g2Fog.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculăm centrul exact al lui Eron pe ecran
        int centerX = gp.player.screenX + (gp.tileSize / 2);
        int centerY = gp.player.screenY + (gp.tileSize / 2);

        float radius = circleSize / 2f;

        // 2. CONFIGURARE GRADIENT INVERS (Lumină -> Întuneric)
        float[] fractions = {0.0f, 0.2f, 0.6f, 1.0f};

        Color[] colors = {
                new Color(5+colour, 5+colour, 15+colour, 0),    // Centru: Complet transparent (se vede Eron perfect)
                new Color(5+colour, 5+colour, 15+colour, 30),   // Pasul 2: Început de penumbră
                new Color(5+colour, 5+colour, 15+colour, 150),  // Pasul 3: Ceață deasă
                new Color(5+colour, 5+colour, 15+colour, 245)   // Margine (100%): Întunericul maxim al Abisului
        };

        // ★ SOLUȚIA: Adăugăm parametrii de focalizare și metoda NO_CYCLE
        // NO_CYCLE îi spune Java-ului: "Tot ce depășește raza acestui cerc, umple la infinit cu ultima culoare din array (adică opacitate 245)"
        RadialGradientPaint rgp = new RadialGradientPaint(
                new Point(centerX, centerY),
                radius,
                fractions,
                colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE
        );

        // Aplicăm gradientul direct
        g2Fog.setComposite(AlphaComposite.SrcOver);
        g2Fog.setPaint(rgp);

        // 3. UMPLEM EFECTIV TOT ECRANUL CU ACEST STIL ★★★
        // Nu mai desenăm doar un cerc mic, ci umplem tot dreptunghiul ferestrei.
        // Gradientul va avea grijă să lase centrul transparent și să pună negru peste tot în rest, fără nicio linie vizibilă!
        g2Fog.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        g2Fog.dispose();

        // 4. Desenăm imaginea finală peste joc
        g2.drawImage(fogImage, 0, 0, null);
    }
}