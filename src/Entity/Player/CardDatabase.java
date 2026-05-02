package Entity.Player;

import Entity.Cards.*;
import Entity.Items.*;
import java.util.ArrayList;
import java.util.Random;

public class CardDatabase {
    public static ArrayList<Card> allCards = new ArrayList<>();

    public static void init() {
        allCards.add(new CadereaStelelor());
        allCards.add(new DeslusireaRunelor());
        allCards.add(new FluxulStrabun());
        allCards.add(new FulgerDeSange());
        allCards.add(new GeamatulEcoului());
        allCards.add(new ManiorulIernii());
        allCards.add(new ParjolulSferic());
        allCards.add(new PavazaAmurgului());
        allCards.add(new PavazaGlaciara());
        allCards.add(new PovaraAnilor());
        allCards.add(new SuflulVietii());
        allCards.add(new UraganDeAzur());
        // Adaugă aici oricâte cărți noi creezi
    }

    public static ArrayList<Card> getRandomRewards(int count) {
        ArrayList<Card> rewards = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            // Luăm o carte la întâmplare din baza de date
            Card randomCard = allCards.get(rand.nextInt(allCards.size()));
            rewards.add(randomCard);
        }
        return rewards;
    }
}