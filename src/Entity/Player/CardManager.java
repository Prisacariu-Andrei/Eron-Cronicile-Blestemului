package Entity.Player;

import Entity.Items.Card;
import Entity.Cards.*; // Importăm tot pachetul unde stau clasele din poză
import java.util.ArrayList;

public class CardManager {
    /**
     * Creează o instanță nouă a unei cărți bazată pe un ID/Nume.
     * Folositor pentru RewardUI și Inventory.
     */
    public static Card createCard(String cardName, Player player) {
        return switch (cardName) {
            case "CadereaStelelor" -> new CadereaStelelor(player );
            case "DeslusireaRunelor" -> new DeslusireaRunelor( player);
            case "FluxulStrabun" -> new FluxulStrabun( player);
            case "FulgerDeSange" -> new FulgerDeSange( player);
            case "GeamatulEcoului" -> new GeamatulEcoului( );
            case "ManiorulIernii" -> new ManiorulIernii( );
            case "ParjolulSferic" -> new ParjolulSferic( player);
            case "PavazaAmurgului" -> new PavazaAmurgului( player);
            case "PavazaGlaciara" -> new PavazaGlaciara( player);
            case "PovaraAnilor" -> new PovaraAnilor( );
            case "SuflulVietii" -> new SuflulVietii( );
            case "UraganDeAzur" -> new UraganDeAzur( player);
            default -> null;
        };
    }

    /**
     * Pachetul de start al lui Eron folosind noile clase.
     */
    public static ArrayList<Card> getStartingDeck(Player player) {
        ArrayList<Card> starterDeck = new ArrayList<>();

        // Adăugăm câteva cărți de bază din lista ta pentru început
        starterDeck.add(createCard("ParjolulSferic",player));
        starterDeck.add(createCard("ParjolulSferic",player));
        starterDeck.add(createCard("UraganDeAzur",player));
        starterDeck.add(createCard("GeamatulEcoului",player));
        starterDeck.add(createCard("GeamatulEcoului",player));
        starterDeck.add(createCard("GeamatulEcoului",player));
        starterDeck.add(createCard("PavazaGlaciara",player));
        starterDeck.add(createCard("PavazaGlaciara",player));
        starterDeck.add(createCard("GeamatulEcoului",player));

        return starterDeck;
    }
}