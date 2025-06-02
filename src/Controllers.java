// controller/LoginController.java
package pl.biblioteka.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pl.biblioteka.model.User;
import pl.biblioteka.service.UserService;
import pl.biblioteka.util.SessionManager;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    
    private UserService userService = new UserService();
    
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Wypełnij wszystkie pola!");
            return;
        }
        
        User user = userService.authenticate(username, password);
        if (user != null) {
            SessionManager.getInstance().setCurrentUser(user);
            navigateToDashboard(user.isAdmin());
        } else {
            showError("Nieprawidłowa nazwa użytkownika lub hasło!");
        }
    }
    
    @FXML
    public void handleRegister(ActionEvent event) {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Błąd podczas otwierania formularza rejestracji");
        }
    }
    
    private void navigateToDashboard(boolean isAdmin) {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            String fxmlFile = isAdmin ? "/fxml/admin-dashboard.fxml" : "/fxml/user-dashboard.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Błąd podczas otwierania panelu głównego");
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

// controller/RegisterController.java
package pl.biblioteka.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pl.biblioteka.model.User;
import pl.biblioteka.service.UserService;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    
    private UserService userService = new UserService();
    
    @FXML
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Walidacja
        if (username.isEmpty() || password.isEmpty()) {
            showError("Nazwa użytkownika i hasło są wymagane!");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Hasła nie są identyczne!");
            return;
        }
        
        if (password.length() < 6) {
            showError("Hasło musi mieć co najmniej 6 znaków!");
            return;
        }
        
        // Rejestracja
        try {
            User user = userService.register(username, password, email);
            if (user != null) {
                navigateToLogin();
            } else {
                showError("Użytkownik o takiej nazwie już istnieje!");
            }
        } catch (Exception e) {
            showError("Błąd podczas rejestracji: " + e.getMessage());
        }
    }
    
    @FXML
    public void handleBack(ActionEvent event) {
        navigateToLogin();
    }
    
    private void navigateToLogin() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Błąd podczas powrotu do logowania");
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

// service/UserService.java
package pl.biblioteka.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import pl.biblioteka.config.DatabaseConfig;
import pl.biblioteka.model.User;
import pl.biblioteka.util.PasswordEncoder;

public class UserService {
    private DatabaseConfig dbConfig = DatabaseConfig.getInstance();
    
    public User authenticate(String username, String password) {
        EntityManager em = dbConfig.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            
            User user = query.getSingleResult();
            if (PasswordEncoder.checkPassword(password, user.getPassword())) {
                return user;
            }
        } catch (NoResultException e) {
            // Użytkownik nie znaleziony
        } finally {
            em.close();
        }
        return null;
    }
    
    public User register(String username, String password, String email) {
        EntityManager em = dbConfig.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        
        try {
            // Sprawdź czy użytkownik już istnieje
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
            query.setParameter("username", username);
            
            if (query.getSingleResult() > 0) {
                return null; // Użytkownik już istnieje
            }
            
            // Stwórz nowego użytkownika
            User user = new User();
            user.setUsername(username);
            user.setPassword(PasswordEncoder.hashPassword(password));
            user.setEmail(email);
            
            transaction.begin();
            em.persist(user);
            transaction.commit();
            
            return user;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}

// util/PasswordEncoder.java
package pl.biblioteka.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordEncoder {
    
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
    
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}

// util/SessionManager.java
package pl.biblioteka.util;

import pl.biblioteka.model.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    
    private SessionManager() {}
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public void logout() {
        this.currentUser = null;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
}