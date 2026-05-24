import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Voter — a registered citizen.
 *
 * Built on the teammate's design: the voter supplies only name and birthday,
 * and the system auto-encodes a purely numeric voterId from those two fields
 * (per-character ASCII codes + YYYYMMDD). Serializable is kept so a future
 * DataManager can persist registered voters to disk.
 *
 * Extended to satisfy the UML contract used by VotingMachine: a hasVoted flag,
 * isEligible(votingAge), markVoted() and clearVotedFlag().
 */
public class Voter implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String    name;
    private final LocalDate birthday;
    private final String    voterId;
    private boolean         hasVoted;

    /**
     * Initializes a Voter and automatically generates their numeric ID.
     */
    public Voter(String name, LocalDate birthday) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (birthday == null) {
            throw new IllegalArgumentException("birthday is required");
        }
        this.name     = name;
        this.birthday = birthday;
        this.voterId  = generateNumericId(name, birthday);
        this.hasVoted = false;
    }

    /**
     * ASCII-code-per-character concatenation + YYYYMMDD birthday.
     * Per-character ints make collisions impossible across distinct names.
     */
    private static String generateNumericId(String name, LocalDate birthday) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            sb.append((int) c);
        }
        sb.append(birthday.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        return sb.toString();
    }

    // ====== Identity & profile ======
    public String getId() {
        return voterId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    // ====== Eligibility & voting state (UML contract) ======
    public boolean isEligible(int votingAge) {
        int age = Period.between(birthday, LocalDate.now()).getYears();
        return age >= votingAge;
    }

    public boolean hasVoted() {
        return hasVoted;
    }

    public void markVoted() {
        this.hasVoted = true;
    }

    public void clearVotedFlag() {
        this.hasVoted = false;
    }

    @Override
    public String toString() {
        return "Voter{name='" + name + "', birthday=" + birthday + ", voterId='" + voterId + "'}";
    }
}
