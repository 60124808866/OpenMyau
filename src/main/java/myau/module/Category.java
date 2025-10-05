package myau.module;
public enum Category {
    Combat("Combat"),
    Movement("Movement"),
    Render("Render"),
    Player("Player"),
    Misc("Misc");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}