package campusnav.model;

import java.util.ArrayList;

/** Result of a route search: the list of location ids from start to destination. */
public class Route {
    private ArrayList<Integer> path;
    private double distance;
    private String mode;

    public Route(ArrayList<Integer> path, double distance, String mode) {
        this.path = new ArrayList<Integer>(path);
        this.distance = distance;
        this.mode = mode;
    }

    public ArrayList<Integer> getPath() {
        return new ArrayList<Integer>(path);
    }

    public double getDistance() {
        return distance;
    }

    public String getMode() {
        return mode;
    }

    public int getStops() {
        return path.size();
    }

    /** Walking time in minutes at about 80 metres per minute. */
    public int getWalkMinutes() {
        int m = (int) Math.ceil(distance / 80.0);
        if (m < 1) {
            m = 1;
        }
        return m;
    }
}
