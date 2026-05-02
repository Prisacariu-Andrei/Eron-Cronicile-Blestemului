package Observer;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class HealthBarUI extends Observer
{
	private BufferedImage heartFull,heart75,heart50,heart25,heart0;
	private String currentLifeDisplay = "100";

	public HealthBarUI() {
		try {
			// Încărcare imaginea inimii
			heartFull = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/FullHeart.png")));
			heart75 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart75.png")));
			heart50 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart50.png")));
			heart25 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart25.png")));
			heart0 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart0.png")));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void OnNotify(String eventData) {
		this.currentLifeDisplay = eventData;
	}

	public void draw(Graphics2D g2) {
		if (currentLifeDisplay == null) return;

		try {
			// 1. Spargem string-ul "100/100" în două bucăți
			String[] parts = currentLifeDisplay.split("/");
			int life = Integer.parseInt(parts[0].trim());
			int maxLife = (parts.length > 1) ? Integer.parseInt(parts[1].trim()) : 100;

			BufferedImage imageToDraw = null;

			// LOGICA DE PRAGURI (Calculăm procentajul pentru a fi siguri, indiferent de maxLife)
			double percent = (double) life / maxLife * 100;

			if (percent > 75) imageToDraw = heartFull;
			else if (percent > 50) imageToDraw = heart75;
			else if (percent > 25) imageToDraw = heart50;
			else if (percent > 0) imageToDraw = heart25;
			else imageToDraw = heart0;

			if (imageToDraw != null) {
				int heartX = 10;
				int heartY = 25;
				int heartSize = 32;

				// Desenezi inima
				g2.drawImage(imageToDraw, heartX, heartY, heartSize, heartSize, null);

				// TEXTUL
				if(life==0) {
					g2.setColor(Color.DARK_GRAY);
				}
				else{
					g2.setColor(Color.RED);
				}
				g2.setFont(new Font("Arial", Font.BOLD, 22));

				// Folosim direct string-ul primit pentru afișare (ex: "80 / 100")
				// Punem spații în jurul slash-ului pentru aspect vizual
				String healthText = life + " / " + maxLife;

				g2.drawString(healthText, heartX + 40, heartY + 24);
			}

		} catch (Exception e) {
			// Ignorăm erorile de parsare dacă string-ul nu e gata
		}
	}
}
