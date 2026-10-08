package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class PavazaAmurgului extends Card {
    public PavazaAmurgului(Player player) {
        super("Păvaza Amurgului", 2, CardType.NEUTRU, "Ridici un zid de spirit ce \noprește orice asalt. \nArmura "+(16+player.bonusShield));
    }
    @Override
    public void play(Player player, Enemy enemy) {
        int amount = 16 + player.bonusShield;
        if(player.areInelDeGheata){
            amount += 3;
        }
        player.addShield(amount);
    }
}