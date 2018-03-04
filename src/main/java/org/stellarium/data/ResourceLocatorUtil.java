/*
 * User: freds
 * Date: Dec 1, 2006
 * Time: 4:41:29 PM
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.stellarium.data;

import org.stellarium.StellariumException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * File to help the retrieval of all data files for Stellarium4Java.<br/>
 * The actual provider of files FileLoader should be injected depending on
 * user needs.
 *
 * @author Fred Simon
 * @version Java
 */
public class ResourceLocatorUtil {

    private static final ResourceLocatorUtil instance = new ResourceLocatorUtil();

    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

    public static final String OS_VERSION = System.getProperty("os.version");

    public static final String CYGWIN_HOME = System.getProperty("cygwin.home");

    public static final String STELLARIUM_DIR_NAME = ".stellarium4java";

    private URL stellariumHome;

    private File configDir;

    private File configFile;

    private URL translatorDir;

    private URL texturesDir;

    private URL dataDir;

    private URL scriptsDir;

    private URL skyCulturesDir;

    private FileLoader fileLoader = new SimpleFileLoaderImpl(this);
    private Logger logger;
    private Logger parentLogger;

    private ResourceLocatorUtil() {
    }

    public static ResourceLocatorUtil getInstance() {
        return instance;
    }

    public FileLoader getFileLoader() {
        return fileLoader;
    }

