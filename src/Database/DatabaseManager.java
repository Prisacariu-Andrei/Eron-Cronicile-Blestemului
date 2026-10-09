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
            connection = DriverManager.getConnection(DB_URL + "?busy_timeout=5000");

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA synchronous=NORMAL;");
                // IMPORTANT: Activăm cheile străine pentru a permite ștergerea în lanț
                st.execute("PRAGMA foreign_keys=ON;");
            }
            createTables();
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleCriticalError("Eroare DB", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            // Tabelul principal pentru salvări
            st.execute("CREATE TABLE IF NOT EXISTS saves (slot INTEGER PRIMARY KEY, player_name TEXT, current_zone TEXT, player_x INTEGER, player_y INTEGER, player_life INTEGER, player_max_life INTEGER, player_shield INTEGER, time_played INTEGER, saved_at TEXT);");

            // Monedele jucătorului: coloană adăugată ulterior, deci și pentru baze de date vechi
            // Progresia: nivel, XP și starea misiunilor din quests.json (text compact, vezi QuestLog.serialize)
            for (String column : new String[]{"xp INTEGER DEFAULT 0", "level INTEGER DEFAULT 1", "quest_data TEXT DEFAULT ''", "story_flags TEXT DEFAULT ''", "party TEXT DEFAULT ''"}) {
                try {
                    st.execute("ALTER TABLE saves ADD COLUMN " + column + ";");
                } catch (SQLException alreadyThere) {
                    // coloana există deja
                }
            }
            // (la fel și relicvele: loadGame le citea, dar nicio parte din cod nu le crea sau nu le salva)
            for (String column : new String[]{"coins", "are_dinte_chilimbar", "are_diamantul_noptii", "are_palos_legendar",
                    "are_inel_gheata", "are_platosa_valerius"}) {
                try {
                    st.execute("ALTER TABLE saves ADD COLUMN " + column + " INTEGER DEFAULT 0;");
                } catch (SQLException alreadyThere) {
                    // coloana există deja: nimic de făcut
                }
            }

            // Tabelul pentru quest-uri (Legat de saves prin FOREIGN KEY)
            st.execute("CREATE TABLE IF NOT EXISTS quest_state (slot INTEGER, quest_id INTEGER, is_main INTEGER, is_received INTEGER, is_completed INTEGER, current_amount INTEGER, PRIMARY KEY (slot, quest_id, is_main), FOREIGN KEY(slot) REFERENCES saves(slot) ON DELETE CASCADE);");

            // Tabelul pentru inventar (Legat de saves prin FOREIGN KEY)
            st.execute("CREATE TABLE IF NOT EXISTS inventory (slot INTEGER, item_type TEXT, item_name TEXT, quantity INTEGER, PRIMARY KEY (slot, item_type, item_name), FOREIGN KEY(slot) REFERENCES saves(slot) ON DELETE CASCADE);");

            // Tabelul pentru leaderboard
            st.execute("CREATE TABLE IF NOT EXISTS scores (id INTEGER PRIMARY KEY AUTOINCREMENT, player_name TEXT, time_seconds INTEGER);");

            st.execute("CREATE TABLE IF NOT EXISTS leaderboard( id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    player_name TEXT NOT NULL," +
                    "    time_played INTEGER NOT NULL," +
                    "    achieved_at TEXT NOT NULL" +
                    ");");
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed cleanly.");
            }
        } catch (SQLException e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la close database", e);
        }
    }
}