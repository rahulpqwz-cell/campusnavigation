package campusnav.model;

/**
 * A place on the campus (a node of the campus graph).
 * x and y are map positions between 0.0 and 1.0 so the map can scale to any window size.
 */
public class Location implements Comparable<Location> {
    private final int id;
    private final String name;
    private final String category;
    private final double x;
    private final double y;
    private final MapLayer layer;
    private final String building;
    private final String floor;
    private final String room;
    private final String description;

    public Location(int id, String name, String category, double x, double y) {
        this(id, name, category, x, y, MapLayer.OUTDOOR, "", "", "", "");
    }

    public Location(int id, String name, String category, double x, double y, MapLayer layer,
                    String building, String floor, String room, String description) {
        this.id = id;
        this.name = safe(name);
        this.category = safe(category);
        this.x = x;
        this.y = y;
        this.layer = layer == null ? MapLayer.OUTDOOR : layer;
        this.building = safe(building);
        this.floor = safe(floor);
        this.room = safe(room);
        this.description = safe(description);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public MapLayer getLayer() {
        return layer;
    }

    public String getBuilding() {
        return building;
    }

    public String getFloor() {
        return floor;
    }

    public String getRoom() {
        return room;
    }

    public String getDescription() {
        return description;
    }

    public boolean isIndoor() {
        return layer == MapLayer.INDOOR;
    }

    public boolean isDestination() {
        return layer == MapLayer.OUTDOOR || "Room".equalsIgnoreCase(category);
    }

    public String getDisplayName() {
        if (isIndoor() && "Room".equalsIgnoreCase(category) && room.length() > 0) {
            return room + " — " + building;
        }
        return name;
    }

    public boolean matches(String query) {
        if (query == null || query.trim().length() == 0) {
            return true;
        }
        String value = query.trim().toLowerCase(java.util.Locale.ROOT);
        return name.toLowerCase(java.util.Locale.ROOT).contains(value)
                || category.toLowerCase(java.util.Locale.ROOT).contains(value)
                || building.toLowerCase(java.util.Locale.ROOT).contains(value)
                || floor.toLowerCase(java.util.Locale.ROOT).contains(value)
                || room.toLowerCase(java.util.Locale.ROOT).contains(value)
                || description.toLowerCase(java.util.Locale.ROOT).contains(value);
    }

    public int compareTo(Location other) {
        return name.compareToIgnoreCase(other.name);
    }

    public String toString() {
        return getDisplayName();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
