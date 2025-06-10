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
    private JButton extendRentalButton;

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
        setSize(900, 700);
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
        extendRentalButton = new JButton("Przedłuż wypożyczenie");

        // Ustawienie kolorów dla przeterminowanych książek
        rentalsList.setCellRenderer(new RentalListCellRenderer());
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

        // Panel z informacjami o wypożyczeniach
        JPanel rentalsInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel rentalsInfoLabel = new JLabel("Moje wypożyczone książki:");
        rentalsInfoPanel.add(rentalsInfoLabel);
        rentalsPanel.add(rentalsInfoPanel, BorderLayout.NORTH);

        rentalsPanel.add(new JScrollPane(rentalsList), BorderLayout.CENTER);

        JPanel rentalsButtonPanel = new JPanel(new FlowLayout());
        rentalsButtonPanel.add(returnButton);
        rentalsButtonPanel.add(extendRentalButton);
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
        extendRentalButton.addActionListener(e -> extendRental());

        // Obsługa podwójnego kliknięcia tylko dla zwrotu książek
        rentalsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showRentalDetails();
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

        // Liczenie przeterminowanych
        int overdueCount = 0;
        for (Rental rental : rentals) {
            if (rental.isOverdue()) {
                overdueCount++;
            }
            rentalsListModel.addElement(rental);
        }

        // Aktualizacja informacji w zakładce
        Component[] components = ((JPanel)((JTabbedPane)getContentPane().getComponent(1)).getComponent(0)).getComponents();
        JPanel infoPanel = (JPanel) components[0];
        JLabel infoLabel = (JLabel) infoPanel.getComponent(0);

        String infoText = "Moje wypożyczone książki: " + rentals.size();
        if (overdueCount > 0) {
            infoText += " (Przeterminowane: " + overdueCount + ")";
        }
        infoLabel.setText(infoText);

        if (overdueCount > 0) {
            infoLabel.setForeground(Color.RED);
        } else {
            infoLabel.setForeground(Color.BLACK);
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
                "Czy chcesz wypożyczyć książkę: " + selectedBook.getTitle() + "?\n" +
                        "Standardowy okres wypożyczenia: 14 dni",
                "Potwierdzenie",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (rentalService.rentBook(currentUser.getId(), selectedBook.getId())) {
                JOptionPane.showMessageDialog(this, "Książka została wypożyczona na 14 dni!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                loadData(); // Odśwież obie listy
            } else {
                JOptionPane.showMessageDialog(this, "Błąd podczas wypożyczania książki!", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void extendRental() {
        Rental selectedRental = rentalsList.getSelectedValue();
        if (selectedRental == null) {
            JOptionPane.showMessageDialog(this, "Proszę wybrać wypożyczenie do przedłużenia!", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Sprawdź status przedłużeń i pokaż odpowiedni komunikat
        String extensionInfo = getExtensionInfoMessage(selectedRental);

        String[] options = {"7 dni", "14 dni", "30 dni"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "O ile dni chcesz przedłużyć wypożyczenie książki:\n" +
                        selectedRental.getBookTitle() + "?\n\n" + extensionInfo,
                "Przedłuż wypożyczenie",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice >= 0) {
            int days = switch (choice) {
                case 0 -> 7;
                case 1 -> 14;
                case 2 -> 30;
                default -> 0;
            };

            RentalService.ExtensionResult result = rentalService.extendRental(selectedRental.getId(), days);

            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        result.getMessage(),
                        "Sukces",
                        JOptionPane.INFORMATION_MESSAGE);
                loadUserRentals(); // Odśwież listę wypożyczeń
            } else {
                if (result.needsAdminApproval()) {
                    JOptionPane.showMessageDialog(this,
                            result.getMessage(),
                            "Prośba wysłana",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            result.getMessage(),
                            "Błąd",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private String getExtensionInfoMessage(Rental rental) {
        if (rental.canSelfExtend()) {
            int remainingExtensions = 2 - rental.getExtensionCount();
            return String.format("Przedłużeń użytych: %d/2\nPozostało przedłużeń samodzielnych: %d",
                    rental.getExtensionCount(), remainingExtensions);
        } else {
            return String.format("Przedłużeń użytych: %d/2\n⚠️ UWAGA: Kolejne przedłużenia wymagają zgody administratora!",
                    rental.getExtensionCount());
        }
    }

    private void showRentalDetails() {
        Rental selectedRental = rentalsList.getSelectedValue();
        if (selectedRental != null) {
            String details = String.format(
                    "Książka: %s\nAutor: %s\nData wypożyczenia: %s\nOczekiwana data zwrotu: %s\nStatus: %s\nLiczba przedłużeń: %d",
                    selectedRental.getBookTitle(),
                    selectedRental.getBookAuthor(),
                    selectedRental.getRentDate(),
                    selectedRental.getExpectedReturnDate(),
                    selectedRental.isOverdue() ? "PRZETERMINOWANA" : "Aktywna",
                    selectedRental.getExtensionCount()
            );

            if (selectedRental.isOverdue()) {
                details += "\nDni przeterminowania: " + selectedRental.getDaysOverdue();
            } else {
                details += "\nDni do zwrotu: " + selectedRental.getDaysUntilReturn();
            }

            // Dodaj informację o możliwości przedłużenia
            if (selectedRental.canSelfExtend()) {
                int remainingExtensions = 2 - selectedRental.getExtensionCount();
                details += "\n\nMożesz jeszcze " + remainingExtensions + " razy samodzielnie przedłużyć to wypożyczenie.";
            } else {
                details += "\n\n⚠️ Kolejne przedłużenia tego wypożyczenia wymagają zgody administratora.";
            }

            JOptionPane.showMessageDialog(this, details, "Szczegóły wypożyczenia", JOptionPane.INFORMATION_MESSAGE);
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
                // Dodaj informacje o przedłużeniach do wyświetlanego tekstu
                String displayText = rental.toString();

                if (!isSelected) {
                    if (rental.isOverdue()) {
                        setBackground(new Color(255, 200, 200)); // Jasny czerwony
                        setForeground(Color.BLACK);
                    } else if (rental.getDaysUntilReturn() <= 3) {
                        setBackground(new Color(255, 255, 200)); // Jasny żółty
                        setForeground(Color.BLACK);
                    } else if (!rental.canSelfExtend()) {
                        setBackground(new Color(255, 220, 180)); // Jasny pomarańczowy - wymaga zgody admina
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