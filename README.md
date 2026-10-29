# CampusNav

CampusNav is an offline Java Swing campus navigation demo. Guests can search places, view the schematic map, and follow simulated outdoor-to-indoor routes. Student and Staff accounts can submit local issue reports; Faculty accounts can manage saved reports.

## Run

**Windows:** double-click `run.bat` or run `./run.bat` from this folder. It uses `JAVA_HOME` when set, checks the common JDK 17 install path, then falls back to `PATH`.

**Linux/macOS:** run `sh run.sh` with a JDK on `PATH`.

**IDE:** open this folder as a Java project and run `campusnav.Main`.

The app creates a `data/` folder in its working directory for accounts, reports, route history, and the editable campus graph. First launch seeds a demo Faculty account: **faculty / faculty123**. Guests can register as Student or Staff. Faculty accounts cannot be created from the UI.

## Project layout

```text
src/campusnav/
  Main.java
  data/CampusData.java       configurable campus and indoor demo graph
  dsa/CampusGraph.java       adjacency-list graph, Dijkstra, BFS, DFS
  dsa/SimpleStack.java       route history, path reconstruction and DFS
  dsa/SimpleQueue.java       BFS and first-in report processing
  gui/                       Welcome, Login, Home, Search, Destination, Route,
                             Map, Facilities, Report Issue, Faculty/Admin,
                             History and Help screens
  model/                     Location, Route, User, Report and history records
  util/                      local persistence, password authentication and sorting
```

## Demo data and limits

Campus paths and distances in `CampusData.java` are demonstration values, not surveyed measurements. The indoor D1–D11, D4A/D4B, D5A/D5B and D11A labels use a corridor-and-doorway schematic. Room functions and the exact floor layout are unknown. The floor-plan image was not available in the workspace, so the app draws a Java2D fallback marked as a demonstration map. Main Gate → Computer Science Dept entrance → D1 is the continuous outdoor-to-indoor demo route.

The application does not connect to GPS, live traffic, Google Maps, a server, or a database. Location progress moves only when the demo controls are used. Passwords are stored as salted PBKDF2-HMAC-SHA-256 hashes. The built-in Faculty login is for local demonstration only.
