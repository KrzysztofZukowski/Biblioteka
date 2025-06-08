package database;

import java.io.File;
import java.sql.Connection;
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
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (book_id) REFERENCES books(id)
                )
                """;
            stmt.execute(createRentalsTable);

            // Dodanie domyślnego administratora
            String insertAdmin = """
                INSERT OR IGNORE INTO users (username, password, email, is_admin) 
                VALUES ('admin', 'admin123', 'admin@library.com', TRUE)
                """;
            stmt.execute(insertAdmin);

            // Dodanie przykładowych książek
            String insertSampleBooks = """
                INSERT OR IGNORE INTO books (isbn, title, author, publisher, year) VALUES
                ('9788375748116', 'Władca Pierścieni', 'J.R.R. Tolkien', 'Iskry', 2012),
                ('9788328020085', 'Wiedźmin: Ostatnie życzenie', 'Andrzej Sapkowski', 'SuperNOWA', 2014),
                ('9788375780932', 'Harry Potter i Kamień Filozoficzny', 'J.K. Rowling', 'Media Rodzina', 2016)
                """;
            stmt.execute(insertSampleBooks);

            System.out.println("Baza danych została zainicjalizowana!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }
}