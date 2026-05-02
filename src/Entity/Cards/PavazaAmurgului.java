package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class PavazaAmurgului extends Card {
    public PavazaAmurgului() {
        super("Păvaza Amurgului", 1, CardType.NEUTRU, "Ridici un zid de spirit ce \noprește orice asalt.");
    }
    @Override
    public void play(Player player, Enemy enemy) {
        player.addShield(20);
    }
}