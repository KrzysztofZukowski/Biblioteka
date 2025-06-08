# System Biblioteczny - Instrukcje Uruchomienia

## Wymagania
- **Java**: JDK 8 lub nowszy
- **IDE**: IntelliJ IDEA, Eclipse lub podobne
- **Baza danych**: SQLite (automatycznie dołączane)

## Struktura plików do utworzenia

Utwórz następującą strukturę katalogów w swoim IDE:

```
library-system/
├── src/
│   ├── Main.java
│   ├── database/
│   │   ├── DatabaseManager.java
│   │   └── DatabaseInitializer.java
│   ├── models/
│   │   ├── User.java
│   │   ├── Book.java
│   │   └── Rental.java
│   ├── services/
│   │   ├── UserService.java
│   │   ├── BookService.java
│   │   └── RentalService.java
│   └── gui/
│       ├── LoginFrame.java
│       ├── RegisterDialog.java
│       ├── UserDashboard.java
│       ├── AdminDashboard.java
│       └── AddBookDialog.java
└── lib/
    └── sqlite-jdbc-3.36.0.3.jar (do pobrania)
```

## Krok 1: Pobieranie SQLite JDBC Driver

1. Pobierz sterownik SQLite JDBC z: https://github.com/xerial/sqlite-jdbc/releases
2. Pobierz plik `sqlite-jdbc-3.36.0.3.jar` (lub nowszą wersję)
3. Umieść go w katalogu `lib/` projektu
4. Dodaj do classpath w IDE:
   - **IntelliJ**: File → Project Structure → Libraries → + → Java → wybierz JAR
   - **Eclipse**: Right-click na projekt → Build Path → Add External Archives

## Krok 2: Tworzenie plików

Skopiuj kod z poprzednich artefaktów do odpowiednich plików:

### Pliki główne:
- `Main.java` - główna klasa uruchamiająca
- `DatabaseManager.java` i `DatabaseInitializer.java` - zarządzanie bazą danych

### Modele danych:
- `User.java`, `Book.java`, `Rental.java` - klasy reprezentujące dane

### Serwisy:
- `UserService.java`, `BookService.java`, `RentalService.java` - logika biznesowa

### Interfejs GUI:
- `LoginFrame.java` - okno logowania
- `RegisterDialog.java` - dialog rejestracji
- `UserDashboard.java` - panel użytkownika
- `AdminDashboard.java` - panel administratora
- `AddBookDialog.java` - dialog dodawania książek

## Krok 3: Uruchomienie

1. Upewnij się, że wszystkie pliki są w odpowiednich pakietach
2. Dodaj sterownik SQLite do classpath
3. Uruchom klasę `Main.java`
4. Przy pierwszym uruchomieniu zostanie utworzona baza danych w katalogu `database/`

## Domyślne dane

Po pierwszym uruchomieniu system utworzy:

### Administrator:
- **Login**: admin
- **Hasło**: admin123

### Przykładowe książki:
- Władca Pierścieni - J.R.R. Tolkien
- Wiedźmin: Ostatnie życzenie - Andrzej Sapkowski  
- Harry Potter i Kamień Filozoficzny - J.K. Rowling

## Funkcjonalności

### Panel Użytkownika:
✅ Rejestracja nowego konta  
✅ Logowanie do systemu  
✅ Przeglądanie wypożyczonych książek  
✅ Wypożyczanie dostępnych książek  
✅ Zwracanie książek  

### Panel Administratora:
✅ Logowanie jako admin  
✅ Przeglądanie wszystkich książek  
✅ Dodawanie nowych książek  
✅ Usuwanie książek  
✅ Przeglądanie wszystkich wypożyczeń  
✅ Oznaczanie książek jako zwróconych  
🔄 Skanowanie ISBN ze zdjęcia (do implementacji)  

## Następne kroki (opcjonalne rozszerzenia)

### Faza 1: Ulepszenia GUI
- Dodanie ikon do przycisków
- Poprawa stylu interfejsu
- Dodanie filtrów wyszukiwania

### Faza 2: OCR i API
- Implementacja rozpoznawania ISBN ze zdjęć (Tess4J)
- Integracja z Google Books API
- Automatyczne pobieranie informacji o książkach

### Faza 3: Dodatkowe funkcje
- Generowanie raportów
- Export danych do plików
- Powiadomienia o terminach zwrotu

## Rozwiązywanie problemów

### Problem z bazą danych:
- Sprawdź czy katalog `database/` ma uprawnienia do zapisu
- Upewnij się, że sterownik SQLite jest w classpath

### Problem z GUI:
- Sprawdź czy wszystkie pakiety są prawidłowo zaimportowane
- Upewnij się, że używasz Java 8+

### Problem z kompilacją:
- Sprawdź czy wszystkie klasy są w odpowiednich pakietach
- Upewnij się, że struktura katalogów odpowiada pakietom

## Testowanie aplikacji

1. **Test rejestracji**: Utwórz nowego użytkownika
2. **Test logowania**: Zaloguj się jako użytkownik i admin
3. **Test wypożyczania**: Wypożycz książkę jako użytkownik
4. **Test dodawania książek**: Dodaj nową książkę jako admin
5. **Test zwracania**: Zwróć wypożyczoną książkę

---

**Uwaga**: To jest wersja uproszczona bez funkcji OCR i integracji z API. Te funkcje można dodać później zgodnie z potrzebami.
