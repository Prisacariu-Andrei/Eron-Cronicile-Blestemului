package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class PovaraAnilor extends Card {
    public PovaraAnilor() {
        super("Povara Anilor", 1, CardType.NEUTRU, "Inamicul se gârbovește sub o \nsoartă potrivnică.");
    }

    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) {
            applyWeakness(enemy); // Inamicul dă damage redus timp de 2 ture
        }
    }
}