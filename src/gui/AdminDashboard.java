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
    private JButton editBookButton;
    private JButton deleteBookButton;
    private JButton scanISBNButton;
    private JButton refreshBooksButton;
    private JButton forceReturnButton;
    private JButton refreshRentalsButton;
    private JButton overdueButton;

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
        editBookButton = new JButton("Edytuj książkę");
        deleteBookButton = new JButton("Usuń książkę");
        scanISBNButton = new JButton("Skanuj ISBN"); // Na później
        refreshBooksButton = new JButton("Odśwież");
        forceReturnButton = new JButton("Oznacz jako zwróconą");
        refreshRentalsButton = new JButton("Odśwież");
        overdueButton = new JButton("Pokaż przeterminowane");

        // Wyłącz przycisk skanowania na razie
        scanISBNButton.setEnabled(false);
        scanISBNButton.setToolTipText("Funkcja będzie dostępna wkrótce");

        // Ustaw renderer dla rentals list aby pokazywać kolory
        rentalsList.setCellRenderer(new RentalListCellRenderer());
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
        booksButtonPanel.add(editBookButton);
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
        rentalsButtonPanel.add(overdueButton);
        rentalsButtonPanel.add(refreshRentalsButton);
        rentalsPanel.add(rentalsButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Wypożyczenia", rentalsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        // Event handlers dla przycisków
        logoutButton.addActionListener(e -> logout());
        addBookButton.addActionListener(e -> addBook());
        editBookButton.addActionListener(e -> editBook());
        deleteBookButton.addActionListener(e -> deleteBook());
        scanISBNButton.addActionListener(e -> scanISBN()); // Na później
        refreshBooksButton.addActionListener(e -> loadBooks());
        forceReturnButton.addActionListener(e -> forceReturn());
        refreshRentalsButton.addActionListener(e -> loadRentals());
        overdueButton.addActionListener(e -> showOverdueRentals());

        // Obsługa podwójnego kliknięcia
        booksList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    editBook();
                }
            }
        });

        rentalsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showRentalDetails();
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
        List<Rental> overdueRentals = rentalService.getOverdueRentals();

        for (Rental rental : rentals) {
            rentalsListModel.addElement(rental);
        }

        // Pokaż powiadomienie o przeterminowanych książkach (tylko raz przy starcie)
        if (!overdueRentals.isEmpty() && rentalsListModel.size() == rentals.size()) {
            String message = "Uwaga! Znaleziono " + overdueRentals.size() + " przeterminowanych wypożyczeń.";
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, message, "Przeterminowane wypożyczenia", JOptionPane.WARNING_MESSAGE)
            );
        }
    }

    private void showOverdueRentals() {
        List<Rental> overdueRentals = rentalService.getOverdueRentals();

        if (overdueRentals.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Brak przeterminowanych wypożyczeń!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder message = new StringBuilder("Przeterminowane wypożyczenia:\n\n");
        for (Rental rental : overdueRentals) {
            message.append(String.format("%s - %s - %s (%d dni przeterminowania)\n",
                    rental.getUsername(),
                    rental.getBookTitle(),
                    rental.getBookAuthor(),
                    rental.getDaysOverdue()));
        }

        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "Przeterminowane wypożyczenia", JOptionPane.WARNING_MESSAGE);
    }

    private void showRentalDetails() {
        Rental selectedRental = rentalsList.getSelectedValue();
        if (selectedRental != null) {
            String expectedDateStr = selectedRental.getExpectedReturnDate() != null
                    ? selectedRental.getExpectedReturnDate().toString()
                    : "Brak ustalonej daty";

            String details = String.format(
                    "Użytkownik: %s\nKsiążka: %s\nAutor: %s\nData wypożyczenia: %s\nOczekiwana data zwrotu: %s\nStatus: %s",
                    selectedRental.getUsername(),
                    selectedRental.getBookTitle(),
                    selectedRental.getBookAuthor(),
                    selectedRental.getRentDate(),
                    expectedDateStr,
                    selectedRental.isOverdue() ? "PRZETERMINOWANA" : "Aktywna"
            );

            if (selectedRental.getExpectedReturnDate() != null) {
                if (selectedRental.isOverdue()) {
                    details += "\nDni przeterminowania: " + selectedRental.getDaysOverdue();
                } else {
                    details += "\nDni do zwrotu: " + selectedRental.getDaysUntilReturn();
                }
            } else {
                details += "\nUwaga: Brak ustalonej daty zwrotu!";
            }

            JOptionPane.showMessageDialog(this, details, "Szczegóły wypożyczenia", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addBook() {
        AddBookDialog dialog = new AddBookDialog(this);
        dialog.setVisible(true);

        // Sprawdź czy książka została dodana
        if (dialog.wasBookAdded()) {
            loadBooks(); // Odśwież listę tylko jeśli coś się zmieniło
        }
    }

    private void editBook() {
        Book selectedBook = booksList.getSelectedValue();
        if (selectedBook == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać książkę do edycji!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        EditBookDialog dialog = new EditBookDialog(this, selectedBook);
        dialog.setVisible(true);

        // Sprawdź czy książka została zaktualizowana
        if (dialog.wasBookUpdated()) {
            loadBooks(); // Odśwież listę tylko jeśli coś się zmieniło
        }
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

    // Renderer dla kolorowania przeterminowanych wypożyczeń
    private static class RentalListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Rental rental) {
                // Formatuj tekst do wyświetlenia
                String expectedDateStr = rental.getExpectedReturnDate() != null
                        ? rental.getExpectedReturnDate().toString()
                        : "brak daty";

                String displayText = String.format("%s - %s - %s (wypożyczona: %s, zwrot do: %s)",
                        rental.getUsername(),
                        rental.getBookTitle(),
                        rental.getBookAuthor(),
                        rental.getRentDate(),
                        expectedDateStr
                );

                if (!isSelected) {
                    if (rental.isOverdue()) {
                        displayText += " ⚠️ PRZETERMINOWANA";
                        setBackground(new Color(255, 200, 200)); // Jasny czerwony
                        setForeground(Color.BLACK);
                    } else if (rental.getExpectedReturnDate() != null && rental.getDaysUntilReturn() <= 3) {
                        displayText += " ⏰ Kończy się wkrótce";
                        setBackground(new Color(255, 255, 200)); // Jasny żółty
                        setForeground(Color.BLACK);
                    } else if (rental.getExpectedReturnDate() == null) {
                        displayText += " ❓ Brak daty zwrotu";
                        setBackground(new Color(255, 220, 180)); // Jasny pomarańczowy
                        setForeground(Color.BLACK);
                    } else {
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                }

                setText(displayText);
            }

            return this;
        }
    }
}