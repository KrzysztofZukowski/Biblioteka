package models;

import java.time.LocalDate;

public class Rental {
    private int id;
    private int userId;
    private int bookId;
    private LocalDate rentDate;
    private LocalDate returnDate;
    private String status;

    // Pola dodatkowe dla łączenia z innymi tabelami
    private String username;
    private String bookTitle;
    private String bookAuthor;

    // Konstruktor domyślny
    public Rental() {}

    // Konstruktor z parametrami
    public Rental(int userId, int bookId, LocalDate rentDate) {
        this.userId = userId;
        this.bookId = bookId;
        this.rentDate = rentDate;
        this.status = "ACTIVE";
    }

    // Gettery i settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public LocalDate getRentDate() { return rentDate; }
    public void setRentDate(LocalDate rentDate) { this.rentDate = rentDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    @Override
    public String toString() {
        return bookTitle + " - " + bookAuthor + " (wypożyczona: " + rentDate + ")";
    }
}