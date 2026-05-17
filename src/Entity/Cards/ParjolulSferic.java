package Entity.Cards; // Numele pachetului unde ai pus fișierul

import Entity.Items.Card; // Importăm clasa abstractă Card
import Entity.Player.Player;
import Entity.Inamici.Enemy;
import Entity.StatusEffect;

public class ParjolulSferic extends Card {
    public static final int baseDamage= 10;
    public static final int burnTurns= 3;
    public static final int burnDamage= 5;
    public ParjolulSferic(Player player) {
        super("Pârjolul Sferic",
                1,
                CardType.FOC,
                "Aruncă o bilă de foc \nputernică.\n Paguba "+(baseDamage+player.damage)+"\n Arsura "+burnTurns);
    }

    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) {
            enemy.takeDamage(player,baseDamage+player.damage);
            enemy.applyStatus(StatusEffect.Type.BURN, burnTurns, burnDamage);
        }
    }

}