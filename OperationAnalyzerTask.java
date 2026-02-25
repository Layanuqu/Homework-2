import java.io.File;
import java.util.List;

public class OperationAnalyzerTask implements Runnable {

    private String operationArg;
    private List<Book> catalog;
    private File catalogFile;

    public OperationAnalyzerTask(String operationArg, List<Book> catalog, File catalogFile) {
        this.operationArg = operationArg;
        this.catalog = catalog;
        this.catalogFile = catalogFile;
    }

    @Override
    public void run() {
        try {
            // Execute the operation (search by title, search by ISBN, or add new book)
            LibraryBookTracker.executeOperation(operationArg, catalog, catalogFile);
            System.out.println("Operation completed successfully.");
        } catch (Exception e) {
            // Increment error counter if any exception occurs
            LibraryBookTracker.incrementErrors();
            System.out.println("Operation error: " + e.getMessage());
        }
    }
}