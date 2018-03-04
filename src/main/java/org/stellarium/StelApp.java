/*

 * Stellarium for Java
 * Copyright (c) 2005-2008 Jerome Beau
 *
 * Java adaptation of <a href="http://www.stellarium.org">Stellarium</a>
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
package org.stellarium;

import org.stellarium.astro.JulianDay;
import org.stellarium.command.ScriptMgr;
import org.stellarium.command.StelCommandInterface;
import org.stellarium.data.IniFileParser;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.projector.ViewportDistorter;
import org.stellarium.ui.FontFactory;
import org.stellarium.ui.StelUI;
import org.stellarium.ui.SwingUI;
import org.stellarium.ui.components.LoadingBar;

import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * The application controller.
 *
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @author Fred Simon
 * @version 2.0.0
 * @since 1.0.0
 */
public class StelApp implements ApplicationCallback {
    public static final int DEFAULT_MOUSE_ZOOM = 30;
    private IniFileParser conf;

    /**
     * The associcated StelCore instance
     */
    private StelCore core;

    /**
     * script filename (without directory) selected in a UI to run when exit UI
     */
    private String selectedScript = "";

    /**
     * script directory for same
     */
    private String selectedScriptDirectory = "";

    // Navigation

    private String positionFile;


    private double presetSkyTime;

    private String startupTimeMode;

    /**
     * Zoom power factor when using Ctrl/Cmd + mouse wheel.
     *
     * @see #DEFAULT_MOUSE_ZOOM
     */
    private int mouseZoom;

    /**
     * Used for fps counter
     */
    private int frame, timefr, timeBase;

    private double fps;

    private float minFPS, maxFPS = 100000.f;

    private boolean flagTimePause;

    /**
     * Used to store time speed while in pause
     */
    private double tempTimeVelocity;

    /**
     * used for adjusting delta_time for script speeds
     */
    private int timeMultiplier = 1;

    // Main elements of the stel_app

    /**
     * interface to perform all UI and scripting actions
     */
    private StelCommandInterface commander;

    /**
     * manage playing and recording scripts
     */
    public ScriptMgr scripts;

    /**
     * The main User Interface
     */
    public SwingUI ui;

    public ViewportDistorter distorter;

    /**
     * Current draw mode
     */
    public DRAWMODE drawMode = DRAWMODE.NONE;

    // Date and time variables
    private TIME_FORMAT timeFormatParameter;

    private DATE_FORMAT dateFormatType;

    /**
     * Can be the system default or a user defined value
     */
    StelApp.TZ_FORMAT timeZoneMode;

    /**
     * Something like "Europe/Paris"
     */
    String customTzName;

    /**
     * Time shift between GMT time and local time in hour. (positive for Est of GMT)
     */
    private double gmtShift;

    private Calendar calendar;

    private DateFormat dateFormat;
    protected final Logger logger;

    public StelApp(Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
    }

    public void setTimeSpeed(double s) {
        core.setTimeSpeed(s);
    }

    public void start() throws Exception {
        loadingBar = SwingUI.getLoadingBar();
        try {
            init();
        } finally {
            loadingBar.close();
        }
        startMainLoop();
    }

    public void stop() throws Exception {
        //m_shapetracker.close();
        //m_frame.setVisible(false);
        //m_frame.dispose();
    }

    /**
     * Possible drawing modes
     */
    public enum DRAWMODE {
        NORMAL, CHART, NIGHT, NIGHTCHART, NONE
    }

    public enum TIME_FORMAT {
        TIME_24H, TIME_12H, system_default
    }

    public enum DATE_FORMAT {
        MMDDYYYY, DDMMYYYY, SYSTEM_DEFAULT, YYYYMMDD
    }

    public enum TZ_FORMAT {
        CUSTOM, GMT_SHIFT, SYSTEM_DEFAULT
    }

    public StelCore getCore() {
        return core;
    }

    /**
     * Return a String with the UTC date formated according to the date_format variable
     *
     * @param jd The Julian day
     * @return The date text, formatted according to the current date format
     * @throws StellariumException If the current date format is not supported
     */
    public String getPrintableDateUTC(double jd) throws StellariumException {
        Date timeUTC = JulianDay.julianToDate(jd);
        return dateFormat.format(timeUTC);
    }

