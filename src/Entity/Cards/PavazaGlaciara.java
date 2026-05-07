package Entity.Cards; // Numele pachetului unde ai pus fișierul

import Entity.Items.Card; // Importăm clasa abstractă Card
import Entity.Player.Player;
import Entity.Inamici.Enemy;

public class PavazaGlaciara extends Card {

    public PavazaGlaciara(Player player) {
        super("Păvaza Glaciară", 1,CardType.INGHET, "Ridică un zid de gheată\nce te protejeaza de atac.\nArmura "+(7+player.bonusShield));

    }
    @Override
    public void play(Player player, Enemy enemy) {
        player.addShield(7+ player.bonusShield);
    }
}