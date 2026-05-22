package backend.dedup;

import java.util.List;

import backend.model.ContactRecord;

public record DedupResult(
    List<ContactRecord> kept, // primary records that did NOT match anything in secondary
    List<RemovedRecord> removed, // records that did match, with reason + confidence
    List<String> appliedStrategies
) {}
