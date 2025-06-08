package gui;

import models.Book;
import services.BookService;

import javax.swing.*;
import java.awt.*;

public class EditBookDialog extends JDialog {
    private BookService bookService = new BookService();
    private Book bookToEdit;
    private JTextField isbnField;
    private JTextField titleField;
    private JTextField authorField;
    private JTextField publisherField;
    private JTextField yearField;
    private JButton saveButton;
    private JButton cancelButton;
    private boolean bookUpdated = false;

    public EditBookDialog(Frame parent, Book book) {
        super(parent, "Edytuj książkę", true);
        this.bookToEdit = book;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadBookData();
    }

    private void initializeComponents() {
        setSize(400, 320);
        setLocationRelativeTo(getParent());
        setResizable(false);

        isbnField = new JTextField(15);
        titleField = new JTextField(15);
        authorField = new JTextField(15);
        publisherField = new JTextField(15);
        yearField = new JTextField(15);
        saveButton = new JButton("Zapisz zmiany");
        cancelButton = new JButton("Anuluj");
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);

        // Informacja o edytowanej książce
        JLabel headerLabel = new JLabel("Edytuj dane książki:");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 12f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 15, 10);
        add(headerLabel, gbc);

        // Pola formularza
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 10, 5, 10);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("ISBN:"), gbc);
        gbc.gridx = 1;
        add(isbnField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Tytuł:*"), gbc);
        gbc.gridx = 1;
        add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Autor:*"), gbc);
        gbc.gridx = 1;
        add(authorField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(new JLabel("Wydawnictwo:"), gbc);
        gbc.gridx = 1;
        add(publisherField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        add(new JLabel("Rok wydania:"), gbc);
        gbc.gridx = 1;
        add(yearField, gbc);

        // Etykieta informacyjna
        JLabel infoLabel = new JLabel("* Pola wymagane");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 10f));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 15, 10);
        add(infoLabel, gbc);

        // Przyciski
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(buttonPanel, gbc);
    }

    private void setupEventHandlers() {
        saveButton.addActionListener(e -> handleSaveBook());
        cancelButton.addActionListener(e -> dispose());

        // Enter w polach tekstowych
        titleField.addActionListener(e -> handleSaveBook());
        authorField.addActionListener(e -> handleSaveBook());
    }

    private void loadBookData() {
        isbnField.setText(bookToEdit.getIsbn() != null ? bookToEdit.getIsbn() : "");
        titleField.setText(bookToEdit.getTitle());
        authorField.setText(bookToEdit.getAuthor());
        publisherField.setText(bookToEdit.getPublisher() != null ? bookToEdit.getPublisher() : "");
        yearField.setText(bookToEdit.getYear() > 0 ? String.valueOf(bookToEdit.getYear()) : "");
    }

    private void handleSaveBook() {
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

        // Walidacja ISBN (opcjonalna ale jeśli podana to sprawdź format)
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

        // Sprawdź czy coś się zmieniło
        boolean hasChanges = !title.equals(bookToEdit.getTitle()) ||
                !author.equals(bookToEdit.getAuthor()) ||
                !publisher.equals(bookToEdit.getPublisher() != null ? bookToEdit.getPublisher() : "") ||
                !isbn.equals(bookToEdit.getIsbn() != null ? bookToEdit.getIsbn() : "") ||
                year != bookToEdit.getYear();

        if (!hasChanges) {
            JOptionPane.showMessageDialog(this,
                    "Nie wprowadzono żadnych zmian.",
                    "Informacja",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        // Aktualizuj dane książki
        bookToEdit.setIsbn(isbn.isEmpty() ? null : isbn);
        bookToEdit.setTitle(title);
        bookToEdit.setAuthor(author);
        bookToEdit.setPublisher(publisher.isEmpty() ? null : publisher);
        bookToEdit.setYear(year);

        if (bookService.updateBook(bookToEdit)) {
            bookUpdated = true;
            JOptionPane.showMessageDialog(this,
                    "Dane książki zostały pomyślnie zaktualizowane!",
                    "Sukces",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Nie można zaktualizować książki!\n\nMożliwe przyczyny:\n" +
                            "• Książka o takich danych już istnieje w systemie\n" +
                            "• Błąd połączenia z bazą danych\n\n" +
                            "Sprawdź czy inne książki nie mają już takich samych danych.",
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

    public boolean wasBookUpdated() {
        return bookUpdated;
    }
}