package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;


public class FulgerDeSange extends Card {
    public static final int baseDamage= 15;

    public FulgerDeSange(Player player) {
        super("Fulger de Sânge",
                2,
                CardType.ELECTRIC,
                "O descărcare aprigă ce \nfrânge carnea.\nPaguba "+(baseDamage+player.damage));
    }
    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) enemy.takeDamage(player,baseDamage+player.damage);
        ++player.voltCount;
    }
}
