package gui;

import models.Book;
import util.GoogleBooksAPI;

import javax.swing.*;
import java.awt.*;

public class ScanISBNDialog extends JDialog {
    private JTextField searchField;
    private JRadioButton isbnRadio;
    private JRadioButton generalRadio;
    private JButton searchButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // Form fields
    private JTextField titleField;
    private JTextField authorField;
    private JTextField publisherField;
    private JTextField yearField;
    private JTextField isbnResultField;
    private JButton addButton;

    private Book foundBook = null;
    private boolean bookAdded = false;

    public ScanISBNDialog(Frame parent) {
        super(parent, "Wyszukaj książkę", true);
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setSize(550, 500);
        setLocationRelativeTo(getParent());
        setResizable(false);

        searchField = new JTextField(25);
        isbnRadio = new JRadioButton("Szukaj po ISBN", true);
        generalRadio = new JRadioButton("Szukaj po tytule/autorze");
        ButtonGroup group = new ButtonGroup();
        group.add(isbnRadio);
        group.add(generalRadio);

        searchButton = new JButton("Szukaj");
        cancelButton = new JButton("Anuluj");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusLabel = new JLabel(" ");

        titleField = new JTextField(25);
        authorField = new JTextField(25);
        publisherField = new JTextField(25);
        yearField = new JTextField(10);
        isbnResultField = new JTextField(25);
        addButton = new JButton("Dodaj do biblioteki");

        setFormEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Top panel - Search
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Wyszukiwanie"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        topPanel.add(isbnRadio, gbc);

        gbc.gridy = 1;
        topPanel.add(generalRadio, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        topPanel.add(new JLabel("Szukaj:"), gbc);

        gbc.gridx = 1;
        topPanel.add(searchField, gbc);

        gbc.gridx = 2;
        topPanel.add(searchButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        topPanel.add(progressBar, gbc);

        gbc.gridy = 4;
        topPanel.add(statusLabel, gbc);

        add(topPanel, BorderLayout.NORTH);

        // Center panel - Book details
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Szczegóły książki"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(new JLabel("Tytuł:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Autor:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(authorField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Wydawnictwo:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(publisherField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Rok:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(yearField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("ISBN:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(isbnResultField, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(addButton);
        bottomPanel.add(cancelButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        searchButton.addActionListener(e -> searchBook());
        cancelButton.addActionListener(e -> dispose());
        addButton.addActionListener(e -> addBookToLibrary());
        searchField.addActionListener(e -> searchBook());

        // Update search field hint based on radio selection
        isbnRadio.addActionListener(e -> searchField.setToolTipText("Np. 9788375748116"));
        generalRadio.addActionListener(e -> searchField.setToolTipText("Np. Władca Pierścieni"));
    }

    private void searchBook() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Proszę wprowadzić dane do wyszukania!",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        searchButton.setEnabled(false);
        searchField.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Wyszukiwanie...");
        setFormEnabled(false);
        clearForm();

        SwingWorker<Book, Void> worker = new SwingWorker<>() {
            @Override
            protected Book doInBackground() throws Exception {
                return GoogleBooksAPI.searchBook(query, isbnRadio.isSelected());
            }

            @Override
            protected void done() {
                try {
                    Book book = get();
                    if (book != null) {
                        foundBook = book;
                        populateForm(book);
                        setFormEnabled(true);
                        statusLabel.setText("Książka znaleziona!");
                        statusLabel.setForeground(new Color(0, 128, 0));
                    } else {
                        statusLabel.setText("Nie znaleziono książki");
                        statusLabel.setForeground(Color.RED);
                        setFormEnabled(true);

                        // If ISBN search, keep the ISBN
                        if (isbnRadio.isSelected()) {
                            isbnResultField.setText(query.replaceAll("[^0-9X]", ""));
                        }
                    }
                } catch (Exception e) {
                    statusLabel.setText("Błąd połączenia z API");
                    statusLabel.setForeground(Color.RED);
                    e.printStackTrace();
                } finally {
                    searchButton.setEnabled(true);
                    searchField.setEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                }
            }
        };

        worker.execute();
    }

    private void populateForm(Book book) {
        titleField.setText(book.getTitle() != null ? book.getTitle() : "");
        authorField.setText(book.getAuthor() != null ? book.getAuthor() : "");
        publisherField.setText(book.getPublisher() != null ? book.getPublisher() : "");
        yearField.setText(book.getYear() > 0 ? String.valueOf(book.getYear()) : "");
        isbnResultField.setText(book.getIsbn() != null ? book.getIsbn() : "");
    }

    private void clearForm() {
        titleField.setText("");
        authorField.setText("");
        publisherField.setText("");
        yearField.setText("");
        isbnResultField.setText("");
        foundBook = null;
    }

    private void setFormEnabled(boolean enabled) {
        titleField.setEnabled(enabled);
        authorField.setEnabled(enabled);
        publisherField.setEnabled(enabled);
        yearField.setEnabled(enabled);
        isbnResultField.setEnabled(enabled);
        addButton.setEnabled(enabled);
    }

    private void addBookToLibrary() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String yearText = yearField.getText().trim();
        String isbn = isbnResultField.getText().trim();

        if (title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Tytuł i autor są wymagane!",
                    "Błąd walidacji",
                    JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(this,
                        "Proszę podać prawidłowy rok (1000-2030)!",
                        "Błąd walidacji",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Book bookToAdd = new Book(isbn, title, author, publisher, year);
        this.foundBook = bookToAdd;
        this.bookAdded = true;

        JOptionPane.showMessageDialog(this,
                "Dane książki są gotowe do dodania!",
                "Sukces",
                JOptionPane.INFORMATION_MESSAGE);

        dispose();
    }

    public Book getFoundBook() {
        return foundBook;
    }

    public boolean wasBookAdded() {
        return bookAdded;
    }
}