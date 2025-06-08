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
        add(new JLabel("Tytuł:"), gbc);
        gbc.gridx = 1;
        add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Autor:"), gbc);
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

        // Przyciski
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Dodaj");
        JButton cancelButton = new JButton("Anuluj");

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        add(buttonPanel, gbc);
    }

    private void setupEventHandlers() {
        JPanel buttonPanel = (JPanel) getContentPane().getComponent(10);
        JButton addButton = (JButton) buttonPanel.getComponent(0);
        JButton cancelButton = (JButton) buttonPanel.getComponent(1);

        addButton.addActionListener(e -> handleAddBook());
        cancelButton.addActionListener(e -> dispose());
    }

    private void handleAddBook() {
        String isbn = isbnField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String yearText = yearField.getText().trim();

        if (title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tytuł i autor są wymagane!", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int year = 0;
        if (!yearText.isEmpty()) {
            try {
                year = Integer.parseInt(yearText);
                if (year < 1000 || year > 2030) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Proszę podać prawidłowy rok wydania!", "Błąd", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Book book = new Book(isbn, title, author, publisher, year);
        if (bookService.addBook(book)) {
            JOptionPane.showMessageDialog(this, "Książka została dodana!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Błąd podczas dodawania książki!", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }
}