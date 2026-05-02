package Entity.Items;

import Entity.Player.Player;
import Entity.Inamici.Enemy;
import java.awt.image.BufferedImage;

public abstract class Card {
    public String name;
    public int manaCost;
    public String description;
    public BufferedImage image;

    // Tipuri pentru sinergii (ex: pentru Uragan de Azur)
    public enum CardType { ELECTRIC, FOC, UMBRA, NEUTRU, INGHET }
    public CardType type;

    public Card(String name, int cost, CardType type, String desc) {
        this.name = name;
        this.manaCost = cost;
        this.type = type;
        this.description = desc;
    }

    // Metoda pe care o apelează Player.java
    public abstract void play(Player player, Enemy enemy);

    // Metode helper pentru a nu repeta codul în fiecare carte
    protected void applyWeakness(Enemy e) {
        e.weaknessTurns += 3;
    }

    protected void applyStun(Enemy e) {
        e.stunTurns += 1;
    }
}