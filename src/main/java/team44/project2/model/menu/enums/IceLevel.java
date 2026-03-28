package team44.project2.model.menu.enums;
/**
 * Enumeration of ice-level options available for customisable menu items, each paired
 * with a user-friendly label shown in the ordering UI.
 */
public enum IceLevel {
    NO_ICE("No Ice"),
    LESS_ICE("Less Ice"),
    REGULAR_ICE("Regular Ice"),
    EXTRA_ICE("Extra Ice");

    private final String label;

    /**
     * Associates an ice-level constant with its display label.
     *
     * @param label The human-readable string shown in the UI.
     */
    IceLevel(String label) {
        this.label = label;
    }

    /**
     * Returns the user-friendly display label for this ice level.
     *
     * @return The label string (e.g. {@code "Less Ice"}).
     */
    public String label() {
        return label;
    }
}