    // Return the time in ISO 8601 format that is : %Y-%m-%d %H:%M:%S
    public String getISO8601TimeLocal(double JD) {
        Date timeLocal;
        if (timeZoneMode == TZ_FORMAT.GMT_SHIFT) {
            timeLocal = JulianDay.julianToDate(JD + gmtShift);
        } else {
            timeLocal = JulianDay.julianToDate(JD + getGMTShift(JD) * 0.041666666666);
        }
        return StelUtility.myStrFTime(254, "YYYY-MM-dd HH:mm:ss", timeLocal);
    }

    /**
     * Return a String with the local date formated according to the date_format variable
     *
     * @param jd The Julian day
     * @return The date text, formatted according to the current date format
     * @throws StellariumException If the current date format is not supported
     */
    public String getPrintableDateLocal(double jd) throws StellariumException {
        Date timeLocal;

        if (timeZoneMode == TZ_FORMAT.GMT_SHIFT) {
            timeLocal = JulianDay.julianToDate(jd + gmtShift);
        } else {
            timeLocal = JulianDay.julianToDate(jd + StelUtility.getGMTShiftFromSystem(jd) * 0.041666666666);
        }

        String date;
        switch (dateFormatType) {
            case SYSTEM_DEFAULT:
                date = StelUtility.myStrFTime(254, "%x", timeLocal);
                break;
            case MMDDYYYY:
                date = StelUtility.myStrFTime(254, "%m/%d/%Y", timeLocal);
                break;
            case DDMMYYYY:
                date = StelUtility.myStrFTime(254, "%d/%m/%Y", timeLocal);
                break;
            case YYYYMMDD:
                date = StelUtility.myStrFTime(254, "%Y-%m-%d", timeLocal);
                break;
            default:
                throw new StellariumException("Unsupported date format: " + dateFormatType);
        }

        return date;
    }

    /**
     * Convert the date format enum to its associated String and reverse
     *
     * @param df The date format parameter ("system_default", "mmddyyyy", "ddmmyyyy", "yyyymmdd")
     * @return The inferred date format constant
     */
    private DATE_FORMAT stringToSDateFormat(String df) {
        if ("system_default".equals(df)) return DATE_FORMAT.SYSTEM_DEFAULT;
        if ("mmddyyyy".equals(df)) return DATE_FORMAT.MMDDYYYY;
        if ("ddmmyyyy".equals(df)) return DATE_FORMAT.DDMMYYYY;
        if ("yyyymmdd".equals(df)) return DATE_FORMAT.YYYYMMDD;// iso8601
        logger.warning("unrecognized date_display_format : " + df + " system_default used.");
        return DATE_FORMAT.SYSTEM_DEFAULT;
    }

    private String sDateFormatToString(DATE_FORMAT df) {
        if (df == DATE_FORMAT.SYSTEM_DEFAULT) return "system_default";
        if (df == DATE_FORMAT.MMDDYYYY) return "mmddyyyy";
        if (df == DATE_FORMAT.DDMMYYYY) return "ddmmyyyy";
        if (df == DATE_FORMAT.YYYYMMDD) return "yyyymmdd";
        logger.warning("unrecognized date_display_format value : " + df + " system_default used.");
        return "system_default";
    }

    /**
     * Return a String with the local time (according to time_zone_mode variable) formated
     * according to the time_format variable.
     *
     * @param jd The Julian day
     * @return The time text, formatted according to the current time format
     * @throws StellariumException If the current time format is not supported
     */
    public String getPrintableTimeLocal(double jd) throws StellariumException {
        Date timeLocal;

        if (timeZoneMode == TZ_FORMAT.GMT_SHIFT) {
            timeLocal = JulianDay.julianToDate(jd + gmtShift);
        } else {
            timeLocal = JulianDay.julianToDate(jd + StelUtility.getGMTShiftFromSystem(jd) * 0.041666666666);
        }

        String heure;
        switch (timeFormatParameter) {
            case system_default:
                heure = StelUtility.myStrFTime(254, "%X", timeLocal);
                break;
            case TIME_24H:
                heure = StelUtility.myStrFTime(254, "%H:%M:%S", timeLocal);
                break;
            case TIME_12H:
                heure = StelUtility.myStrFTime(254, "%I:%M:%S %p", timeLocal);
                break;
            default:
                throw new StellariumException("Unsupported time format: " + timeFormatParameter);
        }
        return heure;
    }

