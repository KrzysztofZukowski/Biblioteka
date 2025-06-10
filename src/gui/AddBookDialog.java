package gui;

import models.Book;
import services.BookService;

import javax.swing.*;
import java.awt.*;

public class AddBookDialog extends JDialog {
    private BookService bookService = new BookService();
    private JTextField isbnField;
    private JTextField titleField;
    private JTextField authorField;
    private JTextField publisherField;
    private JTextField yearField;
    private boolean bookAdded = false;

    public AddBookDialog(Frame parent) {
        super(parent, "Dodaj nową książkę", true);
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setResizable(false);

        isbnField = new JTextField(15);
        titleField = new JTextField(15);
        authorField = new JTextField(15);
        publisherField = new JTextField(15);
        yearField = new JTextField(15);
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);

        // Pola formularza
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("ISBN:"), gbc);
        gbc.gridx = 1;
        add(isbnField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Tytuł:*"), gbc);
        gbc.gridx = 1;
        add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Autor:*"), gbc);
        gbc.gridx = 1;
        add(authorField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Wydawnictwo:"), gbc);
        gbc.gridx = 1;
        add(publisherField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(new JLabel("Rok wydania:"), gbc);
        gbc.gridx = 1;
        add(yearField, gbc);

        // Etykieta informacyjna
        JLabel infoLabel = new JLabel("* Pola wymagane");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 10f));
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 15, 10);
        add(infoLabel, gbc);

        // Przyciski
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Dodaj");
        JButton cancelButton = new JButton("Anuluj");

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(buttonPanel, gbc);
    }

    private void setupEventHandlers() {
        JPanel buttonPanel = (JPanel) getContentPane().getComponent(11); // Ostatni komponent
        JButton addButton = (JButton) buttonPanel.getComponent(0);
        JButton cancelButton = (JButton) buttonPanel.getComponent(1);

        addButton.addActionListener(e -> handleAddBook());
        cancelButton.addActionListener(e -> dispose());

        // Enter w polach tekstowych
        titleField.addActionListener(e -> handleAddBook());
        authorField.addActionListener(e -> handleAddBook());
    }

    private void handleAddBook() {
        String isbn = isbnField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String yearText = yearField.getText().trim();

        // Walidacja wymaganych pól
        if (title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Tytuł i autor są wymagane!",
                    "Błąd walidacji",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Walidacja roku
        int year = 0;
        if (!yearText.isEmpty()) {
            try {
                year = Integer.parseInt(yearText);
                if (year < 1000 || year > 2030) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Proszę podać prawidłowy rok wydania (1000-2030)!",
                        "Błąd walidacji",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Walidacja ISBN
        if (!isbn.isEmpty() && !isValidISBN(isbn)) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Podany ISBN może być nieprawidłowy. Czy chcesz kontynuować?",
                    "Ostrzeżenie",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Utwórz książkę i spróbuj dodać
        Book book = new Book(isbn.isEmpty() ? null : isbn, title, author, publisher, year);

        if (bookService.addBook(book)) {
            bookAdded = true;
            JOptionPane.showMessageDialog(this,
                    "Książka została pomyślnie dodana do systemu!",
                    "Sukces",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            // BookService już wyświetli szczegóły w konsoli
            JOptionPane.showMessageDialog(this,
                    "Nie można dodać książki!\n\nMożliwe przyczyny:\n" +
                            "• Książka o takich danych już istnieje w systemie\n" +
                            "• Błąd połączenia z bazą danych\n\n" +
                            "Sprawdź czy książka o tym tytule i autorze nie została już dodana.",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isValidISBN(String isbn) {
        // Usuń myślniki i spacje
        isbn = isbn.replaceAll("[-\\s]", "");

        // ISBN-10 lub ISBN-13
        if (isbn.length() != 10 && isbn.length() != 13) {
            return false;
        }

        // Sprawdź czy składa się z cyfr (i opcjonalnie X na końcu dla ISBN-10)
        return isbn.matches("\\d{9}[\\dX]") || isbn.matches("\\d{13}");
    }

    public boolean wasBookAdded() {
        return bookAdded;
    }
}