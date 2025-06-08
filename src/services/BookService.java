package services;

import database.DatabaseManager;
import models.Book;

import java.sql.*;
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

    public boolean addBook(Book book) {
        // Sprawdź czy książka już istnieje
        if (bookExists(book)) {
            System.err.println("Książka już istnieje w systemie!");
            return false;
        }

        String sql = "INSERT INTO books (isbn, title, author, publisher, year, available) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getPublisher());
            pstmt.setInt(5, book.getYear());
            pstmt.setBoolean(6, book.isAvailable());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean bookExists(Book book) {
        // Sprawdź po ISBN (jeśli podany)
        if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
            if (bookExistsByISBN(book.getIsbn())) {
                return true;
            }
        }

        // Sprawdź po tytule i autorze
        return bookExistsByTitleAuthor(book.getTitle(), book.getAuthor());
    }

    private boolean bookExistsByISBN(String isbn) {
        String sql = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean bookExistsByTitleAuthor(String title, String author) {
        String sql = "SELECT COUNT(*) FROM books WHERE LOWER(TRIM(title)) = LOWER(TRIM(?)) AND LOWER(TRIM(author)) = LOWER(TRIM(?))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, author);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
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
        // Sprawdź czy po edycji nie będzie duplikatu (wykluczając samą siebie)
        if (bookExistsExcludingId(book)) {
            System.err.println("Książka o takich danych już istnieje w systemie!");
            return false;
        }

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
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean bookExistsExcludingId(Book book) {
        String sql = "SELECT COUNT(*) FROM books WHERE id != ? AND (";

        // Jeśli jest ISBN, sprawdź po ISBN
        if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
            sql += "isbn = ? OR ";
        }

        // Zawsze sprawdź po tytule i autorze
        sql += "LOWER(TRIM(title)) = LOWER(TRIM(?)) AND LOWER(TRIM(author)) = LOWER(TRIM(?)))";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            pstmt.setInt(paramIndex++, book.getId());

            if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
                pstmt.setString(paramIndex++, book.getIsbn());
            }

            pstmt.setString(paramIndex++, book.getTitle());
            pstmt.setString(paramIndex, book.getAuthor());

            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
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
            return affectedRows > 0;
        } catch (SQLException e) {
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

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getInt("id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setPublisher(rs.getString("publisher"));
        book.setYear(rs.getInt("year"));
        book.setAvailable(rs.getBoolean("available"));
        book.setCreatedAt(rs.getString("created_at"));
        return book;
    }
}