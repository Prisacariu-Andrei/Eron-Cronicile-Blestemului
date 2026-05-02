package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class CadereaStelelor extends Card {
    public CadereaStelelor() {
        super("Căderea Stelelor", 3, CardType.FOC, "Focul din cer mistuie \noștile potrivnice.");
    }
    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) enemy.takeDamage(50);
        // Dacă vei avea mai mulți inamici, aici vei face un loop prin toți
    }
}