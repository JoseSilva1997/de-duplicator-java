package backend.readers;

import backend.model.ContactField;
import backend.model.ContactRecord;

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

                // Create a resolver and mapper based on the header row
                HeaderResolver resolver = new HeaderResolver(headerCells);
                ContactRecordMapper recordMapper = new ContactRecordMapper(resolver);  
                
                // Remaining rows = data
                while (it.hasNext()) {
                    CSVRecord row = it.next();
                    ContactRecord record = recordMapper.map(row::get);
                    if (record != null) records.add(record);
                }
        }
        return Map.of(sheetName, records);
    }

    // Helper method to generate a synthetic sheet name from the file name (without extension).
    private static String syntheticSheetName(Path filePath) {
        String name = filePath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name; // Remove extension if present
    }
    
}

