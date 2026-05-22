package backend.readers;

import java.nio.file.Path;
import java.util.Locale;


/**
 * Factory for deciding which file reader to use based on the file extension.
 */
public final class FileReaderFactory {

    private FileReaderFactory() {} // No instances

    public static IFileReader forPath(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            return new CsvReader();
        } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return new ExcelReader();
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + path.getFileName());
        }
    }
    
}
