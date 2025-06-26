package services;

import database.DatabaseManager;
import models.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setAdmin(rs.getBoolean("is_admin"));

                // Bezpieczne odczytywanie created_at - używamy getString zamiast getDate
                String createdAtStr = rs.getString("created_at");
                if (createdAtStr != null && !createdAtStr.trim().isEmpty()) {
                    try {
                        user.setCreatedAt(LocalDate.parse(createdAtStr));
                    } catch (Exception e) {
                        // Jeśli parsowanie się nie uda, ustaw obecną datę
                        user.setCreatedAt(LocalDate.now());
                    }
                } else {
                    user.setCreatedAt(LocalDate.now());
                }

                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean register(User user) {
        String sql = "INSERT INTO users (username, password, email, is_admin, created_at) VALUES (?, ?, ?, ?, DATE('now'))";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setBoolean(4, user.isAdmin());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setAdmin(rs.getBoolean("is_admin"));

                // Bezpieczne odczytywanie created_at - używamy getString zamiast getDate
                String createdAtStr = rs.getString("created_at");
                if (createdAtStr != null && !createdAtStr.trim().isEmpty()) {
                    try {
                        user.setCreatedAt(LocalDate.parse(createdAtStr));
                    } catch (Exception e) {
                        // Jeśli parsowanie się nie uda, ustaw obecną datę
                        user.setCreatedAt(LocalDate.now());
                    }
                } else {
                    user.setCreatedAt(LocalDate.now());
                }

                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}