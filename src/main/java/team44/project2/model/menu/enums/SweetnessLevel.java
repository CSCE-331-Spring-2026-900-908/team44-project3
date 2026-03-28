package team44.project2.model.menu.enums;
/**
 * Enumeration of sweetness-level options available for customisable menu items, each
 * paired with a user-friendly percentage label shown in the ordering UI.
 */
public enum SweetnessLevel {
    ZERO("0%"),
    TEN("10%"),
    THIRTY("30%"),
    FIFTY("50%"),
    HUNDRED("100%"),
    ONE_TWENTY("120%");

    private final String label;

    /**
     * Associates a sweetness-level constant with its display label.
     *
     * @param label The human-readable percentage string shown in the UI (e.g. {@code "50%"}).
     */
    SweetnessLevel(String label) {
        this.label = label;
    }

    /**
     * Returns the user-friendly display label for this sweetness level.
     *
     * @return The label string (e.g. {@code "100%"}).
     */
    public String label() {
        return label;
    }
}