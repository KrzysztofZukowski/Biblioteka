package gui;

import models.User;
import services.UserService;

import javax.swing.*;
import java.awt.*;

public class RegisterDialog extends JDialog {
    private UserService userService = new UserService();
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField emailField;
    private JButton registerButton;
    private JButton cancelButton;

    public RegisterDialog(Frame parent) {
        super(parent, "Rejestracja", true);
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setSize(350, 250);
        setLocationRelativeTo(getParent());
        setResizable(false);

        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        emailField = new JTextField(15);
        registerButton = new JButton("Zarejestruj");
        cancelButton = new JButton("Anuluj");
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);

        // Pola formularza
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Nazwa użytkownika:"), gbc);
        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Hasło:"), gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        add(emailField, gbc);

        // Przyciski
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        add(buttonPanel, gbc);
    }

    private void setupEventHandlers() {
        registerButton.addActionListener(e -> handleRegister());
        cancelButton.addActionListener(e -> dispose());
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String email = emailField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nazwa użytkownika i hasło są wymagane!", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = new User(username, password, email, false);
        if (userService.register(user)) {
            JOptionPane.showMessageDialog(this, "Rejestracja zakończona pomyślnie!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Błąd rejestracji! Nazwa użytkownika może być już zajęta.", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }
}