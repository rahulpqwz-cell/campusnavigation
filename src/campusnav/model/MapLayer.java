package campusnav.model;

/** The visual layer occupied by a map node. Transitions connect layers but are not nodes. */
public enum MapLayer {
    OUTDOOR,
    INDOOR,
    TRANSITION;

    public static MapLayer parse(String value) {
        if (value == null) {
            return OUTDOOR;
        }
        if ("INDOOR".equalsIgnoreCase(value.trim())) {
            return INDOOR;
        }
        if ("TRANSITION".equalsIgnoreCase(value.trim())) {
            return TRANSITION;
        }
        return OUTDOOR;
    }
}
