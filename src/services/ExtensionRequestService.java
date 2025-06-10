package services;

import database.DatabaseManager;
import models.ExtensionRequest;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExtensionRequestService {

    /**
     * Pobiera wszystkie prośby o przedłużenie dla danego użytkownika
     */
    public List<ExtensionRequest> getUserExtensionRequests(int userId) {
        List<ExtensionRequest> requests = new ArrayList<>();
        String sql = """
            SELECT er.*, r.user_id, b.title as book_title, b.author as book_author
            FROM extension_requests er
            JOIN rentals r ON er.rental_id = r.id
            JOIN books b ON r.book_id = b.id
            WHERE er.user_id = ?
            ORDER BY er.request_date DESC
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ExtensionRequest request = mapResultSetToExtensionRequest(rs);
                requests.add(request);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    /**
     * Pobiera wszystkie oczekujące prośby o przedłużenie
     */
    public List<ExtensionRequest> getPendingRequests() {
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

    /**
     * Tworzy nową prośbę o przedłużenie
     */
    public boolean createExtensionRequest(int rentalId, int userId, int requestedDays) {
        // Sprawdź czy nie ma już oczekującej prośby dla tego wypożyczenia
        if (hasPendingRequestForRental(rentalId)) {
            System.err.println("Już istnieje oczekująca prośba o przedłużenie dla tego wypożyczenia!");
            return false;
        }

        String sql = """
            INSERT INTO extension_requests (rental_id, user_id, requested_days, request_date, status) 
            VALUES (?, ?, ?, ?, 'PENDING')
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rentalId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, requestedDays);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sprawdza czy istnieje już oczekująca prośba dla danego wypożyczenia
     */
    private boolean hasPendingRequestForRental(int rentalId) {
        String sql = "SELECT COUNT(*) FROM extension_requests WHERE rental_id = ? AND status = 'PENDING'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rentalId);
            ResultSet rs = pstmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Przetwarza prośbę o przedłużenie (zatwierdza lub odrzuca)
     */
    public boolean processRequest(int requestId, int adminId, String status, String comment) {
        String sql = """
            UPDATE extension_requests 
            SET status = ?, admin_decision_date = ?, admin_id = ?, admin_comment = ?
            WHERE id = ? AND status = 'PENDING'
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(3, adminId);
            pstmt.setString(4, comment);
            pstmt.setInt(5, requestId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Pobiera szczegóły prośby o przedłużenie
     */
    public ExtensionRequest getRequestById(int requestId) {
        String sql = """
            SELECT er.*, u.username, b.title as book_title, b.author as book_author
            FROM extension_requests er
            JOIN rentals r ON er.rental_id = r.id
            JOIN users u ON er.user_id = u.id
            JOIN books b ON r.book_id = b.id
            WHERE er.id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, requestId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToExtensionRequest(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Pobiera liczbę oczekujących próśb
     */
    public int getPendingRequestsCount() {
        String sql = "SELECT COUNT(*) FROM extension_requests WHERE status = 'PENDING'";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Usuwa stare, przetworzone prośby (starsze niż określona liczba dni)
     */
    public int cleanupOldRequests(int daysOld) {
        String sql = """
            DELETE FROM extension_requests 
            WHERE status != 'PENDING' 
            AND admin_decision_date < ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            pstmt.setTimestamp(1, Timestamp.valueOf(cutoffDate));

            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private ExtensionRequest mapResultSetToExtensionRequest(ResultSet rs) throws SQLException {
        ExtensionRequest request = new ExtensionRequest();
        request.setId(rs.getInt("id"));
        request.setRentalId(rs.getInt("rental_id"));
        request.setUserId(rs.getInt("user_id"));
        request.setRequestedDays(rs.getInt("requested_days"));

        Timestamp requestDate = rs.getTimestamp("request_date");
        if (requestDate != null) {
            request.setRequestDate(requestDate.toLocalDateTime());
        }

        request.setStatus(rs.getString("status"));

        Timestamp adminDecisionDate = rs.getTimestamp("admin_decision_date");
        if (adminDecisionDate != null) {
            request.setAdminDecisionDate(adminDecisionDate.toLocalDateTime());
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

        try {
            request.setUsername(rs.getString("username"));
        } catch (SQLException e) {
            // Pole może nie istnieć w niektórych zapytaniach
        }

        try {
            request.setBookTitle(rs.getString("book_title"));
            request.setBookAuthor(rs.getString("book_author"));
        } catch (SQLException e) {
            // Pola mogą nie istnieć w niektórych zapytaniach
        }

        return request;
    }
}