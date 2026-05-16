package Database;

import Main.GamePanel;
import Main.Exception.GameExceptionHandler;
import ProgressManagement.QuestManager;
import Entity.Items.Card;
import Entity.Items.Rune;
import Entity.Cards.CardFactory;
import Entity.Runes.RuneFactory;

import java.sql.*;
import java.util.List;

/**
 * SaveManager — reads and writes the full game state to the SQLite database.
 *
 * Slot system: 3 save slots (1, 2, 3).
 * Call saveGame(slot) to persist, loadGame(slot) to restore.
 */
public class SaveManager {

    private final GamePanel gp;
    private final Connection conn;

    public SaveManager(GamePanel gp) {
        this.gp = gp;
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    public void saveGame(int slot) {
        try {
            conn.setAutoCommit(false); // Transaction — save everything or nothing

            savePlayer(slot);
            saveQuests(slot);
            saveInventory(slot);

            conn.commit();
            conn.setAutoCommit(true);

            System.out.println("✅ Game saved to slot " + slot);

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                GameExceptionHandler.getInstance().handleWarning("Eșec total la rollback în timpul salvării slotului " + slot, ex);
            }
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut salva jocul pentru slotul " + slot, e);
        }
    }

    private void savePlayer(int slot) throws SQLException {
        String sql = """
            INSERT INTO saves (slot, current_zone, player_x, player_y,
                               player_life, player_max_life, player_shield, time_played, saved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
            ON CONFLICT(slot) DO UPDATE SET
                current_zone    = excluded.current_zone,
                player_x        = excluded.player_x,
                player_y        = excluded.player_y,
                player_life     = excluded.player_life,
                player_max_life = excluded.player_max_life,
                player_shield   = excluded.player_shield,
                time_played     = excluded.time_played,
                saved_at        = excluded.saved_at;
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slot);
            ps.setString(2, gp.tileM.currentZone);
            ps.setInt(3, gp.player.worldX);
            ps.setInt(4, gp.player.worldY);
            ps.setInt(5, gp.player.life);
            ps.setInt(6, gp.player.maxLife);
            ps.setInt(7, gp.player.shield);
            ps.setInt(8, gp.gameTimer.getElapsedSeconds());
            ps.executeUpdate();
        }
    }

    private void saveQuests(int slot) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM quest_state WHERE slot = ?")) {
            del.setInt(1, slot);
            del.executeUpdate();
        }

        String sql = """
            INSERT INTO quest_state (slot, quest_id, is_main, is_received, is_completed, current_amount)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            saveQuestList(ps, slot, gp.questManager.mainQuests, 1);
            saveQuestList(ps, slot, gp.questManager.sideQuests, 0);
        }
    }

    private void saveQuestList(PreparedStatement ps, int slot,
                               List<QuestManager.Quest> quests, int isMain) throws SQLException {
        for (QuestManager.Quest q : quests) {
            ps.setInt(1, slot);
            ps.setInt(2, q.id);
            ps.setInt(3, isMain);
            ps.setInt(4, q.isReceived  ? 1 : 0);
            ps.setInt(5, q.isCompleted ? 1 : 0);
            ps.setInt(6, q.currentAmount);
            ps.addBatch();
        }
        ps.executeBatch();
    }

    private void saveInventory(int slot) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM inventory WHERE slot = ?")) {
            del.setInt(1, slot);
            del.executeUpdate();
        }

        String sql = """
            INSERT OR IGNORE INTO inventory (slot, item_type, item_name, quantity)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(slot, item_type, item_name) DO UPDATE SET quantity = quantity + 1;
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Cards in deck
            for (Card c : gp.player.inventory.cards) {
                ps.setInt(1, slot);
                ps.setString(2, "CARD");
                ps.setString(3, c.name);
                ps.setInt(4, 1);
                ps.addBatch();
            }
            // Runes
            for (Rune r : gp.player.inventory.runes) {
                ps.setInt(1, slot);
                ps.setString(2, "RUNE");
                ps.setString(3, r.name);
                ps.setInt(4, 1);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // =========================================================================
    // LOAD
    // =========================================================================

    public void loadGame(int slot) {
        String sql = "SELECT * FROM saves WHERE slot = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slot);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("No save found in slot " + slot);
                    return;
                }

                String zone = rs.getString("current_zone");
                int px      = rs.getInt("player_x");
                int py      = rs.getInt("player_y");
                int life    = rs.getInt("player_life");
                int maxLife = rs.getInt("player_max_life");
                int shield  = rs.getInt("player_shield");
                int time    = rs.getInt("time_played");

                gp.eHandler.changeMap("/Maps/" + zone, zone, px / gp.tileSize, py / gp.tileSize);
                gp.player.life    = life;
                gp.player.maxLife = maxLife;
                gp.player.shield  = shield;
                gp.gameTimer.setElapsedSeconds(time);

                loadQuests(slot);
                loadInventory(slot);

                System.out.println("✅ Game loaded from slot " + slot + " (Zone: " + zone + ")");
            }
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleWarning("A apărut o eroare la încărcarea datelor din slotul " + slot, e);
        }
    }

    private void loadQuests(int slot) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM quest_state WHERE slot = ?")) {
            ps.setInt(1, slot);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id         = rs.getInt("quest_id");
                    boolean isMain = rs.getInt("is_main") == 1;
                    QuestManager.Quest q = gp.questManager.getQuestById(id, isMain);
                    if (q != null) {
                        q.isReceived    = rs.getInt("is_received")  == 1;
                        q.isCompleted   = rs.getInt("is_completed") == 1;
                        q.currentAmount = rs.getInt("current_amount");
                    }
                }
            }
        }
    }

    private void loadInventory(int slot) throws SQLException {
        gp.player.inventory.cards.clear();
        gp.player.inventory.runes.clear();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM inventory WHERE slot = ?")) {
            ps.setInt(1, slot);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("item_type");
                    String name = rs.getString("item_name");
                    int qty     = rs.getInt("quantity");

                    if (type.equals("CARD")) {
                        for (int i = 0; i < qty; i++) {
                            Card c = CardFactory.createCard(name, gp.player);
                            if (c != null) gp.player.inventory.cards.add(c);
                        }
                    } else if (type.equals("RUNE")) {
                        Rune r = RuneFactory.createRune(name);
                        if (r != null) gp.player.inventory.addRune(r);
                    }
                }
            }
        }
    }

    // =========================================================================
    // SCORES / LEADERBOARD
    // =========================================================================

    public void recordScore(String playerName, int timeSeconds) {
        String sql = "INSERT INTO scores (player_name, time_seconds) VALUES (?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setInt(2, timeSeconds);
            ps.executeUpdate();
            System.out.println("🏆 Score recorded: " + playerName + " — " + timeSeconds + "s");
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut înregistra noul scor în tabela de leaderboard", e);
        }
    }

    public String[] getTopScores() {
        String[] result = new String[5];
        String sql = "SELECT player_name, time_seconds FROM scores ORDER BY time_seconds ASC LIMIT 5;";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int i = 0;
            while (rs.next() && i < 5) {
                int secs   = rs.getInt("time_seconds");
                int mins   = secs / 60;
                int remain = secs % 60;
                result[i] = String.format("%s - %02d:%02d", rs.getString("player_name"), mins, remain);
                i++;
            }
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-au putut prelua timpii de top din baza de date", e);
        }
        return result;
    }

    public boolean slotExists(int slot) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM saves WHERE slot = ?")) {
            ps.setInt(1, slot);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la verificarea existenței slotului " + slot, e);
            return false;
        }
    }

    public void clearLeaderboard() {
        String sql = "DELETE FROM scores;";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            System.out.println("🧹 Leaderboard-ul a fost curățat cu succes din baza de date!");
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la golirea tabelului de leaderboard", e);
        }
    }
}