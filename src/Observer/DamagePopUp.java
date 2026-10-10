package Observer;

import Main.Easing;
import Main.GameFeel;

import java.awt.*;

public class DamagePopUp {
    public int x, y;
    public String text;
    public Color color;
    public int lifeSpan = 60; // Va dura 60 de ticks (1 secundă)
    public float alpha = 1.0f; // Pentru efect de fade-out
    public float scale = 0.35f; // Apare mic și face un "pop" până la mărimea normală
    public float size = 1f;     // multiplicator față de fontul de bază: numerele de damage / vindecare / BLOCAT sunt mai mari decât textele obișnuite

    private final int startY;
    private int age = 0;

    public DamagePopUp(int x, int y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.startY = y;
        this.text = text;
        this.color = color;
        if (text.matches("-\\d+( HP)?")) size = 2.6f;          // damage
        else if (text.matches("\\+\\d+( HP)?")) size = 2.2f;   // vindecare
        else if (text.equals("BLOCAT")) size = 2.1f;            // armura a absorbit tot
    }

    public void update() {
        age++;
        // Numărul "plutește" în sus, repede la început și tot mai încet (easing), nu cu viteză constantă
        y = startY - Math.round(46 * Easing.outCubic(age / 60f));
        scale = Easing.lerp(0.35f, 1f, Easing.outBack(age / (float) GameFeel.POPUP_POP_TICKS));
        lifeSpan--;
        if (lifeSpan < 20) {
            alpha -= 0.05f; // Începe să dispară spre final
            if (alpha < 0) alpha = 0;
        }
    }
}
