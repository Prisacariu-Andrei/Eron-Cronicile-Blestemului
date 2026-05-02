package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class ManiorulIernii extends Card {
    public ManiorulIernii() {
        super("Mâniorul Iernii", 2, CardType.INGHET, "Inamicul rămâne încremenit \nsub o tăcere de gheață.");
    }
    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) applyStun(enemy);
    }
}
