package Entity.Cards;

import Entity.Items.Card;
import Entity.Player.Player;
import Main.Exception.GameExceptionHandler;

public class CardFactory {

    public static Card createCard(String cardName, Player player) {
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