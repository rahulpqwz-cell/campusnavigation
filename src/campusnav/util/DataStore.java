package campusnav.util;

import campusnav.dsa.CampusGraph;
import campusnav.model.HistoryEntry;
import campusnav.model.Location;
import campusnav.model.MapLayer;
import campusnav.model.Report;
import campusnav.model.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** Local text-file persistence. Missing files are treated as an empty first-run store. */
public final class DataStore {
    private static final Path DATA_DIR = Paths.get("data");

    private DataStore() { }

    private static Path fileFor(String name) {
        return DATA_DIR.resolve(name);
    }

    public static ArrayList<String> readLines(String name) {
        Path file = fileFor(name);
        if (!Files.isRegularFile(file)) {
            return new ArrayList<String>();
        }
        try {
            return new ArrayList<String>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            System.err.println("Could not read " + name + ": " + ex.getMessage());
            return new ArrayList<String>();
        }
    }

    public static boolean writeLines(String name, ArrayList<String> lines) {
        Path file = fileFor(name);
        try {
            Files.createDirectories(DATA_DIR);
            Files.write(file, lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ex) {
            System.err.println("Could not save " + name + ": " + ex.getMessage());
            return false;
        }
    }

    // Accounts

    public static HashMap<String, User> loadUsers() {
        HashMap<String, User> users = new HashMap<String, User>();
        for (String line : readLines("users.txt")) {
            String[] part = line.split("\\|", 3);
            if (part.length == 3 && part[0].trim().length() > 0) {
                User user = new User(part[0], part[1], part[2]);
                users.put(user.getUsername().toLowerCase(java.util.Locale.ROOT), user);
            }
        }
        return users;
    }

    public static void saveUsers(HashMap<String, User> users) {
        ArrayList<User> ordered = new ArrayList<User>(users.values());
        Collections.sort(ordered, new Comparator<User>() {
            public int compare(User a, User b) {
                return a.getUsername().compareToIgnoreCase(b.getUsername());
            }
        });
        ArrayList<String> lines = new ArrayList<String>();
        for (User user : ordered) {
            lines.add(user.toFileString());
        }
        writeLines("users.txt", lines);
    }

    // Issue reports

    public static ArrayList<Report> loadReports() {
        ArrayList<Report> reports = new ArrayList<Report>();
        for (String line : readLines("reports.txt")) {
            try {
                if (line.trim().length() > 0) {
                    reports.add(Report.fromFileString(line));
                }
            } catch (RuntimeException ex) {
                System.err.println("Skipped a malformed report record.");
            }
        }
        Collections.sort(reports, new Comparator<Report>() {
            public int compare(Report a, Report b) {
                return a.getId() < b.getId() ? -1 : (a.getId() == b.getId() ? 0 : 1);
            }
        });
        return reports;
    }

    public static boolean saveReports(ArrayList<Report> reports) {
        ArrayList<String> lines = new ArrayList<String>();
        for (Report report : reports) {
            lines.add(report.toFileString());
        }
        return writeLines("reports.txt", lines);
    }

    // Per-user route history. The guest key is stored just like any other account key.

    public static ArrayList<HistoryEntry> loadHistory(String username) {
        ArrayList<HistoryEntry> result = new ArrayList<HistoryEntry>();
        String prefix = username + "|";
        for (String line : readLines("history.txt")) {
            if (line.startsWith(prefix)) {
                try {
                    result.add(HistoryEntry.fromFileString(line.substring(prefix.length())));
                } catch (RuntimeException ex) {
                    System.err.println("Skipped a malformed history record.");
                }
            }
        }
        return result;
    }

    public static void saveHistory(String username, ArrayList<HistoryEntry> oldestFirst) {
        String prefix = username + "|";
        ArrayList<String> lines = new ArrayList<String>();
        for (String line : readLines("history.txt")) {
            if (!line.startsWith(prefix)) {
                lines.add(line);
            }
        }
        for (HistoryEntry entry : oldestFirst) {
            lines.add(prefix + entry.toFileString());
        }
        writeLines("history.txt", lines);
    }

    // The graph file is editable and keeps the older six-field format readable.

    public static void saveCampus(CampusGraph graph) {
        ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i < graph.size(); i++) {
            Location location = graph.get(i);
            lines.add("L|" + location.getId() + "|" + encode(location.getName()) + "|"
                    + encode(location.getCategory()) + "|" + location.getX() + "|" + location.getY()
                    + "|" + location.getLayer().name() + "|" + encode(location.getBuilding()) + "|"
                    + encode(location.getFloor()) + "|" + encode(location.getRoom()) + "|"
                    + encode(location.getDescription()));
        }
        for (int a = 0; a < graph.size(); a++) {
            for (int b : graph.neighbours(a)) {
                if (b > a) {
                    lines.add("E|" + a + "|" + b + "|" + graph.getDistance(a, b) + "|"
                            + graph.isClosed(a, b) + "|" + edgeLayer(graph, a, b) + "|"
                            + encode(graph.getEdgeDescription(a, b)));
                }
            }
        }
        writeLines("campus.txt", lines);
    }

