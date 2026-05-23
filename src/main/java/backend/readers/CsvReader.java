package backend.readers;

import backend.model.ContactRecord;
import backend.model.SheetData;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
    public Map<String, SheetData> read(Path filePath, List<String> sheetNames) throws IOException {
        String sheetName = syntheticSheetName(filePath);

        List<ContactRecord> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
                    .parse(reader)) {
        
                Iterator<CSVRecord> it = parser.iterator();

                // Scan for the first row that BOTH has content AND parses as a header.
                // Rows above the real header may be blank (already filtered by
                // setIgnoreEmptyLines) or contain a title/note that HeaderResolver rejects.
                HeaderResolver resolver = null;
                while (it.hasNext()) {
                    CSVRecord candidate = it.next();
                    List<String> headerCells = new ArrayList<>();
                    for (String cell : candidate) headerCells.add(cell);
                    if (headerCells.stream().allMatch(s -> s == null || s.isBlank())) continue;
                    try {
                        resolver = new HeaderResolver(headerCells);
                        break;
                    } catch (IllegalArgumentException ignored) {
                        // Not a header — try the next row.
                    }
                }

                if (resolver == null) {
                    return Map.of(sheetName, new SheetData(records, Collections.emptySet()));
                }

                ContactRecordMapper recordMapper = new ContactRecordMapper(resolver);

                while (it.hasNext()) {
                    CSVRecord row = it.next();
                    // commons-csv's CSVRecord.get(int) throws on out-of-range index. Real-world
                    // CSVs often have ragged rows (header has N columns, some data rows have fewer).
                    ContactRecord record = recordMapper.map(idx -> idx < row.size() ? row.get(idx) : null);
                    if (record != null) records.add(record);
                }
            return Map.of(sheetName, new SheetData(records, resolver.availableFields()));
        }
    }

    // Helper method to generate a synthetic sheet name from the file name (without extension).
    private static String syntheticSheetName(Path filePath) {
        String name = filePath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name; // Remove extension if present
    }
    
}

