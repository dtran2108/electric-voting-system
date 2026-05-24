import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Main — composition root and CLI driver.
 *
 * The composition root is the one place that creates and wires every
 * object together; everything else just receives its collaborators.
 * The CLI sits on top of VotingMachine and never reaches around it,
 * except for open/close/list-candidates on the Election object — those
 * are admin actions, not voting actions, so they go directly to Election.
 *
 * Voters self-register and pick their own PIN via the `register` command;
 * the admin no longer hard-codes credentials.
 */
public class Main {

    private static final int VOTING_AGE = 18;

    private final Election         election;
    private final VotingMachine    machine;
    private final VoterRegistry    registry;
    private final PinAuthenticator pinAuth;
    private final Scanner          in = new Scanner(System.in);

    public static void main(String[] args) {
        new Main().run();
    }

    private Main() {
        this.registry = new VoterRegistry();
        this.pinAuth  = new PinAuthenticator();

        this.election = new Election("2026-general");
        election.addCandidate(new Candidate(1, "Alice Smith", "Green"));
        election.addCandidate(new Candidate(2, "Bob Jones",   "Blue"));
        election.addCandidate(new Candidate(3, "Eve Carter",  "Red"));
        election.open();

        BallotBox ballotBox = new BallotBox();
        this.machine = new VotingMachine(pinAuth, registry, ballotBox, election, VOTING_AGE);
    }

    private void run() {
        System.out.println("=== Electronic Voting System ===");
        System.out.println("No voters yet — type `register` to enroll, then `vote`.");
        printHelp();

        while (true) {
            System.out.print("\n> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;

            String cmd = line.split("\\s+")[0].toLowerCase();
            try {
                switch (cmd) {
                    case "help":       printHelp(); break;
                    case "register":   handleRegister(); break;
                    case "candidates": showCandidates(); break;
                    case "voters":     showVoterDirectory(); break;
                    case "vote":       handleVote(); break;
                    case "tally":      showTally(); break;
                    case "close":      election.close(); System.out.println("Election closed."); break;
                    case "reopen":     election.open();  System.out.println("Election reopened."); break;
                    case "reset":      machine.reset();  System.out.println("Reset done."); break;
                    case "quit":
                    case "exit":       System.out.println("Goodbye."); return;
                    default:           System.out.println("Unknown command. Type 'help'.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  register     — enroll a new voter (name, birthday, PIN)");
        System.out.println("  candidates   — list the candidates on the ballot");
        System.out.println("  voters       — print the registered voters and their generated IDs (demo aid)");
        System.out.println("  vote         — log in and cast a ballot (interactive)");
        System.out.println("  tally        — show current tally");
        System.out.println("  close        — close the election");
        System.out.println("  reopen       — reopen the election");
        System.out.println("  reset        — clear ballots and voted flags");
        System.out.println("  help         — show this help");
        System.out.println("  quit         — exit");
    }

    private void handleRegister() {
        System.out.print("Name: ");
        String name = in.nextLine().trim();

        System.out.print("Birthday (YYYY-MM-DD): ");
        LocalDate birthday;
        try {
            birthday = LocalDate.parse(in.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format — registration aborted.");
            return;
        }

        System.out.print("Choose a PIN (digits only, min 4): ");
        String pin = in.nextLine().trim();
        System.out.print("Confirm PIN: ");
        String confirm = in.nextLine().trim();
        if (!pin.equals(confirm)) {
            System.out.println("PINs do not match — registration aborted.");
            return;
        }

        Voter voter = new Voter(name, birthday);
        registry.register(voter);          // throws if duplicate
        pinAuth.setPin(voter.getId(), pin); // throws if PIN format is invalid

        System.out.println("Registered. Your voter ID is:");
        System.out.println("  " + voter.getId());
        System.out.println("Keep it somewhere safe — you'll need it to log in.");
    }

    private void showVoterDirectory() {
        if (registry.size() == 0) {
            System.out.println("(no voters registered)");
            return;
        }
        System.out.println("Registered voters (id is auto-generated from name + birthday):");
        for (Voter v : registry.getAllVoters()) {
            System.out.printf("  %-8s  born %s  → id=%s%n",
                    v.getName(), v.getBirthday(), v.getId());
        }
    }

    private void showCandidates() {
        if (election.getCandidates().isEmpty()) {
            System.out.println("(no candidates)");
            return;
        }
        System.out.println("Candidates:");
        for (Candidate c : election.getCandidates()) {
            System.out.printf("  %d. %s (%s)%n", c.getNumber(), c.getName(), c.getParty());
        }
    }

    private void handleVote() {
        System.out.print("Voter ID: ");
        String voterId = in.nextLine().trim();
        System.out.print("PIN: ");
        String pin = in.nextLine().trim();

        if (!machine.login(voterId, pin)) {
            System.out.println("Login failed.");
            return;
        }
        System.out.println("Login successful.");

        showCandidates();
        System.out.print("Pick a candidate number, or 'a' to abstain: ");
        String choice = in.nextLine().trim().toLowerCase();

        if (choice.equals("a") || choice.equals("abstain")) {
            machine.castAbstain(voterId);
            System.out.println("Abstain ballot recorded.");
            return;
        }

        int number;
        try {
            number = Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input — ballot not submitted.");
            return;
        }
        machine.castVote(voterId, number);
        System.out.println("Vote recorded for candidate " + number + ".");
    }

    private void showTally() {
        TallyResult result = machine.tally();
        System.out.println("Tally:");
        for (Candidate c : election.getCandidates()) {
            int votes = result.getVotesForCandidate(c.getNumber());
            double pct = result.getValidVoteCount() == 0
                    ? 0.0
                    : (votes * 100.0) / result.getValidVoteCount();
            System.out.printf("  %d. %-14s (%s)  %d vote(s)  (%.1f%% of valid)%n",
                    c.getNumber(), c.getName(), c.getParty(), votes, pct);
        }
        System.out.println("  Abstain: " + result.getAbstainCount());
        System.out.printf("  Total: %d cast (%d valid, %d abstain)%n",
                result.getTotalVoteCount(),
                result.getValidVoteCount(),
                result.getAbstainCount());
    }
}
