package gui;

import models.Book;
import models.Rental;
import models.User;
import models.ExtensionRequest;
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
    private DefaultListModel<ExtensionRequest> extensionRequestsListModel;
    private JList<Book> booksList;
    private JList<Rental> rentalsList;
    private JList<ExtensionRequest> extensionRequestsList;

    // Pola filtrów
    private JTextField titleFilterField;
    private JTextField authorFilterField;
    private JTextField publisherFilterField;
    private JTextField yearFromField;
    private JTextField yearToField;
    private JComboBox<String> availabilityFilterCombo;

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
    private JButton approveRequestButton;
    private JButton rejectRequestButton;
    private JButton refreshRequestsButton;
    private JButton applyFiltersButton;
    private JButton clearFiltersButton;

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
        setSize(1100, 750);
        setLocationRelativeTo(null);

        booksListModel = new DefaultListModel<>();
        rentalsListModel = new DefaultListModel<>();
        extensionRequestsListModel = new DefaultListModel<>();
        booksList = new JList<>(booksListModel);
        rentalsList = new JList<>(rentalsListModel);
        extensionRequestsList = new JList<>(extensionRequestsListModel);

        booksList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rentalsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        extensionRequestsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Inicjalizacja pól filtrów
        titleFilterField = new JTextField(10);
        authorFilterField = new JTextField(10);
        publisherFilterField = new JTextField(10);
        yearFromField = new JTextField(6);
        yearToField = new JTextField(6);
        availabilityFilterCombo = new JComboBox<>(new String[]{"Wszystkie", "Dostępne", "Wypożyczone"});

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
        approveRequestButton = new JButton("Zatwierdź");
        rejectRequestButton = new JButton("Odrzuć");
        refreshRequestsButton = new JButton("Odśwież");
        applyFiltersButton = new JButton("Zastosuj filtry");
        clearFiltersButton = new JButton("Wyczyść filtry");

        // Wyłącz przycisk skanowania na razie
