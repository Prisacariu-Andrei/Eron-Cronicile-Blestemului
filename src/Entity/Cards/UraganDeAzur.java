package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class UraganDeAzur extends Card {
    public UraganDeAzur(Player player) {
        super("Uragan de Azur", 2, CardType.ELECTRIC, "Mânia furtunii sporește cu \nfiecare scânteie. \nPaguba "+(10+player.damage+player.voltCount*8)+"\nCreste cu 8 dupa fiecare\nfulger");
    }

    @Override
    public void play(Player player, Enemy enemy) {
        // Calculăm damage-ul: baza 10 + 8 per fiecare carte electrică jucată anterior
        int totalDamage = 10+ player.damage + (player.voltCount * 8);
        if (enemy != null) {
            enemy.takeDamage(totalDamage);
            ++player.voltCount; // Incrementăm contorul pentru următoarea carte
        }
    }
}