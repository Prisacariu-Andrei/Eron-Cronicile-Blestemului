package Entity.Player;

import Entity.Cards.*;
import Entity.Items.*;
import java.util.ArrayList;
import java.util.Random;

public class CardDatabase {
    public static ArrayList<Card> allCards = new ArrayList<>();

    public static void init(Player player) {
        allCards.add(new CadereaStelelor(player));
        allCards.add(new DeslusireaRunelor(player));
        allCards.add(new FluxulStrabun(player));
        allCards.add(new FulgerDeSange(player));
        allCards.add(new GeamatulEcoului());
        allCards.add(new ManiorulIernii());
        allCards.add(new ParjolulSferic(player));
        allCards.add(new PavazaAmurgului(player));
        allCards.add(new PavazaGlaciara(player));
        allCards.add(new PovaraAnilor());
        allCards.add(new SuflulVietii());
        allCards.add(new UraganDeAzur(player));
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