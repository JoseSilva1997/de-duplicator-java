package backend.file_readers;

import backend.data.ContactField;
import backend.data.ContactRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvReader implements IFileReader {


    @Override
    // CSV files do not have sheets, so we return a single synthetic sheet name based on the filename.
    public List<String> listSheets(Path filePath) throws IOException {
        return List.of(syntheticSheetName(filePath));
    }

    @Override
    public Map<String, List<ContactRecord>> read(Path filePath, List<String> sheetNames) throws IOException {
        String sheetName = syntheticSheetName(filePath);

        List<ContactRecord> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
                    .parse(reader)) {
        
                Iterator<CSVRecord> it = parser.iterator();
                if (!it.hasNext()) return Map.of(sheetName, records); // Empty file -> return empty list, not an error

                // First row = header
                CSVRecord headerRow = it.next();
                List<String> headerCells = new ArrayList<>();
                for (String cell : headerRow) headerCells.add(cell);
                HeaderResolver resolver = new HeaderResolver(headerCells);
                
                // Remaining rows = data
                while (it.hasNext()) {
                    CSVRecord row = it.next();
                    ContactRecord record = buildRecord(row, resolver);
                    if (record != null) records.add(record);
                }
        }
        return Map.of(sheetName, records);
    }

    private static ContactRecord buildRecord(CSVRecord row, HeaderResolver resolver) {
        String firstName = cell(row, resolver.indexOf(ContactField.FIRST_NAME));
        String lastName = cell(row, resolver.indexOf(ContactField.LAST_NAME));
        String fullName = cell(row, resolver.indexOf(ContactField.FULL_NAME));
        String email = cell(row, resolver.indexOf(ContactField.EMAIL));
        String company = cell(row, resolver.indexOf(ContactField.COMPANY));
        String jobTitle = cell(row, resolver.indexOf(ContactField.JOB_TITLE));

        ContactRecord record = new ContactRecord(firstName, lastName, fullName, email, company, jobTitle);
        return record.isEmpty() ? null : record; // Skip empty records
    }


    private static String cell(CSVRecord row, int index) {
        if (index < 0 || index >= row.size()) return null;
        return row.get(index);
    }

    // Helper method to generate a synthetic sheet name from the file name (without extension).
    private static String syntheticSheetName(Path filePath) {
        String name = filePath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name; // Remove extension if present
    }
    
}

