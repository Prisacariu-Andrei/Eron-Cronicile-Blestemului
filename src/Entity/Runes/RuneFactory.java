package Entity.Runes; // Modifică pachetul dacă stau în altă parte

import Entity.Items.Rune;

public class RuneFactory {
    public static Rune createRune(String runeName) {
        if (runeName == null) return null;

        return switch (runeName) {
            case "Colțul de Chihlimbar"   -> new Rune("Colțul de Chihlimbar", "DESCRIERE COLT", 5);
            case "Diamantul Nopții"-> new Rune("Diamantul Nopții", "DESCRIERE DIAMANT", 5);
            case "Paloșul Legendar"-> new Rune("Diamantul Nopții", "DESCRIERE DIAMANT", 5);
            case "Inelul de Gheață"-> new Rune("Diamantul Nopții", "DESCRIERE DIAMANT", 5);
            case "Scutul lui Valerius"-> new Rune("Diamantul Nopții", "DESCRIERE DIAMANT", 5);
            // Adaugă aici case-uri în funcție de ce clase de Rune ai create
            default -> {
                System.out.println("⚠️ Rune necunoscută în Factory: " + runeName);
                yield null;
            }
        };
    }
}