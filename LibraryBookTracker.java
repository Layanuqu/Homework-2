import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class LibraryBookTracker {

    // Counters for session statistics
    private static int validRecordsCount = 0;
    private static int searchResultsCount = 0;
    private static int booksAddedCount = 0;
    private static int errorCount = 0;

    // Log file reference
    private static File logFile;

    public static void main(String[] args) {

        try {

            // Ensure at least two command-line arguments are provided
            if (args.length < 2)
                throw new InsufficientArgumentsException(
                        "Usage: java LibraryBookTracker <catalog.txt> <operation>");

            // Ensure file has .txt extension
            if (!args[0].endsWith(".txt"))
                throw new InvalidFileNameException(
                        "Catalog file must end with .txt");

            File catalogFile = new File(args[0]);

            // Create file and directories if they do not exist
            createFileIfNotExists(catalogFile);

            // Create error log file in same directory as catalog
            File parentDir = catalogFile.getAbsoluteFile().getParentFile();
            if (parentDir == null) parentDir = new File(".");
            logFile = new File(parentDir, "errors.log");

            // Load valid book records
            List<Book> books = loadCatalog(catalogFile);

            // Execute requested operation
            executeOperation(args[1], books, catalogFile);

        } catch (BookCatalogException e) {
            System.out.println("Error: " + e.getMessage());
            errorCount++;
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            errorCount++;
        } finally {
            // Always print statistics
            printStatistics();
            // Mandatory final message
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    /**
     * Creates the catalog file and its parent directories if needed.
     */
    private static void createFileIfNotExists(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();

        if (!file.exists())
            file.createNewFile();
    }

    /**
     * Reads the catalog file and parses valid book entries.
     * Invalid lines are logged.
     */
    private static List<Book> loadCatalog(File file) {

        List<Book> books = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                try {
                    Book book = parseAndValidateBook(line);
                    books.add(book);
                    validRecordsCount++;
                } catch (BookCatalogException e) {
                    logError("INVALID LINE", line, e);
                    errorCount++;
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading catalog file.");
            errorCount++;
        }

        return books;
    }

    /**
     * Parses a line and validates its structure and fields.
     */
    private static Book parseAndValidateBook(String line)
            throws BookCatalogException {

        String[] parts = line.split(":");

        // Ensure correct number of fields
        if (parts.length != 4)
            throw new MalformedBookEntryException("Invalid field count.");

        String title = parts[0].trim();
        String author = parts[1].trim();
        String isbn = parts[2].trim();
        String copiesStr = parts[3].trim();

        // Validate title and author
        if (title.isEmpty())
throw new MalformedBookEntryException("Title is empty.");

        if (author.isEmpty())
            throw new MalformedBookEntryException("Author is empty.");

        // Validate ISBN
        if (!isbn.matches("\\d{13}"))
            throw new InvalidISBNException("ISBN must be exactly 13 digits.");

        // Validate copies
        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Copies must be an integer.");
        }

        if (copies <= 0)
            throw new MalformedBookEntryException("Copies must be positive.");

        return new Book(title, author, isbn, copies);
    }

    /**
     * Determines which operation to perform.
     */
    private static void executeOperation(String arg,List<Book> books,File catalogFile) throws BookCatalogException {
        if (arg.matches("\\d{13}")) {
            searchByISBN(books, arg);
        }
        else if (arg.split(":").length == 4) {
            addBook(arg, books, catalogFile);
        }
        else {
            searchByTitle(books, arg);
        }
    }

    /**
     * Searches for a book by ISBN.
     */
    private static void searchByISBN(List<Book> books, String isbn)
            throws DuplicateISBNException {

        List<Book> matches = new ArrayList<>();

        for (Book book : books)
            if (book.getIsbn().equals(isbn))
                matches.add(book);

        if (matches.size() > 1)
            throw new DuplicateISBNException("Duplicate ISBN found.");

        printHeader();

        for (Book book : matches) {
            System.out.println(book);
            searchResultsCount++;
        }
    }

    /**
     * Searches for books by title keyword (case-insensitive).
     */
    private static void searchByTitle(List<Book> books, String keyword) {

        printHeader();

        for (Book book : books) {
            if (book.getTitle().toLowerCase().contains(keyword.toLowerCase())) {

                System.out.println(book);
                searchResultsCount++;
            }
        }
    }

    /**
     * Adds a new book to the catalog and keeps it sorted.
     */
    private static void addBook(String arg, List<Book> books, File catalogFile) throws BookCatalogException {

        Book newBook = parseAndValidateBook(arg);

        books.add(newBook);
        booksAddedCount++;

        // Sort alphabetically by title
        books.sort(Comparator.comparing(Book::getTitle));

        // Rewrite the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(catalogFile))) {
            for (Book book : books)
                writer.println(book.toString());
        } catch (IOException e) {
            errorCount++;
        }

        printHeader();
        System.out.println(newBook);
    }

    /**
     * Prints table header.
     */
    private static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s%n",
                "Title", "Author", "ISBN", "Copies");
    }

    /**
     * Logs errors into errors.log file.
     */
    private static void logError(String type, String input, Exception e) {

        try (PrintWriter out =
                     new PrintWriter(new FileWriter(logFile, true))) {

            String timestamp =
                    LocalDateTime.now()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            out.printf("[%s] %s: \"%s\" - %s: %s%n",
                    timestamp,
                    type,
                    input,
                    e.getClass().getSimpleName(),
                    e.getMessage());

        } catch (IOException ignored) {}
    }

    /**
     * Prints session statistics.
     */
private static void printStatistics() {

        System.out.println("\n--- Session Statistics ---");
        System.out.println("Valid records processed: " + validRecordsCount);
        System.out.println("Search results found:    " + searchResultsCount);
        System.out.println("Books added:             " + booksAddedCount);
        System.out.println("Errors encountered:      " + errorCount);
    }
}
