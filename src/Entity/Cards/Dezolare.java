package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class Dezolare extends Card {
    public Dezolare() {
        // Am actualizat descrierea pentru a fi mai explicită
        super("Dezolare", 1, Card.CardType.NEUTRU,
                "Inamicul este grav ranit. \n(Primeste bonus paguba)");
    }

    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) {
            // Aplicăm slăbiciunea pentru 2 ture
            // Această metodă va adăuga statusul WEAK în lista activeStatuses a inamicului
            applyVulnerability(enemy, 50);
        }
    }
}
