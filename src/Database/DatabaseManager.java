package Database;

import Main.Exception.GameExceptionHandler;
import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:eron_save.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=DELETE;");
            }

            createTables();
            System.out.println("✅ Database connected: eron_save.db");

        } catch (ClassNotFoundException e) {
            GameExceptionHandler.getInstance().handleCriticalError(
                    "Driverul SQLite JDBC lipsește din classpath!", e);
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleCriticalError(
                    "Nu s-a putut inițializa sau conecta la baza de date SQLite!", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS saves (
                    slot            INTEGER PRIMARY KEY,
                    player_name     TEXT    DEFAULT 'Eron',
                    current_zone    TEXT    DEFAULT 'Eldoria',
                    player_x        INTEGER DEFAULT 0,
                    player_y        INTEGER DEFAULT 0,
                    player_life     INTEGER DEFAULT 100,
                    player_max_life INTEGER DEFAULT 100,
                    player_shield   INTEGER DEFAULT 0,
                    time_played     INTEGER DEFAULT 0,
                    saved_at        TEXT    DEFAULT (datetime('now'))
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS quest_state (
                    slot           INTEGER,
                    quest_id       INTEGER,
                    is_main        INTEGER DEFAULT 1,
                    is_received    INTEGER DEFAULT 0,
                    is_completed   INTEGER DEFAULT 0,
                    current_amount INTEGER DEFAULT 0,
                    PRIMARY KEY (slot, quest_id, is_main)
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    slot      INTEGER,
                    item_type TEXT,
                    item_name TEXT,
                    quantity  INTEGER DEFAULT 1,
                    PRIMARY KEY (slot, item_type, item_name)
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS scores (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name  TEXT DEFAULT 'Eron',
                    time_seconds INTEGER,
                    completed_at TEXT DEFAULT (datetime('now'))
                );
            """);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed cleanly.");
            }
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare întâmpinată la închiderea conexiunii bazei de date", e);
        }
    }
}