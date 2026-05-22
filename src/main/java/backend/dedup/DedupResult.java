package backend.dedup;

import java.util.List;
import java.util.Set;

import backend.model.ContactField;
import backend.model.ContactRecord;

public record DedupResult(
    List<ContactRecord> kept,            // primary records that did NOT match anything in secondary
    List<RemovedRecord> removed,         // records that did match, with reason + confidence
    List<String> appliedStrategies,
    Set<ContactField> sourceFields       // fields the source sheet had — drives output column selection
) {}
