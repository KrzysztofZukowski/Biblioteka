package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.Book;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class GoogleBooksAPI {
    private static final String API_KEY = "AIzaSyDi9-IsQmLGJ039ZXXf8dGcO9KyYRA51WA";
    private static final String BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    public static Book searchBook(String query, boolean isISBN) {
        try {
            // Clean and prepare query
            query = query.trim();

            if (isISBN) {
                // For ISBN, try multiple formats
                query = query.replaceAll("[^0-9X]", "");
                return searchByISBN(query);
            } else {
                // General search by title/author
                return searchByGeneral(query);
            }

        } catch (Exception e) {
            System.err.println("Error searching: " + query);
            e.printStackTrace();
            return null;
        }
    }

    private static Book searchByISBN(String isbn) throws Exception {
        // Try different search formats for ISBN
        Book book = null;

        // Try 1: with isbn: prefix
        book = performSearch("isbn:" + isbn);
        if (book != null && book.getPublisher() != null) {
            return book; // Mamy wydawnictwo, zwróć wynik
        }

        // Try 2: just the number
        Book book2 = performSearch(isbn);
        if (book2 != null) {
            // Jeśli druga próba ma wydawnictwo, a pierwsza nie
            if (book2.getPublisher() != null && (book == null || book.getPublisher() == null)) {
                return book2;
            }
            // W przeciwnym razie zwróć pierwszą próbę (może mieć inne dane)
            return book != null ? book : book2;
        }

        // Try 3: in quotes
        Book book3 = performSearch("\"" + isbn + "\"");
        if (book3 != null) {
            if (book3.getPublisher() != null) {
                return book3;
            }
        }

        // Zwróć najlepszy wynik jaki mamy
        return book != null ? book : (book2 != null ? book2 : book3);
    }

    private static Book searchByGeneral(String query) throws Exception {
        return performSearch(query);
    }

    private static Book performSearch(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = BASE_URL + "?q=" + encodedQuery + "&key=" + API_KEY + "&maxResults=1";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.err.println("API error: " + responseCode);
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return parseBookFromJson(response.toString());
    }

    private static Book parseBookFromJson(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

            int totalItems = root.get("totalItems").getAsInt();
            if (totalItems == 0) {
                return null;
            }

            JsonArray items = root.getAsJsonArray("items");
            JsonObject firstBook = items.get(0).getAsJsonObject();
            JsonObject volumeInfo = firstBook.getAsJsonObject("volumeInfo");

            // Debug: wyświetl całe volumeInfo tylko jeśli potrzebne
            // System.out.println("Volume Info JSON: " + volumeInfo.toString());

            Book book = new Book();

            // Title
            String title = getJsonString(volumeInfo, "title");
            book.setTitle(title);

            // Authors
            JsonArray authors = volumeInfo.getAsJsonArray("authors");
            if (authors != null && authors.size() > 0) {
                StringBuilder authorStr = new StringBuilder();
                for (int i = 0; i < authors.size(); i++) {
                    if (i > 0) authorStr.append(", ");
                    authorStr.append(authors.get(i).getAsString());
                }
                book.setAuthor(authorStr.toString());
            }

            // Publisher - spróbuj różnych pól
            String publisher = getJsonString(volumeInfo, "publisher");

            // Jeśli nie ma standardowego publisher, sprawdź inne możliwe pola
            if (publisher == null || publisher.trim().isEmpty()) {
                // Spróbuj alternaty wymnych nazw pól
                if (volumeInfo.has("publishedBy")) {
                    publisher = getJsonString(volumeInfo, "publishedBy");
                }
                if (publisher == null && volumeInfo.has("imprint")) {
                    publisher = getJsonString(volumeInfo, "imprint");
                }
            }

            book.setPublisher(publisher);

            if (publisher == null) {
                System.out.println("⚠️  Brak danych o wydawnictwie w API - będzie można wpisać ręcznie");
            }

            // Year
            String publishedDate = getJsonString(volumeInfo, "publishedDate");
            if (publishedDate != null && publishedDate.length() >= 4) {
                try {
                    int year = Integer.parseInt(publishedDate.substring(0, 4));
                    book.setYear(year);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            // ISBN
            JsonArray identifiers = volumeInfo.getAsJsonArray("industryIdentifiers");
            if (identifiers != null) {
                String isbn13 = null;
                String isbn10 = null;

                for (JsonElement identifier : identifiers) {
                    JsonObject id = identifier.getAsJsonObject();
                    String type = getJsonString(id, "type");
                    String value = getJsonString(id, "identifier");

                    if ("ISBN_13".equals(type)) {
                        isbn13 = value;
                    } else if ("ISBN_10".equals(type)) {
                        isbn10 = value;
                    }
                }

                String finalIsbn = isbn13 != null ? isbn13 : isbn10;
                book.setIsbn(finalIsbn);
            }

            return book;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getJsonString(JsonObject obj, String field) {
        JsonElement element = obj.get(field);
        return (element != null && !element.isJsonNull()) ? element.getAsString() : null;
    }
}