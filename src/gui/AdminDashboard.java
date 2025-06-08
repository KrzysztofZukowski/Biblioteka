package gui;

import models.Book;
import models.Rental;
import models.User;
import services.BookService;
import services.RentalService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdminDashboard extends JFrame {
    private User currentUser;
    private BookService bookService = new BookService();
    private RentalService rentalService = new RentalService();

    private DefaultListModel<Book> booksListModel;
    private DefaultListModel<Rental> rentalsListModel;
    private JList<Book> booksList;
    private JList<Rental> rentalsList;

    // Przyciski jako pola klasy
    private JButton logoutButton;
    private JButton addBookButton;
    private JButton deleteBookButton;
    private JButton scanISBNButton;
    private JButton refreshBooksButton;
    private JButton forceReturnButton;
    private JButton refreshRentalsButton;

    public AdminDashboard(User user) {
        this.currentUser = user;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadData();
    }

    private void initializeComponents() {
        setTitle("Panel Administratora - " + currentUser.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        booksListModel = new DefaultListModel<>();
        rentalsListModel = new DefaultListModel<>();
        booksList = new JList<>(booksListModel);
        rentalsList = new JList<>(rentalsListModel);

        booksList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rentalsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Inicjalizacja przycisków
        logoutButton = new JButton("Wyloguj");
        addBookButton = new JButton("Dodaj książkę");
        deleteBookButton = new JButton("Usuń książkę");
        scanISBNButton = new JButton("Skanuj ISBN"); // Na później
        refreshBooksButton = new JButton("Odśwież");
        forceReturnButton = new JButton("Oznacz jako zwróconą");
        refreshRentalsButton = new JButton("Odśwież");

        // Wyłącz przycisk skanowania na razie
        scanISBNButton.setEnabled(false);
        scanISBNButton.setToolTipText("Funkcja będzie dostępna wkrótce");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel górny z informacjami o administratorze
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Administrator: " + currentUser.getUsername()));
        topPanel.add(logoutButton);
        add(topPanel, BorderLayout.NORTH);

        // Panel główny z zakładkami
        JTabbedPane tabbedPane = new JTabbedPane();

        // Zakładka "Zarządzanie książkami"
        JPanel booksPanel = new JPanel(new BorderLayout());
        booksPanel.add(new JLabel("Wszystkie książki w systemie:"), BorderLayout.NORTH);
        booksPanel.add(new JScrollPane(booksList), BorderLayout.CENTER);

        JPanel booksButtonPanel = new JPanel(new FlowLayout());
        booksButtonPanel.add(addBookButton);
        booksButtonPanel.add(deleteBookButton);
        booksButtonPanel.add(scanISBNButton);
        booksButtonPanel.add(refreshBooksButton);
        booksPanel.add(booksButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Książki", booksPanel);

        // Zakładka "Wypożyczenia"
        JPanel rentalsPanel = new JPanel(new BorderLayout());
        rentalsPanel.add(new JLabel("Aktywne wypożyczenia:"), BorderLayout.NORTH);
        rentalsPanel.add(new JScrollPane(rentalsList), BorderLayout.CENTER);

        JPanel rentalsButtonPanel = new JPanel(new FlowLayout());
        rentalsButtonPanel.add(forceReturnButton);
        rentalsButtonPanel.add(refreshRentalsButton);
        rentalsPanel.add(rentalsButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Wypożyczenia", rentalsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        // Event handlers dla przycisków
        logoutButton.addActionListener(e -> logout());
        addBookButton.addActionListener(e -> addBook());
        deleteBookButton.addActionListener(e -> deleteBook());
        scanISBNButton.addActionListener(e -> scanISBN()); // Na później
        refreshBooksButton.addActionListener(e -> loadBooks());
        forceReturnButton.addActionListener(e -> forceReturn());
        refreshRentalsButton.addActionListener(e -> loadRentals());

        // Obsługa podwójnego kliknięcia
        booksList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showBookDetails();
                }
            }
        });
    }

    private void loadData() {
        loadBooks();
        loadRentals();
    }

    private void loadBooks() {
        booksListModel.clear();
        List<Book> books = bookService.getAllBooks();
        for (Book book : books) {
            booksListModel.addElement(book);
        }
    }

    private void loadRentals() {
        rentalsListModel.clear();
        List<Rental> rentals = rentalService.getAllActiveRentals();
        for (Rental rental : rentals) {
            String displayText = rental.getUsername() + " - " + rental.toString();
            rental.setBookTitle(displayText); // Hack do wyświetlania
            rentalsListModel.addElement(rental);
        }
    }

    private void addBook() {
        AddBookDialog dialog = new AddBookDialog(this);
        dialog.setVisible(true);
        // Po zamknięciu dialogu odśwież listę
        loadBooks();
    }

    private void deleteBook() {
        Book selectedBook = booksList.getSelectedValue();
        if (selectedBook == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać książkę do usunięcia!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!selectedBook.isAvailable()) {
            JOptionPane.showMessageDialog(this, "Nie można usunąć wypożyczonej książki!", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Czy na pewno chcesz usunąć książkę: " + selectedBook.getTitle() + "?",
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (bookService.deleteBook(selectedBook.getId())) {
                JOptionPane.showMessageDialog(this, "Książka została usunięta!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                loadBooks();
            } else {
                JOptionPane.showMessageDialog(this, "Błąd podczas usuwania książki!", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void forceReturn() {
        Rental selectedRental = rentalsList.getSelectedValue();
        if (selectedRental == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać wypożyczenie!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Czy oznaczyć to wypożyczenie jako zwrócone?",
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (rentalService.returnBook(selectedRental.getId())) {
                JOptionPane.showMessageDialog(this, "Wypożyczenie zostało oznaczone jako zwrócone!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Błąd podczas aktualizacji wypożyczenia!", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showBookDetails() {
        Book selectedBook = booksList.getSelectedValue();
        if (selectedBook != null) {
            String details = String.format(
                    "Tytuł: %s\nAutor: %s\nWydawnictwo: %s\nRok: %d\nISBN: %s\nStatus: %s",
                    selectedBook.getTitle(),
                    selectedBook.getAuthor(),
                    selectedBook.getPublisher(),
                    selectedBook.getYear(),
                    selectedBook.getIsbn(),
                    selectedBook.isAvailable() ? "Dostępna" : "Wypożyczona"
            );
            JOptionPane.showMessageDialog(this, details, "Szczegóły książki", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void scanISBN() {
        // Placeholder dla funkcji skanowania ISBN
        JOptionPane.showMessageDialog(this, "Funkcja skanowania ISBN będzie dodana później", "Informacja", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Czy na pewno chcesz się wylogować?",
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            new LoginFrame().setVisible(true);
        }
    }
}