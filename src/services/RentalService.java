package services;

import database.DatabaseManager;
import models.Rental;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RentalService {
    private BookService bookService = new BookService();

    // Domyślny okres wypożyczenia w dniach
    private static final int DEFAULT_RENTAL_PERIOD_DAYS = 14;

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

    public List<Rental> getOverdueRentals() {
        List<Rental> rentals = new ArrayList<>();
        String sql = """
            SELECT r.*, u.username, b.title as book_title, b.author as book_author 
            FROM rentals r 
            JOIN users u ON r.user_id = u.id 
            JOIN books b ON r.book_id = b.id 
            WHERE r.status = 'ACTIVE' AND r.expected_return_date < ?
            ORDER BY r.expected_return_date ASC
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            ResultSet rs = pstmt.executeQuery();

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
        return rentBook(userId, bookId, DEFAULT_RENTAL_PERIOD_DAYS);
    }

    public boolean rentBook(int userId, int bookId, int rentalPeriodDays) {
        String sql = """
            INSERT INTO rentals (user_id, book_id, rent_date, expected_return_date, status) 
            VALUES (?, ?, ?, ?, 'ACTIVE')
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDate rentDate = LocalDate.now();
            LocalDate expectedReturnDate = rentDate.plusDays(rentalPeriodDays);

            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.setDate(3, Date.valueOf(rentDate));
            pstmt.setDate(4, Date.valueOf(expectedReturnDate));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // Oznacz książkę jako wypożyczoną
                bookService.updateBookAvailability(bookId, false);
                System.out.println("Książka wypożyczona do: " + expectedReturnDate);
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

    public boolean extendRental(int rentalId, int additionalDays) {
        // Pobierz obecną oczekiwaną datę zwrotu
        String selectSQL = "SELECT expected_return_date FROM rentals WHERE id = ? AND status = 'ACTIVE'";
        String updateSQL = "UPDATE rentals SET expected_return_date = ? WHERE id = ? AND status = 'ACTIVE'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
             PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

            // Pobierz obecną datę
            selectStmt.setInt(1, rentalId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                Date currentExpectedDate = rs.getDate("expected_return_date");
                if (currentExpectedDate == null) {
                    // Jeśli nie ma ustalonej daty, użyj daty dzisiejszej + dodatkowe dni
                    currentExpectedDate = Date.valueOf(LocalDate.now());
                }

                // Oblicz nową datę
                LocalDate newExpectedDate = currentExpectedDate.toLocalDate().plusDays(additionalDays);

                // Zaktualizuj datę
                updateStmt.setDate(1, Date.valueOf(newExpectedDate));
                updateStmt.setInt(2, rentalId);

                int affectedRows = updateStmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Wypożyczenie " + rentalId + " przedłużone do: " + newExpectedDate);
                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getDaysUntilReturn(Rental rental) {
        if (rental.getExpectedReturnDate() == null) {
            return -1; // Nieznana data zwrotu
        }

        LocalDate today = LocalDate.now();
        LocalDate expectedReturn = rental.getExpectedReturnDate();

        return (int) today.until(expectedReturn).getDays();
    }

    public boolean isOverdue(Rental rental) {
        return getDaysUntilReturn(rental) < 0;
    }

    private Rental mapResultSetToRental(ResultSet rs) throws SQLException {
        Rental rental = new Rental();
        rental.setId(rs.getInt("id"));
        rental.setUserId(rs.getInt("user_id"));
        rental.setBookId(rs.getInt("book_id"));

        // Bezpieczne odczytywanie dat
        Date rentDate = rs.getDate("rent_date");
        if (rentDate != null) {
            rental.setRentDate(rentDate.toLocalDate());
        }

        Date returnDate = rs.getDate("return_date");
        if (returnDate != null) {
            rental.setReturnDate(returnDate.toLocalDate());
        }

        // Bezpieczne odczytywanie expected_return_date
        try {
            Date expectedReturnDate = rs.getDate("expected_return_date");
            if (expectedReturnDate != null) {
                rental.setExpectedReturnDate(expectedReturnDate.toLocalDate());
            } else {
                // Jeśli brak daty, ustaw domyślną (14 dni od wypożyczenia)
                if (rental.getRentDate() != null) {
                    rental.setExpectedReturnDate(rental.getRentDate().plusDays(14));
                }
            }
        } catch (SQLException e) {
            // Kolumna może nie istnieć w starych rekordach
            if (rental.getRentDate() != null) {
                rental.setExpectedReturnDate(rental.getRentDate().plusDays(14));
            }
        }

        rental.setStatus(rs.getString("status"));

        // Bezpieczne odczytywanie pól książki
        try {
            rental.setBookTitle(rs.getString("book_title"));
        } catch (SQLException e) {
            // Pole może nie istnieć w niektórych zapytaniach
        }

        try {
            rental.setBookAuthor(rs.getString("book_author"));
        } catch (SQLException e) {
            // Pole może nie istnieć w niektórych zapytaniach
        }

        return rental;
    }
}