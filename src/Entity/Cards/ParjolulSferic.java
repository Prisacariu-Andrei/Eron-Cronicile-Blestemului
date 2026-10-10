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
            enemy.takeDamage(player,baseDamage+player.cardDamage(this));
            enemy.applyStatus(StatusEffect.Type.BURN, burnTurns + player.statusTurns(StatusEffect.Type.BURN), burnDamage + player.statusPower(StatusEffect.Type.BURN));
        }
    }


    private String describe(Player p) {
        return "Aruncă o bilă de foc \nputernică.\n Paguba " + (baseDamage + p.cardDamage(this) + (p.areDinteChilimbar ? 3 : 0))
                + "\n Arsura " + (burnDamage + p.statusPower(StatusEffect.Type.BURN)) + " (" + (burnTurns + p.statusTurns(StatusEffect.Type.BURN)) + " ture)";
    }

    /** Descrierea ține cont de bonusurile din acest moment (runele echipate, relicve). */
    @Override
    public String getDynamicDescription(Player player) { return describe(player); }
}
