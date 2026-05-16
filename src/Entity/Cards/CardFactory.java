package Entity.Cards;

import Entity.Items.Card;
import Entity.Player.Player;
// Importă toate clasele de cărți aici

public class CardFactory {

    public static Card createCard(String cardName, Player player) {
        return switch (cardName) {
            case "CadereaStelelor" -> new CadereaStelelor(player);
            case "DeslusireaRunelor" -> new DeslusireaRunelor(player);
            case "FluxulStrabun" -> new FluxulStrabun(player);
            case "FulgerDeSange" -> new FulgerDeSange(player);
            case "ParjolulSferic" -> new ParjolulSferic(player);
            case "PavazaAmurgului" -> new PavazaAmurgului(player);
            case "PavazaGlaciara" -> new PavazaGlaciara(player);
            case "UraganDeAzur" -> new UraganDeAzur(player);
            // Cărțile care nu au nevoie de player în constructor:
            case "GeamatulEcoului" -> new GeamatulEcoului();
            case "ManiorulIernii" -> new ManiorulIernii();
            case "PovaraAnilor" -> new PovaraAnilor();
            case "SuflulVietii" -> new SuflulVietii();
            case "Dezolare" -> new Dezolare();

            default -> throw new IllegalArgumentException("Carte necunoscută: " + cardName);
        };
    }
}