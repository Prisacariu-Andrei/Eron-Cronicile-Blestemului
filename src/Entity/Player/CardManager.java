package Entity.Player;

import Entity.Items.Card;
import Entity.Cards.*; // Importăm tot pachetul unde stau clasele din poză
import java.util.ArrayList;

public class CardManager {

    /**
     * Creează o instanță nouă a unei cărți bazată pe un ID/Nume.
     * Folositor pentru RewardUI și Inventory.
     */
    public static Card createCard(String cardName) {
        return switch (cardName) {
            case "CadereaStelelor" -> new CadereaStelelor();
            case "DeslusireaRunelor" -> new DeslusireaRunelor();
            case "FluxulStrabun" -> new FluxulStrabun();
            case "FulgerDeSange" -> new FulgerDeSange();
            case "GeamatulEcoului" -> new GeamatulEcoului();
            case "ManiorulIernii" -> new ManiorulIernii();
            case "ParjolulSferic" -> new ParjolulSferic();
            case "PavazaAmurgului" -> new PavazaAmurgului();
            case "PavazaGlaciara" -> new PavazaGlaciara();
            case "PovaraAnilor" -> new PovaraAnilor();
            case "SuflulVietii" -> new SuflulVietii();
            case "UraganDeAzur" -> new UraganDeAzur();
            default -> null;
        };
    }

    /**
     * Pachetul de start al lui Eron folosind noile clase.
     */
    public static ArrayList<Card> getStartingDeck() {
        ArrayList<Card> starterDeck = new ArrayList<>();

        // Adăugăm câteva cărți de bază din lista ta pentru început
        starterDeck.add(createCard("ParjolulSferic"));
        starterDeck.add(createCard("ParjolulSferic"));
        starterDeck.add(createCard("PavazaGlaciara"));
        starterDeck.add(createCard("PavazaGlaciara"));
        starterDeck.add(createCard("GeamatulEcoului"));

        return starterDeck;
    }
}