package Entity.Runes;

import Entity.Items.Rune;
import Main.Exception.GameExceptionHandler;

public class RuneFactory {

    public static Rune createRune(String runeName) {
        if (runeName == null || runeName.trim().isEmpty()) {
            GameExceptionHandler.getInstance().handleWarning("S-a încercat crearea unei rune cu nume NULL sau gol în RuneFactory.", null);
            return new Rune("Rună Comună", "O rună fără proprietăți magice evidente.", 0);
        }

        try {
            return switch (runeName) {
                case "Colțul de Chihlimbar" -> new Rune("Colțul de Chihlimbar", "Un colț mistic ce pulsează cu energie brută. Oferă +5 atac.", 5);
                case "Diamantul Nopții"     -> new Rune("Diamantul Nopții", "O piatră prețioasă ce absoarbe lumina din jur. Oferă +5 viață.", 5);
                case "Paloșul Legendar"     -> new Rune("Paloșul Legendar", "O relicvă străveche a apărătorilor Eldoriei. Oferă +5 daune.", 5);
                case "Inelul de Gheață"     -> new Rune("Inelul de Gheață", "Gheața eternă de pe acest inel nu se topește niciodată. Oferă +5 scut.", 5);
                case "Scutul lui Valerius"  -> new Rune("Scutul lui Valerius", "O bucată din scutul marelui cavaler neînvins. Oferă +5 protecție.", 5);

                default -> {
                    GameExceptionHandler.getInstance().handleWarning("Runa '" + runeName + "' nu este definită în RuneFactory. S-a generat o rună neutră.", null);
                    yield new Rune("Fragment de Rună", "O bucată spartă dintr-o rună indescifrabilă.", 0);
                }
            };
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare neprevăzută întâmpinată la generarea runei: " + runeName, e);
            return new Rune("Fragment de Rună", "O bucată spartă dintr-o rună indescifrabilă.", 0);
        }
    }
}