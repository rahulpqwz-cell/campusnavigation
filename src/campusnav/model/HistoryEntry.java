package campusnav.model;

/** One searched route, kept in the user's history stack. */
public class HistoryEntry {
    private int sourceId;
    private int destinationId;
    private boolean fewestStops;
    private double distance;
    private String time;

    public HistoryEntry(int sourceId, int destinationId, boolean fewestStops, double distance, String time) {
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.fewestStops = fewestStops;
        this.distance = distance;
        this.time = time;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public boolean isFewestStops() {
        return fewestStops;
    }

    public double getDistance() {
        return distance;
    }

    public String getTime() {
        return time;
    }

    public String toFileString() {
        return sourceId + "," + destinationId + "," + fewestStops + "," + distance + "," + time;
    }

    public static HistoryEntry fromFileString(String s) {
        String[] p = s.split(",", 5);
        return new HistoryEntry(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                Boolean.parseBoolean(p[2]), Double.parseDouble(p[3]), p[4]);
    }
}
