package org.stellarium;

/*
* Stellarium
* Copyright (C) 2002 Fabien Chereau
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

import org.stellarium.data.ResourceLocatorUtil;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Stellarium for Java main program.
 *
 * @author <a href="mailto:rr0@rr0.org"/>J&eacute;r&ecirc;me Beau</a>
 * @version 2.0.0
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/main.cpp?view=markup&pathrev=stellarium-0-8-2">main.cpp</a>
 * @since 1.0.0
 */
public class Main {
    public static final String APP_NAME = "Stellarium for Java (S4J)";
    public static final String VERSION = "2.0.0 alpha";
    public static final String BUILD_DATE = "June 2008";
    private static Logger logger;

    /**
     * Print a beautiful console logo !!
     */
    static void drawIntro() {
        getLogger().config(APP_NAME + " " + VERSION + ", " + BUILD_DATE);
        getLogger().info("This Java version (http://stellarium4java.sf.net) is copyright (c) 2006-2010 Jérôme Beau, Fred Simon");
        getLogger().info("Original C++ version (http://www.stellarium.org) is copyright (c) 2000-2010 Fabien Chereau et al");
        getLogger().config("Running Java runtime " + System.getProperty("java.runtime.version") + " on " + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
    }

    // TODO: update this from ResourceLocatorUtil info
    public static final String[] USAGE_TEXT = {"Usage: java org.stellarium.Main [OPTION] ...",
            " -v, --version          Output version information and exit.",
            " -h, --help             Display this help and exit.",
            " --home=dir             Set the installation dir of stellarium.",
            " --config=dir           Set the config dir of stellarium.",
            " --data=dir             Set the data dir of stellarium."
    };

    /**
     * Display stellarium usage in the console
     *
     * @param argv The command-line parameters
     */
    static void usage(String[] argv) {
        for (String usageLine : USAGE_TEXT) {
            getLogger().info(usageLine);
        }
    }

    /**
     * Check command line arguments
     *
     * @param argv The command lines arguments to check
     * @return The code to exit with, if application termination is required, or null of no exit is required
     */
    static Integer checkCommandLine(String[] argv) {
        Integer exitCode = null;
        if (argv.length >= 1) {
            if ("--version".equals(argv[0]) || "-v".equals(argv[0])) {
                getLogger().info(APP_NAME);
                exitCode = 0;
            } else if ("--help".equals(argv[0]) || "-h".equals(argv[0])) {
                usage(argv);
                exitCode = 0;
            } else if ("--configFile".equals(argv[0])) {
            } else if ("--home".equals(argv[0])) {
            }
        }

        return exitCode;
    }

    public static final String BAD_COMMAND_MESSAGE = "Bad command line argument(s): {0}\nTry --help' for more information.";

    private static void badCommandLine(String... argv) {
        getLogger().severe(MessageFormat.format(BAD_COMMAND_MESSAGE, argv));
    }

    /**
     * Set the data, textures, and config directories in core.global : test the default
     * installation dir and try to find the files somewhere else if not found there
     * This enable to launch stellarium from the local directory without installing it
     *
     * @param args The command-line arguments
     * @throws StellariumException
     */
    static void setDirectories(List<String> args) throws StellariumException {
        // The variable CONFIG_DATA_DIR must have been set by the configure script
        // Its value is the dataRoot directory, ie the one containing data/ and textures/
        //final String CONFIG_DATA_DIR = System.getenv().get("CONFIG_DATA_DIR");

        try {
            final ResourceLocatorUtil resourceLocator = ResourceLocatorUtil.getInstance();
            resourceLocator.init(args, getLogger());
        } catch (Exception e) {
            throw new StellariumException("Error while looking for data directories. "
                    + "You may launch the application from the stellarium package directory. "
                    + "Or launch the application with --home=[stellarium install dir] --config=[the configuration dir]", e);
        }
    }

    /**
     * Main stellarium procedure
     *
     * @param argv The command line arguments.
     */
    public static void main(String[] argv) {
        try {
            // Check the command line
            Integer exitCode = checkCommandLine(argv);
            if (exitCode != null) {
                System.exit(exitCode);
            }

            // Print the console logo..
            drawIntro();

            // Find what are the Main Data, Textures and Config directories
            ArrayList<String> args = new ArrayList<String>(argv.length);
            args.addAll(Arrays.asList(argv));
            setDirectories(args);

            activate();

        } catch (Exception e) {
            getLogger().severe("Error starting " + APP_NAME + ":" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void activate() {
        try {
            // Create a temporary bundle cache directory and make sure to clean it up on exit.
            final File cachedir = File.createTempFile("org.stellarium", null);
            cachedir.delete();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    deleteFileOrDir(cachedir);
                }
            });

            final StelApp stelApp = new StelApp(getLogger());

/*            Map<String, String> configMap = new StringMap(false);
            configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
                    "org.osgi.framework; version=1.3.0," +
                            "org.osgi.service.packageadmin; version=1.2.0," +
                            "org.osgi.service.startlevel; version=1.0.0," +
                            "org.osgi.service.url; version=1.0.0," +
                            "org.osgi.util.tracker; version=1.3.2," +
                            "org.stellarium; version=2.0.0," +
                            "javax.swing");
            // TODO(JBE): Bundle modules in different jars
            //configMap.put(AutoActivator.AUTO_START_PROP + ".1",
            //        "file:../stellarium.xxx/target/stellarium.xxx-2.0.0.jar " +
            //        "file:../stellarium.yyy/target/stellarium.yyy-2.0.0.jar");
            configMap.put(FelixConstants.LOG_LEVEL_PROP, "1");
            configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, cachedir.getAbsolutePath());

            // Create list to hold custom framework activators.
            List<BundleActivator> list = new ArrayList<BundleActivator>();
            // Add activator to process auto-start/install properties.
            list.add(new AutoActivator(configMap));
            // Add our own activator.
            list.add(stelApp);

            // Now create an instance of the framework.
            Felix felix = new Felix(configMap, list);
            felix.start();
            */
            stelApp.start();
        } catch (Exception ex) {
            getLogger().severe("Could not create framework: " + ex);
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Utility method used to delete the profile directory when run as
     * a stand-alone application.
     *
     * @param file The file to recursively delete.
     */
    private static void deleteFileOrDir(File file) {
        if (file.isDirectory()) {
            File[] childs = file.listFiles();
            for (File child : childs) {
                deleteFileOrDir(child);
            }
        }
        file.delete();
    }

    private static Logger getLogger() {
        if (logger == null) {
            LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
            logger = Logger.getLogger(Main.class.getName());
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            logger.addHandler(consoleHandler);
            logger.setLevel(Level.ALL);
        }
        return logger;
    }

}