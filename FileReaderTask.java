import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;


public class FileReaderTask implements Runnable {

    private String fileName;
    private List<Book> catalog;

    public FileReaderTask(String fileName, List<Book> catalog) {
        this.fileName = fileName;
        this.catalog = catalog;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = br.readLine()) != null) {
                try {
                    // Use the centralized validation method
                    Book book = LibraryBookTracker.parseAndValidateBook(line);
                    catalog.add(book);

                    // Increment valid records count
                    LibraryBookTracker.incrementValidRecords();

                } catch (Exception e) {
                    // Increment error count for invalid line
                    LibraryBookTracker.incrementErrors();

                    // Log the error
                    LibraryBookTracker.logError("INVALID LINE", line, e);
                }
            }

            System.out.println("File reading completed.");

        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            LibraryBookTracker.incrementErrors();
        }
    }
}