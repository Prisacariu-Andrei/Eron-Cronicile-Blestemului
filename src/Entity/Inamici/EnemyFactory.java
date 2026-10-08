package Entity.Inamici;

import Main.GamePanel;
import Entity.Generali.*;

public class EnemyFactory {

    public static Enemy createEnemy(String enemyName, GamePanel gp, int x, int y) {
        return switch (enemyName) {

            // Eldoria
            case "Valerius" -> new Enemy_Valerius(gp,x,y);
            case "Lup Slăbit" -> new Enemy_LupSlabit(gp,x,y);

            // Barlog
            case "Lup" -> new Enemy_Lup(gp, x, y);
            case "Cioara" -> new Enemy_Cioara(gp, x, y);
            case "Lup de Elită"    -> new Enemy_LupElite(gp, x, y);
            case "Fenris cel Alb"   -> new Enemy_LupBoss(gp, x, y);

            // Abis
            case "Trol" -> new Enemy_Trol(gp, x, y);

            case "Trol de Elită" -> new Enemy_TrolElite(gp, x, y);
            case "Feldgug" -> new Enemy_Feldgug(gp, x, y);

            // Citadela
            //case "" -> new Enemy_Cioara(gp, x, y);
            //case "Lup" -> new Enemy_Cioara(gp, x, y);
            case "Jean Plesa" -> new Enemy_JeanPlesa(gp, x, y);

            // Tundra
            //case "Lup" -> new Enemy_Cioara(gp, x, y);
            //case "Lup" -> new Enemy_Cioara(gp, x, y);
            case "Glaciara, Stăpânul Inelului de Gheață" -> new Enemy_Glaciara(gp, x, y);

            // Podul
            //case "Lup" -> new Enemy_Cioara(gp, x, y);
            //case "Lup" -> new Enemy_Cioara(gp, x, y);
            case "Charon, Străjerul Umbrelor fără Tihnă" -> new Enemy_Charon(gp, x, y);

            // Castelul
            //case "Lup" -> new Enemy_Cioara(gp, x, y);
            //case "Lup" -> new Enemy_Cioara(gp, x, y);
            case "Malakor, Monarhul Amurgului" -> new Enemy_Malakor(gp, x, y);


            default -> {
                System.out.println("Eroare: Inamicul '" + enemyName + "' nu există în Factory!");
                yield null;
            }
        };
    }
}