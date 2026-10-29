package campusnav.gui;

import campusnav.dsa.CampusGraph;
import campusnav.dsa.SimpleStack;
import campusnav.model.HistoryEntry;
import campusnav.model.MapLayer;
import campusnav.model.Report;
import campusnav.model.Route;
import campusnav.model.User;
import campusnav.util.DataStore;
import campusnav.util.UserManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/** Central navigation controller for shared data, screen changes and demo route progress. */
public final class Navigation {
    private static final String GUEST_HISTORY_KEY = "__guest__";
    private static CampusGraph graph;
    private static UserManager userManager;
    private static ArrayList<Report> reports;
    private static User currentUser;
    private static SimpleStack<HistoryEntry> history = new SimpleStack<HistoryEntry>();
    private static Route currentRoute;
    private static int sourceId = -1;
    private static int destinationId = -1;
    private static int focusId = -1;
    private static int routeStep;
    private static boolean demoRunning;
    private static boolean pendingReport;

    private Navigation() { }

    public static void init() {
        graph = DataStore.loadCampus();
        if (graph == null || !hasIndoorDemo(graph)) {
            graph = CampusGraph.createDefault();
            DataStore.saveCampus(graph);
        }
        userManager = new UserManager();
        reports = DataStore.loadReports();
        currentUser = null;
        history = loadStack(GUEST_HISTORY_KEY);
        currentRoute = null;
        routeStep = 0;
        demoRunning = false;
        pendingReport = false;
    }

    private static boolean hasIndoorDemo(CampusGraph candidate) {
        for (int i = 0; i < candidate.size(); i++) {
            if (candidate.get(i).isIndoor() && candidate.get(i).getRoom().length() > 0) {
                return true;
            }
        }
        return false;
    }

    private static SimpleStack<HistoryEntry> loadStack(String key) {
        SimpleStack<HistoryEntry> result = new SimpleStack<HistoryEntry>();
        for (HistoryEntry entry : DataStore.loadHistory(key)) {
            result.push(entry);
        }
        return result;
    }

    public static CampusGraph getGraph() { return graph; }
    public static UserManager getUserManager() { return userManager; }
    public static User getCurrentUser() { return currentUser; }
    public static Route getCurrentRoute() { return currentRoute; }
    public static ArrayList<Report> getReports() { return reports; }
    public static SimpleStack<HistoryEntry> getHistory() { return history; }
    public static int getSourceId() { return sourceId; }
    public static int getDestinationId() { return destinationId; }
    public static int getFocusId() { return focusId; }
    public static int getRouteStep() { return routeStep; }
    public static boolean isDemoRunning() { return demoRunning; }
    public static boolean hasPendingReport() { return pendingReport; }

    public static int getRouteStepCount() {
        return currentRoute == null ? 0 : currentRoute.getPath().size();
    }

    public static void setSourceId(int id) {
        sourceId = id >= 0 && id < graph.size() ? id : -1;
    }

    public static void setDestinationId(int id) {
        destinationId = id >= 0 && id < graph.size() ? id : -1;
    }

    public static void setFocusId(int id) {
        focusId = id >= 0 && id < graph.size() ? id : -1;
    }

    public static void setPendingReport(boolean pending) { pendingReport = pending; }
    public static void saveCampus() { DataStore.saveCampus(graph); }
    public static boolean saveReports() { return DataStore.saveReports(reports); }

    public static int nextReportId() {
        int max = 0;
        for (Report report : reports) {
            max = Math.max(max, report.getId());
        }
        return max + 1;
    }

