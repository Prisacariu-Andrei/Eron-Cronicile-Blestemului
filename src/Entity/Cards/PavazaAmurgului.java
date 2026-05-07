package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class PavazaAmurgului extends Card {
    public PavazaAmurgului(Player player) {
        super("Păvaza Amurgului", 1, CardType.NEUTRU, "Ridici un zid de spirit ce \noprește orice asalt. \nArmura "+(9+player.bonusShield));
    }
    @Override
    public void play(Player player, Enemy enemy) {
        player.addShield(9+ player.bonusShield);
    }
}