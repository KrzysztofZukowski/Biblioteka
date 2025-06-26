package services;

import database.DatabaseManager;
import models.Book;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookService {

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                books.add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public List<Book> getAvailableBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE available = TRUE ORDER BY title";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                books.add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    /**
     * Wyszukuje książki z zastosowaniem filtrów
     */
    public List<Book> searchBooksWithFilters(String titleFilter, String authorFilter,
                                             String publisherFilter, Integer yearFrom,
                                             Integer yearTo, Boolean availableOnly) {
        List<Book> books = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM books WHERE 1=1");
        List<Object> parameters = new ArrayList<>();

        // Filtr tytułu
        if (titleFilter != null && !titleFilter.trim().isEmpty()) {
            sql.append(" AND LOWER(title) LIKE LOWER(?)");
            parameters.add("%" + titleFilter.trim() + "%");
        }

        // Filtr autora
        if (authorFilter != null && !authorFilter.trim().isEmpty()) {
            sql.append(" AND LOWER(author) LIKE LOWER(?)");
            parameters.add("%" + authorFilter.trim() + "%");
        }

        // Filtr wydawnictwa
        if (publisherFilter != null && !publisherFilter.trim().isEmpty()) {
            sql.append(" AND LOWER(publisher) LIKE LOWER(?)");
            parameters.add("%" + publisherFilter.trim() + "%");
        }

        // Filtr roku od
        if (yearFrom != null && yearFrom > 0) {
            sql.append(" AND year >= ?");
            parameters.add(yearFrom);
        }

        // Filtr roku do
        if (yearTo != null && yearTo > 0) {
            sql.append(" AND year <= ?");
            parameters.add(yearTo);
        }

        // Filtr dostępności
        if (availableOnly != null) {
            if (availableOnly) {
                sql.append(" AND available = TRUE");
            } else {
                sql.append(" AND available = FALSE");
            }
        }

        sql.append(" ORDER BY title");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            // Ustaw parametry
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                books.add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public boolean addBook(Book book) {
        // BRAK sprawdzania duplikatów - biblioteka może mieć wiele egzemplarzy tej samej książki!
        String sql = "INSERT INTO books (isbn, title, author, publisher, year, available, created_at) VALUES (?, ?, ?, ?, ?, ?, DATE('now'))";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getPublisher());
            pstmt.setInt(5, book.getYear());
            pstmt.setBoolean(6, book.isAvailable());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ Dodano książkę: " + book.getTitle() + " (ISBN: " + book.getIsbn() + ")");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Błąd podczas dodawania książki: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBookAvailability(int bookId, boolean available) {
        String sql = "UPDATE books SET available = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, available);
            pstmt.setInt(2, bookId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBook(Book book) {
        // USUNIĘTO sprawdzanie duplikatów - biblioteka może mieć wiele egzemplarzy!
        // Każdy egzemplarz ma swoje unikalne ID, więc można edytować bez obaw

        String sql = "UPDATE books SET isbn = ?, title = ?, author = ?, publisher = ?, year = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getPublisher());
            pstmt.setInt(5, book.getYear());
            pstmt.setInt(6, book.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ Zaktualizowano książkę: " + book.getTitle() + " (ID: " + book.getId() + ")");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Błąd podczas aktualizacji książki: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBook(int bookId) {
        String sql = "DELETE FROM books WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✅ Usunięto książkę o ID: " + bookId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Błąd podczas usuwania książki: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Book> searchBooks(String query) {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT * FROM books 
            WHERE LOWER(title) LIKE LOWER(?) 
               OR LOWER(author) LIKE LOWER(?) 
               OR LOWER(publisher) LIKE LOWER(?)
               OR isbn LIKE ?
            ORDER BY title
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Book book = mapResultSetToBook(rs);
                books.add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    /**
     * Sprawdza ile egzemplarzy książki o danym ISBN jest w bibliotece
     */
    public int getBookCountByISBN(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Sprawdza ile egzemplarzy książki o danym ISBN jest dostępnych
     */
    public int getAvailableBookCountByISBN(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM books WHERE isbn = ? AND available = TRUE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Sprawdza ile egzemplarzy książki o danym tytule i autorze jest w bibliotece
     */
    public int getBookCountByTitleAuthor(String title, String author) {
        if (title == null || title.trim().isEmpty() || author == null || author.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM books WHERE LOWER(TRIM(title)) = LOWER(TRIM(?)) AND LOWER(TRIM(author)) = LOWER(TRIM(?))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, author);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Sprawdza ile egzemplarzy książki o danym tytule i autorze jest dostępnych
     */
    public int getAvailableBookCountByTitleAuthor(String title, String author) {
        if (title == null || title.trim().isEmpty() || author == null || author.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM books WHERE LOWER(TRIM(title)) = LOWER(TRIM(?)) AND LOWER(TRIM(author)) = LOWER(TRIM(?)) AND available = TRUE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, author);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getInt("id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setPublisher(rs.getString("publisher"));
        book.setYear(rs.getInt("year"));
        book.setAvailable(rs.getBoolean("available"));

        // Bezpieczne odczytywanie created_at - używamy getString zamiast getDate
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null && !createdAtStr.trim().isEmpty()) {
            try {
                book.setCreatedAt(LocalDate.parse(createdAtStr));
            } catch (Exception e) {
                // Jeśli parsowanie się nie uda, ustaw obecną datę
                book.setCreatedAt(LocalDate.now());
            }
        } else {
            book.setCreatedAt(LocalDate.now());
        }

        return book;
    }
}