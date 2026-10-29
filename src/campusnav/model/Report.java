package campusnav.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A locally stored maintenance or facility report. */
public class Report {
    private final int id;
    private final String username;
    private final String location;
    private final String floor;
    private final String room;
    private final String type;
    private final String priority;
    private final String description;
    private String status;
    private final String date;
    private String facultyResponse;

    public Report(int id, String username, String location, String type,
                  String description, String status, String date) {
        this(id, username, location, "", "", type, "Normal", description, status, date, "");
    }

    public Report(int id, String username, String location, String floor, String room,
                  String type, String priority, String description, String status,
                  String date, String facultyResponse) {
        this.id = id;
        this.username = safe(username);
        this.location = safe(location);
        this.floor = safe(floor);
        this.room = safe(room);
        this.type = safe(type);
        this.priority = safe(priority).length() == 0 ? "Normal" : safe(priority);
        this.description = safe(description);
        this.status = normalizeStatus(status);
        this.date = safe(date);
        this.facultyResponse = safe(facultyResponse);
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getLocation() { return location; }
    public String getFloor() { return floor; }
    public String getRoom() { return room; }
    public String getType() { return type; }
    public String getPriority() { return priority; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getFacultyResponse() { return facultyResponse; }

    public void setStatus(String status) { this.status = normalizeStatus(status); }
    public void setFacultyResponse(String response) { this.facultyResponse = safe(response); }

    public String toFileString() {
        return "R|" + id + "|" + encode(username) + "|" + encode(location) + "|" + encode(floor)
                + "|" + encode(room) + "|" + encode(type) + "|" + encode(priority) + "|"
                + status + "|" + encode(date) + "|" + encode(description) + "|" + encode(facultyResponse);
    }

    public static Report fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length == 7 && !"R".equals(p[0])) {
            return new Report(Integer.parseInt(p[0]), p[1], p[2], p[3], p[6], p[4], p[5]);
        }
        if (p.length != 12 || !"R".equals(p[0])) {
            throw new IllegalArgumentException("Invalid report row.");
        }
        return new Report(Integer.parseInt(p[1]), decode(p[2]), decode(p[3]), decode(p[4]),
                decode(p[5]), decode(p[6]), decode(p[7]), decode(p[10]), p[8], decode(p[9]), decode(p[11]));
    }

    public String toString() {
        return "#" + id + "  [" + status + "]  " + location + " — " + type + " (" + priority + ")";
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(safe(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value.length() == 0) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String normalizeStatus(String value) {
        if ("In Progress".equalsIgnoreCase(value)) {
            return "In Progress";
        }
        if ("Resolved".equalsIgnoreCase(value)) {
            return "Resolved";
        }
        return "Pending";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
