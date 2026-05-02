package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class DeslusireaRunelor extends Card {
    public DeslusireaRunelor(){
        super("Deslușirea Runelor", 1, Card.CardType.NEUTRU, "Două taine noi se arată.");
    }

    @Override
    public void play(Player player, Enemy enemy) {
        player.inventory.drawCard();
        player.inventory.drawCard();
    }
}

