import gui.LoginFrame;
import database.DatabaseInitializer;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Inicjalizacja bazy danych
        DatabaseInitializer.initializeDatabase();

        // Uruchomienie GUI
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}