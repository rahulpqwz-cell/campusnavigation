package campusnav.model;

/** Authenticated CampusNav account. Password material is stored only as a salted hash. */
public class User {
    private final String username;
    private String passwordHash;
    private final String role;

    public User(String username, String passwordHash, String role) {
        this.username = username == null ? "" : username.trim();
        this.passwordHash = passwordHash == null ? "" : passwordHash;
        this.role = normalizeRole(role);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash == null ? "" : passwordHash;
    }

    public String getRole() {
        return role;
    }

    public boolean isFaculty() {
        return "FACULTY".equals(role);
    }

    /** Kept for source compatibility with the original AdminFrame. */
    public boolean isAdmin() {
        return isFaculty();
    }

    public boolean isStudent() {
        return "STUDENT".equals(role);
    }

    public boolean isStaff() {
        return "STAFF".equals(role);
    }

    public boolean canReportIssues() {
        return isStudent() || isStaff();
    }

    public String toFileString() {
        return username + "|" + passwordHash + "|" + role;
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "STUDENT";
        }
        String value = role.trim().toUpperCase(java.util.Locale.ROOT);
        if ("ADMIN".equals(value) || "FACULTY".equals(value)) {
            return "FACULTY";
        }
        if ("STAFF".equals(value)) {
            return "STAFF";
        }
        return "STUDENT";
    }
}
