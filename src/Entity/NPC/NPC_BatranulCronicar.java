package Entity.NPC;

import Main.GamePanel;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class NPC_BatranulCronicar extends NPC {

    public NPC_BatranulCronicar(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Batranul Intelept";
        getImage();
        setDialogue();
    }

    @Override
    public void getImage() {
        try {
            // Calea: src/res/NPC/batran.png
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/AIGup.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDialogue() {
        dialogues[0] = "Salutare, tinere Eron! \nMa bucur sa te vad teafar.";
        dialogues[1] = "Lumea s-a schimbat mult de cand \nblestemul a cuprins cele 6 zone.";
        dialogues[2] = "Foloseste tastele cu grija. \nE pentru vorbit, F pentru obiecte.";
    }
}