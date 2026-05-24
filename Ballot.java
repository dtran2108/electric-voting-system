import java.time.Instant;

/**
 * Ballot represents one anonymous ballot.
 */
public class Ballot {

    // private String voterId; // Future use: enable traceable ballots if required.

    private final Integer selectedCandidateId;
    private final boolean abstain;
    private final Instant timestamp;

    private Ballot(Integer selectedCandidateId, boolean abstain, Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }

        this.selectedCandidateId = selectedCandidateId;
        this.abstain = abstain;
        this.timestamp = timestamp;
    }

    /**
     * Creates a ballot for a selected candidate.
     */
    public static Ballot forCandidate(int candidateId, Instant timestamp) {
        return new Ballot(candidateId, false, timestamp);
    }

    /**
     * Creates an abstain ballot.
     */
    public static Ballot abstain(Instant timestamp) {
        return new Ballot(null, true, timestamp);
    }

    /**
     * Returns the selected candidate id.
     * Do not call this method before checking isAbstain().
     */
    public int getSelection() {
        if (abstain) {
            throw new IllegalStateException("Abstain ballot has no candidate selection");
        }
        return selectedCandidateId;
    }

    /**
     * Returns whether this ballot is an abstain ballot.
     */
    public boolean isAbstain() {
        return abstain;
    }

    /**
     * Timestamp can be used for checking when the ballot was created
     * or comparing ballot records with total submission records.
     */
    public Instant getTimestamp() {
        return timestamp;
    }
}
