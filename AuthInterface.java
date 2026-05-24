/**
 * AuthInterface — the contract for any ID-checking procedure.
 *
 * VotingMachine depends only on this interface, so the authentication
 * mechanism (PIN, smart card, biometric, ...) is a plug-in.
 */
public interface AuthInterface {
    boolean authenticate(String voterId, String credential);
}
