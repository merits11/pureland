package merits.funskills.pureland.v2;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import merits.funskills.pureland.utils.AppConfig;

public class Speeches {

    private ResourceBundle resourceBundle;

    private Speeches(final String bundleName, final Locale locale) {
        resourceBundle = ResourceBundle.getBundle(bundleName, locale);
    }

    private Speeches() {
        this("speeches", Locale.US);
    }

    public String get(final String name) {
        return resourceBundle.getString(name);
    }

    private static Map<Locale, Speeches> speechesMap = new HashMap<>();

    public static Speeches getSpeeches() {
        Locale locale = AppConfig.getLocale();
        if (!speechesMap.containsKey(locale)) {
            speechesMap.put(locale, new Speeches("speeches", locale));
        }
        return speechesMap.get(locale);
    }
}
