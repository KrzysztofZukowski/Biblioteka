package gui;

import models.Book;
import util.GoogleBooksAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ScanISBNDialog extends JDialog {
    private JTextField searchField;
    private JRadioButton isbnRadio;
    private JRadioButton generalRadio;
    private JButton searchButton;
    private JButton scanImageButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel imageLabel;

    // Form fields
    private JTextField titleField;
    private JTextField authorField;
    private JTextField publisherField;
    private JTextField yearField;
    private JTextField isbnResultField;
    private JButton addButton;

    private Book foundBook = null;
    private boolean bookAdded = false;
    private BufferedImage currentImage = null;

    public ScanISBNDialog(Frame parent) {
        super(parent, "Wyszukaj/Skanuj książkę", true);
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setSize(700, 650);
        setLocationRelativeTo(getParent());
        setResizable(false);

        searchField = new JTextField(25);
        isbnRadio = new JRadioButton("Szukaj po ISBN", true);
        generalRadio = new JRadioButton("Szukaj po tytule/autorze");
        ButtonGroup group = new ButtonGroup();
        group.add(isbnRadio);
        group.add(generalRadio);

        searchButton = new JButton("Szukaj");
        scanImageButton = new JButton("Skanuj obraz ISBN");
        cancelButton = new JButton("Anuluj");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusLabel = new JLabel(" ");

        imageLabel = new JLabel("Wybierz obraz do skanowania", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(300, 150));
        imageLabel.setBorder(BorderFactory.createEtchedBorder());

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

        // Top panel - Search and OCR
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Wyszukiwanie / Skanowanie"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Search section
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        topPanel.add(isbnRadio, gbc);

        gbc.gridy = 1;
        topPanel.add(generalRadio, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        topPanel.add(new JLabel("Szukaj:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(searchField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        topPanel.add(searchButton, gbc);

        // OCR section
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        topPanel.add(new JLabel("Lub skanuj:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        topPanel.add(scanImageButton, gbc);

        // Image preview
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.BOTH;
        topPanel.add(imageLabel, gbc);

        // Progress and status
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(progressBar, gbc);

        gbc.gridy = 6;
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
        scanImageButton.addActionListener(e -> scanImage());
        cancelButton.addActionListener(e -> dispose());
        addButton.addActionListener(e -> addBookToLibrary());
        searchField.addActionListener(e -> searchBook());

        // Update search field hint based on radio selection
        isbnRadio.addActionListener(e -> searchField.setToolTipText("Np. 9788375748116"));
        generalRadio.addActionListener(e -> searchField.setToolTipText("Np. Władca Pierścieni"));
    }

    private void scanImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Pliki obrazów (*.jpg, *.png, *.bmp, *.gif)",
                "jpg", "jpeg", "png", "bmp", "gif"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File imageFile = chooser.getSelectedFile();

        // Disable UI
        scanImageButton.setEnabled(false);
        searchButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Ładowanie i przetwarzanie obrazu...");
        setFormEnabled(false);
        clearForm();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // Load and display image
                BufferedImage image = ImageIO.read(imageFile);
                currentImage = image;

                SwingUtilities.invokeLater(() -> {
                    displayImage(image);
                    statusLabel.setText("Rozpoznawanie tekstu...");
                });

                // Perform OCR
                return performOCR(image);
            }

            @Override
            protected void done() {
                try {
                    String isbn = get();
                    if (isbn != null && !isbn.isEmpty()) {
                        statusLabel.setText("Znaleziono ISBN: " + isbn);
                        statusLabel.setForeground(new Color(0, 128, 0));

                        // Set ISBN in search field and search automatically
                        searchField.setText(isbn);
                        isbnRadio.setSelected(true);

                        // Auto-search for the book
                        searchBookWithISBN(isbn);
                    } else {
                        statusLabel.setText("Nie znaleziono ISBN na obrazie");
                        statusLabel.setForeground(Color.RED);
                        setFormEnabled(true);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Błąd podczas skanowania: " + e.getMessage());
                    statusLabel.setForeground(Color.RED);
                    e.printStackTrace();
                } finally {
                    scanImageButton.setEnabled(true);
                    searchButton.setEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                }
            }
        };

        worker.execute();
    }

    private String performOCR(BufferedImage image) throws TesseractException {
        Tesseract tesseract = new Tesseract();

        try {
            // Configure Tesseract
            tesseract.setDatapath("./tessdata");
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(7);  // Single text line
            tesseract.setTessVariable("tessedit_char_whitelist", "0123456789- X");
            tesseract.setTessVariable("user_defined_dpi", "300");

            // Try OCR on original image first
            String text = tesseract.doOCR(image);
            String isbn = extractISBN(text);

            if (isbn != null) {
                return isbn;
            }

            // If no ISBN found, try with enhanced image
            BufferedImage enhanced = enhanceImage(image);
            String enhancedText = tesseract.doOCR(enhanced);
            return extractISBN(enhancedText);

        } catch (Exception e) {
            // Fallback if tessdata not found - show helpful message
            throw new TesseractException("Nie można uruchomić OCR. " +
                    "Upewnij się, że folder 'tessdata' znajduje się w katalogu aplikacji " +
                    "i zawiera pliki językowe Tesseract.");
        }
    }

    private BufferedImage enhanceImage(BufferedImage original) {
        // Create enhanced copy
        BufferedImage enhanced = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = enhanced.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // Increase contrast
        float[] scales = {1.3f, 1.3f, 1.3f};
        float[] offsets = {-20f, -20f, -20f};
        RescaleOp rescaleOp = new RescaleOp(scales, offsets, null);
        enhanced = rescaleOp.filter(enhanced, null);

        // Sharpen image
        float[] sharpenMatrix = {
                0, -0.5f, 0,
                -0.5f, 3f, -0.5f,
                0, -0.5f, 0
        };
        ConvolveOp sharpenOp = new ConvolveOp(new Kernel(3, 3, sharpenMatrix));
        enhanced = sharpenOp.filter(enhanced, null);

        return enhanced;
    }

    private void displayImage(BufferedImage image) {
        int maxDisplayWidth = 280;
        int maxDisplayHeight = 140;

        double scaleFactor = Math.min(
                (double)maxDisplayWidth / image.getWidth(),
                (double)maxDisplayHeight / image.getHeight());

        int scaledWidth = (int)(image.getWidth() * scaleFactor);
        int scaledHeight = (int)(image.getHeight() * scaleFactor);

        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setText("");
    }

    private String extractISBN(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Remove all non-digit characters except X (for ISBN-10)
        String cleaned = text.replaceAll("[^0-9X]", "");

        // First try to find ISBN-13 (978/979 + 10 digits)
        Pattern pattern13 = Pattern.compile("97[89]\\d{10}");
        Matcher matcher13 = pattern13.matcher(cleaned);

        if (matcher13.find()) {
            return matcher13.group();
        }

        // If no ISBN-13 found, try ISBN-10 (9 digits + digit or X)
        Pattern pattern10 = Pattern.compile("\\d{9}[0-9X]");
        Matcher matcher10 = pattern10.matcher(cleaned);

        if (matcher10.find()) {
            return matcher10.group();
        }

        return null;
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

        if (isbnRadio.isSelected()) {
            searchBookWithISBN(query);
        } else {
            searchBookGeneral(query);
        }
    }

    private void searchBookWithISBN(String isbn) {
        performBookSearch(isbn, true);
    }

    private void searchBookGeneral(String query) {
        performBookSearch(query, false);
    }

    private void performBookSearch(String query, boolean isISBN) {
        searchButton.setEnabled(false);
        scanImageButton.setEnabled(false);
        searchField.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Wyszukiwanie...");
        setFormEnabled(false);
        clearForm();

        SwingWorker<Book, Void> worker = new SwingWorker<>() {
            @Override
            protected Book doInBackground() throws Exception {
                return GoogleBooksAPI.searchBook(query, isISBN);
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
                        if (isISBN) {
                            isbnResultField.setText(query.replaceAll("[^0-9X]", ""));
                        }
                    }
                } catch (Exception e) {
                    statusLabel.setText("Błąd połączenia z API");
                    statusLabel.setForeground(Color.RED);
                    e.printStackTrace();
                } finally {
                    searchButton.setEnabled(true);
                    scanImageButton.setEnabled(true);
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

        Book bookToAdd = new Book(isbn.isEmpty() ? null : isbn, title, author, publisher, year);
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