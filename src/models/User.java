package models;

import java.time.LocalDate;

public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private boolean isAdmin;
    private LocalDate createdAt;  // Zmieniono z String na LocalDate

    // Konstruktor domyślny
    public User() {}

    // Konstruktor z parametrami
    public User(String username, String password, String email, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.isAdmin = isAdmin;
        this.createdAt = LocalDate.now();  // Ustawienie domyślnej daty
    }

    // Gettery i settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return username + (isAdmin ? " (Admin)" : "");
    }
}