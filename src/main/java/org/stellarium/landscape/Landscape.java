package org.stellarium.landscape;

import org.stellarium.Navigator;
import org.stellarium.StelUtility;
import org.stellarium.StellariumException;
import org.stellarium.ToneReproductor;
import org.stellarium.data.IniFileParser;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.STextureFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Projector.
 * <p/>
 * See <a href="http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/landscape.cpp?rev=1.19&view=markup">C++ version</a> of this file.
 * http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/landscape.h?rev=1.13&view=markup
 * <p/>
 * Updated to <a href="https://svn.sourceforge.net/svnroot/stellarium/tags/stellarium-0-8-1/stellarium/src/landscape.cpp">C++ version</a> from
 * URL: https://svn.sourceforge.net/svnroot/stellarium/tags/stellarium-0-8-1/stellarium/src/landscape.cpp
 * Revision: 1560
 * Last Changed Author: digitalises
 * Last Changed Rev: 1419
 *
 * @author <a href="mailto:javarome@javarome.net">Jerome Beau</a>, Frederic Simon
 * @version Java 6
 */
public abstract class Landscape {

    protected final Logger logger;
    protected final STextureFactory textureFactory;

    private static Logger getLogger(Logger parentLogger) {
        Logger logger = Logger.getLogger(Landscape.class.getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        return logger;
    }

    /**
     * Enumeration names should match the names used in landscape data files
     * TODO: Create a member to separate Java type and names
     */
    public enum LandscapeType {
        old_style,
        fisheye,
        spherical
    }

    Landscape(float radius, Logger parentLogger) {
        logger = getLogger(parentLogger);
        textureFactory = new STextureFactory(logger);
        this.radius = radius;
        this.skyBrightness = 1.0f;
    }

    public static Landscape createFromFile(URL landscapeFile, String sectionName, Logger parentLogger) throws StellariumException {
        IniFileParser pd = getLandscapeIniParser(landscapeFile);
        String s = pd.getStr(sectionName, IniFileParser.TYPE);

        Logger logger = getLogger(parentLogger);
        // TODO: Convert to LandscapeType.valueOf() + switch case
        Landscape landscape;
        if (StelUtility.isEmpty(s)) {
            // This is an old style
            landscape = new LandscapeOldStyle(logger);
        } else if ("old_style".equals(s)) {
            landscape = new LandscapeOldStyle(logger);
        } else if ("spherical".equals(s)) {
            landscape = new LandscapeSpherical(logger);
        } else if ("fisheye".equals(s)) {
            landscape = new LandscapeFishEye(logger);
        } else {
            throw new StellariumException("Unknown landscape type \"" + s + "\" in section " + sectionName + " of " + landscapeFile);
        }
        // TODO: Logging here blocks the thread, possibly because of some non-reentrance issue in Logging or JOGL API
        //logger.fine("Loading landscape " + landscape.getClass());
        landscape.load(landscapeFile, sectionName);
        return landscape;
    }

    //TODO: unify all load mechanism
    protected abstract void load(URL landscapeFile, String sectionName) throws StellariumException;

    // create landscape from parameters passed in a hash (same keys as with ini file)
    // NOTE: maptex must be full path and filename
    public static Landscape createFromHash(Map param, Logger parentLogger) throws StellariumException {
        LandscapeType type = LandscapeType.valueOf(getStr(param, "type", "fisheye"));

        // NOTE: textures should be full filename (and path)
        switch (type) {
            case old_style: {
                LandscapeOldStyle landscape = new LandscapeOldStyle(parentLogger);
                landscape.create(true, param);
                return landscape;
            }
            case spherical: {
                LandscapeSpherical ldscp = new LandscapeSpherical(parentLogger);
                ldscp.create(getStr(param, "name"), true, getStr(param, "path") + getStr(param, "maptex"));
                return ldscp;
            }
            case fisheye: {
                LandscapeFishEye ldscp = new LandscapeFishEye(parentLogger);
                ldscp.create(getStr(param, "name"), true, getStr(param, "path") + getStr(param, "maptex"), getDouble(param, "texturefov", 0.0d));
                return ldscp;
            }
        }

        throw new StellariumException("Type " + type + " is not supported.");
    }

    /**
     * Load attributes common to all landscapes
     *
     * @param landscapeFile
     * @param sectionName
     * @return The landscape data ini file parser
     * @throws StellariumException
     */
    IniFileParser loadCommon(URL landscapeFile, String sectionName) throws StellariumException {
        IniFileParser pd = new IniFileParser(Landscape.class, landscapeFile);// The landscape data ini file parser

        name = pd.getStr(sectionName, IniFileParser.NAME);
        author = pd.getStr(sectionName, "author");
        description = pd.getStr(sectionName, "description");
        if (name.length() == 0) {
            validLandscape = false;
            throw new StellariumException("No valid landscape definition found for section " + sectionName +
                    " in file " + landscapeFile + ". No landscape in use.");
        }
        validLandscape = true;
        return pd;
    }

    public static String getFileContent(URL landscapeFile) throws StellariumException {
        IniFileParser pd = new IniFileParser(Landscape.class, landscapeFile);// The landscape data ini file parser

        String[] secNames = pd.getSectionNames();
        StringBuffer result = new StringBuffer();
        for (String secName : secNames) {
            result.append(secName).append('\n');
        }
        return result.toString();
    }

    public static Object[] getLandscapeNames(IniFileParser pd) throws StellariumException {
        String[] secNames = pd.getSectionNames();
        List<String> result = new ArrayList<String>();
        for (String secName : secNames) {
            result.add(pd.getStr(secName, IniFileParser.NAME));
        }
        return result.toArray();
    }

    public static IniFileParser getLandscapeIniParser(URL landscapeFile) {
        return new IniFileParser(Landscape.class, landscapeFile);
    }

    public static String nameToKey(IniFileParser pd, String name) throws StellariumException {
        String[] secNames = pd.getSectionNames();
        for (String sectionName : secNames) {
            if (name.equals(pd.getStr(sectionName, IniFileParser.NAME)))
                return sectionName;
        }
        throw new StellariumException("No landscape named " + name + " found in file " + pd);
    }

    public void setSkyBrightness(float b) {
        skyBrightness = b;
    }

    /**
     * Set whether landscape is displayed (does not concern fog)
     */
    public void setVisible(boolean b) {
        landFader.set(b);
    }

    /**
     * Get whether landscape is displayed (does not concern fog)
     */
    public boolean isVisible() {
        return landFader.getState();
    }

    /**
     * Set whether fog is displayed
     */
    public void setFlagShowFog(boolean b) {
        fogFader.set(b);
    }

    /**
     * Get whether fog is displayed
     */
    public boolean getFlagShowFog() {
        return fogFader.getState();
    }

    /**
     * Get landscape name
     */
    public String getName() {
        return name;
    }

    /**
     * Get landscape author name
     */
    public String getAuthorName() {
        return author;
    }

    /**
     * Get landscape description
     */
    public String getDescription() {
        return description;
    }

    public void update(long deltaTime) {
//        landFader.update(deltaTime);
//        fogFader.update(deltaTime);
    }

    public abstract void draw(ToneReproductor eye, DefaultProjector prj, Navigator nav);

    protected float radius;

    String name;

    float skyBrightness;

    /**
     * was a landscape loaded properly?
     */
    boolean validLandscape;

    LinearFader landFader = new LinearFader();

    LinearFader fogFader = new LinearFader();

    String author;

    String description;

    // TODO: Utility methods that will be removed after parsing framework architecture implemented
    protected static String getStr(Map param, String key) {
        return getStr(param, key, null);
    }

    protected static String getStr(Map param, String key, String def) {
        String value = (String) param.get(key);
        if (value == null || value.length() == 0)
            return def;
        return value;
    }

    protected static int getInt(Map param, String key) {
        return getInt(param, key, 0);
    }

    protected static int getInt(Map param, String key, int def) {
        String value = getStr(param, key);
        if (value == null)
            return def;
        return Integer.parseInt(value);
    }

    protected static double getDouble(Map param, String key, double def) {
        String value = getStr(param, key);
        if (value == null)
            return def;
        return Double.parseDouble(value);
    }

}
