package services;

import database.DatabaseManager;
import models.Rental;
import models.ExtensionRequest;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RentalService {
    private BookService bookService = new BookService();

    // Domyślny okres wypożyczenia w dniach
    private static final int DEFAULT_RENTAL_PERIOD_DAYS = 14;
    // Maksymalna liczba samodzielnych przedłużeń
    private static final int MAX_SELF_EXTENSIONS = 2;

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
            WHERE r.status = 'ACTIVE' AND r.expected_return_date < DATE('now')
            ORDER BY r.expected_return_date ASC
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
        return rentBook(userId, bookId, DEFAULT_RENTAL_PERIOD_DAYS);
    }

    public boolean rentBook(int userId, int bookId, int rentalPeriodDays) {
        String sql = """
            INSERT INTO rentals (user_id, book_id, rent_date, expected_return_date, status, extension_count) 
            VALUES (?, ?, DATE('now'), DATE('now', '+' || ? || ' days'), 'ACTIVE', 0)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.setInt(3, rentalPeriodDays);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // Oznacz książkę jako wypożyczoną
                bookService.updateBookAvailability(bookId, false);
                LocalDate expectedReturn = LocalDate.now().plusDays(rentalPeriodDays);
                System.out.println("Książka wypożyczona do: " + expectedReturn);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean returnBook(int rentalId) {
        String sql = "UPDATE rentals SET return_date = DATE('now'), status = 'RETURNED' WHERE id = ?";
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
                pstmt.setInt(1, rentalId);

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

    /**
     * Próbuje przedłużyć wypożyczenie. Sprawdza czy użytkownik może to zrobić samodzielnie
     * czy potrzebuje zgody administratora.
     */
    public ExtensionResult extendRental(int rentalId, int additionalDays) {
        // Pobierz informacje o wypożyczeniu
        Rental rental = getRentalById(rentalId);
        if (rental == null) {
            return new ExtensionResult(false, "Nie znaleziono wypożyczenia", false);
        }

        if (rental.canSelfExtend()) {
            // Użytkownik może samodzielnie przedłużyć
            if (performExtension(rentalId, additionalDays)) {
                return new ExtensionResult(true, "Wypożyczenie zostało przedłużone", false);
            } else {
                return new ExtensionResult(false, "Błąd podczas przedłużania wypożyczenia", false);
            }
        } else {
            // Potrzebna zgoda administratora
            if (createExtensionRequest(rentalId, rental.getUserId(), additionalDays)) {
                return new ExtensionResult(false,
                        "Wysłano prośbę o przedłużenie do administratora. " +
                                "Użytkownik już przedłużył wypożyczenie maksymalną liczbę razy (" + MAX_SELF_EXTENSIONS + ").",
                        true);
            } else {
                return new ExtensionResult(false, "Błąd podczas tworzenia prośby o przedłużenie", true);
            }
        }
    }

    /**
     * Administratorska metoda do bezpośredniego przedłużania wypożyczeń
     */
    public boolean adminExtendRental(int rentalId, int additionalDays) {
        return performExtension(rentalId, additionalDays);
    }

    private boolean performExtension(int rentalId, int additionalDays) {
        String selectSQL = "SELECT expected_return_date, extension_count FROM rentals WHERE id = ? AND status = 'ACTIVE'";
        String updateSQL = "UPDATE rentals SET expected_return_date = DATE(expected_return_date, '+' || ? || ' days'), extension_count = extension_count + 1 WHERE id = ? AND status = 'ACTIVE'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
             PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

            // Pobierz obecną datę
            selectStmt.setInt(1, rentalId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String currentExpectedDateStr = rs.getString("expected_return_date");
                int currentExtensionCount = rs.getInt("extension_count");

                if (currentExpectedDateStr == null || currentExpectedDateStr.trim().isEmpty()) {
                    // Jeśli nie ma ustalonej daty, użyj daty dzisiejszej + dodatkowe dni
                    String updateWithTodaySQL = "UPDATE rentals SET expected_return_date = DATE('now', '+' || ? || ' days'), extension_count = extension_count + 1 WHERE id = ? AND status = 'ACTIVE'";
                    try (PreparedStatement todayStmt = conn.prepareStatement(updateWithTodaySQL)) {
                        todayStmt.setInt(1, additionalDays);
                        todayStmt.setInt(2, rentalId);
                        int affectedRows = todayStmt.executeUpdate();
                        if (affectedRows > 0) {
                            LocalDate newExpectedDate = LocalDate.now().plusDays(additionalDays);
                            System.out.println("Wypożyczenie " + rentalId + " przedłużone do: " + newExpectedDate +
                                    " (przedłużenie nr " + (currentExtensionCount + 1) + ")");
                            return true;
                        }
                    }
                } else {
                    // Oblicz nową datę na podstawie obecnej
                    updateStmt.setInt(1, additionalDays);
                    updateStmt.setInt(2, rentalId);

                    int affectedRows = updateStmt.executeUpdate();
                    if (affectedRows > 0) {
                        LocalDate currentExpectedDate = LocalDate.parse(currentExpectedDateStr);
                        LocalDate newExpectedDate = currentExpectedDate.plusDays(additionalDays);
                        System.out.println("Wypożyczenie " + rentalId + " przedłużone do: " + newExpectedDate +
                                " (przedłużenie nr " + (currentExtensionCount + 1) + ")");
                        return true;
                    }
                }
            }

            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Wersja performExtension która używa istniejącego połączenia (dla transakcji)
     */
    private boolean performExtensionWithConnection(Connection conn, int rentalId, int additionalDays) throws SQLException {
        String selectSQL = "SELECT expected_return_date, extension_count FROM rentals WHERE id = ? AND status = 'ACTIVE'";
        String updateSQL = "UPDATE rentals SET expected_return_date = DATE(expected_return_date, '+' || ? || ' days'), extension_count = extension_count + 1 WHERE id = ? AND status = 'ACTIVE'";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
             PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

            // Pobierz obecną datę
            selectStmt.setInt(1, rentalId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String currentExpectedDateStr = rs.getString("expected_return_date");
                int currentExtensionCount = rs.getInt("extension_count");

                if (currentExpectedDateStr == null || currentExpectedDateStr.trim().isEmpty()) {
                    // Jeśli nie ma ustalonej daty, użyj daty dzisiejszej + dodatkowe dni
                    String updateWithTodaySQL = "UPDATE rentals SET expected_return_date = DATE('now', '+' || ? || ' days'), extension_count = extension_count + 1 WHERE id = ? AND status = 'ACTIVE'";
                    try (PreparedStatement todayStmt = conn.prepareStatement(updateWithTodaySQL)) {
                        todayStmt.setInt(1, additionalDays);
                        todayStmt.setInt(2, rentalId);
                        int affectedRows = todayStmt.executeUpdate();
                        if (affectedRows > 0) {
                            LocalDate newExpectedDate = LocalDate.now().plusDays(additionalDays);
                            System.out.println("Wypożyczenie " + rentalId + " przedłużone do: " + newExpectedDate +
                                    " (przedłużenie nr " + (currentExtensionCount + 1) + ")");
                            return true;
                        }
                    }
                } else {
                    // Oblicz nową datę na podstawie obecnej
                    updateStmt.setInt(1, additionalDays);
                    updateStmt.setInt(2, rentalId);

                    int affectedRows = updateStmt.executeUpdate();
                    if (affectedRows > 0) {
                        LocalDate currentExpectedDate = LocalDate.parse(currentExpectedDateStr);
                        LocalDate newExpectedDate = currentExpectedDate.plusDays(additionalDays);
                        System.out.println("Wypożyczenie " + rentalId + " przedłużone do: " + newExpectedDate +
                                " (przedłużenie nr " + (currentExtensionCount + 1) + ")");
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private Rental getRentalById(int rentalId) {
        String sql = "SELECT r.*, b.title as book_title, b.author as book_author FROM rentals r JOIN books b ON r.book_id = b.id WHERE r.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rentalId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToRental(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean createExtensionRequest(int rentalId, int userId, int requestedDays) {
        String sql = """
            INSERT INTO extension_requests (rental_id, user_id, requested_days, request_date, status) 
            VALUES (?, ?, ?, DATE('now'), 'PENDING')
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rentalId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, requestedDays);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ExtensionRequest> getPendingExtensionRequests() {
        List<ExtensionRequest> requests = new ArrayList<>();
        String sql = """
            SELECT er.*, u.username, b.title as book_title, b.author as book_author
            FROM extension_requests er
            JOIN rentals r ON er.rental_id = r.id
            JOIN users u ON er.user_id = u.id
            JOIN books b ON r.book_id = b.id
            WHERE er.status = 'PENDING'
            ORDER BY er.request_date ASC
            """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ExtensionRequest request = mapResultSetToExtensionRequest(rs);
                requests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public boolean approveExtensionRequest(int requestId, int adminId, String comment) {
        return processExtensionRequest(requestId, adminId, "APPROVED", comment);
    }

    public boolean rejectExtensionRequest(int requestId, int adminId, String comment) {
        return processExtensionRequest(requestId, adminId, "REJECTED", comment);
    }

    private boolean processExtensionRequest(int requestId, int adminId, String status, String comment) {
        String selectSQL = "SELECT rental_id, requested_days FROM extension_requests WHERE id = ? AND status = 'PENDING'";
        String updateSQL = """
            UPDATE extension_requests 
            SET status = ?, admin_decision_date = DATE('now'), admin_id = ?, admin_comment = ?
            WHERE id = ?
            """;

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Rozpocznij transakcję

            int rentalId = -1;
            int requestedDays = 0;

            // Pobierz szczegóły prośby
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
                selectStmt.setInt(1, requestId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    rentalId = rs.getInt("rental_id");
                    requestedDays = rs.getInt("requested_days");
                } else {
                    conn.rollback();
                    return false;
                }
            }

            // Zaktualizuj status prośby
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setString(1, status);
                updateStmt.setInt(2, adminId);
                updateStmt.setString(3, comment);
                updateStmt.setInt(4, requestId);
                updateStmt.executeUpdate();
            }

            // Jeśli zatwierdzono, przedłuż wypożyczenie (używając tego samego połączenia)
            if ("APPROVED".equals(status)) {
                if (!performExtensionWithConnection(conn, rentalId, requestedDays)) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit(); // Zatwierdź transakcję
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Przywróć domyślne ustawienie
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
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

        // Bezpieczne odczytywanie dat jako String i parsowanie
        String rentDateStr = rs.getString("rent_date");
        if (rentDateStr != null && !rentDateStr.trim().isEmpty()) {
            try {
                rental.setRentDate(LocalDate.parse(rentDateStr));
            } catch (Exception e) {
                rental.setRentDate(LocalDate.now());
            }
        }

        String returnDateStr = rs.getString("return_date");
        if (returnDateStr != null && !returnDateStr.trim().isEmpty()) {
            try {
                rental.setReturnDate(LocalDate.parse(returnDateStr));
            } catch (Exception e) {
                // Pozostaw null
            }
        }

        // Bezpieczne odczytywanie expected_return_date
        try {
            String expectedReturnDateStr = rs.getString("expected_return_date");
            if (expectedReturnDateStr != null && !expectedReturnDateStr.trim().isEmpty()) {
                try {
                    rental.setExpectedReturnDate(LocalDate.parse(expectedReturnDateStr));
                } catch (Exception e) {
                    // Jeśli parsowanie się nie uda i mamy rent_date, ustaw domyślną
                    if (rental.getRentDate() != null) {
                        rental.setExpectedReturnDate(rental.getRentDate().plusDays(14));
                    }
                }
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

        // Bezpieczne odczytywanie extension_count
        try {
            rental.setExtensionCount(rs.getInt("extension_count"));
        } catch (SQLException e) {
            // Kolumna może nie istnieć w starych rekordach
            rental.setExtensionCount(0);
        }

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

    private ExtensionRequest mapResultSetToExtensionRequest(ResultSet rs) throws SQLException {
        ExtensionRequest request = new ExtensionRequest();
        request.setId(rs.getInt("id"));
        request.setRentalId(rs.getInt("rental_id"));
        request.setUserId(rs.getInt("user_id"));
        request.setRequestedDays(rs.getInt("requested_days"));

        // Bezpieczne odczytywanie request_date
        String requestDateStr = rs.getString("request_date");
        if (requestDateStr != null && !requestDateStr.trim().isEmpty()) {
            try {
                request.setRequestDate(LocalDate.parse(requestDateStr));
            } catch (Exception e) {
                request.setRequestDate(LocalDate.now());
            }
        }

        request.setStatus(rs.getString("status"));

        // Bezpieczne odczytywanie admin_decision_date
        String adminDecisionDateStr = rs.getString("admin_decision_date");
        if (adminDecisionDateStr != null && !adminDecisionDateStr.trim().isEmpty()) {
            try {
                request.setAdminDecisionDate(LocalDate.parse(adminDecisionDateStr));
            } catch (Exception e) {
                // Pozostaw null jeśli parsowanie się nie uda
            }
        }

        try {
            int adminId = rs.getInt("admin_id");
            if (!rs.wasNull()) {
                request.setAdminId(adminId);
            }
        } catch (SQLException e) {
            // Pole może być null
        }

        request.setAdminComment(rs.getString("admin_comment"));
        request.setUsername(rs.getString("username"));
        request.setBookTitle(rs.getString("book_title"));
        request.setBookAuthor(rs.getString("book_author"));

        return request;
    }

    /**
     * Klasa pomocnicza do zwracania wyników operacji przedłużania
     */
    public static class ExtensionResult {
        private final boolean success;
        private final String message;
        private final boolean needsAdminApproval;

        public ExtensionResult(boolean success, String message, boolean needsAdminApproval) {
            this.success = success;
            this.message = message;
            this.needsAdminApproval = needsAdminApproval;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public boolean needsAdminApproval() { return needsAdminApproval; }
    }
}