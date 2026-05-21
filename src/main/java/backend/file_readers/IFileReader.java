package backend.file_readers;

import java.util.List;
import java.util.Map;

import backend.data.ContactRecord;

import java.io.IOException;
import java.nio.file.Path;

public interface IFileReader {


    /**
     * Lists the sheets in a file if applicable (e.g., for Excel files).
     * @param filePath the path to the file to read.
     * @return a list of sheet names in source order. CSV implementations return a single synthetic sheet (typically the filename without extension).
     * @throws IOException if an I/O error occurs reading from the file or a malformed input is encountered.
     */
    List<String> listSheets(Path filePath) throws IOException;

    /**
     * Reads contact records from the specified file and sheets.
     * @param filePath the path to the file to read.
     * @param sheetNames the names of the sheets to read.
     * @return a map of sheet names to lists of contact records.
     * @throws IOException if an I/O error occurs reading from the file or a malformed input is encountered.
     */
    Map<String, List<ContactRecord>> read(Path filePath, List<String> sheetNames) throws IOException;
}

