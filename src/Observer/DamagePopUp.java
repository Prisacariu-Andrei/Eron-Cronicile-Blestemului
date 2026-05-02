package Observer;

import java.awt.*;

public class DamagePopUp {
    public int x, y;
    public String text;
    public Color color;
    public int lifeSpan = 60; // Va dura 60 de frame-uri (1 secundă)
    public float alpha = 1.0f; // Pentru efect de fade-out

    public DamagePopUp(int x, int y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
    }

    public void update() {
        y -= 1; // Numărul "plutește" în sus
        lifeSpan--;
        if (lifeSpan < 20) {
            alpha -= 0.05f; // Începe să dispară spre final
            if (alpha < 0) alpha = 0;
        }
    }
}