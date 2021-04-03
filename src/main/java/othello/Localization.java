package othello;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Simple localization singleton object.
 * Uses the {@code text} resource bundle stored at {@code resources/text*.properties}.
 */
public enum Localization {
    L10N;

    private final ResourceBundle resourceBundle;
    private final static String RESOURCE_BUNDLE_NAME = "text";

    /**
     * Initialize the {@link ResourceBundle} using the default locale.
     */
    Localization() {
        final var locale = Locale.getDefault();
        this.resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale);
    }

    /**
     * @param key the name of the localized string
     * @return the localized string
     */
    public String get(String key) {
        return this.resourceBundle.getString(key);
    }
}
