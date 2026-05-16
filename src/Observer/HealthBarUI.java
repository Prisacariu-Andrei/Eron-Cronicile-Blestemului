package Observer;

import Main.Exception.GameExceptionHandler;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class HealthBarUI extends Observer {
	private BufferedImage heartFull, heart75, heart50, heart25, heart0;
	private String currentLifeDisplay = "100";

	public HealthBarUI() {
		try {
			// Încărcare imagini inimi cu flux securizat
			heartFull = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/FullHeart.png")));
			heart75  = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart75.png")));
			heart50  = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart50.png")));
			heart25  = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart25.png")));
			heart0   = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Heart0.png")));

		} catch (Exception e) {
			GameExceptionHandler.getInstance().handleWarning("Nu s-au putut încărca resursele grafice pentru bara de viață (Inimi)", e);
		}
	}

	@Override
	public void OnNotify(String eventData) {
		this.currentLifeDisplay = eventData;
	}

	public void draw(Graphics2D g2) {
		if (currentLifeDisplay == null) return;

		try {
			String[] parts = currentLifeDisplay.split("/");
			int life = Integer.parseInt(parts[0].trim());
			int maxLife = (parts.length > 1) ? Integer.parseInt(parts[1].trim()) : 100;

			BufferedImage imageToDraw;

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

				g2.drawImage(imageToDraw, heartX, heartY, heartSize, heartSize, null);

				if (life == 0) {
					g2.setColor(Color.DARK_GRAY);
				} else {
					g2.setColor(Color.RED);
				}
				g2.setFont(new Font("Arial", Font.BOLD, 22));

				String healthText = life + " / " + maxLife;
				g2.drawString(healthText, heartX + 40, heartY + 24);
			}

		} catch (Exception e) {
			// Warning silențios în log în caz că eventData vine incomplet sau deformat în primele frame-uri
			GameExceptionHandler.getInstance().handleWarning("Eroare la parsarea datelor text de viață în UI", e);
		}
	}
}