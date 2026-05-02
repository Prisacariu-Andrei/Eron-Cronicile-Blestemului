package Entity.Cards; // Numele pachetului unde ai pus fișierul

import Entity.Items.Card; // Importăm clasa abstractă Card
import Entity.Player.Player;
import Entity.Inamici.Enemy;
import Entity.StatusEffect;

public class ParjolulSferic extends Card {

    public ParjolulSferic() {
        super("Pârjolul Sferic", 1, CardType.FOC,"Aruncă o bilă de foc \nputernică.");
    }

    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) {
            enemy.takeDamage(1500);
            enemy.applyStatus(StatusEffect.Type.BURN, 3, 5);
        }
    }

}