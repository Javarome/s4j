package org.stellarium;

import org.stellarium.data.ResourceLocatorUtil;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Class used to translate strings to any language.
 * Implements a nice interface to gettext which is UTF-8 compliant and is somewhat multiplateform
 * All its operations do not modify the global locale.
 * The purpose of this class is to remove all non-OO C locale functions from stellarium.
 * It could be extended for all locale management using e.g the portable IBM ICU library.
 * <p/>
 * Changes:
 * - Reimplementation of getext in Java by Jerome Beau
 * Reading the .po files and make a map of language (Locale), englishMsgId, translation
 * - Full usage of Java ResourceBundle for Locale and cache management by Fred Simon
 *
 * @author Fabien Chereau for original C++ version
 * @author Jerome Beau
 * @author Frederic Simon
 * @version 24 oct. 2006 01:07:47
 */
public class Translator extends ResourceBundle {
    /**
     * Supports po files
     */
    public static final List<String> PO_FORMATS
            = Collections.unmodifiableList(Arrays.asList("po"));

    /**
     * Contains the set of locales (lang,country) supported by stellarium
     * With the File containing the translations.
     * TODO: Use URL instead of File
     */
    private static final Map<Locale, File> supportedLocales = new HashMap<Locale, File>();

    /**
     * All languages supported by Stellarium
     */
    private static final TreeSet<String> availableLanguages = new TreeSet<String>();

    private static final POResourceControl poControl = new POResourceControl();

    /**
     * The currently used translator
     */
    private static Translator currentTranslator;

    private static final String PO_KEY = "msgid";

    private static final String PO_VALUE = "msgstr";

    private static final String PO_COMMENT_START = "#";

    private final Map<String, String> properties = new HashMap<String, String>();

    /**
     * Try to determine system language from system configuration
     */
    public static void initSystemLanguage() {
        // Create a set of all supported language in Java
        String[] languages = Locale.getISOLanguages();
        Set<String> allLanguages = new HashSet<String>(languages.length);
        Collections.addAll(allLanguages, languages);

        /*
        // Create the set of supported locale in Stellarium
        File[] poFiles = ResourceLocatorUtil.getInstance().getAllPoFiles();
        for (File poFile : poFiles) {
            String poFilename = poFile.getName();

            // Check validity of file name
            if (!poFilename.endsWith(".po") ||
                    !((!poFilename.contains("_") && poFilename.length() == 5) ||
                            (poFilename.contains("_") && poFilename.length() == 8))) {
                System.err.println("PO file " + poFile + " has an invalid name. Should be <language>[_country].po");
                continue;
            }

            Locale localeFound;

            String language = poFilename.substring(0, 2);
            // Check that it's an ISO Language
            if (!allLanguages.contains(language)) {
                System.err.println("PO file " + poFile + " has a language non ISO: " + language);
            } else {
                if (poFilename.length() == 8) {
                    String country = poFilename.substring(3, 5);
                    localeFound = new Locale(language, country);
                } else {
                    localeFound = new Locale(language);
                }
                supportedLocales.put(localeFound, poFile);
                availableLanguages.add(localeFound.getLanguage() + " " + localeFound.getDisplayLanguage());
                if (localeFound.equals(Locale.ENGLISH)) {
                    // Set english has the root PO file
                    supportedLocales.put(Locale.ROOT, poFile);
                }
            }
        }*/
        currentTranslator = getTranslator(Locale.getDefault());
        if (currentTranslator == null) {
            throw new StellariumException("No PO file found for default locale and English fallback fell!");
        }
    }

    public static Translator getTranslator(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return (Translator) ResourceBundle.getBundle("stel", locale, poControl);
    }

    public static Locale codeToLocale(String localeCode) {
        if (localeCode == null || localeCode.length() == 0) {
            // Return the default locale
            return Locale.getDefault();
        }
        final String lc = localeCode.trim();
        if (lc.contains("_")) {
            // Has a language and a country
            final int i = lc.indexOf('_');
            String lang = lc.substring(0, i);
            String country = lc.substring(i + 1);
            if (lang.equalsIgnoreCase("system")) {
                lang = Locale.getDefault().getLanguage();
            }
            if (country.equalsIgnoreCase("default")) {
                country = Locale.getDefault().getCountry();
            }
            return new Locale(lang, country);
        } else {
            // Just a language
            return langToLocale(lc);
        }
    }

    private static Locale langToLocale(String language) {
        if (language.equalsIgnoreCase("system")) {
            // Default system language
            return Locale.getDefault();
        }
        return new Locale(language);
    }