    public static String now() {
        return new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date());
    }

    public static void loginUser(User user) {
        saveHistory();
        boolean keepGuestContext = pendingReport;
        currentUser = user;
        history = loadStack(user.getUsername());
        if (!keepGuestContext) {
            sourceId = -1;
            destinationId = -1;
            focusId = -1;
            currentRoute = null;
            routeStep = 0;
            demoRunning = false;
        }
    }

    public static void continueAsGuest() {
        if (currentUser != null) {
            saveHistory();
        }
        currentUser = null;
        history = loadStack(GUEST_HISTORY_KEY);
        currentRoute = null;
        routeStep = 0;
        demoRunning = false;
    }

    public static void afterLogin(JFrame from) {
        if (pendingReport) {
            pendingReport = false;
            showReport(from);
        } else {
            showHome(from);
        }
    }

    public static void logout(JFrame from) {
        saveHistory();
        continueAsGuest();
        pendingReport = false;
        showWelcome(from);
    }

    /** Calculate one continuous route across outdoor paths, a building entrance and indoor corridors. */
    public static Route findRoute(int source, int destination, boolean fewestStops) {
        if (graph == null || source < 0 || source >= graph.size()
                || destination < 0 || destination >= graph.size()) {
            return null;
        }
        Route result = fewestStops ? graph.fewestStops(source, destination)
                : graph.shortestPath(source, destination);
        if (result != null && !result.getPath().isEmpty()) {
            currentRoute = result;
            sourceId = source;
            destinationId = destination;
            focusId = destination;
            routeStep = 0;
            demoRunning = false;
            history.push(new HistoryEntry(source, destination, fewestStops, result.getDistance(), now()));
            saveHistory();
        }
        return result;
    }

    public static void startDemoNavigation() {
        if (currentRoute != null && !currentRoute.getPath().isEmpty()) {
            routeStep = 0;
            demoRunning = true;
        }
    }

    public static void stopDemoNavigation() {
        demoRunning = false;
        routeStep = 0;
    }

    public static void advanceRouteStep() {
        if (currentRoute != null && routeStep < currentRoute.getPath().size() - 1) {
            routeStep++;
        }
    }

    public static void previousRouteStep() {
        if (routeStep > 0) {
            routeStep--;
        }
    }

    public static void setRouteStep(int step) {
        if (currentRoute == null) {
            routeStep = 0;
            return;
        }
        routeStep = Math.max(0, Math.min(step, currentRoute.getPath().size() - 1));
    }

    public static MapLayer getCurrentMapLayer() {
        if (currentRoute != null && !currentRoute.getPath().isEmpty()) {
            ArrayList<Integer> path = currentRoute.getPath();
            return graph.get(path.get(Math.min(routeStep, path.size() - 1))).getLayer();
        }
        if (focusId >= 0 && focusId < graph.size()) {
            return graph.get(focusId).getLayer();
        }
        return MapLayer.OUTDOOR;
    }

    public static int getCurrentPositionId() {
        if (currentRoute == null || currentRoute.getPath().isEmpty()) {
            return -1;
        }
        ArrayList<Integer> path = currentRoute.getPath();
        return path.get(Math.min(routeStep, path.size() - 1));
    }

    public static void saveHistory() {
        ArrayList<HistoryEntry> newestFirst = history.toList();
        ArrayList<HistoryEntry> oldestFirst = new ArrayList<HistoryEntry>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            oldestFirst.add(newestFirst.get(i));
        }
        DataStore.saveHistory(currentUser == null ? GUEST_HISTORY_KEY : currentUser.getUsername(), oldestFirst);
    }

    public static void switchTo(JFrame from, JFrame to) {
        if (from != null) {
            to.setBounds(from.getBounds());
            to.setExtendedState(from.getExtendedState());
        }
        to.setVisible(true);
        if (from != null) {
            from.dispose();
        }
    }

    public static void showWelcome(JFrame from) { switchTo(from, new WelcomeFrame()); }
    public static void showLogin(JFrame from) { switchTo(from, new LoginFrame()); }
    public static void showHome(JFrame from) { switchTo(from, new HomeFrame()); }
    public static void showSearch(JFrame from) { switchTo(from, new SearchFrame()); }
    public static void showDestination(JFrame from) { switchTo(from, new DestinationFrame()); }
    public static void showRoute(JFrame from) { switchTo(from, new RouteFrame()); }

    public static void showReport(JFrame from) {
        if (currentUser == null) {
            pendingReport = true;
            showLogin(from);
            return;
        }
        if (!currentUser.canReportIssues()) {
            JOptionPane.showMessageDialog(from, "Issue reports are available to Student and Staff accounts.",
                    "Account access", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        switchTo(from, new ReportIssueFrame());
    }

    public static void showMap(JFrame from) { switchTo(from, new MapFrame()); }
    public static void showFacilities(JFrame from) { switchTo(from, new FacilitiesFrame()); }
    public static void showHistory(JFrame from) { switchTo(from, new HistoryFrame()); }
    public static void showHelp(JFrame from) { switchTo(from, new HelpFrame()); }

    public static void showAdmin(JFrame from, int tab) {
        if (currentUser == null || !currentUser.isFaculty()) {
            JOptionPane.showMessageDialog(from, "Only authenticated Faculty accounts can manage reports.",
                    "Account access", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        switchTo(from, new AdminFrame(tab));
    }
}
