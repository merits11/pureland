package merits.funskills.pureland.v2;

import java.util.Locale;
import java.util.ResourceBundle;

public class Speeches {

    private ResourceBundle resourceBundle;

    public Speeches(final String bundleName, final Locale locale) {
        resourceBundle = ResourceBundle.getBundle(bundleName, locale);
    }

    public Speeches() {
        this("speeches", Locale.US);
    }

    public String get(final String name) {
        return resourceBundle.getString(name);
    }
}
