package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class CadereaStelelor extends Card {

    private static final int BASE_DAMAGE = 90;

    public CadereaStelelor(Player player) {
        // Trimitem descrierea direct în constructorul clasei părinte (Card)
        super("Căderea Stelelor",
                4,
                CardType.FOC,
                "Focul din cer mistuie \noștile potrivnice.\n Paguba: " + (BASE_DAMAGE + player.damage));
    }

    @Override
    public void play(Player player, Enemy enemy) {
        if (enemy != null) {
            // Calculăm damage-ul final: Baza + Bonusul din Rune
            int totalDamage = BASE_DAMAGE + player.damage;
            if(player.areDinteChilimbar){
                totalDamage += 3;
            }
            enemy.takeDamage(player,totalDamage);
        }
    }

    // play() adaugă +3 cu Colțul, iar Enemy.takeDamage încă +3
    private static String describe(Player p) {
        return "Focul din cer mistuie \noștile potrivnice.\n Paguba: " + (BASE_DAMAGE + p.damage + (p.areDinteChilimbar ? 6 : 0));
    }

    @Override
    public String getDynamicDescription(Player player) { return describe(player); }
}
