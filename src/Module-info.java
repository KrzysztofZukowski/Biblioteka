// src/main/java/module-info.java
module library.system {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires jbcrypt;
    requires tess4j;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    
    opens pl.biblioteka to javafx.fxml;
    opens pl.biblioteka.controller to javafx.fxml;
    opens pl.biblioteka.model to org.hibernate.orm.core, javafx.base;
    
    exports pl.biblioteka;
    exports pl.biblioteka.controller;
    exports pl.biblioteka.model;
    exports pl.biblioteka.service;
}

/*
INSTRUKCJE URUCHOMIENIA APLIKACJI:

1. Upewnij się, że masz zainstalowane:
   - Java 17 lub nowszą
   - Maven 3.6 lub nowszy
   - (Opcjonalnie) Tesseract OCR dla funkcji rozpoznawania ISBN

2. Struktura katalogów:
   library-system/
   ├── pom.xml
   ├── src/
   │   ├── main/
   │   │   ├── java/
   │   │   │   ├── module-info.java
   │   │   │   └── pl/biblioteka/
   │   │   │       ├── Main.java
   │   │   │       ├── config/
   │   │   │       ├── controller/
   │   │   │       ├── model/
   │   │   │       ├── service/
   │   │   │       └── util/
   │   │   └── resources/
   │   │       ├── META-INF/
   │   │       │   └── persistence.xml
   │   │       ├── fxml/
   │   │       │   ├── login.fxml
   │   │       │   └── register.fxml
   │   │       ├── css/
   │   │       │   └── style.css
   │   │       └── application.properties
   │   └── test/java/

3. Instalacja zależności:
   mvn clean install

4. Uruchomienie aplikacji:
   mvn javafx:run

5. Domyślne dane logowania:
   - Admin: username: admin, password: admin123
   - Możesz też zarejestrować nowego użytkownika

6. Następne kroki:
   - Dodaj widoki dashboard dla użytkownika i admina
   - Zaimplementuj repozytoria dla Book i Rental
   - Dodaj serwisy BookService i RentalService
   - Zaimplementuj funkcjonalność OCR
   - Zintegruj z Google Books API

7. Rozwiązywanie problemów:
   - Jeśli aplikacja nie startuje, sprawdź czy wszystkie pliki FXML są w odpowiednich lokalizacjach
   - Upewnij się, że plik persistence.xml jest w src/main/resources/META-INF/
   - Baza danych SQLite zostanie utworzona automatycznie jako plik library.db
*/