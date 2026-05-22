package backend.dedup;

import java.util.List;
import java.util.Map;
import java.util.Set;

import backend.model.ContactField;
import backend.model.ContactRecord;

public interface DedupStrategy {

    /**
     * Display name used in RemovedRecord.reason and in GUI messaging.
     * @return the name of the strategy
     */
    String name();

    /**
     * Fields that must be resolvable on BOTH primary and secondary records for this strategy to be applicable.
     * @return the set of required fields
     */
    Set<ContactField> requiredFields();

    /**
    * Find primary records that match any secondary record per this strategy's rules.
    * Strategies should not modify the input lists.
    * @param primary the list of primary contact records
    * @param secondary the list of secondary contact records
    * @return a map of matched records to their confidence scores
    */
    Map<ContactRecord, Integer> findMatches(List<ContactRecord> primary, List<ContactRecord> secondary);
}
