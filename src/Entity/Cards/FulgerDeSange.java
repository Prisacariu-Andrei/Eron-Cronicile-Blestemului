package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;


public class FulgerDeSange extends Card {
    public FulgerDeSange() {
        super("Fulger de Sânge", 2, CardType.ELECTRIC, "O descărcare aprigă ce \nfrânge carnea.");
    }
    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) enemy.takeDamage(30);
    }
}
