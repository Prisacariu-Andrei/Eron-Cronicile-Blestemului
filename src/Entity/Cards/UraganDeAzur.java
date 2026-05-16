package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class UraganDeAzur extends Card {
    private final int baseDamage = 600;
    private final int scalePerVolt = 4;

    public UraganDeAzur(Player player) {
        // Punem o descriere generică în constructor, oricum va fi suprascrisă vizual
        super("Uragan de Azur", 0, CardType.ELECTRIC, "Mânia furtunii sporește \ncu fiecare scânteie.\nPaguba: 10\nScântei(4 dmg per card electric)");
    }

    @Override
    public String getDynamicDescription(Player player) {
        int currentDmg = baseDamage + player.damage + (player.voltCount * scalePerVolt);
        return "Mânia furtunii sporește \ncu fiecare scânteie. \nPaguba: " + currentDmg +
                "\n(: " + player.voltCount + ")";
    }

    @Override
    public void play(Player player, Enemy enemy) {
        int totalDamage = baseDamage + player.damage + (player.voltCount * scalePerVolt);
        if (enemy != null) {
            enemy.takeDamage(totalDamage);
            // Incrementăm contorul global al jucătorului
            player.voltCount++;
        }
    }
}