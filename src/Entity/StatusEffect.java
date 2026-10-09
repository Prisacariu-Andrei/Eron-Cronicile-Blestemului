package Entity;

public class StatusEffect {
    public enum Type { BURN, POISON, WEAK, SHIELD, STUN, VULNERABLE, WET, FROZEN, CHARGED }

    public Type type;
    public int duration; // Câte turnuri mai durează
    public int value;    // Puterea efectului (ex: 5 damage pe turn)

    public StatusEffect(Type type, int duration, int value) {
        this.type = type;
        this.duration = duration;
        this.value = value;
    }
}