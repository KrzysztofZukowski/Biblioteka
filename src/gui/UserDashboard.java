package gui;

import models.Book;
import models.Rental;
import models.User;
import services.BookService;
import services.RentalService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UserDashboard extends JFrame {
    private User currentUser;
    private BookService bookService = new BookService();
    private RentalService rentalService = new RentalService();

    private DefaultListModel<Rental> rentalsListModel;
    private DefaultListModel<Book> availableBooksListModel;
    private JList<Rental> rentalsList;
    private JList<Book> availableBooksList;

    // Przyciski jako pola klasy
    private JButton logoutButton;
    private JButton returnButton;
    private JButton refreshRentalsButton;
    private JButton rentButton;
    private JButton refreshBooksButton;

    public UserDashboard(User user) {
        this.currentUser = user;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadData();
    }

    private void initializeComponents() {
        setTitle("Panel Użytkownika - " + currentUser.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        rentalsListModel = new DefaultListModel<>();
        availableBooksListModel = new DefaultListModel<>();
        rentalsList = new JList<>(rentalsListModel);
        availableBooksList = new JList<>(availableBooksListModel);

        rentalsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableBooksList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Inicjalizacja przycisków
        logoutButton = new JButton("Wyloguj");
        returnButton = new JButton("Zwróć książkę");
        refreshRentalsButton = new JButton("Odśwież");
        rentButton = new JButton("Wypożycz książkę");
        refreshBooksButton = new JButton("Odśwież");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel górny z informacjami o użytkowniku
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Zalogowany jako: " + currentUser.getUsername()));
        topPanel.add(logoutButton);
        add(topPanel, BorderLayout.NORTH);

        // Panel główny z zakładkami
        JTabbedPane tabbedPane = new JTabbedPane();

        // Zakładka "Moje wypożyczenia"
        JPanel rentalsPanel = new JPanel(new BorderLayout());
        rentalsPanel.add(new JLabel("Moje wypożyczone książki:"), BorderLayout.NORTH);
        rentalsPanel.add(new JScrollPane(rentalsList), BorderLayout.CENTER);

        JPanel rentalsButtonPanel = new JPanel(new FlowLayout());
        rentalsButtonPanel.add(returnButton);
        rentalsButtonPanel.add(refreshRentalsButton);
        rentalsPanel.add(rentalsButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Moje wypożyczenia", rentalsPanel);

        // Zakładka "Dostępne książki"
        JPanel booksPanel = new JPanel(new BorderLayout());
        booksPanel.add(new JLabel("Dostępne książki:"), BorderLayout.NORTH);
        booksPanel.add(new JScrollPane(availableBooksList), BorderLayout.CENTER);

        JPanel booksButtonPanel = new JPanel(new FlowLayout());
        booksButtonPanel.add(rentButton);
        booksButtonPanel.add(refreshBooksButton);
        booksPanel.add(booksButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Dostępne książki", booksPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        // Event handlers dla przycisków
        logoutButton.addActionListener(e -> logout());
        returnButton.addActionListener(e -> returnBook());
        refreshRentalsButton.addActionListener(e -> loadUserRentals());
        rentButton.addActionListener(e -> rentBook());
        refreshBooksButton.addActionListener(e -> loadAvailableBooks());

        // Obsługa podwójnego kliknięcia na listach
        rentalsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    returnBook();
                }
            }
        });

        availableBooksList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    rentBook();
                }
            }
        });
    }

    private void loadData() {
        loadUserRentals();
        loadAvailableBooks();
    }

    private void loadUserRentals() {
        rentalsListModel.clear();
        List<Rental> rentals = rentalService.getUserRentals(currentUser.getId());
        for (Rental rental : rentals) {
            rentalsListModel.addElement(rental);
        }
    }

    private void loadAvailableBooks() {
        availableBooksListModel.clear();
        List<Book> books = bookService.getAvailableBooks();
        for (Book book : books) {
            availableBooksListModel.addElement(book);
        }
    }

    private void returnBook() {
        Rental selectedRental = rentalsList.getSelectedValue();
        if (selectedRental == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać książkę do zwrotu!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Czy na pewno chcesz zwrócić książkę: " + selectedRental.getBookTitle() + "?",
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (rentalService.returnBook(selectedRental.getId())) {
                JOptionPane.showMessageDialog(this, "Książka została zwrócona!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                loadData(); // Odśwież obie listy
            } else {
                JOptionPane.showMessageDialog(this, "Błąd podczas zwracania książki!", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void rentBook() {
        Book selectedBook = availableBooksList.getSelectedValue();
        if (selectedBook == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać książkę do wypożyczenia!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Czy chcesz wypożyczyć książkę: " + selectedBook.getTitle() + "?",
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (rentalService.rentBook(currentUser.getId(), selectedBook.getId())) {
                JOptionPane.showMessageDialog(this, "Książka została wypożyczona!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                loadData(); // Odśwież obie listy
            } else {
                JOptionPane.showMessageDialog(this, "Błąd podczas wypożyczania książki!", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
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
