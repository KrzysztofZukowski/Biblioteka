package models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Rental {
    private int id;
    private int userId;
    private int bookId;
    private LocalDate rentDate;
    private LocalDate returnDate;
    private LocalDate expectedReturnDate;
    private String status;
    private int extensionCount; // Nowe pole

    // Pola dodatkowe dla łączenia z innymi tabelami
    private String username;
    private String bookTitle;
    private String bookAuthor;

    // Konstruktor domyślny
    public Rental() {
        this.extensionCount = 0;
    }

    // Konstruktor z parametrami
    public Rental(int userId, int bookId, LocalDate rentDate) {
        this.userId = userId;
        this.bookId = bookId;
        this.rentDate = rentDate;
        this.status = "ACTIVE";
        this.extensionCount = 0;
        // Domyślnie 14 dni na zwrot
        this.expectedReturnDate = rentDate.plusDays(14);
    }

    // Konstruktor z okresem wypożyczenia
    public Rental(int userId, int bookId, LocalDate rentDate, int rentalPeriodDays) {
        this.userId = userId;
        this.bookId = bookId;
        this.rentDate = rentDate;
        this.status = "ACTIVE";
        this.extensionCount = 0;
        this.expectedReturnDate = rentDate.plusDays(rentalPeriodDays);
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

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getExtensionCount() { return extensionCount; }
    public void setExtensionCount(int extensionCount) { this.extensionCount = extensionCount; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    // Metody pomocnicze
    public boolean isOverdue() {
        if (expectedReturnDate == null || !"ACTIVE".equals(status)) {
            return false;
        }
        return LocalDate.now().isAfter(expectedReturnDate);
    }

    public long getDaysUntilReturn() {
        if (expectedReturnDate == null || !"ACTIVE".equals(status)) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(expectedReturnDate)) {
            return 0; // Już przeterminowana
        }
        return today.until(expectedReturnDate).getDays();
    }

    public long getDaysOverdue() {
        if (expectedReturnDate == null || !"ACTIVE".equals(status)) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        if (!today.isAfter(expectedReturnDate)) {
            return 0; // Nie jest przeterminowana
        }
        return expectedReturnDate.until(today).getDays();
    }

    public boolean canSelfExtend() {
        return extensionCount < 2; // Można samodzielnie przedłużyć maksymalnie 2 razy
    }

    public boolean needsAdminApproval() {
        return extensionCount >= 2; // Od 3. przedłużenia potrzebna zgoda admina
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(bookTitle).append(" - ").append(bookAuthor);
        sb.append(" (wypożyczona: ").append(rentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        if (expectedReturnDate != null) {
            sb.append(", zwrot do: ").append(expectedReturnDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

            if (isOverdue()) {
                sb.append(" - PRZETERMINOWANA!");
            } else if (getDaysUntilReturn() <= 3) {
                sb.append(" - kończy się wkrótce");
            }
        } else {
            sb.append(", brak ustalonej daty zwrotu");
        }

        if (extensionCount > 0) {
            sb.append(" [Przedłużona ").append(extensionCount).append(" raz(y)]");
        }

        sb.append(")");
        return sb.toString();
    }
}