import java.util.HashMap;
import java.util.Map;

/**
 * PinAuthenticator — verifies a voter against a stored PIN.
 *
 * The map is voterId -> pin. Voters set their own PIN at registration time
 * via setPin(); the admin no longer hard-codes credentials. A future
 * IdCardAuthenticator or biometric implementation would simply implement
 * AuthInterface differently.
 */
public class PinAuthenticator implements AuthInterface {

    private static final int MIN_PIN_LENGTH = 4;

    private final Map<String, String> pins = new HashMap<>();

    public PinAuthenticator() {
    }

    /**
     * Set or update the PIN for a voter. The voter is expected to call this
     * themselves at registration; admins should not pass PINs in by hand.
     */
    public void setPin(String voterId, String pin) {
        if (voterId == null || voterId.isBlank()) {
            throw new IllegalArgumentException("voterId is required");
        }
        if (pin == null || pin.length() < MIN_PIN_LENGTH) {
            throw new IllegalArgumentException(
                    "PIN must be at least " + MIN_PIN_LENGTH + " characters");
        }
        if (!pin.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("PIN must contain digits only");
        }
        pins.put(voterId, pin);
    }

    public boolean hasPin(String voterId) {
        return pins.containsKey(voterId);
    }

    @Override
    public boolean authenticate(String voterId, String credential) {
        if (voterId == null || credential == null) {
            return false;
        }
        String expected = pins.get(voterId);
        return expected != null && expected.equals(credential);
    }
}
