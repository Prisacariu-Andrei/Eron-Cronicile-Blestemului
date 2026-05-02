package Main;

import javax.swing.*;

public class Main {

    static void main() {
        // Crearea și configurarea ferestrei principale.
        JFrame window = new JFrame();

        // Setarea închiderii procesului la închiderea ferestrei.
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Dezactivarea opțiunii de redimensionare a ferestrei.
        window.setResizable(false);

        // Stabilirea titlului ferestrei de joc.
        window.setTitle("ERON: CRONICILE BLESTEMULUI");

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        // Ajustarea dimensiunii ferestrei în funcție de componentele interne.
        window.pack();

        gamePanel.setupGame();

        // Poziționarea ferestrei în centrul ecranului la lansare.
        window.setLocationRelativeTo(null);

        // Afișarea ferestrei pe ecran.
        window.setVisible(true);

        // Inițierea firului de execuție principal al jocului.
        gamePanel.startGameThread();
    }
}