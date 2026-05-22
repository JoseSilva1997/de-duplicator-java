package backend.readers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import backend.model.ContactRecord;
import backend.model.SheetData;

public final class ExcelReader implements IFileReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    @Override
    public List<String> listSheets(Path filePath) throws IOException {
        try (InputStream input = Files.newInputStream(filePath);
            Workbook workbook = WorkbookFactory.create(input)) {
            List<String> sheetNames = new ArrayList<>(workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.getSheetVisibility(i) == SheetVisibility.VISIBLE) {
                    sheetNames.add(workbook.getSheetAt(i).getSheetName());
                }
            }
            return sheetNames;
        }
    }

    @Override
    public Map<String, SheetData> read(Path filePath, List<String> sheetNames) throws IOException {
        Map<String, SheetData> result = new LinkedHashMap<>();

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

    private static SheetData readSheet(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();

        // Scan for the first row that BOTH has content AND parses as a header.
        // Rows above the real header may be blank or contain a title/note that
        // HeaderResolver rejects — skip those and keep looking.
        HeaderResolver resolver = null;
        int headerRowIndex = -1;
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            List<String> cells = readRowCells(row);
            if (cells.stream().allMatch(s -> s == null || s.isBlank())) continue;
            try {
                resolver = new HeaderResolver(cells);
                headerRowIndex = r;
                break;
            } catch (IllegalArgumentException ignored) {
                // Not a header — try the next non-empty row.
            }
        }

        if (resolver == null) return new SheetData(List.of(), Collections.emptySet());

        ContactRecordMapper mapper = new ContactRecordMapper(resolver);
        List<ContactRecord> records = new ArrayList<>();
        for (int r = headerRowIndex + 1; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            ContactRecord record = mapper.map(idx -> cellString(row.getCell(idx)));
            if (record != null) records.add(record);
        }
        return new SheetData(records, resolver.availableFields());
    }

    private static List<String> readRowCells(Row row) {
        List<String> cells = new ArrayList<>();
        int lastCol = row.getLastCellNum();   // -1 for empty rows
        for (int c = 0; c < lastCol; c++) {
            cells.add(cellString(row.getCell(c)));
        }
        return cells;
    }

    private static String cellString(Cell cell) {
        if (cell == null) return null;
        return FORMATTER.formatCellValue(cell);
    }
}