    public static CampusGraph loadCampus() {
        ArrayList<String> lines = readLines("campus.txt");
        if (lines.isEmpty()) {
            return null;
        }
        try {
            ArrayList<String[]> locationRows = new ArrayList<String[]>();
            ArrayList<String[]> edgeRows = new ArrayList<String[]>();
            for (String line : lines) {
                String[] part = line.split("\\|", -1);
                if (part.length > 0 && "L".equals(part[0])) {
                    locationRows.add(part);
                } else if (part.length > 0 && "E".equals(part[0])) {
                    edgeRows.add(part);
                }
            }
            Collections.sort(locationRows, new Comparator<String[]>() {
                public int compare(String[] a, String[] b) {
                    return integerAt(a, 1) - integerAt(b, 1);
                }
            });
            if (locationRows.isEmpty()) {
                return null;
            }

            CampusGraph graph = new CampusGraph();
            HashMap<Integer, Integer> ids = new HashMap<Integer, Integer>();
            for (String[] row : locationRows) {
                if (row.length < 6) {
                    throw new IllegalArgumentException("A location row is incomplete.");
                }
                int oldId = Integer.parseInt(row[1]);
                String name = decodeCompat(row[2]);
                String category = decodeCompat(row[3]);
                double x = Double.parseDouble(row[4]);
                double y = Double.parseDouble(row[5]);
                MapLayer layer = row.length > 6 ? MapLayer.parse(row[6]) : MapLayer.OUTDOOR;
                String building = row.length > 7 ? decodeCompat(row[7]) : "";
                String floor = row.length > 8 ? decodeCompat(row[8]) : "";
                String room = row.length > 9 ? decodeCompat(row[9]) : "";
                String description = row.length > 10 ? decodeCompat(row[10]) : "";
                int newId = graph.addLocation(name, category, x, y, layer, building, floor, room, description);
                ids.put(oldId, newId);
            }
            for (String[] row : edgeRows) {
                if (row.length < 4) {
                    continue;
                }
                int oldA = Integer.parseInt(row[1]);
                int oldB = Integer.parseInt(row[2]);
                Integer a = ids.get(oldA);
                Integer b = ids.get(oldB);
                if (a == null || b == null) {
                    continue;
                }
                double distance = Double.parseDouble(row[3]);
                MapLayer layer = row.length > 5 ? MapLayer.parse(row[5])
                        : graph.get(a).getLayer() == graph.get(b).getLayer() ? graph.get(a).getLayer() : MapLayer.TRANSITION;
                String description = row.length > 6 ? decodeCompat(row[6]) : "Follow the connected campus path.";
                graph.addPath(a, b, distance, description, layer);
                if (row.length > 4) {
                    graph.setClosed(a, b, Boolean.parseBoolean(row[4]));
                }
            }
            return graph.size() == 0 ? null : graph;
        } catch (RuntimeException ex) {
            System.err.println("campus.txt is damaged; the configurable demonstration map will be used.");
            return null;
        }
    }

    private static int integerAt(String[] row, int index) {
        try {
            return Integer.parseInt(row[index]);
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private static String edgeLayer(CampusGraph graph, int a, int b) {
        MapLayer layer = graph.getEdgeLayer(a, b);
        return layer == null ? MapLayer.TRANSITION.name() : layer.name();
    }

    private static String encode(String value) {
        return "b64:" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeCompat(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        if (!value.startsWith("b64:")) {
            return value;
        }
        try {
            return new String(java.util.Base64.getUrlDecoder().decode(value.substring(4)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
