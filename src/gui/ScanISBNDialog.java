package gui;

import models.Book;
import util.GoogleBooksAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ScanISBNDialog extends JDialog {
    private JButton selectImageButton;
    private JButton cancelButton;
    private JButton addBookButton;
    private JLabel imagePreviewLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    // Wynik skanowania ISBN
    private JTextField isbnField;
    private JButton searchByISBNButton;

    // Formularz książki
    private JTextField titleField;
    private JTextField authorField;
    private JTextField publisherField;
    private JTextField yearField;

    private Book foundBook = null;
    private boolean bookAdded = false;

    public ScanISBNDialog(Frame parent) {
        super(parent, "Skanuj ISBN z obrazu", true);
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setSize(500, 600);
        setLocationRelativeTo(getParent());
        setResizable(false);

        selectImageButton = new JButton("Wybierz obraz");
        cancelButton = new JButton("Anuluj");
        addBookButton = new JButton("Dodaj książkę");

        imagePreviewLabel = new JLabel("Wybierz obraz do skanowania", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(300, 200));
        imagePreviewLabel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        isbnField = new JTextField(20);
        isbnField.setToolTipText("Możesz edytować ISBN jeśli został błędnie odczytany");
        searchByISBNButton = new JButton("Szukaj");
        searchByISBNButton.setEnabled(false);

        titleField = new JTextField(20);
        authorField = new JTextField(20);
        publisherField = new JTextField(20);
        yearField = new JTextField(10);

        setFormEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel górny - wybór obrazu i podgląd
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Skanowanie obrazu"));

        // Informacja o sposobie działania
        JLabel infoLabel = new JLabel(
                "<html><center>System automatycznie:<br>" +
                        "1. Wyszuka kody kreskowe na obrazie<br>" +
                        "2. Jeśli nie znajdzie - użyje OCR do odczytu tekstu</center></html>",
                SwingConstants.CENTER);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
        topPanel.add(infoLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(selectImageButton);

        JPanel centerImagePanel = new JPanel(new BorderLayout());
        centerImagePanel.add(buttonPanel, BorderLayout.NORTH);
        centerImagePanel.add(imagePreviewLabel, BorderLayout.CENTER);
        topPanel.add(centerImagePanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.SOUTH);
        topPanel.add(statusPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Panel środkowy - wyniki
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Wyniki skanowania"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(new JLabel("Znaleziony ISBN:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(isbnField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(searchByISBNButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Tytuł:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Autor:*"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(authorField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Wydawnictwo:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(publisherField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Rok:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(yearField, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // Panel dolny - przyciski
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(addBookButton);
        bottomPanel.add(cancelButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        selectImageButton.addActionListener(e -> selectAndScanImage());
        cancelButton.addActionListener(e -> dispose());
        addBookButton.addActionListener(e -> addBookToLibrary());
        searchByISBNButton.addActionListener(e -> manualSearchByISBN());

        // Umożliwienie wyszukania po wciśnięciu Enter w polu ISBN
        isbnField.addActionListener(e -> manualSearchByISBN());

        // Włączenie przycisku wyszukania gdy użytkownik wpisuje ISBN
        isbnField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { checkISBNField(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { checkISBNField(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { checkISBNField(); }
        });
    }

    private void checkISBNField() {
        String isbn = isbnField.getText().trim();
        searchByISBNButton.setEnabled(!isbn.isEmpty() && isValidISBNFormat(isbn));
    }

    private boolean isValidISBNFormat(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            return false;
        }

        // Usuń myślniki i spacje
        String cleaned = isbn.replaceAll("[\\s-]", "");

        // Sprawdź różne formaty ISBN
        return cleaned.matches("\\d{10}") ||           // ISBN-10 bez znaku kontrolnego X
                cleaned.matches("\\d{9}[0-9X]") ||      // ISBN-10 ze znakiem kontrolnym
                cleaned.matches("97[89]\\d{10}");       // ISBN-13 (978 lub 979 + 10 cyfr)
    }

    private void selectAndScanImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Obrazy (*.jpg, *.png, *.bmp)", "jpg", "jpeg", "png", "bmp"));

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File imageFile = fileChooser.getSelectedFile();
        scanImageForISBN(imageFile);
    }

    private void scanImageForISBN(File imageFile) {
        // Wyłącz interfejs podczas skanowania
        selectImageButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Ładowanie obrazu...");
        clearForm();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            private BufferedImage image;

            @Override
            protected String doInBackground() throws Exception {
                // Wczytaj obraz
                image = ImageIO.read(imageFile);

                // Wyświetl podgląd w wątku EDT
                SwingUtilities.invokeLater(() -> {
                    displayImagePreview(image);
                    statusLabel.setText("Szukanie kodu kreskowego...");
                });

                // Krok 1: Próba odczytu kodu kreskowego z ZXing
                String barcodeResult = scanBarcode(image);
                if (barcodeResult != null) {
                    SwingUtilities.invokeLater(() ->
                            statusLabel.setText("Znaleziono kod kreskowy, sprawdzanie ISBN..."));
                    return barcodeResult;
                }

                // Krok 2: Jeśli ZXing zawodzi, użyj Tesseract OCR
                SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Kod kreskowy nie znaleziony, używam OCR..."));

                return performOCR(image);
            }

            @Override
            protected void done() {
                try {
                    String isbn = get();
                    if (isbn != null && !isbn.isEmpty()) {
                        isbnField.setText(isbn);
                        searchByISBNButton.setEnabled(true);

                        // Sprawdź czy to rezultat kodu kreskowego czy OCR
                        if (isbn.length() == 13 && (isbn.startsWith("978") || isbn.startsWith("979"))) {
                            statusLabel.setText("Znaleziono kod kreskowy ISBN: " + isbn);
                        } else {
                            statusLabel.setText("OCR znalazł ISBN: " + isbn);
                        }
                        statusLabel.setForeground(new Color(0, 128, 0));

                        // Automatyczne wyszukanie w API
                        searchBookByISBN(isbn);
                    } else {
                        statusLabel.setText("Nie znaleziono ISBN (ani kod kreskowy, ani OCR)");
                        statusLabel.setForeground(Color.RED);
                        setFormEnabled(true);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Błąd podczas skanowania: " + e.getMessage());
                    statusLabel.setForeground(Color.RED);
                    e.printStackTrace();
                } finally {
                    selectImageButton.setEnabled(true);
                    progressBar.setVisible(false);
                }
            }
        };

        worker.execute();
    }

    private String scanBarcode(BufferedImage image) {
        try {
            // Przygotuj obraz dla ZXing
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // Konfiguruj czytnik kodów kreskowych
            MultiFormatReader reader = new MultiFormatReader();

            // Spróbuj odczytać kod kreskowy
            Result result = reader.decode(bitmap);
            String barcodeText = result.getText();
            BarcodeFormat format = result.getBarcodeFormat();

            // Sprawdź czy to format który może zawierać ISBN
            if (format == BarcodeFormat.EAN_13 || format == BarcodeFormat.EAN_8 ||
                    format == BarcodeFormat.CODE_128 || format == BarcodeFormat.CODE_39) {

                // Dla EAN-13 sprawdź czy zaczyna się od 978 lub 979 (książki)
                if (format == BarcodeFormat.EAN_13 && (barcodeText.startsWith("978") || barcodeText.startsWith("979"))) {
                    return barcodeText;
                }

                // Dla innych formatów sprawdź czy wygląda jak ISBN
                if (isValidISBNFormat(barcodeText)) {
                    return barcodeText;
                }
            }

            return null; // Znaleziono kod kreskowy, ale to nie ISBN

        } catch (NotFoundException e) {
            // Nie znaleziono kodu kreskowego - to normalne, przejdź do OCR
            return null;
        } catch (Exception e) {
            // Inny błąd ZXing
            System.err.println("Błąd ZXing: " + e.getMessage());
            return null;
        }
    }

    private void manualSearchByISBN() {
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Proszę wprowadzić kod ISBN!",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isValidISBNFormat(isbn)) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Wprowadzony ISBN może być nieprawidłowy. Czy chcesz kontynuować wyszukiwanie?",
                    "Ostrzeżenie",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Wyczyść stary formularz i wyszukaj ponownie
        clearBookForm();
        searchBookByISBN(isbn);
    }

    private String performOCR(BufferedImage image) throws TesseractException {
        try {
            Tesseract tesseract = new Tesseract();

            // Podstawowa konfiguracja Tesseract
            tesseract.setDatapath("./tessdata");
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(7); // Single text line
            tesseract.setTessVariable("tessedit_char_whitelist", "0123456789X-");

            // Podstawowe przetworzenie obrazu dla lepszej czytelności
            BufferedImage processedImage = preprocessImage(image);

            // Wykonaj OCR
            String text = tesseract.doOCR(processedImage);

            // Wyodrębnij ISBN z tekstu
            return extractISBN(text);

        } catch (Exception e) {
            throw new TesseractException("Błąd OCR: " + e.getMessage());
        }
    }

    private BufferedImage preprocessImage(BufferedImage original) {
        // Skalowanie do wyższej rozdzielczości (300 DPI)
        int scaleFactor = 3;
        int newWidth = original.getWidth() * scaleFactor;
        int newHeight = original.getHeight() * scaleFactor;

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Konwersja do skali szarości z zwiększonym kontrastem
        BufferedImage grayscale = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscale.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();

        return grayscale;
    }

    private String extractISBN(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Usuń wszystkie znaki oprócz cyfr i X
        String cleaned = text.replaceAll("[^0-9X]", "");

        // Szukaj ISBN-13 (978/979 + 10 cyfr)
        Pattern isbn13Pattern = Pattern.compile("97[89]\\d{10}");
        Matcher matcher13 = isbn13Pattern.matcher(cleaned);
        if (matcher13.find()) {
            return matcher13.group();
        }

        // Szukaj ISBN-10 (9 cyfr + cyfra lub X)
        Pattern isbn10Pattern = Pattern.compile("\\d{9}[0-9X]");
        Matcher matcher10 = isbn10Pattern.matcher(cleaned);
        if (matcher10.find()) {
            return matcher10.group();
        }

        return null;
    }

    private void displayImagePreview(BufferedImage image) {
        // Skaluj obraz do podglądu
        int maxWidth = 280;
        int maxHeight = 180;

        double scaleX = (double) maxWidth / image.getWidth();
        double scaleY = (double) maxHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (image.getWidth() * scale);
        int scaledHeight = (int) (image.getHeight() * scale);

        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imagePreviewLabel.setIcon(new ImageIcon(scaledImage));
        imagePreviewLabel.setText("");
    }

    private void searchBookByISBN(String isbn) {
        statusLabel.setText("Wyszukiwanie książki w API...");

        SwingWorker<Book, Void> worker = new SwingWorker<>() {
            @Override
            protected Book doInBackground() throws Exception {
                return GoogleBooksAPI.searchBook(isbn, true);
            }

            @Override
            protected void done() {
                try {
                    Book book = get();
                    if (book != null) {
                        foundBook = book;
                        populateBookForm(book);
                        setFormEnabled(true);

                        // Sprawdź czy wszystkie dane są dostępne
                        boolean hasPublisher = book.getPublisher() != null && !book.getPublisher().trim().isEmpty();

                        if (hasPublisher) {
                            statusLabel.setText("Znaleziono dane książki!");
                        } else {
                            statusLabel.setText("Znaleziono książkę (brak danych o wydawnictwie - można wpisać ręcznie)");
                        }
                        statusLabel.setForeground(new Color(0, 128, 0));
                    } else {
                        statusLabel.setText("Nie znaleziono książki w API");
                        statusLabel.setForeground(Color.ORANGE);
                        setFormEnabled(true);

                        // Pozostaw pole ISBN wypełnione dla ręcznego dodania
                        if (!isbnField.getText().trim().isEmpty()) {
                            statusLabel.setText(statusLabel.getText() + " - można dodać ręcznie");
                        }
                    }
                } catch (Exception e) {
                    statusLabel.setText("Błąd połączenia z API");
                    statusLabel.setForeground(Color.RED);
                    setFormEnabled(true);
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private void populateBookForm(Book book) {
        titleField.setText(book.getTitle() != null ? book.getTitle() : "");
        authorField.setText(book.getAuthor() != null ? book.getAuthor() : "");
        publisherField.setText(book.getPublisher() != null ? book.getPublisher() : "");
        yearField.setText(book.getYear() > 0 ? String.valueOf(book.getYear()) : "");
    }

    private void clearForm() {
        isbnField.setText("");
        clearBookForm();
        searchByISBNButton.setEnabled(false);
    }

    private void clearBookForm() {
        titleField.setText("");
        authorField.setText("");
        publisherField.setText("");
        yearField.setText("");
        foundBook = null;
        setFormEnabled(false);
    }

    private void setFormEnabled(boolean enabled) {
        titleField.setEnabled(enabled);
        authorField.setEnabled(enabled);
        publisherField.setEnabled(enabled);
        yearField.setEnabled(enabled);
        addBookButton.setEnabled(enabled);

        // Przycisk ISBN jest włączony tylko gdy jest tekst w polu ISBN
        if (!enabled) {
            searchByISBNButton.setEnabled(false);
        } else {
            checkISBNField();
        }
    }

    private void addBookToLibrary() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String yearText = yearField.getText().trim();
        String isbn = isbnField.getText().trim();

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
                        "Proszę podać prawidłowy rok (1000-2030)!",
                        "Błąd walidacji",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Utwórz książkę do dodania
        this.foundBook = new Book(isbn.isEmpty() ? null : isbn, title, author, publisher, year);
        this.bookAdded = true;

        JOptionPane.showMessageDialog(this,
                "Książka jest gotowa do dodania do biblioteki!",
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