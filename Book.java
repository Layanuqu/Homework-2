public class Book {
    // Fields to store book data
    private final String title;
    private final String author;
    private final String isbn;
    private final int copies;

    // Constructor
    public Book(String title, String author, String isbn, int copies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.copies = copies;
    }

    // Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getCopies() { return copies; }

    /**
     * Returns a formatted string for console output.
     * Avoids using %5d to prevent '?' in certain terminals.
     */
    @Override
    public String toString() {
        // Print numbers directly without fixed-width formatting
        return String.format("%-30s %-20s %-15s %s", title, author, isbn, copies);
    }
    
    /**
     * Returns the string formatted for file storage.
     * Format: Title:Author:ISBN:copies
     */
    public String toFileString() {
        return title + ":" + author + ":" + isbn + ":" + copies;
    }
}