package gui;

import java.util.List;
import java.io.File;

@FunctionalInterface
public interface SheetSelector {
    
    /** Returns the user's sheet selection for a freshly-picked file, or null if cancelled. */
    List<String> select(File file);
}
