import java.util.HashMap;
import java.util.Map;

/**
 * TallyResult stores the final tally result.
 *
 * VotingMachine is responsible for counting ballots.
 * This class only stores and provides access to the result.
 */
public class TallyResult {

    private final Map<Integer, Integer> counts;
    private final int abstainCount;

    public TallyResult(Map<Integer, Integer> counts, int abstainCount) {
        if (counts == null) {
            throw new IllegalArgumentException("Counts cannot be null");
        }
        if (abstainCount < 0) {
            throw new IllegalArgumentException("Abstain count cannot be negative");
        }

        this.counts = new HashMap<>(counts);
        this.abstainCount = abstainCount;
    }

    /**
     * Returns a copy of candidate vote counts.
     * Key: candidate id / candidate number
     * Value: vote count
     */
    public Map<Integer, Integer> getCounts() {
        return new HashMap<>(counts);
    }

    /**
     * Returns abstain ballot count.
     */
    public int getAbstainCount() {
        return abstainCount;
    }

    /**
     * Returns votes for a specific candidate.
     * If the candidate has no votes, return 0.
     */
    public int getVotesForCandidate(int candidateId) {
        return counts.getOrDefault(candidateId, 0);
    }

    /**
     * Valid votes are all non-abstain votes.
     */
    public int getValidVoteCount() {
        int total = 0;
        for (int count : counts.values()) {
            total += count;
        }
        return total;
    }

    /**
     * Total votes include valid votes and abstain votes.
     */
    public int getTotalVoteCount() {
        return getValidVoteCount() + abstainCount;
    }
}
