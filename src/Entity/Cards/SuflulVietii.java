package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class SuflulVietii extends Card {
    public SuflulVietii() {
        super("Suflul Vieții", 1, CardType.NEUTRU, "Rănile se închid sub mângâierea \nunei puteri binefăcătoare.");
    }
    @Override
    public void play(Player player, Enemy enemy) {
        player.life = Math.min(player.life + 20, player.maxLife);
    }
}