//        scanISBNButton.setEnabled(false);
//        scanISBNButton.setToolTipText("Funkcja będzie dostępna wkrótce");

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

        // Zakładka "Zarządzanie książkami" z filtrami
        JPanel booksPanel = new JPanel(new BorderLayout());

        // Panel filtrów
        JPanel filtersPanel = createFiltersPanel();
        booksPanel.add(filtersPanel, BorderLayout.NORTH);

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

        // Zakładka "Prośby o przedłużenie"
        JPanel extensionRequestsPanel = new JPanel(new BorderLayout());
        extensionRequestsPanel.add(new JLabel("Prośby o przedłużenie wypożyczeń:"), BorderLayout.NORTH);
        extensionRequestsPanel.add(new JScrollPane(extensionRequestsList), BorderLayout.CENTER);

        JPanel extensionButtonPanel = new JPanel(new FlowLayout());
        extensionButtonPanel.add(approveRequestButton);
        extensionButtonPanel.add(rejectRequestButton);
        extensionButtonPanel.add(refreshRequestsButton);
        extensionRequestsPanel.add(extensionButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Prośby o przedłużenie", extensionRequestsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createFiltersPanel() {
        JPanel filtersPanel = new JPanel(new BorderLayout());
        filtersPanel.setBorder(BorderFactory.createTitledBorder("Filtry wyszukiwania książek"));

        // Panel z polami filtrów
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Pierwsza linia - Tytuł, Autor, Wydawnictwo
        gbc.gridx = 0; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Tytuł:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(titleFilterField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Autor:"), gbc);
        gbc.gridx = 3;
        fieldsPanel.add(authorFilterField, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Wydawnictwo:"), gbc);
        gbc.gridx = 5;
        fieldsPanel.add(publisherFilterField, gbc);

        // Druga linia - Rok od, Rok do, Dostępność
        gbc.gridx = 0; gbc.gridy = 1;
        fieldsPanel.add(new JLabel("Rok od:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(yearFromField, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        fieldsPanel.add(new JLabel("do:"), gbc);
        gbc.gridx = 3;
        fieldsPanel.add(yearToField, gbc);

        gbc.gridx = 4; gbc.gridy = 1;
        fieldsPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 5;
        fieldsPanel.add(availabilityFilterCombo, gbc);

        filtersPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Panel z przyciskami filtrów
        JPanel filterButtonsPanel = new JPanel(new FlowLayout());
        filterButtonsPanel.add(applyFiltersButton);
        filterButtonsPanel.add(clearFiltersButton);
        filtersPanel.add(filterButtonsPanel, BorderLayout.EAST);

        return filtersPanel;
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
        approveRequestButton.addActionListener(e -> approveExtensionRequest());
        rejectRequestButton.addActionListener(e -> rejectExtensionRequest());
        refreshRequestsButton.addActionListener(e -> loadExtensionRequests());
        applyFiltersButton.addActionListener(e -> applyFilters());
        clearFiltersButton.addActionListener(e -> clearFilters());

        // Enter w polach filtrów
        titleFilterField.addActionListener(e -> applyFilters());
        authorFilterField.addActionListener(e -> applyFilters());
        publisherFilterField.addActionListener(e -> applyFilters());
        yearFromField.addActionListener(e -> applyFilters());
        yearToField.addActionListener(e -> applyFilters());

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

        extensionRequestsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showExtensionRequestDetails();
                }
            }
        });
    }

    private void applyFilters() {
        String titleFilter = titleFilterField.getText().trim();
        String authorFilter = authorFilterField.getText().trim();
        String publisherFilter = publisherFilterField.getText().trim();

        Integer yearFrom = null;
        Integer yearTo = null;
        Boolean availableOnly = null;

        // Parsowanie roku od
        String yearFromText = yearFromField.getText().trim();
        if (!yearFromText.isEmpty()) {
            try {
                yearFrom = Integer.parseInt(yearFromText);
                if (yearFrom < 1000 || yearFrom > 2030) {
                    JOptionPane.showMessageDialog(this,
                            "Rok 'od' musi być między 1000 a 2030",
                            "Błąd filtra",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Nieprawidłowy format roku 'od'",
                        "Błąd filtra",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Parsowanie roku do
        String yearToText = yearToField.getText().trim();
        if (!yearToText.isEmpty()) {
            try {
                yearTo = Integer.parseInt(yearToText);
                if (yearTo < 1000 || yearTo > 2030) {
                    JOptionPane.showMessageDialog(this,
                            "Rok 'do' musi być między 1000 a 2030",
                            "Błąd filtra",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Nieprawidłowy format roku 'do'",
                        "Błąd filtra",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Sprawdź czy rok od nie jest większy niż rok do
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            JOptionPane.showMessageDialog(this,
                    "Rok 'od' nie może być większy niż rok 'do'",
                    "Błąd filtra",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Parsowanie filtru dostępności
        String selectedAvailability = (String) availabilityFilterCombo.getSelectedItem();
        if ("Dostępne".equals(selectedAvailability)) {
            availableOnly = true;
        } else if ("Wypożyczone".equals(selectedAvailability)) {
            availableOnly = false;
        }
        // Dla "Wszystkie" pozostawiamy null

        // Zastosuj filtry
        booksListModel.clear();
        List<Book> filteredBooks = bookService.searchBooksWithFilters(
                titleFilter.isEmpty() ? null : titleFilter,
                authorFilter.isEmpty() ? null : authorFilter,
                publisherFilter.isEmpty() ? null : publisherFilter,
                yearFrom,
                yearTo,
                availableOnly
        );

        for (Book book : filteredBooks) {
            booksListModel.addElement(book);
        }
    }

    private void clearFilters() {
        titleFilterField.setText("");
        authorFilterField.setText("");
        publisherFilterField.setText("");
        yearFromField.setText("");
        yearToField.setText("");
        availabilityFilterCombo.setSelectedIndex(0); // "Wszystkie"
        loadBooks(); // Wczytaj wszystkie książki
    }

    private void loadData() {
        loadBooks();
        loadRentals();
        loadExtensionRequests();
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

    private void loadExtensionRequests() {
        extensionRequestsListModel.clear();
        List<ExtensionRequest> requests = rentalService.getPendingExtensionRequests();

        for (ExtensionRequest request : requests) {
            extensionRequestsListModel.addElement(request);
        }

        // Pokaż powiadomienie o nowych prośbach (tylko przy pierwszym ładowaniu)
        if (!requests.isEmpty() && extensionRequestsListModel.size() == requests.size()) {
            String message = "Uwaga! Oczekuje " + requests.size() + " próśb o przedłużenie wypożyczeń.";
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, message, "Nowe prośby o przedłużenie", JOptionPane.INFORMATION_MESSAGE)
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
                    "Użytkownik: %s\nKsiążka: %s\nAutor: %s\nData wypożyczenia: %s\nOczekiwana data zwrotu: %s\nStatus: %s\nLiczba przedłużeń: %d",
                    selectedRental.getUsername(),
                    selectedRental.getBookTitle(),
                    selectedRental.getBookAuthor(),
                    selectedRental.getRentDate(),
                    expectedDateStr,
                    selectedRental.isOverdue() ? "PRZETERMINOWANA" : "Aktywna",
                    selectedRental.getExtensionCount()
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

    private void showExtensionRequestDetails() {
        ExtensionRequest selectedRequest = extensionRequestsList.getSelectedValue();
        if (selectedRequest != null) {
            String details = String.format(
                    "Użytkownik: %s\nKsiążka: %s\nAutor: %s\nLiczba dni do przedłużenia: %d\nData prośby: %s\nStatus: %s",
                    selectedRequest.getUsername(),
                    selectedRequest.getBookTitle(),
                    selectedRequest.getBookAuthor(),
                    selectedRequest.getRequestedDays(),
                    selectedRequest.getRequestDate(),
                    selectedRequest.getStatus()
            );

            JOptionPane.showMessageDialog(this, details, "Szczegóły prośby o przedłużenie", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void approveExtensionRequest() {
        ExtensionRequest selectedRequest = extensionRequestsList.getSelectedValue();
        if (selectedRequest == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać prośbę do zatwierdzenia!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String comment = JOptionPane.showInputDialog(this,
                "Czy chcesz dodać komentarz do zatwierdzenia?\n(Opcjonalne)",
                "Komentarz administratora",
                JOptionPane.QUESTION_MESSAGE);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Czy na pewno chcesz zatwierdzić przedłużenie o %d dni dla:\n%s - %s?",
                        selectedRequest.getRequestedDays(),
                        selectedRequest.getUsername(),
                        selectedRequest.getBookTitle()),
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (rentalService.approveExtensionRequest(selectedRequest.getId(), currentUser.getId(), comment)) {
                JOptionPane.showMessageDialog(this, "Prośba została zatwierdzona!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                loadExtensionRequests();
                loadRentals(); // Odśwież również listę wypożyczeń
            } else {
                JOptionPane.showMessageDialog(this, "Błąd podczas zatwierdzania prośby!", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void rejectExtensionRequest() {
        ExtensionRequest selectedRequest = extensionRequestsList.getSelectedValue();
        if (selectedRequest == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać prośbę do odrzucenia!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String comment = JOptionPane.showInputDialog(this,
                "Proszę podać powód odrzucenia prośby:",
                "Komentarz administratora",
                JOptionPane.QUESTION_MESSAGE);

        if (comment == null || comment.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Komentarz jest wymagany przy odrzucaniu prośby!", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Czy na pewno chcesz odrzucić przedłużenie dla:\n%s - %s?\n\nPowód: %s",
                        selectedRequest.getUsername(),
                        selectedRequest.getBookTitle(),
                        comment),
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (rentalService.rejectExtensionRequest(selectedRequest.getId(), currentUser.getId(), comment)) {
                JOptionPane.showMessageDialog(this, "Prośba została odrzucona!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                loadExtensionRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Błąd podczas odrzucania prośby!", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
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
        ScanISBNDialog dialog = new ScanISBNDialog(this);
        dialog.setVisible(true);

        if (dialog.wasBookAdded()) {
            Book book = dialog.getFoundBook();
            if (book != null && bookService.addBook(book)) {
                JOptionPane.showMessageDialog(this,
                        "Książka została dodana!",
                        "Sukces",
                        JOptionPane.INFORMATION_MESSAGE);
                loadBooks();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Nie można dodać książki. Może już istnieje w systemie.",
                        "Błąd",
                        JOptionPane.ERROR_MESSAGE);
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

                if (rental.getExtensionCount() > 0) {
                    displayText += " [" + rental.getExtensionCount() + " przedłużeń]";
                }

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