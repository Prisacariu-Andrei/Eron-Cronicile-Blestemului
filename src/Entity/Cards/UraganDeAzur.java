package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class UraganDeAzur extends Card {
    public UraganDeAzur() {
        super("Uragan de Azur", 3, CardType.ELECTRIC, "Mânia furtunii sporește cu \nfiecare scânteie.");
    }

    @Override
    public void play(Player player, Enemy enemy) {
        // Calculăm damage-ul: baza 10 + 8 per fiecare carte electrică jucată anterior
        int totalDamage = 10 + (player.voltCount * 8);
        if (enemy != null) {
            enemy.takeDamage(totalDamage);
            player.voltCount++; // Incrementăm contorul pentru următoarea carte
        }
    }
}