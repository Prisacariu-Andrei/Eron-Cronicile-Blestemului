package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class CadereaStelelor extends Card {

    private static final int BASE_DAMAGE = 30;

    public CadereaStelelor(Player player) {
        // Trimitem descrierea direct în constructorul clasei părinte (Card)
        super("Căderea Stelelor",
                3,
                CardType.FOC,
                "Focul din cer mistuie \noștile potrivnice.\n Paguba: " + (BASE_DAMAGE + player.damage));
    }

    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) {
            // Calculăm damage-ul final: Baza + Bonusul din Rune
            int totalDamage = BASE_DAMAGE + player.damage;
            enemy.takeDamage(totalDamage);
        }
    }
}