    /**
     * Set the current time shift at observator time zone with respect to GMT time
     *
     * @param t The new value
     */
    public void setGMTShift(int t) {
        gmtShift = t;
    }

    public String getCustomTzName() {
        return customTzName;
    }

    StelApp.TZ_FORMAT getTzFormat() {
        return timeZoneMode;
    }

    /**
     * Set a custom time zone.
     *
     * @param tzname The name of the time zone.
     */
    public void setCustomTzName(String tzname) {

        if (!customTzName.equals("")) {
            // TODO: Fred Find the java property equivalent to TZ
            // set the TZ environment variable and update c locale stuff
            //putenv(strdup("TZ=" + customTzName));
            //tzset();
        }
    }

    /**
     * Return the current time shift at observator time zone with respect to GMT time
     *
     * @param jd The Julian day
     * @return the GMT shift value
     */
    public double getGMTShift(double jd) {
        return getGMTShift(jd, false);
    }

    /**
     * Return the current time shift at observator time zone with respect to GMT time
     *
     * @param jd      The Julian day
     * @param isLocal
     * @return
     */
    public double getGMTShift(double jd, boolean isLocal) {
        if (timeZoneMode == StelApp.TZ_FORMAT.GMT_SHIFT) {
            return gmtShift;
        } else {
            return StelUtility.getGMTShiftFromSystem(jd, isLocal);
        }
    }

    /**
     * Convert the time format enum to its associated String and reverse
     *
     * @param tf The time format parameter ("system_default", "24h", "12h")
     * @return The inferred time format constant
     */
    private TIME_FORMAT stringToSTimeFormat(String tf) {
        if ("system_default".equals(tf)) return TIME_FORMAT.system_default;
        if ("24h".equals(tf)) return TIME_FORMAT.TIME_24H;
        if ("12h".equals(tf)) return TIME_FORMAT.TIME_12H;
        logger.warning("unrecognized time_display_format : " + tf + " system_default used.");
        return TIME_FORMAT.system_default;
    }

    private String sTimeFormatToString(TIME_FORMAT tf) {
        if (tf == TIME_FORMAT.system_default) return "system_default";
        if (tf == TIME_FORMAT.TIME_24H) return "24h";
        if (tf == TIME_FORMAT.TIME_12H) return "12h";
        logger.warning("unrecognized time_display_format value : " + tf + " system_default used.");
        return "system_default";
    }

    public void setViewPortDistorterType(ViewportDistorter.TYPE type) throws StellariumException {
        if (distorter != null) {
            if (distorter.getType() == type) {
                return;
            }
            distorter = null;
        }
        distorter = ViewportDistorter.create(type, getUi().getScreenW(), getUi().getScreenH(), core.getProjection());
        //IniFileParser conf = new IniFileParser(configDir + "config.ini");
        distorter.init(conf);
    }

    public ViewportDistorter.TYPE getViewPortDistorterType() {
        if (distorter != null) {
            return distorter.getType();
        }
        return ViewportDistorter.TYPE.none;
    }

    private LoadingBar loadingBar;

    /**
     * Initialize application and core
     *
     * @throws StellariumException
     */
    public void init() throws StellariumException {
        conf = new IniFileParser(getClass(), ResourceLocatorUtil.getInstance().getConfigFile());

        // Main section
        String version = conf.getStr("main", "version");

        if (!version.equals(Main.VERSION)) {
            int firstDot = version.indexOf('.');
            int v1 = Integer.parseInt(version.substring(0, firstDot));
            int v2 = Integer.parseInt(version.substring(firstDot + 1, version.indexOf('.', firstDot + 1)));

            // Config versions less than 0.6.0 are not supported, otherwise we will try to use it
            if (v1 == 0 && v2 < 6) {
                // The config file is too old to try an importation
                logger.warning("The current config file is from a version too old for parameters to be imported (" + (version == null ? "<0.6.0" : version)
                        + ").\nIt will be replaced by the default config file.");
                ResourceLocatorUtil.getInstance().copyDefaultConfig();
                // Read new config!
                conf = new IniFileParser(getClass(), ResourceLocatorUtil.getInstance().getConfigFile());
            } else {
                logger.warning("Attempting to use an existing older config file.");
            }
        }

        initCore();
        calendar = Calendar.getInstance(Locale.getDefault());
        commander = new StelCommandInterface(this);
        scripts = new ScriptMgr(commander);
        initUi();
        initData();
    }

