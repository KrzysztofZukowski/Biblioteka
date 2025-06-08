package services;

import database.DatabaseManager;
import models.Rental;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RentalService {
    private BookService bookService = new BookService();

    public List<Rental> getUserRentals(int userId) {
        List<Rental> rentals = new ArrayList<>();
        String sql = """
            SELECT r.*, b.title as book_title, b.author as book_author 
            FROM rentals r 
            JOIN books b ON r.book_id = b.id 
            WHERE r.user_id = ? AND r.status = 'ACTIVE'
            ORDER BY r.rent_date DESC
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Rental rental = mapResultSetToRental(rs);
                rentals.add(rental);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rentals;
    }

    public List<Rental> getAllActiveRentals() {
        List<Rental> rentals = new ArrayList<>();
        String sql = """
            SELECT r.*, u.username, b.title as book_title, b.author as book_author 
            FROM rentals r 
            JOIN users u ON r.user_id = u.id 
            JOIN books b ON r.book_id = b.id 
            WHERE r.status = 'ACTIVE'
            ORDER BY r.rent_date DESC
            """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Rental rental = mapResultSetToRental(rs);
                rental.setUsername(rs.getString("username"));
                rentals.add(rental);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rentals;
    }

    public boolean rentBook(int userId, int bookId) {
        String sql = "INSERT INTO rentals (user_id, book_id, rent_date, status) VALUES (?, ?, ?, 'ACTIVE')";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.setDate(3, Date.valueOf(LocalDate.now()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // Oznacz książkę jako wypożyczoną
                bookService.updateBookAvailability(bookId, false);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean returnBook(int rentalId) {
        String sql = "UPDATE rentals SET return_date = ?, status = 'RETURNED' WHERE id = ?";
        String getBookIdSql = "SELECT book_id FROM rentals WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            // Pobierz ID książki
            int bookId;
            try (PreparedStatement pstmt = conn.prepareStatement(getBookIdSql)) {
                pstmt.setInt(1, rentalId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    bookId = rs.getInt("book_id");
                } else {
                    return false;
                }
            }

            // Zaktualizuj wypożyczenie
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, Date.valueOf(LocalDate.now()));
                pstmt.setInt(2, rentalId);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    // Oznacz książkę jako dostępną
                    bookService.updateBookAvailability(bookId, true);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Rental mapResultSetToRental(ResultSet rs) throws SQLException {
        Rental rental = new Rental();
        rental.setId(rs.getInt("id"));
        rental.setUserId(rs.getInt("user_id"));
        rental.setBookId(rs.getInt("book_id"));
        rental.setRentDate(rs.getDate("rent_date").toLocalDate());

        Date returnDate = rs.getDate("return_date");
        if (returnDate != null) {
            rental.setReturnDate(returnDate.toLocalDate());
        }

        rental.setStatus(rs.getString("status"));
        rental.setBookTitle(rs.getString("book_title"));
        rental.setBookAuthor(rs.getString("book_author"));

        return rental;
    }
}