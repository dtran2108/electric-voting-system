import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Election — the election day itself.
 *
 * Owns the candidate list (capped at MAX_CANDIDATES) and the open/closed flag.
 * Only Election may change either; everyone else reads.
 */
public class Election {

    public static final int MAX_CANDIDATES = 5;

    private final String electionId;
    private final List<Candidate> candidates;
    private boolean isOpen;

    public Election(String electionId) {
        if (electionId == null || electionId.isBlank()) {
            throw new IllegalArgumentException("electionId is required");
        }
        this.electionId = electionId;
        this.candidates = new ArrayList<>();
        this.isOpen = false;
    }

    public String getElectionId() {
        return electionId;
    }

    public void addCandidate(Candidate candidate) {
        if (isOpen) {
            throw new IllegalStateException("Cannot add candidates once the election is open");
        }
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate cannot be null");
        }
        if (candidates.size() >= MAX_CANDIDATES) {
            throw new IllegalStateException("Cannot add more than " + MAX_CANDIDATES + " candidates");
        }
        for (Candidate c : candidates) {
            if (c.getNumber() == candidate.getNumber()) {
                throw new IllegalArgumentException("Duplicate candidate number: " + candidate.getNumber());
            }
        }
        candidates.add(candidate);
    }

    public void open() {
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Cannot open an election with no candidates");
        }
        this.isOpen = true;
    }

    public void close() {
        this.isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public List<Candidate> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }
}
