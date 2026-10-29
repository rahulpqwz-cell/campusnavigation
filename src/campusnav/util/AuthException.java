package campusnav.util;

/** User-defined checked exception for login and registration problems. */
public class AuthException extends Exception {
    public AuthException(String message) {
        super(message);
    }
}