    public void setFileLoader(FileLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    public URL getTranslatorDir() {
        return translatorDir;
    }

    /**
     * Will initialize all root dir or URL resource path.
     * <p/>
     * Stellarium home comes from command line --home=DIR or --home DIR
     * or System property stellarium.home
     * or the current dir by default
     *
     * @param commandLineArgs a list of command line args, and will remove all used arguments
     * @param logger
     */
    public void init(List<String> commandLineArgs, Logger logger) {
        String homeParam = getParamValue(commandLineArgs, "home", null);
        if (homeParam == null) {
            String className = "org";
            stellariumHome = getClass().getResource("/" + className);
            homeParam = stellariumHome.toExternalForm();
            homeParam = homeParam.substring(0, homeParam.indexOf(className));
        }
        try {
            stellariumHome = new URL(homeParam);

            getLogger().config("Stellarium home=" + stellariumHome.toExternalForm());
            translatorDir = getSubDir(commandLineArgs, stellariumHome, "po", "en.po");
            getLogger().config("translatorDir=" + translatorDir.toExternalForm());
            texturesDir = getSubDir(commandLineArgs, stellariumHome, "textures", "logo24bits.png");
            dataDir = getSubDir(commandLineArgs, stellariumHome, "data", "hipparcos.fab");
            scriptsDir = getSubDir(commandLineArgs, dataDir, "data/scripts", "startup.sts");
            skyCulturesDir = getSubDir(commandLineArgs, dataDir, "data/sky_cultures", "western/info.ini");

            String confFileParam = getParamValue(commandLineArgs, "configFile");
            if (confFileParam != null) {
                configFile = new File(confFileParam);
                configDir = configFile.getParentFile();
            } else {
                File userHomeDir = getHomeDir();
                if (userHomeDir == null) {
                    // keep default
                    configDir = new File(stellariumHome.toExternalForm(), "config");
                } else {
                    configDir = new File(userHomeDir, STELLARIUM_DIR_NAME + "/config");
                }
            }
            checkConfigDir();

            getLogger().config("Config Dir=" + configDir.getAbsolutePath());

            // Now configDir exists for sure, check the config file
            configFile = new File(configDir, "config.ini");
            if (!configFile.exists()) {
                copyDefaultConfig();
            }

            getLogger().config("Config File=" + configFile.getAbsolutePath());
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }
    }

    public File getHomeDir() {
        File homeDir = null;
        String homeDirName;
        if (isWindows()) {
            homeDirName = System.getenv("USERPROFILE");
        } else {
            homeDirName = System.getenv("HOME");
        }
        if (homeDirName != null) {
            homeDir = new File(homeDirName);
            if (!homeDir.exists()) {
                // I don't create a user home
                homeDir = null;
            }
        }
        return homeDir;
    }

    private void checkConfigDir() {
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                throw new StellariumException("Cannot create directory " + configDir);
            }
        }
    }

    public void copyDefaultConfig() {
        copyTextFile(this.getDataFile("default_config.ini"),
                configFile);
    }

    private URL getSubDir(List<String> commandLineArgs, URL parentFolder, String subDirName, String containFileName) throws MalformedURLException {
        URL subDir;

        String dirParam = getParamValue(commandLineArgs, subDirName);
        if (dirParam == null) {
            subDir = new URL(parentFolder, subDirName);
        } else {
            subDir = new URL(parentFolder, dirParam);
        }

        /*File testFile = getOrLoadFile(subDir, containFileName);
        if (!testFile.exists()) {
            try {
                throw new StellariumException("File " + testFile.getCanonicalPath() + " does not exist");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (!testFile.canRead()) {
            try {
                throw new StellariumException("File " + testFile.getCanonicalPath() + " cannot be read");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } */
        return subDir;
    }

    public URL getOrLoadFile(String fullPathFileName) {
        try {
            return getOrLoadFile(new URL(new File(fullPathFileName).toURI().toURL().toExternalForm()));
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }
    }

    public URL getOrLoadFile(URL subDir, String fileName) {
        try {
            return getOrLoadFile(new URL(subDir.toExternalForm() + "/" + fileName));
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }
    }

    public URL getOrLoadFile(URL file) {
        return fileLoader.getOrLoadFile(file);
    }

    public static String getParamValue(List<String> commandLineArgs, String paramName) {
        return getParamValue(commandLineArgs, paramName, null);
    }

    public static String getParamValue(List<String> commandLineArgs, String paramName, String defaultValue) {
        String paramValue = null;
        if (commandLineArgs != null && !commandLineArgs.isEmpty()) {
            // try from parameters
            String prefix = "--" + paramName;
            Iterator<String> stringIterator = commandLineArgs.iterator();
            while (stringIterator.hasNext()) {
                String arg = stringIterator.next();
                if (arg.startsWith(prefix)) {
                    // The next character should be space or equal
                    if (arg.length() > prefix.length()) {
                        // arg of format --paramName=paramValue
                        if (arg.charAt(prefix.length()) != '=') {
                            // Not for me
                        } else {
                            paramValue = arg.substring(prefix.length() + 1);
                            break;
                        }
                    } else {
                        // arg of format --paramName paramValue
                        if (!stringIterator.hasNext()) {
                            throw new StellariumException("Argument " + arg + " does not respect the format --" + paramName + " value");
                        }
                        paramValue = stringIterator.next();
                        break;
                    }
                }
            }
        }
        if (paramValue == null) {
            // try from system properties
            paramValue = System.getProperty("stellarium." + paramName);
        }
        if (paramValue == null) {
            paramValue = defaultValue;
        }
        return paramValue;
    }

    /**
     * This folder should not be used directly to insert resources,
     * but is there for developers convenience.
     *
     * @return The home directory where stellarium was installed
     */
    public File getStellariumHome() {
        return new File(stellariumHome.getFile());
    }

    /**
     * A dynamic user folder where all configuration files, recorded scripts or audio
     * and so on will be placed.
     *
     * @return the dynamic user configuration directory
     */
    public File getConfigDir() {
        return configDir;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getScriptsDir() {
        return new File(scriptsDir.getFile());
    }

    public URL getSkyCulturesDir() {
        return skyCulturesDir;
    }

    private URL getDataDir() {
        return dataDir;
    }

    /*public InputStream getTranslatorResource(String resourceName) {
        return loadFromFile(getTranslatorDir(), resourceName);
    } */

    public URL getTextureResource(String textureName) {
        return loadFromFile(getTexturesDir(), textureName);
    }

    private URL getTexturesDir() {
        return texturesDir;
    }

    public URL getTextureURL(String textureName) {
        try {
            return getOrLoadFile(getTexturesDir(), textureName).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new StellariumException("Could not build a texture URL for " + textureName, e);
        } catch (URISyntaxException e) {
            throw new StellariumException("Could not build a texture URI for " + textureName, e);
        }
    }

    private URL loadFromFile(URL dir, String resourceName) {
        return getOrLoadFile(dir, resourceName);
    }

    /**
     * Small copy utility method in order to avoid platform specifics
     *
     * @param source The source file path
     * @param dest   The destination file path
     */
    public static void copyTextFile(URL source, File dest) {
        try {
            BufferedReader sourceReader = new BufferedReader(new InputStreamReader(source.openStream()));
            try {
                PrintWriter destWriter = new PrintWriter(new FileWriter(dest));
                try {
                    boolean lineRead;
                    do {
                        String line = sourceReader.readLine();
                        lineRead = line != null;
                        if (lineRead) {
                            destWriter.println(line);
                        }
                    } while (lineRead);
                } finally {
                    destWriter.close();
                }
            } finally {
                sourceReader.close();
            }
        } catch (IOException e) {
            throw new StellariumException("Error while copying " + source + " to " + dest, e);
        }
    }

    public static boolean isMacOSX() {
        return isMacOS() && OS_VERSION.startsWith("10");
    }

    public static boolean isWindows() {
        return OS_NAME.startsWith("windows");
    }

    public static boolean isMacOS() {
        return OS_NAME.startsWith("mac os");
    }

    public static boolean isCygwin() {
        return isWindows() && CYGWIN_HOME != null;
    }

    public static boolean isSgi() {
        return false;
    }

    public URL getDataFile(String childFile) {
        return getOrLoadFile(getDataDir(), childFile);
    }

    /*public File[] getAllPoFiles() {
        return fileLoader.listFiles(
                getTranslatorDir(),
                new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".po");
                    }
                });
    } */

    /*public File[] listSkyCultures() {
        return fileLoader.listFiles(getSkyCulturesDir());
    } */

    public URL getSkyCultureFile(String skyCultureDir, String fileName) {
        try {
            return new URL(getSkyCulturesDir().toExternalForm() + "/" + skyCultureDir + "/" + fileName);
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }
    }

    public void execScript(String scriptName) {
        throw new RuntimeException("Not implemented");
        /*try {
            File scriptFile = getDataFile(scriptName);
            Runtime.getRuntime().exec(scriptFile.getAbsolutePath());
        } catch (IOException e) {
            throw new StellariumException("Could not launch script " + scriptName, e);
        }*/
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(getClass().getName());
            if (parentLogger != null) {
                logger.setParent(parentLogger);
            }
        }
        return logger;
    }
}
