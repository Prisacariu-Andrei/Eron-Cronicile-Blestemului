package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class FluxulStrabun extends Card {
    public FluxulStrabun(Player player) {
        super("Fluxul Străbun", 0, CardType.NEUTRU, "O scânteie de Esență \nrevine din adâncul timpului.");
    }
    @Override
    public void play(Player player, Enemy enemy) {
        player.addMana(1);
    }
}
