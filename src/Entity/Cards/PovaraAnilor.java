package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class PovaraAnilor extends Card {
    public PovaraAnilor() {
        // Am actualizat descrierea pentru a fi mai explicită
        super("Povara Anilor", 1, CardType.NEUTRU,
                "Inamicul se gârbovește sub o \nsoartă potrivnică.\n(Damage înjumătățit 2 ture)");
    }

    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) {
            // Aplicăm slăbiciunea pentru 2 ture
            // Această metodă va adăuga statusul WEAK în lista activeStatuses a inamicului
            applyWeakness(enemy, 2);
        }
    }
}