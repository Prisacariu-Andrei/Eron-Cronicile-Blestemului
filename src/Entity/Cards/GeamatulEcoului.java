package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;

public class GeamatulEcoului extends Card {
    public GeamatulEcoului() {
        super("Geamătul Ecoului", 0, CardType.NEUTRU, "Puterea de adineauri \nrăsună iarăși.");
    }
    @Override
    public void play(Player player, Enemy enemy) {
        if (player.lastCardPlayed != null && !(player.lastCardPlayed instanceof GeamatulEcoului)) {
            player.lastCardPlayed.play(player, enemy);
        }
    }
}