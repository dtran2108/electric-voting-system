import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * UI — Swing front-end for the Electronic Voting System.
 *
 * This is a graphical alternative to Main.java. Both wire the same
 * domain objects (Election, VotingMachine, VoterRegistry, PinAuthenticator)
 * and never reach around VotingMachine for voting operations.
 *
 * Layout: a single JFrame with a CardLayout. Each "card" is a self-contained
 * panel (Home, Register, Vote, Tally, Admin) that talks to the domain via
 * the shared collaborators passed in at construction.
 */
public class UI {

    private static final int VOTING_AGE = 18;

    // --- Domain (the composition root, same as Main.java) ---
    private final Election         election;
    private final VotingMachine    machine;
    private final VoterRegistry    registry;
    private final PinAuthenticator pinAuth;

    // --- UI ---
    private final JFrame      frame;
    private final CardLayout  cards;
    private final JPanel      cardHost;
    private final JLabel      statusBar;

    private static final String CARD_HOME     = "home";
    private static final String CARD_REGISTER = "register";
    private static final String CARD_VOTE     = "vote";
    private static final String CARD_TALLY    = "tally";
    private static final String CARD_ADMIN    = "admin";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to default L&F
        }
        SwingUtilities.invokeLater(() -> new UI().show());
    }

    private UI() {
        // --- Wire the domain (mirrors Main.java) ---
        this.registry = new VoterRegistry();
        this.pinAuth  = new PinAuthenticator();

        this.election = new Election("2026-general");
        election.addCandidate(new Candidate(1, "Alice Smith", "Green"));
        election.addCandidate(new Candidate(2, "Bob Jones",   "Blue"));
        election.addCandidate(new Candidate(3, "Eve Carter",  "Red"));
        election.open();

        BallotBox ballotBox = new BallotBox();
        this.machine = new VotingMachine(pinAuth, registry, ballotBox, election, VOTING_AGE);

        // --- Build the frame ---
        this.frame    = new JFrame("Electronic Voting System");
        this.cards    = new CardLayout();
        this.cardHost = new JPanel(cards);
        this.statusBar = new JLabel(" Ready.");
        statusBar.setBorder(new EmptyBorder(6, 12, 6, 12));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(0xEC, 0xEF, 0xF1));
        statusBar.setForeground(new Color(0x37, 0x47, 0x4F));

        cardHost.add(new HomePanel(),     CARD_HOME);
        cardHost.add(new RegisterPanel(), CARD_REGISTER);
        cardHost.add(new VotePanel(),     CARD_VOTE);
        cardHost.add(new TallyPanel(),    CARD_TALLY);
        cardHost.add(new AdminPanel(),    CARD_ADMIN);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(buildNavBar(), BorderLayout.NORTH);
        frame.add(cardHost,      BorderLayout.CENTER);
        frame.add(statusBar,     BorderLayout.SOUTH);
        frame.setSize(720, 560);
        frame.setMinimumSize(new Dimension(640, 480));
        frame.setLocationRelativeTo(null);
    }

    private void show() {
        cards.show(cardHost, CARD_HOME);
        frame.setVisible(true);
    }

    private JComponent buildNavBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 6));
        bar.setBorder(new EmptyBorder(4, 8, 4, 8));
        bar.setBackground(new Color(0x26, 0x32, 0x38));

        bar.add(navButton("Home",     CARD_HOME));
        bar.add(navButton("Register", CARD_REGISTER));
        bar.add(navButton("Vote",     CARD_VOTE));
        bar.add(navButton("Tally",    CARD_TALLY));
        bar.add(navButton("Admin",    CARD_ADMIN));
        return bar;
    }

    private JButton navButton(String label, String card) {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.addActionListener((ActionEvent e) -> {
            cards.show(cardHost, card);
            // Refresh dynamic cards on entry.
            for (Component c : cardHost.getComponents()) {
                if (c.isVisible() && c instanceof Refreshable) {
                    ((Refreshable) c).refresh();
                }
            }
            setStatus("Switched to " + label + ".");
        });
        return b;
    }

    private void setStatus(String text) {
        statusBar.setText(" " + text);
    }

    /** Panels that need to recompute their content on entry implement this. */
    private interface Refreshable {
        void refresh();
    }

    // ---------------------------------------------------------------
    // HomePanel
    // ---------------------------------------------------------------
    private final class HomePanel extends JPanel {
        HomePanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel title = new JLabel("Electronic Voting System");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

            JLabel subtitle = new JLabel(
                    "<html><body style='width:520px'>"
                            + "Use the tabs above to register as a voter, cast a ballot, "
                            + "view the live tally, or perform admin actions such as opening "
                            + "or closing the election."
                            + "</body></html>");
            subtitle.setBorder(new EmptyBorder(12, 0, 0, 0));

            JPanel header = new JPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setOpaque(false);
            header.add(title);
            header.add(subtitle);

            add(header, BorderLayout.NORTH);
            add(buildQuickActions(), BorderLayout.CENTER);
        }

        private JComponent buildQuickActions() {
            JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
            grid.setBorder(new EmptyBorder(24, 0, 0, 0));
            grid.add(quickCard("Register", "Enroll a new voter and choose a PIN.", CARD_REGISTER));
            grid.add(quickCard("Vote",     "Log in with voter ID + PIN and cast a ballot.", CARD_VOTE));
            grid.add(quickCard("Tally",    "View the running count of votes.", CARD_TALLY));
            grid.add(quickCard("Admin",    "Open/close the election, list voters, or reset.", CARD_ADMIN));
            return grid;
        }

        private JComponent quickCard(String title, String body, String card) {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xCF, 0xD8, 0xDC)),
                    new EmptyBorder(16, 16, 16, 16)));

            JLabel t = new JLabel(title);
            t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));

            JLabel b = new JLabel("<html><body style='width:200px'>" + body + "</body></html>");
            b.setForeground(new Color(0x54, 0x6E, 0x7A));

            JButton go = new JButton("Open " + title + " →");
            go.addActionListener(e -> cards.show(cardHost, card));

            p.add(t, BorderLayout.NORTH);
            p.add(b, BorderLayout.CENTER);
            p.add(go, BorderLayout.SOUTH);
            return p;
        }
    }

    // ---------------------------------------------------------------
    // RegisterPanel
    // ---------------------------------------------------------------
    private final class RegisterPanel extends JPanel {
        private final JTextField     nameField     = new JTextField(20);
        private final JTextField     birthdayField = new JTextField(20);
        private final JPasswordField pinField      = new JPasswordField(20);
        private final JPasswordField pinConfirm    = new JPasswordField(20);
        private final JTextArea      result        = new JTextArea(4, 40);

        RegisterPanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel title = new JLabel("Register a new voter");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
            add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6, 6, 6, 6);
            c.anchor = GridBagConstraints.LINE_START;

            addRow(form, c, 0, "Name:",             nameField);
            addRow(form, c, 1, "Birthday (YYYY-MM-DD):", birthdayField);
            addRow(form, c, 2, "PIN (digits, min 4):",   pinField);
            addRow(form, c, 3, "Confirm PIN:",       pinConfirm);

            JButton submit = new JButton("Register");
            submit.addActionListener(e -> doRegister());

            c.gridx = 1; c.gridy = 4;
            form.add(submit, c);

            result.setEditable(false);
            result.setLineWrap(true);
            result.setWrapStyleWord(true);
            result.setBorder(BorderFactory.createTitledBorder("Result"));

            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.add(form, BorderLayout.NORTH);
            center.add(new JScrollPane(result), BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
        }

        private void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
            c.gridx = 0; c.gridy = row; c.weightx = 0;
            form.add(new JLabel(label), c);
            c.gridx = 1; c.weightx = 1;
            form.add(field, c);
        }

        private void doRegister() {
            String name = nameField.getText().trim();
            String birthdayText = birthdayField.getText().trim();
            String pin     = new String(pinField.getPassword()).trim();
            String confirm = new String(pinConfirm.getPassword()).trim();

            if (name.isEmpty()) {
                showError("Name is required.");
                return;
            }

            LocalDate birthday;
            try {
                birthday = LocalDate.parse(birthdayText);
            } catch (DateTimeParseException e) {
                showError("Invalid date format. Use YYYY-MM-DD.");
                return;
            }

            if (!pin.equals(confirm)) {
                showError("PINs do not match.");
                return;
            }

            try {
                Voter voter = new Voter(name, birthday);
                registry.register(voter);
                pinAuth.setPin(voter.getId(), pin);
                result.setText(
                        "Registered successfully.\n"
                                + "Voter ID: " + voter.getId() + "\n"
                                + "Keep it somewhere safe — you'll need it to log in.");
                setStatus("Registered " + name + ".");
                nameField.setText("");
                birthdayField.setText("");
                pinField.setText("");
                pinConfirm.setText("");
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        }

        private void showError(String msg) {
            result.setText("Error: " + msg);
            setStatus("Registration failed: " + msg);
        }
    }

    // ---------------------------------------------------------------
    // VotePanel
    // ---------------------------------------------------------------
    private final class VotePanel extends JPanel implements Refreshable {
        private final JTextField     voterIdField = new JTextField(20);
        private final JPasswordField pinField     = new JPasswordField(20);
        private final JComboBox<CandidateChoice> choice = new JComboBox<>();
        private final JButton        loginBtn     = new JButton("Log in");
        private final JButton        castBtn      = new JButton("Cast ballot");
        private final JTextArea      result       = new JTextArea(4, 40);

        private String authedVoterId;   // null until login succeeds

        VotePanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel title = new JLabel("Cast a ballot");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
            add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6, 6, 6, 6);
            c.anchor = GridBagConstraints.LINE_START;
            c.fill   = GridBagConstraints.HORIZONTAL;

            c.gridx = 0; c.gridy = 0;
            form.add(new JLabel("Voter ID:"), c);
            c.gridx = 1; c.weightx = 1;
            form.add(voterIdField, c);

            c.gridx = 0; c.gridy = 1; c.weightx = 0;
            form.add(new JLabel("PIN:"), c);
            c.gridx = 1; c.weightx = 1;
            form.add(pinField, c);

            c.gridx = 1; c.gridy = 2;
            form.add(loginBtn, c);

            c.gridx = 0; c.gridy = 3; c.weightx = 0;
            form.add(new JLabel("Candidate:"), c);
            c.gridx = 1; c.weightx = 1;
            form.add(choice, c);

            c.gridx = 1; c.gridy = 4;
            form.add(castBtn, c);

            result.setEditable(false);
            result.setLineWrap(true);
            result.setWrapStyleWord(true);
            result.setBorder(BorderFactory.createTitledBorder("Status"));

            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.add(form, BorderLayout.NORTH);
            center.add(new JScrollPane(result), BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);

            loginBtn.addActionListener(e -> doLogin());
            castBtn.addActionListener(e -> doCast());

            setLoggedIn(false);
        }

        @Override
        public void refresh() {
            // If the election state changed (e.g. closed via Admin), reflect it.
            populateChoices();
            if (!election.isOpen()) {
                result.setText("Election is currently closed. No ballots may be cast.");
            }
        }

        private void populateChoices() {
            choice.removeAllItems();
            for (Candidate cand : election.getCandidates()) {
                choice.addItem(new CandidateChoice(cand.getNumber(),
                        cand.getNumber() + ". " + cand.getName() + " (" + cand.getParty() + ")"));
            }
            choice.addItem(new CandidateChoice(-1, "Abstain"));
        }

        private void setLoggedIn(boolean loggedIn) {
            choice.setEnabled(loggedIn);
            castBtn.setEnabled(loggedIn);
            voterIdField.setEnabled(!loggedIn);
            pinField.setEnabled(!loggedIn);
            loginBtn.setText(loggedIn ? "Log out" : "Log in");
            if (loggedIn) {
                populateChoices();
            }
        }

        private void doLogin() {
            if (authedVoterId != null) {
                authedVoterId = null;
                pinField.setText("");
                voterIdField.setText("");
                result.setText("Logged out.");
                setLoggedIn(false);
                setStatus("Logged out.");
                return;
            }

            String voterId = voterIdField.getText().trim();
            String pin     = new String(pinField.getPassword()).trim();
            if (voterId.isEmpty() || pin.isEmpty()) {
                result.setText("Enter both Voter ID and PIN.");
                return;
            }
            if (machine.login(voterId, pin)) {
                authedVoterId = voterId;
                Voter v = registry.findById(voterId);
                String name = (v == null) ? voterId : v.getName();
                result.setText("Welcome, " + name + ". Choose your candidate (or Abstain) and cast.");
                setLoggedIn(true);
                setStatus("Logged in as " + name + ".");
            } else {
                result.setText("Login failed. Check your Voter ID and PIN.");
                setStatus("Login failed.");
            }
        }

        private void doCast() {
            CandidateChoice selected = (CandidateChoice) choice.getSelectedItem();
            if (selected == null || authedVoterId == null) {
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    UI.this.frame,
                    "Cast ballot for: " + selected.label + "?\nThis cannot be undone.",
                    "Confirm ballot",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                if (selected.number < 0) {
                    machine.castAbstain(authedVoterId);
                    result.setText("Abstain ballot recorded. Thank you for participating.");
                    setStatus("Abstain ballot recorded.");
                } else {
                    machine.castVote(authedVoterId, selected.number);
                    result.setText("Vote recorded for: " + selected.label
                            + ".\nThank you for participating.");
                    setStatus("Vote recorded.");
                }
                // Force logout after successful ballot.
                authedVoterId = null;
                pinField.setText("");
                voterIdField.setText("");
                setLoggedIn(false);
            } catch (RuntimeException e) {
                result.setText("Error: " + e.getMessage());
                setStatus("Vote failed: " + e.getMessage());
            }
        }
    }

    /** Tiny value type for the candidate dropdown. */
    private static final class CandidateChoice {
        final int    number;
        final String label;
        CandidateChoice(int number, String label) {
            this.number = number;
            this.label  = label;
        }
        @Override public String toString() { return label; }
    }

    // ---------------------------------------------------------------
    // TallyPanel
    // ---------------------------------------------------------------
    private final class TallyPanel extends JPanel implements Refreshable {
        private final DefaultTableModel model;
        private final JTable            table;
        private final JLabel            summary;

        TallyPanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel title = new JLabel("Live tally");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
            add(title, BorderLayout.NORTH);

            model = new DefaultTableModel(
                    new Object[]{"#", "Candidate", "Party", "Votes", "% of valid"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
                @Override public Class<?> getColumnClass(int columnIndex) {
                    return (columnIndex == 0 || columnIndex == 3) ? Integer.class : String.class;
                }
            };
            table = new JTable(model);
            table.setRowHeight(24);
            table.getTableHeader().setReorderingAllowed(false);

            DefaultTableCellRenderer right = new DefaultTableCellRenderer();
            right.setHorizontalAlignment(SwingConstants.RIGHT);
            table.getColumnModel().getColumn(3).setCellRenderer(right);
            table.getColumnModel().getColumn(4).setCellRenderer(right);

            summary = new JLabel(" ");
            summary.setBorder(new EmptyBorder(8, 0, 0, 0));

            JButton refresh = new JButton("Refresh");
            refresh.addActionListener(e -> refresh());

            JPanel south = new JPanel(new BorderLayout());
            south.add(summary, BorderLayout.CENTER);
            south.add(refresh, BorderLayout.EAST);

            JPanel center = new JPanel(new BorderLayout(0, 8));
            center.add(new JScrollPane(table), BorderLayout.CENTER);
            center.add(south, BorderLayout.SOUTH);
            add(center, BorderLayout.CENTER);

            refresh();
        }

        @Override
        public void refresh() {
            TallyResult r = machine.tally();
            model.setRowCount(0);
            int valid = r.getValidVoteCount();
            for (Candidate c : election.getCandidates()) {
                int v = r.getVotesForCandidate(c.getNumber());
                double pct = valid == 0 ? 0.0 : (v * 100.0) / valid;
                model.addRow(new Object[]{
                        c.getNumber(),
                        c.getName(),
                        c.getParty(),
                        v,
                        String.format("%.1f%%", pct)
                });
            }
            summary.setText(String.format(
                    " Total cast: %d   |   Valid: %d   |   Abstain: %d   |   Election: %s",
                    r.getTotalVoteCount(), valid, r.getAbstainCount(),
                    election.isOpen() ? "OPEN" : "CLOSED"));
        }
    }

    // ---------------------------------------------------------------
    // AdminPanel
    // ---------------------------------------------------------------
    private final class AdminPanel extends JPanel implements Refreshable {
        private final JLabel  electionStatus = new JLabel();
        private final JButton openBtn        = new JButton("Open election");
        private final JButton closeBtn       = new JButton("Close election");
        private final JButton resetBtn       = new JButton("Reset (clear ballots & voted flags)");
        private final DefaultTableModel voterModel;
        private final JTable voterTable;

        AdminPanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JLabel title = new JLabel("Administration");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
            add(title, BorderLayout.NORTH);

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            controls.setBorder(BorderFactory.createTitledBorder("Election"));
            controls.add(electionStatus);
            controls.add(openBtn);
            controls.add(closeBtn);
            controls.add(resetBtn);

            openBtn.addActionListener(e -> {
                try {
                    election.open();
                    setStatus("Election opened.");
                } catch (RuntimeException ex) {
                    showError(ex.getMessage());
                }
                refresh();
            });
            closeBtn.addActionListener(e -> {
                election.close();
                setStatus("Election closed.");
                refresh();
            });
            resetBtn.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(
                        UI.this.frame,
                        "Clear all ballots and reset all voted flags?\nThis cannot be undone.",
                        "Confirm reset",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (ok == JOptionPane.OK_OPTION) {
                    machine.reset();
                    setStatus("Reset complete.");
                    refresh();
                }
            });

            voterModel = new DefaultTableModel(
                    new Object[]{"Name", "Birthday", "Voter ID", "Voted?"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            voterTable = new JTable(voterModel);
            voterTable.setRowHeight(24);
            JScrollPane scroller = new JScrollPane(voterTable);
            scroller.setBorder(BorderFactory.createTitledBorder("Registered voters"));

            JButton candBtn = new JButton("Show candidates…");
            candBtn.addActionListener(e -> showCandidates());

            JPanel north = new JPanel(new BorderLayout(0, 12));
            north.add(controls, BorderLayout.NORTH);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actions.add(candBtn);
            north.add(actions, BorderLayout.SOUTH);

            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.add(north, BorderLayout.NORTH);
            center.add(scroller, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);

            refresh();
        }

        @Override
        public void refresh() {
            electionStatus.setText("Status: " + (election.isOpen() ? "OPEN " : "CLOSED ") + "   ");
            openBtn.setEnabled(!election.isOpen());
            closeBtn.setEnabled(election.isOpen());

            voterModel.setRowCount(0);
            for (Voter v : registry.getAllVoters()) {
                voterModel.addRow(new Object[]{
                        v.getName(),
                        v.getBirthday().toString(),
                        v.getId(),
                        v.hasVoted() ? "Yes" : "No"
                });
            }
        }

        private void showCandidates() {
            StringBuilder sb = new StringBuilder();
            if (election.getCandidates().isEmpty()) {
                sb.append("(no candidates)");
            } else {
                for (Candidate c : election.getCandidates()) {
                    sb.append(c.getNumber()).append(". ")
                            .append(c.getName()).append(" (").append(c.getParty()).append(")\n");
                }
            }
            JOptionPane.showMessageDialog(
                    UI.this.frame, sb.toString(), "Candidates",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        private void showError(String msg) {
            JOptionPane.showMessageDialog(
                    UI.this.frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
            setStatus("Error: " + msg);
        }
    }
}
