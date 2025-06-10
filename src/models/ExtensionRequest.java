package models;

import java.time.LocalDateTime;

public class ExtensionRequest {
    private int id;
    private int rentalId;
    private int userId;
    private int requestedDays;
    private LocalDateTime requestDate;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDateTime adminDecisionDate;
    private Integer adminId;
    private String adminComment;

    // Pola dodatkowe dla łączenia z innymi tabelami
    private String username;
    private String bookTitle;
    private String bookAuthor;

    // Konstruktor domyślny
    public ExtensionRequest() {}

    // Konstruktor z parametrami
    public ExtensionRequest(int rentalId, int userId, int requestedDays) {
        this.rentalId = rentalId;
        this.userId = userId;
        this.requestedDays = requestedDays;
        this.requestDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Gettery i settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRentalId() { return rentalId; }
    public void setRentalId(int rentalId) { this.rentalId = rentalId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getRequestedDays() { return requestedDays; }
    public void setRequestedDays(int requestedDays) { this.requestedDays = requestedDays; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAdminDecisionDate() { return adminDecisionDate; }
    public void setAdminDecisionDate(LocalDateTime adminDecisionDate) { this.adminDecisionDate = adminDecisionDate; }

    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    // Metody pomocnicze
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%d dni) - %s",
                username, bookTitle, requestedDays, status);
    }
}