package campusnav.util;

import campusnav.model.User;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Local account service indexed by normalized username. Passwords use PBKDF2-HMAC-SHA-256. */
public class UserManager {
    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final HashMap<String, User> users;

    public UserManager() {
        users = DataStore.loadUsers();
        boolean hasFaculty = false;
        for (User user : users.values()) {
            if (user.isFaculty()) {
                hasFaculty = true;
                break;
            }
        }
        if (!hasFaculty) {
            User demo = new User("faculty", hash("faculty123"), "FACULTY");
            users.put(key(demo.getUsername()), demo);
            DataStore.saveUsers(users);
        }
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + toHex(salt) + "$" + toHex(derived);
    }

    public User login(String username, String password) throws AuthException {
        if (username == null || username.trim().length() == 0 || password == null || password.length() == 0) {
            throw new AuthException("Enter both your username and password.");
        }
        User user = users.get(key(username));
        if (user == null || !matches(user.getPasswordHash(), password)) {
            throw new AuthException("The username or password is incorrect.");
        }
        // Upgrade accounts written by the earlier String.hashCode prototype on successful login.
        if (!user.getPasswordHash().startsWith("pbkdf2$")) {
            user.setPasswordHash(hash(password));
            DataStore.saveUsers(users);
        }
        return user;
    }

    public User register(String username, String password, String confirm) throws AuthException {
        return register(username, password, confirm, "STUDENT");
    }

    public User register(String username, String password, String confirm, String requestedRole)
            throws AuthException {
        String name = username == null ? "" : username.trim();
        if (name.length() < 3 || name.length() > 32) {
            throw new AuthException("Username must contain 3 to 32 characters.");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                throw new AuthException("Username may contain letters, digits and underscore only.");
            }
        }
        if (password == null || password.length() < 8) {
            throw new AuthException("Use a password with at least 8 characters.");
        }
        if (!password.equals(confirm)) {
            throw new AuthException("The passwords do not match.");
        }
        String role = requestedRole == null ? "STUDENT" : requestedRole.trim().toUpperCase(Locale.ROOT);
        if (!"STUDENT".equals(role) && !"STAFF".equals(role)) {
            throw new AuthException("New accounts can be Student or Staff. Faculty access is managed separately.");
        }
        String normalized = key(name);
        if (users.containsKey(normalized)) {
            throw new AuthException("That username is already in use.");
        }
        User user = new User(name, hash(password), role);
        users.put(normalized, user);
        DataStore.saveUsers(users);
        return user;
    }

    public ArrayList<User> getAllUsers() {
        ArrayList<User> result = new ArrayList<User>(users.values());
        Collections.sort(result, new Comparator<User>() {
            public int compare(User a, User b) {
                return a.getUsername().compareToIgnoreCase(b.getUsername());
            }
        });
        return result;
    }

    public void deleteUser(String username) {
        if (username != null && !"faculty".equals(key(username))) {
            users.remove(key(username));
            DataStore.saveUsers(users);
        }
    }

    private boolean matches(String stored, String password) {
        if (stored == null) {
            return false;
        }
        if (!stored.startsWith("pbkdf2$")) {
            // Read-only compatibility path for accounts from the old demo; success upgrades the hash.
            return stored.equals(Integer.toString(password.hashCode()));
        }
        try {
            String[] part = stored.split("\\$", 4);
            if (part.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(part[1]);
            if (iterations < 10000 || iterations > 1000000) {
                return false;
            }
            byte[] salt = fromHex(part[2]);
            byte[] expected = fromHex(part[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("This Java runtime does not provide PBKDF2-HMAC-SHA-256.", ex);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            value.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        }
        return value.toString();
    }

    private static byte[] fromHex(String value) {
        if (value.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hash encoding.");
        }
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid hash encoding.");
            }
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }
}
