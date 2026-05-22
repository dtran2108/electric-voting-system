import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * VotingMachine — the coordinator of the entire voting flow (the poll worker).
 *
 * It is the only entry point the UI is allowed to call. It chains together
 * "authenticate -> eligibility check -> cast ballot -> mark as voted",
 * ensuring the UI cannot bypass any rule.
 *
 * Depends only on AuthInterface (not directly on PinAuthenticator),
 * so the authentication mechanism is pluggable.
 */
public class VotingMachine {

    private final AuthInterface authenticator;
    private final VoterRegistry registry;
    private final BallotBox     ballotBox;
    private final Election      election;
    private final int           votingAge;

    public VotingMachine(AuthInterface authenticator,
                         VoterRegistry registry,
                         BallotBox ballotBox,
                         Election election,
                         int votingAge) {
        this.authenticator = authenticator;
        this.registry      = registry;
        this.ballotBox     = ballotBox;
        this.election      = election;
        this.votingAge     = votingAge;
    }

    // ---------- Step 1: Authentication ----------
    public boolean login(String voterId, String credential) {
        return authenticator.authenticate(voterId, credential);
    }

    // ---------- Step 2: Cast a vote for a candidate ----------
    public void castVote(String voterId, int candidateNumber) {
        preconditionCheck(voterId);

        if (!isValidCandidate(candidateNumber)) {
            throw new IllegalArgumentException("Candidate number is not on the ballot");
        }

        // Build an anonymous ballot — Ballot itself does not store voterId.
        Ballot ballot = Ballot.forCandidate(candidateNumber, Instant.now());

        // Submit to the ballot box first; only mark as voted on success.
        // Order matters: if submit() fails, the voter is not marked, so they can retry.
        ballotBox.submit(ballot);
        registry.markAsVoted(voterId);
    }

    // ---------- Step 2b: Cast an abstain (spoiled) ballot ----------
    public void castAbstain(String voterId) {
        preconditionCheck(voterId);
        ballotBox.submit(Ballot.abstain(Instant.now()));
        registry.markAsVoted(voterId);
    }

    /**
     * Runs all eligibility checks required before voting. Throws on any failure,
     * leaving no partial state behind.
     */
    private Voter preconditionCheck(String voterId) {
        if (!election.isOpen()) {
            throw new IllegalStateException("Election is not open");
        }
        Voter voter = registry.findById(voterId);
        if (voter == null) {
            throw new IllegalArgumentException("Unknown voter");
        }
        if (!voter.isEligible(votingAge)) {
            throw new IllegalStateException("Voter is under the voting age");
        }
        if (voter.hasVoted()) {
            throw new IllegalStateException("Voter has already cast a ballot");
        }
        return voter;
    }

    private boolean isValidCandidate(int candidateNumber) {
        for (Candidate c : election.getCandidates()) {
            if (c.getNumber() == candidateNumber) {
                return true;
            }
        }
        return false;
    }

    // ---------- Step 3: Tally the votes ----------
    public TallyResult tally() {
        Map<Integer, Integer> counts = new HashMap<>();
        int abstainCount = 0;
        for (Ballot b : ballotBox.retrieveAll()) {
            if (b.isAbstain()) {
                abstainCount++;
            } else {
                counts.merge(b.getSelection(), 1, Integer::sum);
            }
        }
        return new TallyResult(counts, abstainCount);
    }

    // ---------- Reset: clear the ballot box and all hasVoted flags ----------
    public void reset() {
        ballotBox.clear();
        registry.clearAllVotedFlags();
    }
}
