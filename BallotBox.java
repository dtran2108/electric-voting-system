import java.util.ArrayList;
import java.util.List;

/**
 * BallotBox stores all submitted ballots.
 */
public class BallotBox {

    private final List<Ballot> ballots;

    public BallotBox() {
        this.ballots = new ArrayList<>();
    }

    /**
     * Submit one ballot into the ballot box.
     */
    public void submit(Ballot ballot) {
        if (ballot == null) {
            throw new IllegalArgumentException("Ballot cannot be null");
        }
        ballots.add(ballot);
    }

    /**
     * Retrieve all ballots.
     * This returns a copy to prevent external code from directly changing the ballot box.
     */
    public List<Ballot> retrieveAll() {
        return new ArrayList<>(ballots);
    }

    /**
     * Clear all ballots.
     * Used by VotingMachine.reset().
     */
    public void clear() {
        ballots.clear();
    }
}
