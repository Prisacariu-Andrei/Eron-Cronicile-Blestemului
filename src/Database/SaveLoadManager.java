package Database;

import Entity.Player.Player;
import Entity.Items.Card;
import Main.Exception.GameExceptionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import Main.GamePanel;

public class SaveLoadManager {

    private final DatabaseManager dbManager;

    public SaveLoadManager() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // ==========================================
    // 1. SALVAREA JOCULUI
    // ==========================================
    public void saveGame(Player player, int slot, String currentZone, long timePlayedSeconds) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return;

        // Folosim INSERT OR REPLACE pentru a suprascrie o salvare existentă pe același slot
        String saveQuery = "INSERT OR REPLACE INTO saves " +
                "(slot, player_name, current_zone, player_x, player_y, player_life, player_max_life, player_shield, time_played, saved_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(saveQuery)) {
            // Pentru securitatea tranzacției, putem dezactiva autocommit-ul opțional, dar o vom ține simplu

            pstmt.setInt(1, slot);
            pstmt.setString(2, player.name); // Asigură-te că folosești player.name sau player.playerName
            pstmt.setString(3, currentZone);
            pstmt.setInt(4, player.worldX);
            pstmt.setInt(5, player.worldY);
            pstmt.setInt(6, player.life);
            pstmt.setInt(7, player.maxLife);
            pstmt.setInt(8, player.shield);
            pstmt.setLong(9, timePlayedSeconds);

            // Formatăm data curentă
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pstmt.setString(10, currentTime);

            pstmt.executeUpdate();

            // Salvăm inventarul separat
            saveInventory(player, slot, conn);

