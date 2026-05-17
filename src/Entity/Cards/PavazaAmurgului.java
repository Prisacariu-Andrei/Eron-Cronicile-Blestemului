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
        int amount = 9 + player.bonusShield;
        if(player.areInelDeGheata){
            amount += 3;
        }
        player.addShield(amount);
    }
}