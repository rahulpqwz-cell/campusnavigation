package campusnav.dsa;

import campusnav.data.CampusData;
import campusnav.model.Location;
import campusnav.model.MapLayer;
import campusnav.model.Route;
import java.util.ArrayList;
import java.util.Collections;

/**
 * An undirected weighted adjacency-list graph. Dijkstra selects the next node by scanning
 * the unvisited distances, so the shortest-path algorithm is implemented here directly.
 */
public class CampusGraph {
    public static final double INF = Double.POSITIVE_INFINITY;
    private final ArrayList<Location> locations = new ArrayList<Location>();
    private final ArrayList<ArrayList<Edge>> adjacency = new ArrayList<ArrayList<Edge>>();

    private static final class Edge {
        private final int target;
        private final double distance;
        private final String description;
        private final MapLayer layer;
        private boolean closed;

        private Edge(int target, double distance, String description, MapLayer layer) {
            this.target = target;
            this.distance = distance;
            this.description = description;
            this.layer = layer;
        }
    }

    public int addLocation(String name, String category, double x, double y) {
        return addLocation(new Location(locations.size(), name, category, x, y));
    }

    public int addLocation(String name, String category, double x, double y, MapLayer layer,
                           String building, String floor, String room, String description) {
        Location location = new Location(locations.size(), name, category, x, y, layer,
                building, floor, room, description);
        return addLocation(location);
    }