    static class POResourceControl extends Control {
        public List<String> getFormats(String baseName) {
            return PO_FORMATS;
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            URL translatorDir = ResourceLocatorUtil.getInstance().getTranslatorDir();
            URL url = new URL(translatorDir + "/" + locale + ".po");
            try {
                InputStream content = url.openStream();
                return new Translator(content);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static Set<Locale> getSupportedLocales() {
        return supportedLocales.keySet();
    }

    /**
     * Get available language codes from directory tree
     */
    public static Set<String> getNamesOfAvailableLanguages() {
        return availableLanguages;
    }

    public static Translator getCurrentTranslator() {
        return currentTranslator;
    }

    public static void setCurrentTranslator(Locale locale) {
        // TODO: Should the default locale of the JVM change also?
        currentTranslator = getTranslator(locale);
    }

    public String getTrueLocaleName() {
        final Locale curLoc = getLocale();
        return curLoc.toString() + " " + curLoc.getDisplayName();
    }

    /**
     * Translate input message.
     *
     * @param s input string in english.
     * @return The translated string in wide characters.
     */
    public String translate(String s) {
        if (StelUtility.isEmpty(s)) {
            return "";
        }
        return getString(s);
    }

    private Translator(InputStream poFile) {
        read(poFile);
    }

    private String normalizeKey(String key) {
        StringBuffer sb = new StringBuffer(key);
        boolean forceUppercase = false;
        int specialCharacter = -1;
        int unicode = -1;
        for (int i = 0; i < sb.length(); i++) {
            if (unicode > 0) {
                unicode--;
                continue;
            }
            char c = sb.charAt(i);
            switch (c) {
                case '\\':
                    if (i + 1 < sb.length() && sb.charAt(i + 1) == 'u') {
                        unicode = 4;
                        continue;
                    }
                    specialCharacter = i;
                    sb.delete(i, i + 1);
                    i--;
                    break;
                case ' ':
                    forceUppercase = true;
                case ':':
                case '\'':
                case '[':
                case ']':
                case '(':
                case ')':
                case '\n':
                case '.':
                case '-':
                case '+':
                case '&':
                case ',':
                case '`':
                case '/':
                    sb.delete(i, i + 1);
                    i--;
                    break;
                case '%':
                default:
                    if (specialCharacter == i) {
                        sb.delete(i, i + 1);
                        i--;
                        specialCharacter = -1;
                    } else if (forceUppercase) {
                        c = Character.toUpperCase(c);
                        sb.setCharAt(i, c);
                        forceUppercase = false;
                    }
            }
        }
        return sb.toString();
    }

    private void read(InputStream poFile) {
        // If we got here it means Translator checked that the po file exists
        BufferedReader poReader = null;
        try {
            Charset charset = Charset.forName("UTF-8");
            poReader = new LineNumberReader(new InputStreamReader(poFile, charset));
            String poLine;
            boolean readingKey = false;
            boolean readingValue = false;
            String currentKey = null;
            String currentValue = null;
            do {
                poLine = poReader.readLine();
                if (poLine != null && poLine.length() > 0) {
                    poLine = poLine.trim();
                    if (poLine.startsWith(PO_COMMENT_START)) {
                        String comment = poLine.length() == 1 ? poLine : poLine.substring(1).trim();
                        processComment(comment);
                    } else if (poLine.startsWith(PO_KEY)) {
                        if (readingValue) {
                            processKeyValue(normalizeKey(currentKey), interpretValue(currentValue));
                            readingValue = false;
                            currentValue = null;
                        }
                        currentKey = poLine.substring(PO_KEY.length() + 2, poLine.length() - 1);
                        readingKey = true;
                    } else if (poLine.startsWith(PO_VALUE)) {
                        currentValue = poLine.substring(PO_VALUE.length() + 2, poLine.length() - 1);
                        readingKey = false;
                        readingValue = true;
                    } else if (readingKey) {
                        currentKey += poLine.substring(1, poLine.length() - 1);
                    } else if (readingValue) {
                        currentValue += poLine.substring(1, poLine.length() - 1);
                    }
                }
            } while (poLine != null);
        } catch (IOException e) {
            throw new StellariumException("Error while reading " + poFile, e);
        } finally {
            if (poReader != null) {
                try {
                    poReader.close();
                } catch (Exception ignore) {
                    // Ignore exception on close
                    ignore.printStackTrace(System.out);
                }
            }
        }
    }

    private String interpretValue(String currentValue) {
        StringBuffer sb = new StringBuffer(currentValue);
        boolean found;
        int i = 0;
        do {
            String escapeSequence1 = "\\n";
            i = sb.indexOf(escapeSequence1, i);
            found = i >= 0;
            if (found) {
                sb.delete(i, i + escapeSequence1.length());
                String escapeSequence2 = "\n";
                sb.insert(i, escapeSequence2);
                i++;
            }
        } while (found);
        return sb.toString();
    }

    private void processKeyValue(String currentKey, String currentValue) {
        properties.put(currentKey, currentValue);
    }

    private void processComment(String comment) {
        // TODO: May be process the comments in PO files?
    }

    /**
     * Gets an object for the given key from this resource bundle.
     * Returns the key if this resource bundle does not contain an
     * object for the given key.
     *
     * @param key the key for the desired object
     * @return the object for the given key, or the key if object is not found
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     */
    protected Object handleGetObject(String key) {
        String value = properties.get(key);
        return value == null ? key : value;
    }

    public Enumeration<String> getKeys() {
        return Collections.enumeration(properties.keySet());
    }
}
