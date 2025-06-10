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

            // Tworzenie tabeli users
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(100),
                    is_admin BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            stmt.execute(createUsersTable);

            // Tworzenie tabeli books
            String createBooksTable = """
                CREATE TABLE IF NOT EXISTS books (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    isbn VARCHAR(13),
                    title VARCHAR(255) NOT NULL,
                    author VARCHAR(255) NOT NULL,
                    publisher VARCHAR(255),
                    year INTEGER,
                    available BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            stmt.execute(createBooksTable);

            // Tworzenie tabeli rentals
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

            // Tworzenie tabeli extension_requests dla próśb wymagających zgody admina
            String createExtensionRequestsTable = """
                CREATE TABLE IF NOT EXISTS extension_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rental_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    requested_days INTEGER NOT NULL,
                    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status VARCHAR(20) DEFAULT 'PENDING',
                    admin_decision_date TIMESTAMP,
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

            // Dodanie domyślnego administratora (tylko jeśli nie istnieje)
            if (!userExists(conn, "admin")) {
                String insertAdmin = """
                    INSERT INTO users (username, password, email, is_admin) 
                    VALUES ('admin', 'admin123', 'admin@library.com', TRUE)
                    """;
                stmt.execute(insertAdmin);
                System.out.println("Dodano domyślnego administratora.");
            }

            // Usuń duplikaty książek (bezpieczna operacja)
            removeDuplicateBooks(conn);

            // Dodanie przykładowych książek (tylko jeśli nie istnieją)
            addSampleBooksIfNotExist(conn);

            // Napraw istniejące wypożyczenia bez expected_return_date
            fixExistingRentals(conn);

            // Ustaw domyślną wartość extension_count dla istniejących wypożyczeń
            fixExtensionCount(conn);

            System.out.println("Baza danych została zainicjalizowana!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
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

    private static void removeDuplicateBooks(Connection conn) {
        // Usuń duplikaty książek na podstawie ISBN (zachowaj najstarszą)
        String removeDuplicatesByISBN = """
            DELETE FROM books WHERE id NOT IN (
                SELECT MIN(id) FROM books 
                WHERE isbn IS NOT NULL AND isbn != '' 
                GROUP BY isbn
            ) AND isbn IS NOT NULL AND isbn != ''
            """;

        // Usuń duplikaty na podstawie tytuł+autor (zachowaj najstarszą)
        String removeDuplicatesByTitleAuthor = """
            DELETE FROM books WHERE id NOT IN (
                SELECT MIN(id) FROM books 
                GROUP BY LOWER(TRIM(title)), LOWER(TRIM(author))
            )
            """;

        try (PreparedStatement stmt1 = conn.prepareStatement(removeDuplicatesByISBN);
             PreparedStatement stmt2 = conn.prepareStatement(removeDuplicatesByTitleAuthor)) {

            int removedByISBN = stmt1.executeUpdate();
            int removedByTitle = stmt2.executeUpdate();

            if (removedByISBN > 0 || removedByTitle > 0) {
                System.out.println("Usunięto " + (removedByISBN + removedByTitle) + " duplikatów książek.");
            }
        } catch (SQLException e) {
            System.out.println("Informacja: Nie można usunąć duplikatów: " + e.getMessage());
        }
    }

    private static boolean bookExistsByISBN(Connection conn, String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean bookExistsByTitleAuthor(Connection conn, String title, String author) {
        String sql = "SELECT COUNT(*) FROM books WHERE LOWER(TRIM(title)) = LOWER(TRIM(?)) AND LOWER(TRIM(author)) = LOWER(TRIM(?))";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void addSampleBooksIfNotExist(Connection conn) {
        String[][] sampleBooks = {
                {"9788375748116", "Władca Pierścieni", "J.R.R. Tolkien", "Iskry", "2012"},
                {"9788328020085", "Wiedźmin: Ostatnie życzenie", "Andrzej Sapkowski", "SuperNOWA", "2014"},
                {"9788375780932", "Harry Potter i Kamień Filozoficzny", "J.K. Rowling", "Media Rodzina", "2016"}
        };

        String insertSQL = "INSERT INTO books (isbn, title, author, publisher, year) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (String[] book : sampleBooks) {
                // Sprawdź zarówno ISBN jak i kombinację tytuł+autor
                if (!bookExistsByISBN(conn, book[0]) && !bookExistsByTitleAuthor(conn, book[1], book[2])) {
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