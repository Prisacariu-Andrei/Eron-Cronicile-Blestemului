package Main;

import javax.swing.*;

public class Main {

    // Trebuie să adaugi String[] args
    public static void main(String[] args) {
        JFrame window = new JFrame();

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("ERON: CRONICILE BLESTEMULUI");

        GamePanel gamePanel = new GamePanel();
        gamePanel.window = window;
        window.add(gamePanel);

        window.pack();

        gamePanel.setupGame();

        window.setLocationRelativeTo(null);
        if (Settings.fullscreen) ScreenMode.apply(window, gamePanel, true);   // alegerea din setări se păstrează între porniri
        window.setVisible(true);
        gamePanel.requestFocusInWindow();

        gamePanel.startGameThread();
    }
}