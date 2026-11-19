package noirbox.model;

/** A city shown in the city selector of the website. */
public class City {

    private final String slug;
    private final String name;
    private final String state;

    public City(String slug, String name, String state) {
        this.slug = slug;
        this.name = name;
        this.state = state;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }
}
