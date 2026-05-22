package backend.dedup;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import backend.model.ContactRecord;

public final class NormalisationUtils {
    
    private NormalisationUtils() {} // No instances

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{Nd}\\s]");
    private static final Pattern COMPANY_SUFFIX = Pattern.compile(
    "\\b(inc|ltd|corp|corporation|llc|gmbh|plc|s\\.a\\.|co)\\b");


    /** NFD-normalize and strip diacritical marks. Null in → empty out. */
    public static String stripAccents(String s) {
        if (s == null) return "";
        String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{Mn}", "");
    }

    /** General string normalisation for comparison: strip accents, lowercase, trim, collapse whitespace. */
    public static String normaliseString(String s) {
        if (s == null) return "";
        String x = stripAccents(s).toLowerCase().trim();
        return WHITESPACE.matcher(x).replaceAll(" ");
    }

    /** Company normalisation: like normaliseString plus strip common legal suffixes and punctuation. */
    public static String normaliseCompany(String s) {
        if (s == null) return "";
        String x = stripAccents(s).toLowerCase().trim();
        x = COMPANY_SUFFIX.matcher(x).replaceAll("");
        x = NON_WORD.matcher(x).replaceAll("");
        return WHITESPACE.matcher(x).replaceAll(" ").trim();
    }

    /** Tokenise a name: normalize, split on whitespace, drop single-character tokens. */
    public static Set<String> tokeniseName(String name) {
        String normalised = normaliseString(name);
        if (normalised.isEmpty()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String token : WHITESPACE.split(normalised)) {
            if (token.length() > 1) result.add(token);
        }
        return result;
    }

    /** Build the canonical "firstName|lastName|company" key used by the name+company strategies. */
    public static String keyFor(ContactRecord record) {
        return normaliseString(record.firstName()) 
        + "|" + normaliseString(record.lastName())
        + "|" + normaliseCompany(record.company());
    }
}
