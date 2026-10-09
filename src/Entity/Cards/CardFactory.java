package Entity.Cards;

import Entity.Items.Card;
import Entity.Player.Player;
import Main.Exception.GameExceptionHandler;

public class CardFactory {

    /** Cheile stabile ale tuturor cărților (se salvează în baza de date, nu numele afișate). */
    /** Cărțile scrise în cod. Cele din res/Data/cards.json se adaugă în allIds(). */
    public static final String[] ALL_IDS = {
            "CadereaStelelor", "DeslusireaRunelor", "FluxulStrabun", "FulgerDeSange", "ParjolulSferic", "PavazaAmurgului",
            "PavazaGlaciara", "UraganDeAzur", "GeamatulEcoului", "ManiorulIernii", "PovaraAnilor", "SuflulVietii", "Dezolare"
    };

    private static java.util.Map<String, String> idByDisplayName;

    /** Toate cheile: cărțile din cod + cele din cards.json. */
    public static java.util.List<String> allIds() {
        java.util.List<String> out = new java.util.ArrayList<>(java.util.Arrays.asList(ALL_IDS));
        out.addAll(CardCatalog.dataIds());
        return out;
    }

    /**
     * Cheia unei cărți dintr-o salvare: acceptă cheia nouă ("ParjolulSferic") sau numele afișat din salvările vechi
     * ("Pârjolul Sferic"). Null dacă nu o recunoaște.
     */
    public static synchronized String resolveId(String saved, Player player) {
        if (saved == null) return null;
        for (String id : allIds()) if (id.equals(saved)) return id;
        if (idByDisplayName == null) {
            idByDisplayName = new java.util.HashMap<>();
            for (String id : allIds()) idByDisplayName.put(build(id, player).name, id);
        }
        return idByDisplayName.get(saved);
    }

    public static Card createCard(String cardName, Player player) {
        Card c = build(cardName, player);
        c.id = isKnown(cardName) ? cardName : "Dezolare";
        CardCatalog.decorate(c, c.id);   // elemente, statusuri aplicate, raritate (cards.json)
        return c;
    }

    private static boolean isKnown(String id) {
        return id != null && allIds().contains(id);
    }

    private static Card build(String cardName, Player player) {
        if (cardName == null || cardName.trim().isEmpty()) {
            GameExceptionHandler.getInstance().handleWarning("S-a încercat crearea unei cărți cu nume NULL sau gol în CardFactory.", null);
            return new Dezolare(); // Returnăm o carte inertă ca fallback
        }

        try {
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

                default -> {
                    var data = CardCatalog.def(cardName);   // carte nouă, definită doar în cards.json
                    if (data != null) yield new Card_Data(data, player);
                    // Semnalăm problema în consolă/fișier log fără să oprim jocul
                    GameExceptionHandler.getInstance().handleWarning(
                            "Cartea '" + cardName + "' nu este înregistrată în CardFactory. A fost returnat un fallback (Dezolare).",
                            new IllegalArgumentException("Carte necunoscută: " + cardName)
                    );
                    yield new Dezolare();
                }
            };
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare critică la instanțierea cărții: " + cardName, e);
            return new Dezolare();
        }
    }
}