package Entity.Items;

public class Rune {
    public String name;
    public String description;
    public int statValue; // Poate reprezenta un bonus de atac, viață, sau scut
    public boolean isEquipped;

    public Rune(String name, String description, int statValue) {
        this.name = name;
        this.description = description;
        this.statValue = statValue;
        this.isEquipped = false;
    }
}