package Entity.Cards; // Numele pachetului unde ai pus fișierul

import Entity.Items.Card; // Importăm clasa abstractă Card
import Entity.Player.Player;
import Entity.Inamici.Enemy;

public class PavazaGlaciara extends Card {

    public PavazaGlaciara(Player player) {
        super("Păvaza Glaciară", 1,CardType.INGHET, "Ridică un zid de gheată\nce te protejeaza de atac.\nArmura "+(CardCatalog.codeShield("PavazaGlaciara", 6)+player.bonusShield));

    }
    @Override
    public void play(Player player, Enemy enemy) {
        int amount = CardCatalog.codeShield("PavazaGlaciara", 6) + player.bonusShield + player.runeSum("cardShield");
        if(player.areInelDeGheata){
            amount += 3;
        }
        player.addShield(amount);
    }

    // play() adaugă +3 cu Inelul, iar Player.addShield încă +3
    private static String describe(Player p) {
        return "Ridică un zid de gheată\nce te protejeaza de atac.\nArmura " + (CardCatalog.codeShield("PavazaGlaciara", 6) + p.bonusShield + p.runeSum("cardShield") + (p.areInelDeGheata ? 6 : 0));
    }

    @Override
    public String getDynamicDescription(Player player) { return describe(player); }
}
