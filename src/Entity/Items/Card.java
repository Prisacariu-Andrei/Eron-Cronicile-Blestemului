package Entity.Items;

import Entity.Player.Player;
import Entity.Inamici.Enemy;
import Entity.StatusEffect;
import java.awt.image.BufferedImage;

public abstract class Card {
    public String name;
    public String id;   // cheia stabilă din CardFactory (ex. "ParjolulSferic"); numele de mai sus se afișează și se poate schimba
    public int manaCost;
    public String description;
    public BufferedImage image;

    // Tipuri pentru sinergii
    public enum CardType { ELECTRIC, FOC, UMBRA, NEUTRU, INGHET }
    public CardType type;

    // Sinergii (res/Data/cards.json): elementele cărții, dacă lovește (poate declanșa reacții), ce statusuri pune după ce se joacă
    public final java.util.Set<String> tags = new java.util.LinkedHashSet<>();
    public String rarity = "Comună";
    public boolean offensive = false;
    public final java.util.List<java.util.Map<String, Object>> applies = new java.util.ArrayList<>();

    public Card(String name, int cost, CardType type, String desc) {
        this.name = name;
        this.manaCost = cost;
        this.type = type;
        this.description = desc;
        switch (type) {   // elementul implicit după tipul cărții; "overrides" din cards.json îl poate schimba
            case FOC -> tags.add("FOC");
            case INGHET -> tags.add("GHEATA");
            case ELECTRIC -> tags.add("FULGER");
            case UMBRA -> tags.add("UMBRA");
            default -> { }
        }
    }

    /** Un rând cu condiția de combo a cărții (de ex. "După o carte Umbră: +22 paguba"), sau null. Cărțile din date îl suprascriu. */
    public String comboHint() { return null; }

    /** Combo-ul propriu al cărții e îndeplinit acum? (pentru evidențierea din mână). Cărțile din date îl suprascriu. */
    public boolean comboMet(Player player, Enemy target) { return false; }

    // Metoda pe care o apelează Player.java
    public abstract void play(Player player, Enemy enemy);

    // --- METODE HELPER ACTUALIZATE PENTRU SISTEMUL DE STATUSURI ---

    /**
     * Reduce damage-ul inamicului la jumătate.
     */
    protected void applyWeakness(Enemy e, int turns) {
        if (e != null) {
            e.applyStatus(StatusEffect.Type.WEAK, turns, 0);
            e.weaknessTurns += turns;
        }
    }

    /**
     * Inamicul sare peste tura următoare.
     */
    protected void applyStun(Enemy e, int turns) {
        if (e != null) {
            // Folosim variabila stunTurns din Entity sau noul sistem StatusEffect
            e.stunTurns += turns;
            e.applyStatus(StatusEffect.Type.STUN, turns, 0);
        }
    }

    protected void applyVulnerability(Enemy e, int turns) {
        if (e != null) {
            // Aplicăm statusul. Valoarea poate fi folosită pentru procentaj (ex: 50 pentru 50% mai mult)
            e.applyStatus(StatusEffect.Type.VULNERABLE, turns, 50);
        }
    }

    /**
     * Aplică daune periodice de foc.
     */
    protected void applyBurn(Enemy e, int turns, int damagePerTurn) {
        if (e != null) {
            e.burnTurns += turns;
            e.applyStatus(StatusEffect.Type.BURN, turns, damagePerTurn);
        }
    }

    /**
     * Aplică daune periodice de otravă.
     */
    protected void applyPoison(Enemy e, int turns, int damagePerTurn) {
        if (e != null) {
            e.applyStatus(StatusEffect.Type.POISON, turns, damagePerTurn);
        }
    }

    // --- DESCRIERE DINAMICĂ ---

    public String getDynamicDescription(Player player) {
        // Implicit returnează descrierea statică definită în constructor
        return this.description;
    }
}