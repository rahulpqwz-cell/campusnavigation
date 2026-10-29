package campusnav.gui;

import campusnav.dsa.CampusGraph;
import campusnav.model.Location;
import campusnav.model.MapLayer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/** Custom Java2D map. The indoor view is a schematic fallback, drawn to scale with normalized points. */
public class MapPanel extends JPanel {
    public interface MapClickListener {
        void onLocationClicked(int id);
        void onEmptyClicked(double x, double y);
    }

    private static final int MARGIN = 44;
    private final CampusGraph graph;
    private ArrayList<Integer> highlight = new ArrayList<Integer>();
    private int selectedId = -1;
    private int currentPositionId = -1;
    private int destinationId = -1;
    private boolean showDistances = true;
    private MapLayer layer = MapLayer.OUTDOOR;
    private double markerX = -1;
    private double markerY = -1;
    private MapClickListener listener;

    public MapPanel(CampusGraph graph) {
        this.graph = graph;
        setBackground(new Color(229, 239, 226));
        setPreferredSize(new Dimension(560, 460));
        setMinimumSize(new Dimension(280, 240));
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                handleClick(event.getX(), event.getY());
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(MouseEvent event) {
                int id = hit(event.getX(), event.getY());
                setToolTipText(id < 0 ? null : graph.get(id).getDisplayName() + " (" + graph.get(id).getCategory() + ")");
            }
        });
    }

    public void setListener(MapClickListener value) { listener = value; }
    public void setLayer(MapLayer value) {
        layer = value == MapLayer.INDOOR ? MapLayer.INDOOR : MapLayer.OUTDOOR;
        repaint();
    }
    public MapLayer getLayer() { return layer; }
    public void setHighlightPath(ArrayList<Integer> path) {
        highlight = path == null ? new ArrayList<Integer>() : new ArrayList<Integer>(path);
        repaint();
    }
    public void setSelected(int id) { selectedId = id; repaint(); }
    public void setDestination(int id) { destinationId = id; repaint(); }
    public void setCurrentPosition(int id) { currentPositionId = id; repaint(); }
    public void setShowDistances(boolean value) { showDistances = value; repaint(); }
    public void setMarker(double x, double y) { markerX = x; markerY = y; repaint(); }

    private int margin() {
        return Math.max(24, Math.min(MARGIN, Math.min(getWidth(), getHeight()) / 9));
    }

    private int px(double x) {
        int m = margin();
        return (int) (m + x * (getWidth() - 2 * m));
    }

    private int py(double y) {
        int m = margin();
        return (int) (m + y * (getHeight() - 2 * m));
    }

    private int hit(int x, int y) {
        for (int i = graph.size() - 1; i >= 0; i--) {
            Location location = graph.get(i);
            if (location.getLayer() != layer) {
                continue;
            }
            int dx = x - px(location.getX());
            int dy = y - py(location.getY());
            int radius = "Room".equalsIgnoreCase(location.getCategory()) ? 42 : 18;
            if (dx * dx + dy * dy <= radius * radius) {
                return i;
            }
        }
        return -1;
    }

    private void handleClick(int x, int y) {
        int id = hit(x, y);
        if (id >= 0) {
            selectedId = id;
            repaint();
            if (listener != null) {
                listener.onLocationClicked(id);
            }
        } else if (listener != null) {
            int m = margin();
            double nx = (x - m) / (double) Math.max(1, getWidth() - 2 * m);
            double ny = (y - m) / (double) Math.max(1, getHeight() - 2 * m);
            listener.onEmptyClicked(Math.max(0, Math.min(1, nx)), Math.max(0, Math.min(1, ny)));
        }
    }

    public static Color categoryColor(String category) {
        String value = category == null ? "" : category.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("entrance")) return new Color(46, 125, 50);
        if (value.contains("library")) return new Color(121, 85, 72);
        if (value.contains("department")) return new Color(21, 101, 192);
        if (value.contains("hall") || value.contains("auditorium")) return new Color(0, 131, 143);
        if (value.contains("food")) return new Color(230, 126, 34);
        if (value.contains("hostel")) return new Color(173, 20, 87);
        if (value.contains("sport")) return new Color(85, 139, 47);
        if (value.contains("medical")) return new Color(211, 47, 47);
        if (value.contains("parking")) return new Color(69, 90, 100);
        if (value.contains("room")) return new Color(57, 73, 171);
        if (value.contains("corridor")) return new Color(96, 125, 139);
        if (value.contains("stair")) return new Color(245, 124, 0);
        return new Color(96, 96, 96);
    }

    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (layer == MapLayer.INDOOR) {
            paintIndoorBackground(g);
        } else {
            paintOutdoorBackground(g);
        }
        paintEdges(g);
        paintRoute(g);
        paintLocations(g);
        paintCurrentPosition(g);
        if (markerX >= 0 && layer == MapLayer.OUTDOOR) {
            int x = px(markerX);
            int y = py(markerY);
            g.setColor(new Color(220, 30, 30));
            g.setStroke(new BasicStroke(3f));
            g.drawLine(x - 9, y - 9, x + 9, y + 9);
            g.drawLine(x - 9, y + 9, x + 9, y - 9);
            g.drawOval(x - 13, y - 13, 26, 26);
        }
        g.dispose();
    }

    private void paintOutdoorBackground(Graphics2D g) {
        g.setColor(new Color(234, 243, 229));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(208, 226, 199));
        g.fillRoundRect(px(.36), py(.33), px(.67) - px(.36), py(.67) - py(.33), 38, 38);
        g.setColor(new Color(88, 124, 99));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("CAMPUS DEMONSTRATION MAP  •  NOT TO SCALE", 16, 22);
    }

    private void paintIndoorBackground(Graphics2D g) {
        g.setColor(new Color(248, 247, 241));
        g.fillRect(0, 0, getWidth(), getHeight());
        int left = px(.07);
        int top = py(.07);
        int width = px(.93) - left;
        int height = py(.94) - top;
        g.setColor(new Color(83, 97, 106));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(left, top, width, height, 12, 12);
        g.setColor(new Color(219, 233, 241));
        int hallX = px(.45);
        int hallY = py(.10);
        int hallW = px(.55) - hallX;
        int hallH = py(.88) - hallY;
        g.fillRoundRect(hallX, hallY, Math.max(24, hallW), hallH, 10, 10);
        g.setColor(new Color(72, 94, 107));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("CENTRAL CORRIDOR", hallX + 8, hallY + hallH / 2);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("DEPARTMENT FLOOR PLAN  •  DEMONSTRATION SCHEMATIC, NOT TO SCALE", 12, 22);
        g.setColor(new Color(255, 236, 179));
        g.fillRoundRect(px(.455), py(.39), Math.max(28, px(.545) - px(.455)), Math.max(28, py(.49) - py(.39)), 8, 8);
        g.setColor(new Color(102, 77, 26));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString("STAIRS", px(.46), py(.44));
    }

    private void paintEdges(Graphics2D g) {
        boolean drawDistances = showDistances && getWidth() >= 520;
        Font small = new Font("SansSerif", Font.PLAIN, 9);
        for (int a = 0; a < graph.size(); a++) {
            Location first = graph.get(a);
            if (first.getLayer() != layer) continue;
            for (int b : graph.neighbours(a)) {
                if (b <= a) continue;
                Location second = graph.get(b);
                if (second.getLayer() != layer) continue;
                int x1 = px(first.getX());
                int y1 = py(first.getY());
                int x2 = px(second.getX());
                int y2 = py(second.getY());
                if (graph.isClosed(a, b)) {
                    g.setColor(new Color(198, 61, 61));
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            10f, new float[] {6f, 5f}, 0f));
                } else {
                    g.setColor(layer == MapLayer.INDOOR ? new Color(122, 146, 157) : new Color(153, 164, 164));
                    g.setStroke(new BasicStroke(layer == MapLayer.INDOOR ? 2.5f : 3f));
                }
                g.drawLine(x1, y1, x2, y2);
                if (drawDistances && layer == MapLayer.OUTDOOR) {
                    String value = (int) graph.getDistance(a, b) + " m";
                    g.setFont(small);
                    g.setColor(new Color(72, 82, 88));
                    g.drawString(value, (x1 + x2) / 2, (y1 + y2) / 2 - 2);
                }
            }
        }
    }

    private void paintRoute(Graphics2D g) {
        if (highlight.size() < 2) return;
        g.setColor(new Color(235, 117, 35));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < highlight.size() - 1; i++) {
            Location first = graph.get(highlight.get(i));
            Location second = graph.get(highlight.get(i + 1));
            if (first.getLayer() == layer && second.getLayer() == layer) {
                g.drawLine(px(first.getX()), py(first.getY()), px(second.getX()), py(second.getY()));
            }
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        for (int i = 0; i < highlight.size(); i++) {
            Location point = graph.get(highlight.get(i));
            if (point.getLayer() != layer || point.getCategory().equalsIgnoreCase("Corridor")
                    || point.getCategory().equalsIgnoreCase("Doorway")) continue;
            int x = px(point.getX());
            int y = py(point.getY());
            g.setColor(new Color(255, 255, 255, 235));
            g.fillOval(x + 5, y - 15, 20, 20);
            g.setColor(new Color(171, 78, 20));
            g.drawOval(x + 5, y - 15, 20, 20);
            g.drawString(Integer.toString(i + 1), x + 12, y - 1);
        }
    }

    private void paintLocations(Graphics2D g) {
        int fontSize = Math.max(9, Math.min(13, getWidth() / 54));
        Font labelFont = new Font("SansSerif", Font.BOLD, fontSize);
        FontMetrics metrics = g.getFontMetrics(labelFont);
        for (int i = 0; i < graph.size(); i++) {
            Location location = graph.get(i);
            if (location.getLayer() != layer) continue;
            int x = px(location.getX());
            int y = py(location.getY());
            String category = location.getCategory();
            if (layer == MapLayer.INDOOR && "Room".equalsIgnoreCase(category)) {
                int roomWidth = Math.max(68, Math.min(100, getWidth() / 7));
                int roomHeight = Math.max(28, Math.min(40, getHeight() / 12));
                g.setColor(i == selectedId ? new Color(255, 242, 185) : Color.WHITE);
                g.fillRoundRect(x - roomWidth / 2, y - roomHeight / 2, roomWidth, roomHeight, 7, 7);
                g.setColor(categoryColor(category));
                g.setStroke(new BasicStroke(i == selectedId ? 3f : 1.8f));
                g.drawRoundRect(x - roomWidth / 2, y - roomHeight / 2, roomWidth, roomHeight, 7, 7);
                g.setFont(labelFont);
                g.drawString(location.getRoom(), x - metrics.stringWidth(location.getRoom()) / 2, y + metrics.getAscent() / 2 - 1);
                continue;
            }
            int radius = "Corridor".equalsIgnoreCase(category) || "Doorway".equalsIgnoreCase(category) ? 4 : 8;
            g.setColor(categoryColor(category));
            g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            if (!"Corridor".equalsIgnoreCase(category) && !"Doorway".equalsIgnoreCase(category)) {
                String text = layer == MapLayer.INDOOR ? location.getName() : location.getDisplayName();
                int tx = Math.max(3, Math.min(getWidth() - metrics.stringWidth(text) - 8, x - metrics.stringWidth(text) / 2));
                int ty = y + radius + metrics.getHeight() + 3;
                g.setFont(labelFont);
                g.setColor(new Color(255, 255, 255, 225));
                g.fillRoundRect(tx - 3, ty - metrics.getAscent(), metrics.stringWidth(text) + 6, metrics.getHeight(), 7, 7);
                g.setColor(new Color(35, 48, 55));
                g.drawString(text, tx, ty);
            }
        }
        for (int marker : new int[] {selectedId, destinationId}) {
            if (marker >= 0 && marker < graph.size() && graph.get(marker).getLayer() == layer) {
                int x = px(graph.get(marker).getX());
                int y = py(graph.get(marker).getY());
                g.setColor(marker == destinationId ? new Color(200, 48, 48) : new Color(255, 190, 0));
                g.setStroke(new BasicStroke(2.5f));
                g.drawOval(x - 14, y - 14, 28, 28);
            }
        }
    }

    private void paintCurrentPosition(Graphics2D g) {
        int id = currentPositionId;
        if (id < 0 || id >= graph.size()) return;
        Location current = graph.get(id);
        if (current.getLayer() != layer) return;
        int x = px(current.getX());
        int y = py(current.getY());
        g.setColor(new Color(36, 126, 225, 65));
        g.fillOval(x - 18, y - 18, 36, 36);
        g.setColor(new Color(23, 104, 205));
        g.fillOval(x - 8, y - 8, 16, 16);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(x - 8, y - 8, 16, 16);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(new Color(24, 72, 128));
        g.drawString("YOU HERE • SIMULATED", x + 12, y - 10);
    }
}