            System.out.println("Joc salvat cu succes în slotul " + slot + "!");

        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleCriticalError("Eroare la salvarea jocului în DB", e);
        }
    }

    private void saveInventory(Player player, int slot, Connection conn) throws SQLException {
        // Deoarece am pus ON DELETE CASCADE în DB, datele vechi din inventar se pot șterge dacă am face DELETE pe saves,
        // DAR pentru că folosim INSERT OR REPLACE pe 'saves', foreign key-ul ar putea să păstreze inventarul vechi.
        // Cel mai sigur este să golim inventarul pentru acest slot înainte să inserăm noul inventar.
        try (PreparedStatement deleteInv = conn.prepareStatement("DELETE FROM inventory WHERE slot = ?")) {
            deleteInv.setInt(1, slot);
            deleteInv.executeUpdate();
        }

        // Grupăm cărțile pentru a obține cantitatea (deoarece PK este slot, item_type, item_name)
        Map<String, Integer> cardCounts = new HashMap<>();
        for (Card card : player.inventory.cards) {
            cardCounts.put(card.name, cardCounts.getOrDefault(card.name, 0) + 1);
        }

        String insertInvQuery = "INSERT INTO inventory (slot, item_type, item_name, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmtInv = conn.prepareStatement(insertInvQuery)) {
            for (Map.Entry<String, Integer> entry : cardCounts.entrySet()) {
                pstmtInv.setInt(1, slot);
                pstmtInv.setString(2, "Card"); // Specificăm tipul item-ului
                pstmtInv.setString(3, entry.getKey()); // Numele cărții
                pstmtInv.setInt(4, entry.getValue());  // Cantitatea
                pstmtInv.addBatch(); // Folosim batch pentru performanță
            }
            pstmtInv.executeBatch();
        }
    }

    // ==========================================
    // ÎNCĂRCAREA JOCULUI (Actualizat)
    // ==========================================
    public boolean loadGame(GamePanel gp, int slot) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        String loadQuery = "SELECT * FROM saves WHERE slot = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(loadQuery)) {
            pstmt.setInt(1, slot);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // 1. Restaurăm atributele de bază ale jucătorului
                gp.player.name = rs.getString("player_name");
                gp.player.worldX = rs.getInt("player_x");
                gp.player.worldY = rs.getInt("player_y");
                gp.player.life = rs.getInt("player_life");
                gp.player.maxLife = rs.getInt("player_max_life");
                gp.player.shield = rs.getInt("player_shield");

                // Restaurăm relicvele
                gp.player.areDinteChilimbar = rs.getInt("are_dinte_chilimbar") == 1;
                gp.player.areDiamantulNoptii = rs.getInt("are_diamantul_noptii") == 1;
                gp.player.arePalosLegendar = rs.getInt("are_palos_legendar") == 1;
                gp.player.areInelDeGheata = rs.getInt("are_inel_gheata") == 1;
                gp.player.arePlatosaValerius = rs.getInt("are_platosa_valerius") == 1;

                // 2. Încărcăm inventarul jucătorului
                loadInventory(gp.player, slot, conn);

                // 3. RECONSTRUIM HARTA SALVATĂ
                String savedZone = rs.getString("current_zone");
                System.out.println("Restaurare hartă: " + savedZone);

                // Încărcăm tile-urile și matricea hărții pentru zona salvată
                gp.tileM.loadZoneTiles(savedZone);
                gp.tileM.loadMap("/Maps/" + savedZone);

                // Re-populăm inamicii și NPC-urile pentru acea zonă
                gp.aSetter.setEnemyForMap("/Maps/" + savedZone);
                gp.aSetter.setNPC(savedZone);

                // 4. Resetăm UI-ul și anunțăm jucătorul
                gp.player.notifyObservers();
                System.out.println("Joc încărcat cu succes din slotul " + slot + "!");
                return true;
            } else {
                System.out.println("Nu s-a găsit nicio salvare în slotul " + slot + ".");
                return false;
            }
        } catch (SQLException e) {
            Main.Exception.GameExceptionHandler.getInstance().handleCriticalError("Eroare la încărcarea jocului", e);
            return false;
        }
    }

    private void loadInventory(Player player, int slot, Connection conn) throws SQLException {
        // Golim pachetul curent pentru a nu duplica cărțile
        player.inventory.cards.clear();

        String loadInvQuery = "SELECT item_name, quantity FROM inventory WHERE slot = ? AND item_type = 'Card'";
        try (PreparedStatement pstmt = conn.prepareStatement(loadInvQuery)) {
            pstmt.setInt(1, slot);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String cardName = rs.getString("item_name");
                int quantity = rs.getInt("quantity");

                // Adăugăm cartea de 'quantity' ori în pachet
                for (int i = 0; i < quantity; i++) {
                    // Aici folosești metoda ta existentă pentru a genera cartea după nume
                    player.inventory.addCardToDeck(cardName, player);
                }
            }
        }
    }

    // ==========================================
    // METODE PENTRU LEADERBOARD / TOP SCORURI
    // ==========================================

    /**
     * Înregistrează un timp nou în baza de date când jucătorul termină jocul.
     */
    public void recordScore(String playerName, long timePlayedSeconds) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return;

        String insertScoreQuery = "INSERT INTO leaderboard (player_name, time_played, achieved_at) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertScoreQuery)) {
            pstmt.setString(1, playerName);
            pstmt.setLong(2, timePlayedSeconds);

            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pstmt.setString(3, currentTime);

            pstmt.executeUpdate();
            System.out.println("Scor înregistrat în Panteon: " + playerName + " - " + timePlayedSeconds + " secunde.");
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleCriticalError("Eroare la înregistrarea scorului în leaderboard", e);
        }
    }

    /**
     * Returnează primii 5 timpi din baza de date, sortați crescător (cel mai rapid timp e primul).
     */
    public String[] getTopScores() {
        String[] scores = new String[5];
        Connection conn = dbManager.getConnection();
        if (conn == null) return scores;

        // Sortăm după timp crescător (ASC) pentru că la runda contra-cronometru, timpul mai mic e mai bun
        String selectQuery = "SELECT player_name, time_played FROM leaderboard ORDER BY time_played ASC LIMIT 5";

        try (PreparedStatement pstmt = conn.prepareStatement(selectQuery);
             ResultSet rs = pstmt.executeQuery()) {

            int index = 0;
            while (rs.next() && index < 5) {
                String name = rs.getString("player_name");
                long totalSeconds = rs.getLong("time_played");

                // Formatăm secundele brute într-un format frumos (Ex: 02h 15m 30s sau 04:20)
                String formattedTime = formatTime(totalSeconds);

                // Salvăm linia formatată direct în vector
                scores[index] = name + "  -  " + formattedTime;
                index++;
            }
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleCriticalError("Eroare la preluarea topului de scoruri", e);
        }

        return scores;
    }

    /**
     * Rutină simplă de transformat secunde în format text MM:SS sau HH:MM:SS
     */
    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

}