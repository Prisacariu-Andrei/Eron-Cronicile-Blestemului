package Main;

import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * Trece fereastra jocului între mod fereastră și ecran complet "borderless" (fără ramă, cât ecranul), mai stabil pe Windows decât
 * ecranul complet exclusiv. Jocul se desenează tot la 1600x900; GamePanel îl mărește păstrând raportul 16:9 (vezi GamePanel.paintComponent).
 */
public final class ScreenMode {
    private ScreenMode() {}

    public static void apply(JFrame window, GamePanel panel, boolean fullscreen) {
        boolean wasVisible = window.isVisible();
        if (wasVisible) window.dispose();   // decorațiile se pot schimba doar când fereastra nu e afișată
        window.setUndecorated(fullscreen);
        if (fullscreen) {
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            window.setBounds(screen);
        } else {
            window.pack();                  // revine la mărimea panoului (1600x900)
            window.setLocationRelativeTo(null);
        }
        if (wasVisible) window.setVisible(true);
        // fereastra nouă trebuie să devină activă, altfel Windows ține tastatura pe fereastra veche și tastele nu mai ajung în joc
        javax.swing.SwingUtilities.invokeLater(() -> {
            window.toFront();
            window.requestFocus();
            panel.requestFocusInWindow();
        });
    }
}