    private void initCore() {
        core = new StelCore(this, logger);
        core.propertyChangeSupport.addPropertyChangeListener(loadingBar);
    }

    private void initUi() {
        String uiClassName = ResourceLocatorUtil.isMacOS() ? "org.stellarium.ui.MacSwingUI" : "org.stellarium.ui.SwingUI";
        try {
            Class<?> uiClass = Class.forName(uiClassName);
            Constructor<?> constructor = uiClass.getConstructor(StelCore.class, StelApp.class, Logger.class);
            ui = (SwingUI) constructor.newInstance(core, this, logger);

            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace();
                    String title;
                    String message;
                    Translator translator = getCore().getTranslator();
                    if (e instanceof StellariumException) {
                        title = translator.translate("Error");
                        message = translator.translate(e.getMessage());
                    } else {
                        title = translator.translate("UnexpectedError");
                        message = e.getClass().getName();
                        if (e.getMessage() != null && e.getMessage().length() > 0) {
                            message += ": " + e.getMessage();
                        }
                    }
                    getUi().showError(title, message);
                }
            });
        } catch (Throwable e) {
            throw new StellariumException("Could not instantiate User Interface", e);
        }
    }

    public void initData() {
        maxFPS = (float) conf.getDouble(IniFileParser.VIDEO_SECTION, "maximum_fps", 10000);
        minFPS = (float) conf.getDouble(IniFileParser.VIDEO_SECTION, "minimum_fps", 10000);
        String appLocaleName = conf.getStr(IniFileParser.LOCALIZATION_SECTION, "app_locale", "system");
        setDateFormatStr(conf.getStr(IniFileParser.LOCALIZATION_SECTION, "date_display_format"), conf.getStr(IniFileParser.LOCALIZATION_SECTION, "time_display_format"));
        setAppLanguage(Translator.codeToLocale(appLocaleName));
        scripts.setAllowUI(conf.getBoolean(IniFileParser.GUI_SECTION, "flag_script_allow_ui", false));

        // time_zone used to be in init_location section of config,
        // so use that as fallback when reading config - Rob
        String tzstr = conf.getStr(IniFileParser.LOCALIZATION_SECTION, "time_zone", conf.getStr("init_location", "time_zone", "system_default"));
        if ("system_default".equals(tzstr)) {
            timeZoneMode = TZ_FORMAT.SYSTEM_DEFAULT;
            // Set the program global intern timezones variables from the system locale
            //            tzset();
        } else {
            /*
            if ("gmt+x".equals(tzstr)) {// TODO : handle GMT+X timezones form
                timeZoneMode = TZ_FORMAT.GMT_SHIFT;
                // GMT_shift = x;
            } else {
                // We have a custom time zone name
                timeZoneMode = TZ_FORMAT.CUSTOM;
                setCustomTzName(tzstr);
            }
            */

            // We have a custom time zone name
            timeZoneMode = TZ_FORMAT.CUSTOM;
            setCustomTimezone(tzstr);
        }

        core.initData(conf);
        core.initGL3(conf);

        // Navigation section
        presetSkyTime = conf.getDouble(IniFileParser.NAVIGATION_SECTION, "preset_sky_time", 2451545.);
        startupTimeMode = conf.getStr(IniFileParser.NAVIGATION_SECTION, "startup_time_mode");// Can be "now" or "preset"
        mouseZoom = conf.getInt(IniFileParser.NAVIGATION_SECTION, IniFileParser.MOUSE_ZOOM_FACTOR, DEFAULT_MOUSE_ZOOM);

        if ("preset".equalsIgnoreCase(startupTimeMode))
            core.setJDay(presetSkyTime - getGMTShift(presetSkyTime) * Navigator.JD_HOUR);
        else
            core.setTimeNow();
    }

    /**
     * Update all object according to the delta time
     *
     * @param deltaTime The time that elapsed.
     * @throws StellariumException
     */
    public void update(long deltaTime) throws StellariumException {
        ++frame;
        timefr += deltaTime;
        if (timefr - timeBase > 1000) {
            fps = frame * 1000.0 / (timefr - timeBase);// Calc the FPS rate
            frame = 0;
            timeBase += 1000;
        }

        // change time rate if needed to fast forward scripts
        deltaTime *= timeMultiplier;

        // keep audio position updated if changing time multiplier
        if (!scripts.isPaused()) {
            //         commander.update(deltaTime);
        }

        // run command from a running script
        scripts.update(deltaTime);

        if (!scripts.isPaused()) {
            core.getImageMgr().update(deltaTime);
        }

        core.update(deltaTime);
    }

    /**
     * Draw all / Main drawinf function called at each frame
     *
     * @param deltaTime
     * @return the max squared distance in pixels that any object has travelled since the last update.
     */
    public void draw(int deltaTime) {
        // Render all the main objects of stellarium
        core.draw(deltaTime);

        // Draw the Graphical ui and the Text ui
        //ui.draw();

        distorter.distort();
    }

    /**
     * Start the main loop until the end of the execution
     */
    public void startMainLoop() {
        getUi().startMainLoop();
    }

    // n.b. - do not confuse this with sky time rate
    public int getTimeMultiplier() {
        return timeMultiplier;
    }

    /**
     * End the application
     */
    public void quit() {
        /*try {
            bundleContext.getBundle(0).stop();
        } catch (BundleException e) {
            throw new StellariumException("Error while stopping StelApp bundle", e);
        }*/
        System.exit(0);
    }

    public FontFactory getFontFactory() {
        return getUi().getFontFactory();
    }

    public void playStartupScript() {

    }

    /**
     * Set the application language
     * This applies to GUI, console messages etc..
     * This function has no permanent effect on the global locale
     *
     * @param newAppLocaleName The name of the language (e.g fr) to use for GUI, TUI and console messages etc..
     */
    public void setAppLanguage(Locale newAppLocaleName) {
        // Update the translator with new locale name
        Translator.setCurrentTranslator(newAppLocaleName);
        logger.config("Application locale is " + Translator.getCurrentTranslator().getTrueLocaleName());

        // update translations and font in tui
        //        ui.localizeTui();

        // TODO: GUI needs to be reinitialized to load new translations and/or fonts
    }

    public String getAppLanguage() {
        return Translator.getCurrentTranslator().getTrueLocaleName();
    }

    /**
     * Set flag for activating night vision mode
     */
    public void setVisionModeNight() throws StellariumException {
        if (!getVisionModeNight()) {
            core.setColorScheme(conf, "night_color");
            //            ui.setColorScheme(getConfigFile(), "night_color");
        }
        drawMode = DRAWMODE.NIGHT;
    }

    /**
     * Get flag for activating night vision mode
     */
    public boolean getVisionModeNight() {
        return drawMode == DRAWMODE.NIGHT;
    }

    /**
     * Set flag for activating chart vision mode
     */
    public void setVisionModeChart() throws StellariumException {
        if (!getVisionModeChart()) {
            core.setColorScheme(conf, "chart_color");
            //            ui.setColorScheme(getConfigFile(), "chart_color");
        }
        drawMode = DRAWMODE.CHART;
    }

    /**
     * Get flag for activating chart vision mode
     *
     * @return If the vision draw mode is CHART
     */
    public boolean getVisionModeChart() {
        return drawMode == DRAWMODE.CHART;
    }

    /**
     * Set flag for activating chart vision mode
     * ["color" section name used for easier backward compatibility for older configs - Rob]
     */
    public void setVisionModeNormal() throws StellariumException {
        if (!isVisionModeNormal()) {
            core.setColorScheme(conf, "color");
            //            ui.setColorScheme(getConfigFile(), "color");
        }
        drawMode = DRAWMODE.NORMAL;
    }

    /**
     * Get flag for activating chart vision mode
     */
    public boolean isVisionModeNormal() {
        return drawMode == DRAWMODE.NORMAL;
    }

    public IniFileParser getConf() {
        return conf;
    }

    // for use by TUI
    public void saveCurrentConfig(IniFileParser conf) throws StellariumException {
        // No longer resaves everything, just settings user can change through UI
        logger.fine("Saving configuration file...");

        // Main section
        Preferences mainSec = conf.getSection("main");
        mainSec.put("version", Main.VERSION);

        // localization section
        Preferences localSec = conf.getSection(IniFileParser.LOCALIZATION_SECTION);
        localSec.put("sky_culture", core.getSkyCultureDir());
        localSec.put("sky_locale", core.getSkyLanguage().toString());
        localSec.put("app_locale", getAppLanguage());
        localSec.put("time_display_format", getTimeFormatStr());
        localSec.put("date_display_format", getDateFormatStr());
        if (timeZoneMode == TZ_FORMAT.CUSTOM) {
            localSec.put("time_zone", customTzName);
        }
        if (timeZoneMode == TZ_FORMAT.SYSTEM_DEFAULT) {
            localSec.put("time_zone", "system_default");
        }
        if (timeZoneMode == TZ_FORMAT.GMT_SHIFT) {
            localSec.put("time_zone", "gmt+x");
        }

        // viewing section
        Preferences viewingSec = conf.getSection("viewing");
        viewingSec.putBoolean("flag_constellation_drawing", core.isConstellationLinesEnabled());
        viewingSec.putBoolean("flag_constellation_name", core.getFlagConstellationNames());
        viewingSec.putBoolean("flag_constellation_art", core.getFlagConstellationArt());
        viewingSec.putBoolean("flag_constellation_boundaries", core.getFlagConstellationBoundaries());
        viewingSec.putBoolean("flag_constellation_pick", core.getFlagConstellationIsolateSelected());
        viewingSec.putDouble("moon_scale", core.getMoonScale());
        //viewingSec.putBoolean("use_common_names", FlagUseCommonNames);
        viewingSec.putBoolean("flag_equatorial_grid", core.isEquatorGridEnabled());
        viewingSec.putBoolean("flag_azimutal_grid", core.isAzimutalGridEnabled());
        viewingSec.putBoolean("flag_equator_line", core.isEquatorLineEnabled());
        viewingSec.putBoolean("flag_ecliptic_line", core.isEclipticLineEnabled());
        viewingSec.putBoolean("flag_cardinal_points", core.isCardinalsPointsEnabled());
        viewingSec.putBoolean("flag_meridian_line", core.getFlagMeridianLine());
        viewingSec.putBoolean("flag_moon_scaled", core.isMoonScaled());
        viewingSec.putDouble("constellation_art_intensity", core.getConstellationArtIntensity());
        viewingSec.putDouble("constellation_art_fade_duration", core.getConstellationArtFadeDuration());

        // Landscape section
        Preferences landscapeSec = conf.getSection("landscape");
        landscapeSec.putBoolean("flag_landscape", core.isLandscapeEnabled());
        landscapeSec.putBoolean("flag_atmosphere", core.isAtmosphereEnabled());
        landscapeSec.putBoolean("flag_fog", core.isFogEnabled());
        //conf.set_double ("viewing:atmosphere_fade_duration", core.getAtmosphereFadeDuration());

        // Star section
        Preferences starsSec = conf.getSection("stars");
        starsSec.putDouble("star_scale", core.getStarScale());
        starsSec.putDouble("star_mag_scale", core.getStarMagScale());
        starsSec.putBoolean("flag_point_star", core.getPointStar());
        starsSec.putDouble("max_mag_star_name", core.getMaxMagStarName());
        starsSec.putBoolean("flag_star_twinkle", core.isStarTwinkleEnabled());
        starsSec.putDouble("star_twinkle_amount", core.getStarTwinkleAmount());
        // starsSec.putDouble("star_limiting_mag", hip_stars.core.get_limiting_mag());

        // Color section
        Preferences colorSec = conf.getSection("color");
        colorSec.put("azimuthal_color", StelUtility.colorToString(core.getColorAzimutalGrid()));
        colorSec.put("equatorial_color", StelUtility.colorToString(core.getColorEquatorGrid()));
        colorSec.put("equator_color", StelUtility.colorToString(core.getColorEquatorLine()));
        colorSec.put("ecliptic_color", StelUtility.colorToString(core.getColorEclipticLine()));
        colorSec.put("meridian_color", StelUtility.colorToString(core.getColorMeridianLine()));
        colorSec.put("const_lines_color", StelUtility.colorToString(core.getColorConstellationLine()));
        colorSec.put("const_names_color", StelUtility.colorToString(core.getColorConstellationNames()));
        colorSec.put("const_boundary_color", StelUtility.colorToString(core.getColorConstellationBoundaries()));
        colorSec.put("nebula_label_color", StelUtility.colorToString(core.getColorNebulaLabels()));
        colorSec.put("nebula_circle_color", StelUtility.colorToString(core.getColorNebulaCircle()));
        colorSec.put("cardinal_color", StelUtility.colorToString(core.getColorCardinalPoints()));
        colorSec.put("planet_names_color", StelUtility.colorToString(core.getColorPlanetsNames()));
        colorSec.put("planet_orbits_color", StelUtility.colorToString(core.getColorPlanetsOrbits()));
        colorSec.put("object_trails_color", StelUtility.colorToString(core.getColorPlanetsTrails()));
        //  Are these used?
        // colorSec.put    ("star_label_color", StelUtility.tuple3dToString(core.getColorStarNames()));
        //  colorSec.put    ("star_circle_color", StelUtility.tuple3dToString(core.getColorStarCircles()));

        // gui section
        conf.getSection("gui").putDouble("mouse_cursor_timeout", getMouseCursorTimeout());

        // not user settable yet
        // conf.set_str ("gui:gui_base_color", StelUtility::vec3f_to_str(GuiBaseColor));
        // conf.set_str ("gui:gui_text_color", StelUtility::vec3f_to_str(GuiTextColor));
        // conf.set_double ("gui:base_font_size", BaseFontSize);

        // Text ui section
        Preferences tuiSec = conf.getSection("tui");
        //        tuiSec.putBoolean("flag_show_gravity_ui", ui.getFlagShowGravityUi());
        //        tuiSec.putBoolean("flag_show_tui_datetime", ui.getFlagShowTuiDateTime());
        //        tuiSec.putBoolean("flag_show_tui_short_obj_info", ui.getFlagShowTuiShortObjInfo());

        // Navigation section
        Preferences navigationSec = conf.getSection(IniFileParser.NAVIGATION_SECTION);
        navigationSec.putBoolean("flag_manual_zoom", core.getFlagManualAutoZoom());
        navigationSec.putDouble("auto_move_duration", core.getAutoMoveDuration());
        navigationSec.putDouble("zoom_speed", core.getZoomSpeed());
        navigationSec.putDouble("preset_sky_time", presetSkyTime);
        navigationSec.put("startup_time_mode", startupTimeMode);

        // Astro section
        Preferences astroSec = conf.getSection("astro");
        astroSec.putBoolean("flag_object_trails", core.getFlagPlanetsTrails());
        astroSec.putBoolean("flag_bright_nebulae", core.getFlagBrightNebulae());
        astroSec.putBoolean("flag_stars", core.isStarEnabled());
        astroSec.putBoolean("flag_star_name", core.isStarNameEnabled());
        astroSec.putBoolean("flag_nebula", core.getFlagNebula());
        astroSec.putBoolean("flag_nebula_name", core.isNebulaHintEnabled());
        astroSec.putDouble("max_mag_nebula_name", core.getNebulaMaxMagHints());
        astroSec.putBoolean("flag_planets", core.isPlanetsEnabled());
        astroSec.putBoolean("flag_planets_hints", core.isPlanetsHintsEnabled());
        astroSec.putBoolean("flag_planets_orbits", core.getFlagPlanetsOrbits());

        astroSec.putBoolean("flag_milky_way", core.getFlagMilkyWay());
        astroSec.putDouble("milky_way_intensity", core.getMilkyWayIntensity());

        // Get landscape and other observatory info
        // TODO: shouldn't observator already know what section to save in?
        core.getObservatory().setConf(conf, "init_location");

        conf.flush();
    }

    public double getMouseCursorTimeout() {
        return ui.getMouseCursorTimeout();
    }

    /**
     * Required because stelcore doesn't have access to the script manager anymore!
     * Records a command if script recording is on
     *
     * @param commandline The command text to record
     */
    public void recordCommand(String commandline) {
        scripts.recordCommand(commandline);
    }

    public String getTimeFormatStr() {
        return sTimeFormatToString(timeFormatParameter);
    }

    public String getDateFormatStr() {
        return sDateFormatToString(dateFormatType);
    }

    public void setDateFormatStr(String df, String tf) {
        dateFormatType = stringToSDateFormat(df);
        String dateFormatPattern;
        switch (dateFormatType) {
            case SYSTEM_DEFAULT:
                dateFormatPattern = "";
                break;
            case MMDDYYYY:
                dateFormatPattern = "EEE, MM/dd/yyyy";
                break;
            case DDMMYYYY:
                dateFormatPattern = "EEE, dd/MM/yyyy";
                break;
            case YYYYMMDD:
                dateFormatPattern = "EEE, yyyy-MM-dd";
                break;
            default:
                throw new StellariumException("Unsupported date format: " + dateFormatType);
        }
        dateFormatPattern += " ";
        timeFormatParameter = stringToSTimeFormat(tf);
        switch (timeFormatParameter) {
            case system_default:
                dateFormatPattern += "";
                break;
            case TIME_24H:
                dateFormatPattern += "HH:mm:ss";
                break;
            case TIME_12H:
                dateFormatPattern += "hh:mm:ss a";
                break;
            default:
                throw new StellariumException("Unsupported time format: " + timeFormatParameter);
        }
        if (dateFormatPattern.equals(" ")) {
            dateFormat = DateFormat.getDateTimeInstance();
        } else {
            dateFormat = new SimpleDateFormat(dateFormatPattern);
        }
        dateFormat.setCalendar(calendar);
    }

    public void setCustomTimezone(String timeZoneId) {
        String[] timeZoneIds = TimeZone.getAvailableIDs();
        for (String tId : timeZoneIds) {
            if (tId.equalsIgnoreCase(timeZoneId)) {
                TimeZone.setDefault(TimeZone.getTimeZone(tId));
                customTzName = tId;
                timeZoneMode = TZ_FORMAT.CUSTOM;
                break;
            }
        }
        // If not found leave what it was
    }

    /**
     * Initialize openGL screen with SDL
     */
    //    private void initSDL(int w, int h, int bbpMode, boolean fullScreen, String iconFile);
    //
    //    //! Terminate the application with SDL
    //    private void terminateApplication();
    public double getFramesPerSecond() {
        return fps;
    }

    public StelCommandInterface getCommander() {
        return commander;
    }

    /**
     * Get zoom power factor when using Ctrl/Cmd + mouse wheel.
     *
     * @return
     */
    public int getMouseZoom() {
        return mouseZoom;
    }

    public void setPresetSkyTime(double presetSkyTime) {
        this.presetSkyTime = presetSkyTime;
    }

    public double getPresetSkyTime() {
        return presetSkyTime;
    }

    public void setStartupTimeMode(String startupTimeMode) {
        this.startupTimeMode = startupTimeMode;
    }

    public String getStartupTimeMode() {
        return startupTimeMode;
    }

    public String getSelectedScript() {
        return selectedScript;
    }

    public void setSelectedScript(String selectedScript) {
        this.selectedScript = selectedScript;
    }

    public void setSelectedScriptDirectory(String selectedScriptDirectory) {
        this.selectedScriptDirectory = selectedScriptDirectory;
    }

    public void setTimeMultiplier(int timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
    }

    public ScriptMgr getScripts() {
        return scripts;
    }

    public StelUI getUi() {
        return ui;
    }

    public boolean isFlagTimePause() {
        return flagTimePause;
    }

    public void setFlagTimePause(boolean flagTimePause) {
        this.flagTimePause = flagTimePause;
    }

    public double getTempTimeVelocity() {
        return tempTimeVelocity;
    }

    public void setTempTimeVelocity(double tempTimeVelocity) {
        this.tempTimeVelocity = tempTimeVelocity;
    }

    public String getSelectedScriptDirectory() {
        return selectedScriptDirectory;
    }
}
