package backend;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import backend.dedup.DedupOrchestrator;
import backend.dedup.DedupResult;
import backend.model.ContactRecord;
import backend.model.SheetData;
import backend.readers.FileReaderFactory;
import backend.readers.IFileReader;
import backend.writers.ExcelOutputWriter;

public final class DeduplicationService {

    private final DedupOrchestrator orchestrator = new DedupOrchestrator();
    private final ExcelOutputWriter writer = new ExcelOutputWriter();

    /** Used by the GUI to populate the sheet picker before reading. */
    public List<String> listSheets(Path file) throws IOException {
        return FileReaderFactory.forPath(file).listSheets(file);
    }

    /** Reads the selected sheets and returns the total number of records across them. */
    public int countRecords(Path file, List<String> sheetSelection) throws IOException {
        Map<String, SheetData> sheets = FileReaderFactory.forPath(file).read(file, sheetSelection);
        return sheets.values().stream()
                .mapToInt(sd -> sd.records().size())
                .sum();
    }

    /** Read both files for the selected sheets, dedup per primary sheet, write outputs. */
    public Summary run(
            Path primaryPath,   List<String> primarySheetSelection,
            Path secondaryPath, List<String> secondarySheetSelection,
            UserSettings settings) throws IOException {

        IFileReader primaryReader   = FileReaderFactory.forPath(primaryPath);
        IFileReader secondaryReader = FileReaderFactory.forPath(secondaryPath);

        Map<String, SheetData> primarySheets   = primaryReader.read(primaryPath, primarySheetSelection);
        Map<String, SheetData> secondarySheets = secondaryReader.read(secondaryPath, secondarySheetSelection);

        int primaryRowsBefore = primarySheets.values().stream()
                .mapToInt(sd -> sd.records().size())
                .sum();

        // Optional: drop guests without an email before dedup. Stakeholder rule
        // (when enabled): they can't be contacted, so they don't belong in the
        // output. Secondary records without email are kept either way, since
        // they can still pull guests out via name+company matches.
        int noEmailDropped = 0;
        if (settings.dropRowsWithoutEmail()) {
            primarySheets = requireEmail(primarySheets);
            int primaryRowsAfter = primarySheets.values().stream()
                    .mapToInt(sd -> sd.records().size())
                    .sum();
            noEmailDropped = primaryRowsBefore - primaryRowsAfter;
        }

        // All selected secondary sheets get treated as one combined pool of "people
        // to match against" — same as the Python reference (pd.concat).
        List<ContactRecord> secondaryPool = secondarySheets.values().stream()
                .flatMap(sd -> sd.records().stream())
                .toList();

        Map<String, DedupResult> results = new LinkedHashMap<>();
        int totalKept = 0;
        int totalRemoved = 0;
        for (var entry : primarySheets.entrySet()) {
            DedupResult res = orchestrator.deduplicate(entry.getValue(), secondaryPool);
            results.put(entry.getKey(), res);
            totalKept += res.kept().size();
            totalRemoved += res.removed().size();
        }

        Path outputDir = writer.write(primaryPath, results);

        return new Summary(
            primarySheets.size(),
            totalKept,
            totalRemoved,
            noEmailDropped,
            outputDir
        );
    }

    /** Returns a copy of the sheet map with records lacking an email filtered out. */
    private static Map<String, SheetData> requireEmail(Map<String, SheetData> sheets) {
        Map<String, SheetData> result = new LinkedHashMap<>();
        for (var entry : sheets.entrySet()) {
            SheetData sd = entry.getValue();
            List<ContactRecord> filtered = sd.records().stream()
                    .filter(r -> r.email() != null)
                    .toList();
            result.put(entry.getKey(), new SheetData(filtered, sd.availableFields()));
        }
        return result;
    }

    /** What the GUI shows in the result dialog. */
    public record Summary(
        int sheetsProcessed,
        int totalKept,
        int totalRemoved,
        int noEmailDropped,
        Path outputDirectory
    ) {}
}
