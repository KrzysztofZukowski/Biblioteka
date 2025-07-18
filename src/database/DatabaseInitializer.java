package database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        // Utworzenie katalogu database jeśli nie istnieje
        File dbDir = new File("database");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Tworzenie tabeli users - zmieniono created_at na DATE
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(100),
                    is_admin BOOLEAN DEFAULT FALSE,
                    created_at DATE DEFAULT (DATE('now'))
                )
                """;
            stmt.execute(createUsersTable);

            // Tworzenie tabeli books - zmieniono created_at na DATE
            // USUNIĘTO UNIQUE z ISBN - biblioteka może mieć wiele egzemplarzy!
            String createBooksTable = """
                CREATE TABLE IF NOT EXISTS books (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    isbn VARCHAR(13),
                    title VARCHAR(255) NOT NULL,
                    author VARCHAR(255) NOT NULL,
                    publisher VARCHAR(255),
                    year INTEGER,
                    available BOOLEAN DEFAULT TRUE,
                    created_at DATE DEFAULT (DATE('now'))
                )
                """;
            stmt.execute(createBooksTable);

            // Tworzenie tabeli rentals - wszystkie daty jako DATE
            String createRentalsTable = """
                CREATE TABLE IF NOT EXISTS rentals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    book_id INTEGER NOT NULL,
                    rent_date DATE NOT NULL,
                    return_date DATE,
                    expected_return_date DATE,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    extension_count INTEGER DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (book_id) REFERENCES books(id)
                )
                """;
            stmt.execute(createRentalsTable);

            // Tworzenie tabeli extension_requests - zmieniono na DATE
            String createExtensionRequestsTable = """
                CREATE TABLE IF NOT EXISTS extension_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rental_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    requested_days INTEGER NOT NULL,
                    request_date DATE DEFAULT (DATE('now')),
                    status VARCHAR(20) DEFAULT 'PENDING',
                    admin_decision_date DATE,
                    admin_id INTEGER,
                    admin_comment TEXT,
                    FOREIGN KEY (rental_id) REFERENCES rentals(id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (admin_id) REFERENCES users(id)
                )
                """;
            stmt.execute(createExtensionRequestsTable);

            // Dodaj kolumny jeśli nie istnieją (dla starych baz)
            try {
                String addExpectedReturnColumn = "ALTER TABLE rentals ADD COLUMN expected_return_date DATE";
                stmt.execute(addExpectedReturnColumn);
            } catch (SQLException e) {
                // Kolumna już istnieje - ignoruj błąd
            }

            try {
                String addExtensionCountColumn = "ALTER TABLE rentals ADD COLUMN extension_count INTEGER DEFAULT 0";
                stmt.execute(addExtensionCountColumn);
            } catch (SQLException e) {
                // Kolumna już istnieje - ignoruj błąd
            }

            // Konwertuj istniejące dane TIMESTAMP na DATE
            convertTimestampsToDate(conn);

            // Dodanie domyślnego administratora (tylko jeśli nie istnieje)
            if (!userExists(conn, "admin")) {
                String insertAdmin = """
                    INSERT INTO users (username, password, email, is_admin, created_at) 
                    VALUES ('admin', 'admin123', 'admin@library.com', TRUE, DATE('now'))
                    """;
                stmt.execute(insertAdmin);
                System.out.println("Dodano domyślnego administratora.");
            }

            // Dodanie przykładowych książek - każda jako osobny egzemplarz
            addSampleBooksIfNotExist(conn);

            // Napraw istniejące wypożyczenia bez expected_return_date
            fixExistingRentals(conn);

            // Ustaw domyślną wartość extension_count dla istniejących wypożyczeń
            fixExtensionCount(conn);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }

    private static void convertTimestampsToDate(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Konwertuj created_at w users jeśli jest TIMESTAMP
            String updateUsersCreatedAt = """
                UPDATE users 
                SET created_at = DATE(created_at) 
                WHERE created_at IS NOT NULL 
                AND created_at NOT LIKE '____-__-__'
                """;
            stmt.execute(updateUsersCreatedAt);

            // Konwertuj created_at w books jeśli jest TIMESTAMP
            String updateBooksCreatedAt = """
                UPDATE books 
                SET created_at = DATE(created_at) 
                WHERE created_at IS NOT NULL 
                AND created_at NOT LIKE '____-__-__'
                """;
            stmt.execute(updateBooksCreatedAt);

            // Konwertuj request_date w extension_requests jeśli jest TIMESTAMP
            String updateExtRequestDate = """
                UPDATE extension_requests 
                SET request_date = DATE(request_date) 
                WHERE request_date IS NOT NULL 
                AND request_date NOT LIKE '____-__-__'
                """;
            stmt.execute(updateExtRequestDate);

            // Konwertuj admin_decision_date w extension_requests jeśli jest TIMESTAMP
            String updateExtDecisionDate = """
                UPDATE extension_requests 
                SET admin_decision_date = DATE(admin_decision_date) 
                WHERE admin_decision_date IS NOT NULL 
                AND admin_decision_date NOT LIKE '____-__-__'
                """;
            stmt.execute(updateExtDecisionDate);

        } catch (SQLException e) {
            System.out.println("Informacja: Nie można przekonwertować wszystkich dat: " + e.getMessage());
        }
    }

    private static boolean userExists(Connection conn, String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void addSampleBooksIfNotExist(Connection conn) {
        // Dodajemy przykładowe książki ZAWSZE (mogą być egzemplarze)
        // Sprawdzamy tylko czy istnieje już jakikolwiek egzemplarz danego tytułu
        String[][] sampleBooks = {
                {"9788375748116", "Władca Pierścieni", "J.R.R. Tolkien", "Iskry", "2012"},
                {"9788328020085", "Wiedźmin: Ostatnie życzenie", "Andrzej Sapkowski", "SuperNOWA", "2014"},
                {"9788375780932", "Harry Potter i Kamień Filozoficzny", "J.K. Rowling", "Media Rodzina", "2016"}
        };

        String insertSQL = "INSERT INTO books (isbn, title, author, publisher, year, created_at) VALUES (?, ?, ?, ?, ?, DATE('now'))";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (String[] book : sampleBooks) {
                // Sprawdź czy istnieje już jakiś egzemplarz tego tytułu
                if (!anyBookExistsByTitle(conn, book[1])) {
                    // Dodaj pierwszy egzemplarz
                    pstmt.setString(1, book[0]); // ISBN
                    pstmt.setString(2, book[1]); // Title
                    pstmt.setString(3, book[2]); // Author
                    pstmt.setString(4, book[3]); // Publisher
                    pstmt.setInt(5, Integer.parseInt(book[4])); // Year
                    pstmt.executeUpdate();
                    System.out.println("Dodano książkę: " + book[1]);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean anyBookExistsByTitle(Connection conn, String title) {
        String sql = "SELECT COUNT(*) FROM books WHERE LOWER(TRIM(title)) = LOWER(TRIM(?))";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void fixExistingRentals(Connection conn) {
        String updateSQL = """
            UPDATE rentals 
            SET expected_return_date = DATE(rent_date, '+14 days')
            WHERE expected_return_date IS NULL AND status = 'ACTIVE'
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Naprawiono " + updated + " wypożyczeń bez daty zakończenia.");
            }
        } catch (SQLException e) {
            System.out.println("Informacja: Nie można naprawić wypożyczeń: " + e.getMessage());
        }
    }

    private static void fixExtensionCount(Connection conn) {
        String updateSQL = """
            UPDATE rentals 
            SET extension_count = 0
            WHERE extension_count IS NULL
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Naprawiono " + updated + " wypożyczeń bez licznika przedłużeń.");
            }
        } catch (SQLException e) {
            System.out.println("Informacja: Nie można naprawić licznika przedłużeń: " + e.getMessage());
        }
    }
}