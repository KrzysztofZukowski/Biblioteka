// Main.java
package pl.biblioteka;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.biblioteka.config.DatabaseConfig;

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicjalizacja bazy danych
        DatabaseConfig.getInstance().initializeDatabase();
        
        // Ładowanie głównego widoku
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        primaryStage.setTitle("System Biblioteczny");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

// config/DatabaseConfig.java
package pl.biblioteka.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseConfig {
    private static DatabaseConfig instance;
    private EntityManagerFactory entityManagerFactory;
    private static final String DB_URL = "jdbc:sqlite:library.db";
    
    private DatabaseConfig() {}
    
    public static DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    public void initializeDatabase() {
        try {
            // Tworzenie tabel jeśli nie istnieją
            createTables();
            
            // Inicjalizacja EntityManagerFactory
            entityManagerFactory = Persistence.createEntityManagerFactory("library-pu");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Błąd inicjalizacji bazy danych", e);
        }
    }
    
    private void createTables() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Tabela użytkowników
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username VARCHAR(50) UNIQUE NOT NULL," +
                "password VARCHAR(255) NOT NULL," +
                "email VARCHAR(100)," +
                "is_admin BOOLEAN DEFAULT FALSE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            
            // Tabela książek
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "isbn VARCHAR(13) UNIQUE," +
                "title VARCHAR(255) NOT NULL," +
                "author VARCHAR(255) NOT NULL," +
                "publisher VARCHAR(255)," +
                "year INTEGER," +
                "description TEXT," +
                "cover_url VARCHAR(500)," +
                "available BOOLEAN DEFAULT TRUE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            
            // Tabela wypożyczeń
            stmt.execute("CREATE TABLE IF NOT EXISTS rentals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "book_id INTEGER NOT NULL," +
                "rent_date DATE NOT NULL," +
                "return_date DATE," +
                "status VARCHAR(20) DEFAULT 'ACTIVE'," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (book_id) REFERENCES books(id)" +
                ")");
            
            // Dodaj domyślnego admina jeśli nie istnieje
            stmt.execute("INSERT OR IGNORE INTO users (username, password, is_admin) " +
                "VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', TRUE)");
            // Hasło: admin123
            
            // Dodaj przykładowe książki
            stmt.execute("INSERT OR IGNORE INTO books (isbn, title, author, publisher, year, description, available) VALUES " +
                "('9788324631766', 'Thinking in Java', 'Bruce Eckel', 'Helion', 2006, 'Biblia programowania w Javie', TRUE)," +
                "('9788328334267', 'Clean Code', 'Robert C. Martin', 'Helion', 2010, 'Podręcznik dobrego programisty', TRUE)," +
                "('9788328351790', 'Java. Efektywne programowanie', 'Joshua Bloch', 'Helion', 2018, 'Wydanie III', TRUE)," +
                "('9788328309470', 'Spring w akcji', 'Craig Walls', 'Helion', 2015, 'Wydanie IV', TRUE)," +
                "('9788324681907', 'Algorytmy. Wprowadzenie', 'Thomas H. Cormen', 'Helion', 2013, 'Wydanie III', TRUE)");
        }
    }
    
    public EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }
    
    public void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
}

// model/User.java
package pl.biblioteka.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(length = 100)
    private String email;
    
    @Column(name = "is_admin")
    private boolean isAdmin = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Konstruktory
    public User() {}
    
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    // Gettery i settery
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

// model/Book.java
package pl.biblioteka.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 13, unique = true)
    private String isbn;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String author;
    
    private String publisher;
    
    private Integer year;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "cover_url", length = 500)
    private String coverUrl;
    
    private boolean available = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Konstruktory
    public Book() {}
    
    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
    }
    
    // Gettery i settery
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIsbn() {
        return isbn;
    }
    
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getPublisher() {
        return publisher;
    }
    
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCoverUrl() {
        return coverUrl;
    }
    
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

// model/Rental.java
package pl.biblioteka.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rentals")
public class Rental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @Column(name = "rent_date", nullable = false)
    private LocalDate rentDate;
    
    @Column(name = "return_date")
    private LocalDate returnDate;
    
    @Column(length = 20)
    private String status = "ACTIVE";
    
    // Konstruktory
    public Rental() {}
    
    public Rental(User user, Book book) {
        this.user = user;
        this.book = book;
        this.rentDate = LocalDate.now();
    }
    
    // Gettery i settery
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Book getBook() {
        return book;
    }
    
    public void setBook(Book book) {
        this.book = book;
    }
    
    public LocalDate getRentDate() {
        return rentDate;
    }
    
    public void setRentDate(LocalDate rentDate) {
        this.rentDate = rentDate;
    }
    
    public LocalDate getReturnDate() {
        return returnDate;
    }
    
    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}