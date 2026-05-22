package backend.readers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import backend.model.ContactRecord;

public final class ExcelReader implements IFileReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    @Override
    public List<String> listSheets(Path filePath) throws IOException {
        try (InputStream input = Files.newInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(input)) {
                List<String> sheetNames = new ArrayList<>(workbook.getNumberOfSheets());
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    sheetNames.add(workbook.getSheetAt(i).getSheetName());
                }
                return sheetNames;
            }
    }

    @Override
    public Map<String, List<ContactRecord>> read(Path filePath, List<String> sheetNames) throws IOException {
        Map<String, List<ContactRecord>> result = new LinkedHashMap<>();

        try (InputStream input = Files.newInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(input)) {
                for (String sheetName : sheetNames) {
                    Sheet sheet = workbook.getSheet(sheetName);
                    if (sheet == null) {
                        throw new IllegalArgumentException("Sheet not found: " + sheetName + " in " + filePath.getFileName());
                    }
                    result.put(sheetName, readSheet(sheet));
                }
        }
        return result;
    }

    private static List<ContactRecord> readSheet(Sheet sheet) {
        int headerRowIndex = findHeaderRowIndex(sheet);
        if (headerRowIndex < 0) return List.of(); // No header row -> return empty list, not an error
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) return List.of();

        List<String> headerCells = new ArrayList<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            headerCells.add(cellString(headerRow.getCell(c)));
        }
        HeaderResolver resolver = new HeaderResolver(headerCells);
        ContactRecordMapper mapper = new ContactRecordMapper(resolver);

        List<ContactRecord> records = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();
        for (int r = headerRowIndex + 1; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            ContactRecord record = mapper.map(idx -> cellString(row.getCell(idx)));
            if (record != null) records.add(record);
        }
        return records;
    }
    
    private static String cellString(Cell cell) {
        if (cell == null) return null;
        return FORMATTER.formatCellValue(cell);  
    }

    // Some sheets may have leading empty rows before the header. We try to detect and skip them.
    private static int findHeaderRowIndex(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
                String value = cellString(cell);
                if (value != null && !value.isBlank()) {
                    return r;
                }
            }
        }
        return -1; // No header row found
    }
}
