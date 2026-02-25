import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class LibraryBookTracker {

  // Static counters to track session statistics
  private static int validRecordsCount = 0;
  private static int searchResultsCount = 0;
  private static int booksAddedCount = 0;
  private static int errorCount = 0;

  // File object for the error log
  private static File logFile;

  public static void main(String[] args) {
    try {
      // Check if the user provided at least 2 arguments
      if (args.length < 2) {
        throw new InsufficientArgumentsException("Usage: java LibraryBookTracker <catalog.txt> <operation>");
      }
      String catalogFileName = args[0];
      // Ensure the file extension is .txt
      if (!catalogFileName.endsWith(".txt")) {
        throw new InvalidFileNameException("Catalog file must end with .txt extension.");
      }
      File catalogFile = new File(catalogFileName);
      // Create parent directories and the file itself if they don't exist
      createFileIfNotExists(catalogFile);

      // Setup Log File path (Must be in the same directory as the catalog)
      File parentDir = catalogFile.getAbsoluteFile().getParentFile();
      if (parentDir == null) parentDir = new File("."); // Fallback to current directory
      logFile = new File(parentDir, "errors.log");

      // Read the file and convert valid lines into Book objects
      List<Book> books = loadCatalog(catalogFile);
      
      String operationArg = args[1];
      executeOperation(operationArg, books, catalogFile);

    } catch (InsufficientArgumentsException | InvalidFileNameException e) {
      // Handle specific input configuration errors
      System.out.println("Error: " + e.getMessage());
      errorCount++;
    } catch (Exception e) {
      // Handle any unexpected errors
      System.out.println("An unexpected error occurred: " + e.getMessage());
      errorCount++;
    } finally {
      // Print session statistics
      printStatistics();
      // Mandatory final message
      System.out.println("Thank you for using the Library Book Tracker.");
    }
  }

  /**
   * Creates the catalog file and its parent directories if they do not exist.
   */
  private static void createFileIfNotExists(File file) throws IOException {
    File parent = file.getAbsoluteFile().getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs(); // Create directories
    }
    if (!file.exists()) {
      file.createNewFile(); // Create file
    }
  }

  /**
   * Reads the catalog file line by line.
   * Parses valid lines into Book objects and logs invalid lines.
   */
  private static List<Book> loadCatalog(File file) {
    List<Book> books = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          // Attempt to parse the line
          Book book = parseAndValidateBook(line);
          books.add(book);
          validRecordsCount++;
        } catch (BookCatalogException e) {
          logError("INVALID LINE", line, e);
          errorCount++;
        }
      }
    } catch (IOException e) {
      System.out.println("Error reading catalog file: " + e.getMessage());
      errorCount++;
    }
    return books;
  }

  private static void executeOperation(String arg, List<Book> books, File catalogFile) {
    try {
      // ISBN Search
      if (arg.matches("\\d{13}")) {
        searchByISBN(books, arg);
      }
      // Add Book
      else if (arg.split(":").length == 4) {
        addBook(arg, books, catalogFile);
      }
      // Title Search
      else {
        searchByTitle(books, arg);
      }
    } catch (DuplicateISBNException e) {
      // Handle logic error for duplicate ISBNs
      System.out.println(e.getMessage());
      errorCount++;
    } catch (BookCatalogException e) {
      // Handle validation errors during "Add Book" attempt
      logError("INVALID ARGUMENT", arg, e);
      errorCount++;
      System.out.println("Failed to add book: " + e.getMessage());
    } catch (IOException e) {
      // Handle file writing errors
      System.out.println("Error writing to catalog: " + e.getMessage());
      errorCount++;
    }
  }

  private static void searchByTitle(List<Book> books, String keyword) {
    List<Book> results = new ArrayList<>();
    String lowerKeyword = keyword.toLowerCase();
    for (Book b : books) {
      if (b.getTitle().toLowerCase().contains(lowerKeyword)) {
        results.add(b);
      }
    }
    if (!results.isEmpty()) {
      printHeader();
      for (Book b : results) {
        System.out.println(b);
      }
      searchResultsCount = results.size();
    } else {
      System.out.println("No books found with title containing: " + keyword);
    }
  }

  private static void searchByISBN(List<Book> books, String isbn) throws DuplicateISBNException {
    List<Book> results = new ArrayList<>();
    for (Book b : books) {
      if (b.getIsbn().equals(isbn)) {
        results.add(b);
      }
    }
    if (results.size() > 1) {
      throw new DuplicateISBNException("Duplicate ISBN found in catalog: " + isbn);
    }
    if (results.size() == 1) {
      printHeader();
      System.out.println(results.get(0));
      searchResultsCount = 1;
    } else {
      System.out.println("No book found with ISBN: " + isbn);
    }
  }

  private static void addBook(String record, List<Book> books, File catalogFile)
      throws BookCatalogException, IOException {

    // Validate the new book data
    Book newBook = parseAndValidateBook(record);
    // Add to the in-memory list
    books.add(newBook);
    // Sort list by Title (Alphabetically, ignoring case)
    books.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
    // Rewrite the entire catalog file to maintain sorted order
    try (PrintWriter writer = new PrintWriter(new FileWriter(catalogFile))) {
      for (Book b : books) {
        writer.println(b.toFileString());
      }
    }
    // Print success result
    printHeader();
    System.out.println(newBook);
    booksAddedCount = 1;
  }

  private static Book parseAndValidateBook(String line) throws BookCatalogException {
    String[] parts = line.split(":", -1);
    if (parts.length != 4) {
      throw new MalformedBookEntryException("Incorrect number of fields.");
    }
    // Extract and trim fields
    String title = parts[0].trim();
    String author = parts[1].trim();
    String isbn = parts[2].trim();
    String copiesStr = parts[3].trim();

    // Field Validation: Title
    if (title.isEmpty()) {
      throw new MalformedBookEntryException("Title is empty.");
    }
    // Field Validation: Author
    if (author.isEmpty()) {
      throw new MalformedBookEntryException("Author is empty.");
    }
    // Field Validation: ISBN
    // Must be exactly 13 digits
    if (!isbn.matches("\\d{13}")) {
      throw new InvalidISBNException("ISBN must be exactly 13 digits.");
    }
    // Field Validation: Copies
    // Must be a positive integer
    int copies;
    try {
      copies = Integer.parseInt(copiesStr);
      if (copies <= 0) {
        throw new MalformedBookEntryException("Copies must be a positive integer.");
      }
    } catch (NumberFormatException e) {
      throw new MalformedBookEntryException("Copies is not a valid integer.");
    }
    // Return a valid Book object
    return new Book(title, author, isbn, copies);
  }

  private static void printHeader() {
    System.out.printf("%-30s %-20s %-15s %5s%n", "Title", "Author", "ISBN", "Copies");
    System.out.println(String.join("", Collections.nCopies(72, "-")));
  }

  private static void logError(String errorType, String input, Exception e) {
    try (FileWriter fw = new FileWriter(logFile, true);
         BufferedWriter bw = new BufferedWriter(fw);
         PrintWriter out = new PrintWriter(bw)) {

      // Get current time
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

      // Write formatted log entry
      out.printf("[%s] %s: \"%s\" - %s: %s%n",
              timestamp, errorType, input, e.getClass().getSimpleName(), e.getMessage());

    } catch (IOException logException) {
      System.err.println("CRITICAL: Could not write to error log.");
    }
  }

  private static void printStatistics() {
    System.out.println("\n--- Session Statistics ---");
    System.out.println("Valid records processed: " + validRecordsCount);
    System.out.println("Search results found:    " + searchResultsCount);
    System.out.println("Books added:            " + booksAddedCount);
    System.out.println("Errors encountered:     " + errorCount);
  }
}