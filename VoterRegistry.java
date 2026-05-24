import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * VoterRegistry — the master list of registered voters.
 *
 * Adopts two ideas from the teammate's VotingSystem class:
 *   - Serializable, so a future DataManager can save/restore state to disk.
 *   - markAsVoted(voterId) / clearAllVotedFlags() (their resetVoterData())
 *     for duplicate-vote prevention and admin reset.
 *
 * Storage is the Map<String,Voter> the UML requires. Voted state lives on
 * each Voter as hasVoted, so findById(voterId).hasVoted() answers the
 * duplicate-vote question — no separate HashSet needed.
 */
public class VoterRegistry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Voter> voters = new HashMap<>();

    public void register(Voter voter) {
        if (voter == null) {
            throw new IllegalArgumentException("Voter cannot be null");
        }
        if (voters.containsKey(voter.getId())) {
            throw new IllegalArgumentException("Voter already registered: " + voter.getId());
        }
        voters.put(voter.getId(), voter);
    }

    /**
     * Returns the voter, or null if no voter with that id exists.
     */
    public Voter findById(String voterId) {
        return voters.get(voterId);
    }

    public void markAsVoted(String voterId) {
        Voter voter = voters.get(voterId);
        if (voter == null) {
            throw new IllegalArgumentException("Unknown voter: " + voterId);
        }
        voter.markVoted();
    }

    /**
     * Resets all hasVoted flags. Called by VotingMachine.reset().
     * Mirrors @teammate's VotingSystem.resetVoterData().
     */
    public void clearAllVotedFlags() {
        for (Voter v : voters.values()) {
            v.clearVotedFlag();
        }
    }

    public int size() {
        return voters.size();
    }

    /**
     * Read-only view of registered voters. Used by demo/admin tooling
     * (e.g. the CLI directory listing) — not by the voting flow.
     */
    public Collection<Voter> getAllVoters() {
        return Collections.unmodifiableCollection(voters.values());
    }
}
