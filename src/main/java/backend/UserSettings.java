package backend;

/**
 * User-controlled settings threaded into a {@link DeduplicationService#run} call.
 * Covers anything the user can toggle from the UI (filtering, future output
 * shaping, etc.). Add new fields here rather than growing the run() signature.
 */
public record UserSettings(
    boolean dropRowsWithoutEmail
) {
    /** Defaults matching the original always-on behaviour. */
    public static UserSettings defaults() {
        return new UserSettings(true);
    }
}
