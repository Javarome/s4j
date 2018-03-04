package org.stellarium.i18n;

import org.stellarium.Translator;
import org.stellarium.astro.AbstractStellariumTest;
import org.stellarium.data.ResourceLocatorUtil;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Check S4J internationalization (I18N) features.
 *
 * @author <a href="mailto:javarome@javarome.net">Jérôme Beau</a>
 * @version 27 nov. 2006 00:35:38
 */
public class I18NTest extends AbstractStellariumTest {
    private ResourceLocatorUtil locatorUtil;

    protected void setUp() throws Exception {
        locatorUtil = ResourceLocatorUtil.getInstance();
        locatorUtil.init(getCommandLineArgs(), Logger.getAnonymousLogger());
        Translator.initSystemLanguage();
    }

    /*   public void testBundleAvailiability() {
     File[] allPoFiles = locatorUtil.getAllPoFiles();

     List<File> allPoFileList = new ArrayList<File>(allPoFiles.length);
     Collections.addAll(allPoFileList, allPoFiles);
     System.out.println("Found " + allPoFileList.size() + " po files");

     String[] languages = Locale.getISOLanguages();
     Set<String> allLanguages = new HashSet<String>(languages.length);
     Collections.addAll(allLanguages, languages);
     System.out.println("Found " + allLanguages.size() + " languages in Java");

     String[] countries = Locale.getISOCountries();
     Set<String> allCountries = new HashSet<String>(countries.length);
     Collections.addAll(allCountries, countries);
     System.out.println("Found " + allCountries.size() + " countries in Java");

     Locale[] locales = Locale.getAvailableLocales();
     Set<Locale> allLocales = new HashSet<Locale>(locales.length);
     Collections.addAll(allLocales, locales);
     System.out.println("Found " + allLocales.size() + " locales in Java");

     Set<Locale> stellariumSupportedLocales = new HashSet<Locale>();

     for (File poFile : allPoFileList) {
         String poFilename = poFile.getName();
         assertTrue(poFilename.endsWith(".po"));
         assertTrue(poFilename.length() == 5 || poFilename.length() == 8);
         assertTrue((!poFilename.contains("_") && poFilename.length() == 5) ||
                 (poFilename.contains("_") && poFilename.length() == 8));

         Locale localeFound;

         String language = poFilename.substring(0, 2);
         assertTrue(allLanguages.contains(language));
         if (poFilename.length() == 8) {
             String country = poFilename.substring(3, 5);
             assertTrue(allCountries.contains(country));
             localeFound = new Locale(language, country);
         } else {
             localeFound = new Locale(language);
         }
         if (!allLocales.contains(localeFound)) {
             System.out.println("Found po file " + poFile + " which has a Locale " + localeFound + " that is not supported in Java");
             System.out.println("Still the language or country is a known ISO one. So, we are OK.");
         }
         stellariumSupportedLocales.add(localeFound);
     }

     System.out.println("Supported locales for stellarium are:");
     for (Locale stellariumSupportedLocale : stellariumSupportedLocales) {
         System.out.println(stellariumSupportedLocale);
     }
 }   */

    public void testPO2Properties() {
        Translator frenchTranslator = Translator.getTranslator(Locale.FRANCE);
        assertEquals("Terre", frenchTranslator.translate("Earth"));
        // Space
        assertEquals("Trois Marches", frenchTranslator.translate("ThreeSteps"));
        // Square brackets (key shortcut)
        assertEquals("Noms des constellations [V]", frenchTranslator.translate("NamesOfTheConstellationsV"));
        // Accents and colon
        assertEquals("Multiplicateur de magnitude des étoiles : ", frenchTranslator.translate("StarMagnitudeMultiplier").intern());
        // Escaped character
        assertEquals("Enregistre les commandes dans le fichier script :\n", frenchTranslator.translate("RecordingCommandsToScriptFile"));

        assertEquals("Date & Heure", frenchTranslator.translate("DateTime"));

        Translator englishTranslator = Translator.getTranslator(Locale.ENGLISH);
        assertEquals("Earth", englishTranslator.translate("Earth"));
        // Space
        assertEquals("Three Steps", englishTranslator.translate("ThreeSteps"));
        // Square brackets (key shortcut)
        assertEquals("Names of the Constellations [V]", englishTranslator.translate("NamesOfTheConstellationsV"));
        // Accents and colon
        assertEquals("Star Magnitude Multiplier: ", englishTranslator.translate("StarMagnitudeMultiplier"));
        // Escaped character
        assertEquals("Recording commands to script file:\n", englishTranslator.translate("RecordingCommandsToScriptFile"));

        assertEquals("Date & Time", englishTranslator.translate("DateTime"));
    }
}