    public int addLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("A location is required.");
        }
        if (locations.size() >= 200) {
            throw new IllegalStateException("The demonstration map supports at most 200 nodes.");
        }
        int id = locations.size();
        locations.add(new Location(id, location.getName(), location.getCategory(), location.getX(),
                location.getY(), location.getLayer(), location.getBuilding(), location.getFloor(),
                location.getRoom(), location.getDescription()));
        adjacency.add(new ArrayList<Edge>());
        return id;
    }

    public void addPath(int a, int b, double distance) {
        addPath(a, b, distance, "Follow the connected campus path.",
                layerFor(a, b));
    }

    public void addPath(int a, int b, double distance, String description, MapLayer edgeLayer) {
        requireNode(a);
        requireNode(b);
        if (a == b || !Double.isFinite(distance) || distance <= 0) {
            throw new IllegalArgumentException("A path needs two different nodes and a positive distance.");
        }
        putEdge(a, b, distance, description, edgeLayer);
        putEdge(b, a, distance, description, edgeLayer);
    }

    private void putEdge(int from, int to, double distance, String description, MapLayer layer) {
        ArrayList<Edge> edges = adjacency.get(from);
        for (Edge edge : edges) {
            if (edge.target == to) {
                edge.closed = false;
                // The most recently configured path description is the authoritative one.
                edges.set(edges.indexOf(edge), new Edge(to, distance, safe(description), layer));
                return;
            }
        }
        edges.add(new Edge(to, distance, safe(description), layer));
    }

    private MapLayer layerFor(int a, int b) {
        requireNode(a);
        requireNode(b);
        return locations.get(a).getLayer() == locations.get(b).getLayer()
                ? locations.get(a).getLayer() : MapLayer.TRANSITION;
    }

    private static String safe(String value) {
        return value == null ? "Follow the connected path." : value.trim();
    }

    public int size() {
        return locations.size();
    }

    public Location get(int id) {
        requireNode(id);
        return locations.get(id);
    }

    public ArrayList<Location> getLocations() {
        return new ArrayList<Location>(locations);
    }

    /** Public destinations exclude graph-only corridor and staircase nodes. */
    public ArrayList<Location> getDestinations() {
        ArrayList<Location> result = new ArrayList<Location>();
        for (Location location : locations) {
            if (location.isDestination()) {
                result.add(location);
            }
        }
        return result;
    }

    public boolean hasPath(int a, int b) {
        return valid(a) && valid(b) && findEdge(a, b) != null;
    }

    public boolean isClosed(int a, int b) {
        Edge edge = findEdge(a, b);
        return edge != null && edge.closed;
    }

    public void setClosed(int a, int b, boolean value) {
        Edge forward = findEdge(a, b);
        Edge reverse = findEdge(b, a);
        if (forward != null && reverse != null) {
            forward.closed = value;
            reverse.closed = value;
        }
    }

    public double getDistance(int a, int b) {
        Edge edge = findEdge(a, b);
        return edge == null ? 0 : edge.distance;
    }

    public String getEdgeDescription(int a, int b) {
        Edge edge = findEdge(a, b);
        return edge == null ? "" : edge.description;
    }

    public MapLayer getEdgeLayer(int a, int b) {
        Edge edge = findEdge(a, b);
        return edge == null ? null : edge.layer;
    }

    public ArrayList<Integer> neighbours(int id) {
        requireNode(id);
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (Edge edge : adjacency.get(id)) {
            result.add(edge.target);
        }
        Collections.sort(result);
        return result;
    }

    public ArrayList<String> getCategories() {
        ArrayList<String> result = new ArrayList<String>();
        for (Location location : locations) {
            if (location.isDestination() && !result.contains(location.getCategory())) {
                result.add(location.getCategory());
            }
        }
        Collections.sort(result);
        return result;
    }

    public String direction(int a, int b) {
        requireNode(a);
        requireNode(b);
        double dx = locations.get(b).getX() - locations.get(a).getX();
        double dy = locations.get(a).getY() - locations.get(b).getY();
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        int idx = (int) Math.round(angle / 45.0);
        String[] names = {"East", "North-East", "North", "North-West", "West", "South-West", "South", "South-East"};
        return names[((idx % 8) + 8) % 8];
    }

    public double[] distancesFrom(int source) {
        double[] distance = new double[size()];
        int[] previous = new int[size()];
        if (!valid(source)) {
            for (int i = 0; i < distance.length; i++) {
                distance[i] = INF;
            }
            return distance;
        }
        dijkstra(source, distance, previous);
        return distance;
    }

    public Route shortestPath(int source, int destination) {
        if (!valid(source) || !valid(destination)) {
            return null;
        }
        double[] distance = new double[size()];
        int[] previous = new int[size()];
        dijkstra(source, distance, previous);
        if (!Double.isFinite(distance[destination])) {
            return null;
        }
        return buildRoute(source, destination, previous, distance[destination], "Shortest distance (Dijkstra)");
    }

    private void dijkstra(int source, double[] distance, int[] previous) {
        boolean[] visited = new boolean[size()];
        for (int i = 0; i < size(); i++) {
            distance[i] = INF;
            previous[i] = -1;
        }
        distance[source] = 0;
        for (int pass = 0; pass < size(); pass++) {
            int current = -1;
            for (int node = 0; node < size(); node++) {
                if (!visited[node] && Double.isFinite(distance[node])
                        && (current < 0 || distance[node] < distance[current])) {
                    current = node;
                }
            }
            if (current < 0) {
                break;
            }
            visited[current] = true;
            for (Edge edge : adjacency.get(current)) {
                int next = edge.target;
                if (!edge.closed && !visited[next] && distance[current] + edge.distance < distance[next]) {
                    distance[next] = distance[current] + edge.distance;
                    previous[next] = current;
                }
            }
        }
    }

    public Route fewestStops(int source, int destination) {
        if (!valid(source) || !valid(destination)) {
            return null;
        }
        boolean[] visited = new boolean[size()];
        int[] previous = new int[size()];
        double[] distance = new double[size()];
        for (int i = 0; i < size(); i++) {
            previous[i] = -1;
            distance[i] = INF;
        }
        SimpleQueue<Integer> queue = new SimpleQueue<Integer>();
        visited[source] = true;
        distance[source] = 0;
        queue.enqueue(source);
        while (!queue.isEmpty()) {
            int current = queue.dequeue();
            if (current == destination) {
                break;
            }
            for (Edge edge : adjacency.get(current)) {
                if (!edge.closed && !visited[edge.target]) {
                    visited[edge.target] = true;
                    previous[edge.target] = current;
                    distance[edge.target] = distance[current] + edge.distance;
                    queue.enqueue(edge.target);
                }
            }
        }
        return visited[destination]
                ? buildRoute(source, destination, previous, distance[destination], "Fewest stops (BFS)") : null;
    }

    public boolean[] reachableFrom(int source) {
        boolean[] visited = new boolean[size()];
        if (!valid(source)) {
            return visited;
        }
        SimpleStack<Integer> stack = new SimpleStack<Integer>();
        stack.push(source);
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (visited[current]) {
                continue;
            }
            visited[current] = true;
            for (Edge edge : adjacency.get(current)) {
                if (!edge.closed && !visited[edge.target]) {
                    stack.push(edge.target);
                }
            }
        }
        return visited;
    }

    private Route buildRoute(int source, int destination, int[] previous, double distance, String mode) {
        SimpleStack<Integer> reverse = new SimpleStack<Integer>();
        int current = destination;
        for (int count = 0; current >= 0 && count <= size(); count++) {
            reverse.push(current);
            if (current == source) {
                ArrayList<Integer> path = new ArrayList<Integer>();
                while (!reverse.isEmpty()) {
                    path.add(reverse.pop());
                }
                return new Route(path, distance, mode);
            }
            current = previous[current];
        }
        return null;
    }

    public static CampusGraph createDefault() {
        return CampusData.createGraph();
    }

    private boolean valid(int id) {
        return id >= 0 && id < locations.size();
    }

    private void requireNode(int id) {
        if (!valid(id)) {
            throw new IllegalArgumentException("Unknown map node: " + id);
        }
    }

    private Edge findEdge(int from, int to) {
        if (!valid(from) || !valid(to)) {
            return null;
        }
        for (Edge edge : adjacency.get(from)) {
            if (edge.target == to) {
                return edge;
            }
        }
        return null;
    }
}
