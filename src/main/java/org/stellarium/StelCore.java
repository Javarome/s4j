/*
 * Stellarium for Java
 * Copyright (c) 2005-2006 Jerome Beau
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

import org.osgi.framework.BundleContext;
import org.stellarium.astro.*;
import org.stellarium.data.IniFileParser;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.landscape.Landscape;
import org.stellarium.landscape.LandscapeOldStyle;
import org.stellarium.landscape.StelAtmosphere;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.projector.Projector;
import org.stellarium.telescope.TelescopeMgr;
import org.stellarium.ui.Cardinals;
import org.stellarium.ui.render.ImageMgr;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.vecmath.Rectangle4i;

import javax.vecmath.Point3d;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.StrictMath.*;
import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * <p>Main class for Stellarium.</p>
 * <p>Manage all the objects to be used in the program.</p>
 *
 * @author <a href="mailto:rr0@rr0.org"/>J&eacute;r&ocirc;me Beau</a>
 * @author Fred Simon
 * @version 2.0.0
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_core.cpp?view=log&pathrev=stellarium-0-8-2">stel_core.cpp</a>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_core.h?view=log&pathrev=stellarium-0-8-2">stel_core.h</a>
 * @since 1.0.0
 */
public class StelCore implements StelComponent, PropertyChangeListener {
    public static final int cardinalPointsFontSize = 30;

    private static final int solarSystemFontSize = 14;

    public static final int generalFontSize = 12;

    private static final int constellationsFontSize = 16;

    public PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    final protected Logger logger;

    public void propertyChange(PropertyChangeEvent evt) {
        propertyChangeSupport.firePropertyChange(evt);  // Propagate
    }

    public enum MOUNT_MODE {
        ALTAZIMUTAL, EQUATORIAL
    }

    // START OF stel_core.cpp

    void setTelescopes(boolean b) {
        telescopeMgr.setFlagTelescopes(b);
    }

    boolean getFlagTelescopes() {
        return telescopeMgr.getFlagTelescopes();
    }

    void setTelescopeName(boolean b) {
        telescopeMgr.setFlagTelescopeName(b);
    }

    boolean getFlagTelescopeName() {
        return telescopeMgr.getFlagTelescopeName();
    }

    public void telescopeGoto(int nr) {
        if (selectedObject != null) {
            telescopeMgr.telescopeGoto(nr, selectedObject.getObsJ2000Pos(navigation));
        }
    }

    public void start(BundleContext bundleContext) throws Exception {
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }

    public StelCore(ApplicationCallback someApplicationCallback, Logger parentLogger) throws StellariumException {
        assert someApplicationCallback != null : "Application callback should not be null";
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }

        Translator.initSystemLanguage();
        skyTranslator = Translator.getCurrentTranslator();

        applicationCallback = someApplicationCallback;

        projection = DefaultProjector.create(DefaultProjector.TYPE.perspective, new Rectangle4i(0, 0, 800, 600), 60, logger);
        glFrontFace(projection.needGlFrontFaceCW() ? GL_CW : GL_CCW);

        stars = new HipStarMgr(logger);
        initConstellations();
        solarSystem = new SolarSystem(logger);
        observatory = new Observator(solarSystem, logger);
        navigation = new Navigator(observatory);
        nebulas = new NebulaMgr(logger);
        milkyWay = new MilkyWay(logger);
        equatorLine = new SkyLine(SkyLine.TYPE.EQUATOR);
        eclipticLine = new SkyLine(SkyLine.TYPE.ECLIPTIC);
        meridianLine = new SkyLine(SkyLine.TYPE.MERIDIAN, 1, 36);
        meteors = new MeteorMgr(10, 60);

        landscape = new LandscapeOldStyle(logger);
        scriptImages = new ImageMgr(logger);
        telescopeMgr = new TelescopeMgr(logger);
    }

    private void initConstellations() {
        asterisms = new ConstellationMgr(stars, logger);
        asterisms.propertyChangeSupport.addPropertyChangeListener(this);
    }

    public void close() {
        // TODO:
    }

    public void initData(IniFileParser conf) throws StellariumException {
        baseFontName = conf.getStr(IniFileParser.GUI_SECTION, "base_font_name", "DejaVuSans.ttf");

        // Init the solar system first
        if (firstTime) {
            solarSystem.load();
        }

        SFontIfc planetNameFont = applicationCallback.getFontFactory().create(solarSystemFontSize, baseFontName);
        solarSystem.setFont(planetNameFont);
        setPlanetsScale(getStarScale());

        observatory.load(conf, "init_location");

        navigation.setJDay(JulianDay.getJulianFromSys());
        navigation.setLocalVision(new Point3d(1, 1e-05, 0.2));

        initStars();
        initNebula();
    }

    private void initNebula() {
        nebulas.propertyChangeSupport.addPropertyChangeListener(this);
        SFontIfc nebulaFont = applicationCallback.getFontFactory().create(generalFontSize, baseFontName);
        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        nebulas.read(nebulaFont,
                locatorUtil.getDataFile("ngc2000.dat"),
                locatorUtil.getDataFile("ngc2000names.dat"),
                locatorUtil.getDataFile("nebula_textures.fab"));
    }

    private void initStars() {
        stars.propertyChangeSupport.addPropertyChangeListener(this);
        SFontIfc hipStarFont = applicationCallback.getFontFactory().create(generalFontSize, baseFontName);
        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        stars.init(hipStarFont, locatorUtil.getDataFile("hipparcos.fab"),
                locatorUtil.getDataFile("sky_cultures/western/star_names.fab"),
                locatorUtil.getDataFile("name.fab"));
    }

    public boolean initGL3(IniFileParser conf) throws StellariumException {
        if (firstTime) {
            telescopeMgr.init(conf);
        }

        // now redo this so we fill the autocomplete dialogs now UI inititalised
        // set_system_locale_by_name(SkyLocale); // and UILocale are the same but different format fra vs fr_FR!!!! TONY

        toneConverter.setWorldAdaptationLuminance(3.75f + atmosphere.getIntensity() * 40000.f);

        // Compute planets data and init viewing position
        // Position of sun and all the satellites (ie planets)
        solarSystem.computePositions(navigation.getJulianDay(), navigation.getHomePlanet());
        // Matrix for sun and all the satellites (ie planets)
        solarSystem.computeTransMatrices(navigation.getJulianDay(), navigation.getHomePlanet());

        // Compute transform matrices between coordinates systems
        navigation.updateTransformMatrices();
        navigation.updateModelViewMat();

        // Load constellations from the correct sky culture
        String tmp = conf.getStr(IniFileParser.LOCALIZATION_SECTION, "sky_culture", ResourceLocatorUtil.getInstance().getSkyCulturesDir() + "/western");
        setSkyCultureDir(tmp);
        skyCultureDir = tmp;

        setPlanetsSelected("");// Fix a bug on macosX! Thanks Fumio!

        String skyLocaleName = conf.getStr(IniFileParser.LOCALIZATION_SECTION, "sky_locale", "system");
        constellationFontSize = (int) conf.getDouble(IniFileParser.VIEWING_SECTION, "constellation_font_size", constellationsFontSize);
        setSkyLocale(Translator.codeToLocale(skyLocaleName));

        // Star section
        setStarScale((float) conf.getDouble(IniFileParser.STARS_SECTION, IniFileParser.STAR_SCALE));
        setPlanetsScale(conf.getDouble(IniFileParser.STARS_SECTION, IniFileParser.STAR_SCALE));// if reload config

        setStarMagScale((float) conf.getDouble(IniFileParser.STARS_SECTION, "star_mag_scale"));
        setStarTwinkleAmount((float) conf.getDouble(IniFileParser.STARS_SECTION, "star_twinkle_amount"));
        setMaxMagStarName((float) conf.getDouble(IniFileParser.STARS_SECTION, "max_mag_star_name"));
        setStarTwinkle(conf.getBoolean(IniFileParser.STARS_SECTION, "flag_star_twinkle"));
        setPointStar(conf.getBoolean(IniFileParser.STARS_SECTION, "flag_point_star"));
        setStarLimitingMag((float) conf.getDouble(IniFileParser.STARS_SECTION, "star_limiting_mag", 6.5f));

        flagEnableZoomKeys = conf.getBoolean(IniFileParser.NAVIGATION_SECTION, "flag_enable_zoom_keys");
        flagEnableMoveKeys = conf.getBoolean(IniFileParser.NAVIGATION_SECTION, "flag_enable_move_keys");
        flagManualZoom = conf.getBoolean(IniFileParser.NAVIGATION_SECTION, "flag_manual_zoom");


        autoMoveDuration = (float) conf.getDouble(IniFileParser.NAVIGATION_SECTION, "auto_move_duration", 1.5);
        moveSpeed = conf.getDouble(IniFileParser.NAVIGATION_SECTION, "move_speed", 0.0004);
        zoomSpeed = conf.getDouble(IniFileParser.NAVIGATION_SECTION, "zoom_speed", 0.0004);

        // Viewing Mode
        String tmpstr = conf.getStr(IniFileParser.NAVIGATION_SECTION, "viewing_mode");
        if (tmpstr.equals("equator")) navigation.setViewingMode(Navigator.VIEWING_MODE_TYPE.EQUATOR);
        else {
            if (tmpstr.equals("horizon")) navigation.setViewingMode(Navigator.VIEWING_MODE_TYPE.HORIZON);
            else {
                throw new StellariumException("Unknown viewing mode type : " + tmpstr);
            }
        }

        initFov = (float) conf.getDouble(IniFileParser.NAVIGATION_SECTION, "init_fov", 60.);
        projection.setFieldOfView(initFov);

        initViewPos = StelUtility.stringToPoint3d(conf.getStr(IniFileParser.NAVIGATION_SECTION, "init_view_pos"));
        navigation.setLocalVision(initViewPos);

        // Landscape section
        setLandscapeEnabled(conf.getBoolean(IniFileParser.LANDSCAPE_SECTION, "flag_landscape", conf.getBoolean(IniFileParser.LANDSCAPE_SECTION, "flag_ground", true)));// name change
        setFlagFog(conf.getBoolean(IniFileParser.LANDSCAPE_SECTION, "flag_fog"));
        setAtmosphere(conf.getBoolean(IniFileParser.LANDSCAPE_SECTION, "flag_atmosphere"));
        setAtmosphereFadeDuration((float) conf.getDouble(IniFileParser.LANDSCAPE_SECTION, "atmosphere_fade_duration", 1.5));

        // Viewing section
        Preferences viewingSec = conf.getSection(IniFileParser.VIEWING_SECTION);
        setFlagConstellationLines(viewingSec.getBoolean("flag_constellation_drawing", false));
        setFlagConstellationNames(viewingSec.getBoolean("flag_constellation_name", false));
        setFlagConstellationBoundaries(viewingSec.getBoolean("flag_constellation_boundaries", false));
        setFlagConstellationArt(viewingSec.getBoolean("flag_constellation_art", false));
        setFlagConstellationIsolateSelected(viewingSec.getBoolean("flag_constellation_isolate_selected", viewingSec.getBoolean("flag_constellation_pick", false)));
        setConstellationArtIntensity((float) viewingSec.getDouble("constellation_art_intensity", 0.5));
        setConstellationArtFadeDuration((float) viewingSec.getDouble("constellation_art_fade_duration", 2.));

        setAzimutalGrid(viewingSec.getBoolean("flag_azimutal_grid", false));
        setEquatorGrid(viewingSec.getBoolean("flag_equatorial_grid", false));
        setEquatorLine(viewingSec.getBoolean("flag_equator_line", false));
        setEclipticLine(viewingSec.getBoolean("flag_ecliptic_line", false));
        setMeridianLine(viewingSec.getBoolean("flag_meridian_line", false));
        cardinalsPoints.setFlagShow(viewingSec.getBoolean("flag_cardinal_points", false));
        setGravityLabels(viewingSec.getBoolean("flag_gravity_labels", false));
        setMoonScaled(conf.getBoolean(IniFileParser.VIEWING_SECTION, "flag_moon_scaled", conf.getBoolean(IniFileParser.VIEWING_SECTION, "flag_init_moon_scaled", false)));// name change
        setMoonScale((float) conf.getDouble(IniFileParser.VIEWING_SECTION, "moon_scale", 5.));

        // Astro section
        setStars(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_stars"));
        setStarNames(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_star_name"));
        setTelescopes(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_telescopes"));
        setTelescopeName(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_telescope_name"));
        setPlanets(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_planets"));
        setPlanetsHints(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_planets_hints"));
        setPlanetsOrbits(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_planets_orbits"));
        setLightTravelTime(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_light_travel_time"));
        setPlanetsTrails(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_object_trails", false));
        startPlanetsTrails(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_object_trails", false));
        setNebula(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_nebula"));
        setNebulaHints(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_nebula_name"));
        setNebulaMaxMagHints((float) conf.getDouble(IniFileParser.ASTRO_SECTION, "max_mag_nebula_name", 99));
        setNebulaCircleScale((float) conf.getDouble(IniFileParser.ASTRO_SECTION, "nebula_scale", 1.0f));
        setFlagNebulaDisplayNoTexture(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_nebula_display_no_texture", false));
        setMilkyWay(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_milky_way"));
        setMilkyWayIntensity((float) conf.getDouble(IniFileParser.ASTRO_SECTION, "milky_way_intensity", 1.));
        setBrightNebulae(conf.getBoolean(IniFileParser.ASTRO_SECTION, "flag_bright_nebulae"));

        setMeteorsRate(conf.getInt(IniFileParser.ASTRO_SECTION, "meteor_rate", 10));

        return false;
    }

    /**
     * Update all the objects in function of the time
     *
     * @param deltaTime
     * @throws StellariumException
     */
    void update(long deltaTime) throws StellariumException {
        // Update the position of observation and time etc...
        observatory.update(deltaTime);
        navigation.updateTime(deltaTime);

        double julianDay = navigation.getJulianDay();
        Planet homePlanet = navigation.getHomePlanet();

        // Position of sun and all the satellites (ie planets)
        solarSystem.computePositions(julianDay, homePlanet);
        // Matrix for sun and all the satellites (ie planets)
        solarSystem.computeTransMatrices(julianDay, homePlanet);

        // communicate with the telescopes:
        telescopeMgr.communicate();

        // Transform matrices between coordinates systems
        navigation.updateTransformMatrices();
        // Direction of vision
        navigation.updateVisionVector(deltaTime, selectedObject);
        // Field of view
        projection.updateAutoZoom(deltaTime);

        // planet trails (call after nav is updated)
        solarSystem.update(deltaTime, navigation);

        // Move the view direction and/or Field Of View
        updateMove(deltaTime);

        // Update info about selected object
        if (selectedObject != null) {
            selectedObject.update(deltaTime);
        }

        // Update faders
        //equatorialGrid.update(deltaTime);
        //azimutalGrid.update(deltaTime);
        equatorLine.update(deltaTime);
        eclipticLine.update(deltaTime);
        meridianLine.update(deltaTime);
        //asterisms.update(deltaTime);
        atmosphere.update(deltaTime);
        landscape.update(deltaTime);
        stars.update(deltaTime);
        nebulas.update(deltaTime);
        //cardinalsPoints.update(deltaTime);
        //milkyWay.update(deltaTime);
        telescopeMgr.update(deltaTime);

        // Compute the sun position in local coordinate
        Point3d sunPos = new Point3d(0, 0, 0);
        navigation.helioToLocal(sunPos);

        // Compute the moon position in local coordinate
        Point3d moonPos = solarSystem.getMoon().getHeliocentricEclipticPos();
        navigation.helioToLocal(moonPos);

        // Compute the atmosphere color and intensity
        final int temperatureC = 15;
        final int relativeHumidityPercent = 40;
        atmosphere.computeColor(julianDay, sunPos, moonPos,
                solarSystem.getMoon().getPhase(solarSystem.getEarth().getHeliocentricEclipticPos()),
                toneConverter, projection, observatory.getLatitude(), observatory.getAltitude(),
                temperatureC, relativeHumidityPercent);
        toneConverter.setWorldAdaptationLuminance(atmosphere.getWorldAdaptationLuminance());

        StelUtility.normalize(sunPos);
        StelUtility.normalize(moonPos);

        // compute global sky brightness TODO : make this more "scientifically"
        // TODO: also add moonlight illumination

        if (sunPos.z < -0.1 / 1.5) {
            skyBrightness = (float) 0.01;
        } else {
            skyBrightness = (float) (0.01 + 1.5 * (sunPos.z + 0.1 / 1.5));
        }

        // TODO make this more generic for non-atmosphere planets
        if (atmosphere.getFadeIntensity() == 1) {
            // If the atmosphere is on, a solar eclipse might darken the sky
            // otherwise we just use the sun position calculation above
            skyBrightness *= (atmosphere.getIntensity() + 0.1);
        }

        // TODO: should calculate dimming with solar eclipse even without atmosphere on
        landscape.setSkyBrightness(skyBrightness + 0.05f);
    }

    /**
     * Execute all the drawing functions
     *
     * @param deltaTime The time that has elapsed
     */
    public void draw(int deltaTime) {
        // Init openGL viewing with fov, screen size and clip planes
        projection.setClippingPlanes(0.000001, 50);

        // Init viewport to current projector values
        projection.applyViewport();

        // Give the updated standard projection matrices to the projector
        projection.setModelviewMatrices(navigation.getEarthEquToEyeMat(), navigation.getHelioToEyeMat(), navigation.getLocalToEyeMat(), navigation.getJ2000ToEyeMat());

        // Set openGL drawings in equatorial coordinates
        navigation.switchToEarthEquatorial();
        try {
            milkyWay.draw(toneConverter, projection, navigation);
            asterisms.draw(projection, navigation);
            nebulas.draw(projection, navigation, toneConverter);
            stars.draw(navigation.getPrecEquVision(), toneConverter, projection);
            equatorialGrid.draw(projection);
            azimutalGrid.draw(projection);
            equatorLine.draw(projection, navigation);
            eclipticLine.draw(projection, navigation);
            meridianLine.draw(projection, navigation);
            solarSystem.draw(projection, navigation, toneConverter, getPointStar());
        } finally {
            navigation.switchToLocal(); // Set openGL drawings in local coordinates i.e. generally altazimuthal coordinates
        }
        meteors.update(projection, navigation, toneConverter, deltaTime);
        if (!isAtmosphereEnabled() || skyBrightness < 0.1) {
            meteors.draw(projection, navigation);
        }

        atmosphere.draw(projection, deltaTime);
        if (pointer != null) {
            pointer.draw(deltaTime);
        }
        landscape.draw(toneConverter, projection, navigation);
        cardinalsPoints.draw(projection, observatory.getLatitude());
        telescopeMgr.draw(projection, navigation);

        // draw images loaded by a script
        scriptImages.draw(navigation, projection);

        projection.drawViewportShape();
    }

    /**
     * Set the landscape
     */
    public boolean setLandscape(String newLandscapeName) throws StellariumException {
        //	if (newLandscapeName.empty() || newLandscapeName==observatory.getLandscapeName()) return;
        if (StelUtility.isEmpty(newLandscapeName)) {
            return false;
        }

        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        Landscape newLandscape = Landscape.createFromFile(locatorUtil.getDataFile("landscapes.ini"), newLandscapeName, logger);
        if (newLandscape == null) {
            return false;
        }

        if (landscape != null) {
            // Copy parameters from previous landscape to new one
            newLandscape.setVisible(landscape.isVisible());
            newLandscape.setFlagShowFog(landscape.getFlagShowFog());
            landscape = newLandscape;
        }

        observatory.setLandscapeName(newLandscapeName);

        return true;
    }

    /**
     * Load a landscape based on a hash of parameters mirroring the landscape.ini file
     * and make it the current landscape
     */
    public boolean loadLandscape(Map<String, String> param) throws StellariumException {
        Landscape newLandscape = Landscape.createFromHash(param, logger);
        if (newLandscape == null)
            return false;

        if (landscape != null) {
            // Copy parameters from previous landscape to new one
            newLandscape.setVisible(landscape.isVisible());
            newLandscape.setFlagShowFog(landscape.getFlagShowFog());
            landscape = newLandscape;
        }
        observatory.setLandscapeName(param.get(IniFileParser.NAME));
        // probably not particularly useful, as not in landscape.ini file

        return true;
    }

    /**
     * Set the viewport width and height
     *
     * @param w The new width
     * @param h The new height
     */
    public void setViewportSize(int w, int h) {
        if (w == getViewportWidth() && h == getViewportHeight()) {
            return;
        }
        projection.setViewportHeight(w, h);
    }

    /**
     * Find any kind of object by the name
     *
     * @param name
     * @return
     */
    private StelObject searchByNameI18n(String name) {
        StelObject rval;
        rval = solarSystem.searchByNamesI18(name);
        if (rval != null) return rval;
        rval = nebulas.searchByNameI18n(name);
        if (rval != null) return rval;
        rval = stars.searchByNameI18n(name);
        if (rval != null) return rval;
        rval = asterisms.searchByNameI18n(name);
        if (rval != null) return rval;
        rval = telescopeMgr.searchByNameI18n(name);
        return rval;
    }

    /**
     * Find and select an object from its translated name
     *
     * @param nameI18n the case sensitive object translated name
     * @return true if a object was found with the passed name
     */
    public boolean findAndSelectI18n(String nameI18n) {
        // Then look for another object
        StelObject obj = searchByNameI18n(nameI18n);
        if (obj == null) return false;
        else return selectObject(obj);
    }

    /**
     * Find and select an object based on selection type and standard name or number
     *
     * @return true if an object was selected
     */
    public boolean selectObject(String type, String id) throws StellariumException {
        if (type == null) {
            logger.severe("Invalid selection null type specified");
        }
        if ("hp".equals(type)) {
            int hpnum = Integer.parseInt(id);
            selectedObject = stars.searchHP(hpnum);
            asterisms.setSelected(selectedObject);
            setPlanetsSelected("");
        } else if ("star".equals(type)) {
            selectedObject = stars.search(id);
            asterisms.setSelected(selectedObject);
            setPlanetsSelected("");
        } else if ("planet".equals(type)) {
            setPlanetsSelected(id);
            selectedObject = solarSystem.getSelected();
            asterisms.setSelected(StelObject.getUninitializedObject());
        } else if ("nebula".equals(type)) {
            selectedObject = nebulas.search(id);
            setPlanetsSelected("");
            asterisms.setSelected(StelObject.getUninitializedObject());

        } else if ("constellation".equals(type)) {
            // Select only constellation, nothing else
            asterisms.setSelected(id);

            stopPointer();
            selectedObject = null;
            setPlanetsSelected("");
        } else if ("constellation_star".equals(type)) {

            // For Find capability, select a star in constellation so can center view on constellation
            asterisms.setSelected(id);

            selectedObject = ((Constellation) asterisms.getSelected())
                    .getBrightestStarInConstellation();

            /// what is this?
            /// 1) Find the hp-number of the 1st star in the selected constellation,
            /// 2) find the star of this hpnumber
            /// 3) select the constellation of this star ???
            ///		const unsigned int hpnum = asterisms->getFirstSelectedHP();
            ///		selected_object = hip_stars->searchHP(hpnum);
            ///		asterisms->setSelected(selected_object);

            setPlanetsSelected("");

            ///		// Some stars are shared, so now force constellation
            ///		asterisms->setSelected(id);
        } else {
            logger.severe("Invalid selection type specified: " + type);
            return false;
        }


        if (selectedObject != null) {
            if (navigation.getTracking()) navigation.setLockEquPos(true);
            navigation.setTracking(false);

            return true;
        }

        return false;
    }

    /**
     * Find and select an object near given equatorial position
     *
     * @return true if a object was found at position (this does not necessarily means it is selected)
     */
    public boolean findAndSelect(Point3d pos) {
        StelObject tempselect = cleverFind(pos);
        return selectObject(tempselect);
    }

    /**
     * Find and select an object near given screen position
     *
     * @param x
     * @return true if a object was found at position (this does not necessarily means it is selected)
     */
    public boolean findAndSelect(int x, int y) {
        Point3d v = new Point3d();
        projection.unprojectEarthEqu(x, getViewportHeight() - y, v);
        return findAndSelect(v);
    }

    /**
     * Find in a "clever" way an object from its equatorial position
     *
     * @param v
     * @return
     */
    StelObject cleverFind(final Point3d v) {
        StelObject sobj = null;
        List<StelObject> candidates = new ArrayList<StelObject>();
        List<StelObject> temp;
        Point3d winpos = new Point3d();

        // Field of view for a 30 pixel diameter circle on screen
        double fovAround = (projection.getFieldOfView() * 30.0d) / min(projection.getViewportWidth(), projection.getViewportHeight());

        double xpos, ypos;
        projection.projectEarthEqu(v, winpos);
        xpos = winpos.x;
        ypos = winpos.y;

        // Collect the planets inside the range
        if (isPlanetsEnabled()) {
            temp = solarSystem.searchAround(v, fovAround, navigation, projection);
            candidates.addAll(temp);
        }

        // nebulas and stars used precessed equ coords
        Point3d p = navigation.earthEquToJ2000(v);

        // The nebulas inside the range
        if (getFlagNebula()) {
            temp = nebulas.searchAround(p, fovAround);
            candidates.addAll(temp);
        }

        // And the stars inside the range
        if (isStarEnabled()) {
            temp = stars.searchAround(p, fovAround);
            candidates.addAll(temp);
        }

        if (getFlagTelescopes()) {
            temp = telescopeMgr.searchAround(p, fovAround);
            candidates.addAll(temp);
        }

        // Now select the object minimizing the function y = distance(in pixel) + magnitude
        double bestObjectValue = 100000;
        for (StelObject iter : candidates) {
            projection.projectEarthEqu(iter.getEarthEquPos(navigation), winpos);

            double distance = sqrt((xpos - winpos.x) * (xpos - winpos.x) + (ypos - winpos.y) * (ypos - winpos.y));
            double mag = iter.getMag(navigation);
            if (iter.getType() == StelObject.TYPE.NEBULA) {
                if (nebulas.getFlagHints()) {
                    // make very easy to select if labeled
                    mag = -1;
                }
            }
            if (iter.getType() == StelObject.TYPE.PLANET) {
                if (isPlanetsHintsEnabled()) {
                    // easy to select, especially pluto
                    mag -= 15.f;
                } else {
                    mag -= 8.f;
                }
            }
            if (distance + mag < bestObjectValue) {
                bestObjectValue = distance + mag;
                sobj = iter;
            }
        }

        return sobj;
    }

    /**
     * Find in a "clever" way an object from its screen position
     *
     * @param x
     * @param y
     * @return
     */
    public StelObject cleverFind(int x, int y) {
        Point3d v = new Point3d();
        projection.unprojectEarthEqu(x, y, v);
        return cleverFind(v);
    }

    /**
     * Go and zoom to the selected object.
     *
     * @param moveDuration
     */
    public void autoZoomIn(float moveDuration) {
        autoZoomIn(moveDuration, true);
    }

    public void autoZoomIn(float moveDuration, boolean allowManualZoom) {
        double manualMoveDuration;

        if (selectedObject == null) {
            return;
        }

        if (!navigation.getTracking()) {
            navigation.setTracking(true);
            navigation.moveTo(selectedObject.getEarthEquPos(navigation), moveDuration, false, Navigator.ZoomingMode.ZOOMING);
            manualMoveDuration = moveDuration;
        } else {
            // faster zoom in manual zoom mode once object is centered
            manualMoveDuration = moveDuration * .66f;
        }

        if (allowManualZoom && flagManualZoom) {
            // if manual zoom mode, user can zoom in incrementally
            double newfov = projection.getFieldOfView() * 0.5f;
            projection.zoomTo(newfov, manualMoveDuration);

        } else {
            double satfov = selectedObject.getSatellitesFOV(navigation);

            if (satfov > 0. && projection.getFieldOfView() * 0.9 > satfov) {
                projection.zoomTo(satfov, moveDuration);
            } else {
                double closefov = selectedObject.getCloseFOV(navigation);
                if (projection.getFieldOfView() > closefov)
                    projection.zoomTo(closefov, moveDuration);
            }
        }
    }

    /**
     * Unzoom and go to the init position
     *
     * @param moveDuration
     */
    public void autoZoomOut(float moveDuration) {
        autoZoomOut(moveDuration, false);
    }

    public void autoZoomOut(float moveDuration, boolean full) {
        if (selectedObject != null && !full) {
            // If the selected object has satellites, unzoom to satellites view
            // unless specified otherwise
            double satfov = selectedObject.getSatellitesFOV(navigation);
            if (satfov > 0 && projection.getFieldOfView() <= satfov * 0.9) {
                projection.zoomTo(satfov, moveDuration);
                return;
            }

            // If the selected object is part of a Planet subsystem (other than sun),
            // unzoom to subsystem view
            satfov = selectedObject.getParentSatellitesFOV(navigation);
            if (satfov > 0 && projection.getFieldOfView() <= satfov * 0.9) {
                projection.zoomTo(satfov, moveDuration);
                return;
            }
        }

        projection.zoomTo(initFov, moveDuration);
        navigation.moveTo(initViewPos, moveDuration, true, Navigator.ZoomingMode.UNZOOMING);
        navigation.setTracking(false);
        navigation.setLockEquPos(false);
    }

    /**
     * Set the current sky culture according to passed name
     *
     * @param cultureName
     */
    public boolean setSkyCulture(String cultureName) throws StellariumException {
        return setSkyCultureDir(skyLocalizer.getFolderName(cultureName));
    }

    /**
     * Set the current sky culture from the passed directory
     */
    boolean setSkyCultureDir(String cultureDir) throws StellariumException {

        if (!StelUtility.isEmpty(skyCultureDir) && skyCultureDir.equals(cultureDir))
            return true;

        // make sure culture definition exists before attempting or will die
        // Do not comment this out! Rob
        if (StelUtility.isEmpty(skyLocalizer.getSkyCultureName(cultureDir))) {
            logger.severe("Invalid sky culture directory: " + cultureDir);
            return false;
        }

        skyCultureDir = cultureDir;

        if (asterisms != null && !asterisms.isEmpty()) {
            return true;
        }

        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        asterisms.loadLinesAndArt(
                locatorUtil.getSkyCultureFile(skyCultureDir, "constellationship.fab"),
                locatorUtil.getSkyCultureFile(skyCultureDir, "constellationsart.fab"),
                locatorUtil.getDataFile("constellations_boundaries.dat"));
        asterisms.loadNames(locatorUtil.getSkyCultureFile(skyCultureDir, "constellation_names.eng.fab"));

        // Re-translated constellation names
        asterisms.translateNames(skyTranslator);

        // as constellations have changed, clear out any selection and retest for match!
        if (selectedObject != null && selectedObject.getType() == StelObject.TYPE.STAR) {
            asterisms.setSelected(selectedObject);
        } else {
            asterisms.setSelected(StelObject.getUninitializedObject());
        }

        // Load culture star names in english
        stars.loadCommonNames(locatorUtil.getSkyCultureFile(skyCultureDir, "star_names.fab"));

        // Turn on sci names/catalog names for western culture only
        stars.setFlagSciNames(skyCultureDir.contains("western"));

        // translate
        stars.translateNames(skyTranslator);

        return true;
    }

    /**
     * @param newSkyLocaleName The name of the locale (e.g fr) to use for sky object labels
     * @brief Set the sky locale and reload the sky objects names for gettext translation
     */
    public void setSkyLocale(Locale newSkyLocaleName) throws StellariumException {
        if (stars == null || cardinalsPoints == null || asterisms == null)
            return;// objects not initialized yet

        Locale oldLocale = getSkyLanguage();

        skyTranslator = Translator.getTranslator(newSkyLocaleName);
        logger.config("Sky locale is " + skyTranslator.getTrueLocaleName());

        // If font has changed or init is being called for first time...
        if (firstTime) {
            SFontIfc cardinalPointsFont = applicationCallback.getFontFactory().create(cardinalPointsFontSize, null);
            cardinalsPoints.setFont(cardinalPointsFont);
            SFontIfc constellationFont = applicationCallback.getFontFactory().create(constellationFontSize, null);  // size is read from config
            asterisms.setFont(constellationFont);
            SFontIfc planetNameFont = applicationCallback.getFontFactory().create(solarSystemFontSize, null);
            solarSystem.setFont(planetNameFont);
            // not translating yet
            //		nebulas->setFont(generalFontSize, font);
            SFontIfc hipStarFont = applicationCallback.getFontFactory().create(generalFontSize, null);
            stars.setFont(hipStarFont);
            SFontIfc telescopeFont = applicationCallback.getFontFactory().create(generalFontSize, null);
            telescopeMgr.setFont(telescopeFont);

            // TODO: TUI short info font needs updating also
            // TEST - need different fixed font
            // ui->setFonts(newFontScale, newFontFile, newFontScale, newFontFile);
        }

        // Translate all labels with the new language
        cardinalsPoints.translateLabels(skyTranslator);
        asterisms.translateNames(skyTranslator);
        solarSystem.translateNames(skyTranslator);
        nebulas.translateNames(skyTranslator);
        stars.translateNames(skyTranslator);
    }

    public Translator getTranslator() {
        return skyTranslator;
    }

    /**
     * Please keep saveCurrentSettings up to date with any new color settings added here
     */
    void setColorScheme(IniFileParser conf, String section) throws StellariumException {
        // simple default color, rather than black which doesn't show up
        String defaultColor = "0.6,0.4,0";

        // Load colors from config file
        nebulas.setLabelColor(StelUtility.stringToColor(conf.getStr(section, "nebula_label_color", defaultColor)));
        nebulas.setCircleColor(StelUtility.stringToColor(conf.getStr(section, "nebula_circle_color", defaultColor)));
        stars.setLabelColor(StelUtility.stringToColor(conf.getStr(section, "star_label_color", defaultColor)));
        stars.setCircleColor(StelUtility.stringToColor(conf.getStr(section, "star_circle_color", defaultColor)));
        telescopeMgr.setLabelColor(StelUtility.stringToColor(conf.getStr(section, "telescope_label_color", defaultColor)));
        telescopeMgr.setCircleColor(StelUtility.stringToColor(conf.getStr(section, "telescope_circle_color", defaultColor)));
        solarSystem.setLabelColor(StelUtility.stringToColor(conf.getStr(section, "planet_names_color", defaultColor)));
        solarSystem.setOrbitColor(StelUtility.stringToColor(conf.getStr(section, "planet_orbits_color", defaultColor)));
        solarSystem.setTrailColor(StelUtility.stringToColor(conf.getStr(section, "object_trails_color", defaultColor)));
        equatorialGrid.setColor(StelUtility.stringToColor(conf.getStr(section, "equatorial_color", defaultColor)));
        //equ_grid.set_top_transparency(draw_mode==DM_NORMAL);
        azimutalGrid.setColor(StelUtility.stringToColor(conf.getStr(section, "azimuthal_color", defaultColor)));
        //azi_grid.set_top_transparency(draw_mode==DM_NORMAL);
        equatorLine.setColor(StelUtility.stringToColor(conf.getStr(section, "equator_color", defaultColor)));
        eclipticLine.setColor(StelUtility.stringToColor(conf.getStr(section, "ecliptic_color", defaultColor)));
        SFontIfc meridianLineFont = applicationCallback.getFontFactory().create(generalFontSize, baseFontName);
        meridianLine.setFont(meridianLineFont);
        meridianLine.setColor(StelUtility.stringToColor(conf.getStr(section, "meridian_color", defaultColor)));
        cardinalsPoints.setColor(StelUtility.stringToColor(conf.getStr(section, "cardinal_color", defaultColor)));
        asterisms.setLineColor(StelUtility.stringToColor(conf.getStr(section, "const_lines_color", defaultColor)));
        asterisms.setBoundaryColor(StelUtility.stringToColor(conf.getStr(section, "const_boundary_color", "0.8,0.3,0.3")));
        asterisms.setLabelColor(StelUtility.stringToColor(conf.getStr(section, "const_names_color", defaultColor)));

        // Init milky way
        // 	if (draw_mode == DM_NORMAL)	milky_way.set_texture("milkyway.png");
        // 	else
        // 	{
        // 		milky_way.set_texture("milkyway_chart.png",true);
        // 	}

        chartColor = StelUtility.stringToColor(conf.getStr(section, "chart_color", defaultColor));
    }

    /**
     * Get a color used to display info about the currently selected object
     *
     * @return
     */
    public Color getSelectedObjectInfoColor() {
        Color selectedObjectColor;
        if (selectedObject == null) {
            logger.warning("No object is currently selected");
            selectedObjectColor = Color.WHITE;
        } else switch (selectedObject.getType()) {
            case NEBULA:
                selectedObjectColor = nebulas.getLabelColor();
                break;
            case PLANET:
                selectedObjectColor = solarSystem.getLabelColor();
                break;
            case STAR:
                selectedObjectColor = selectedObject.getRGB();
                break;
            default:
                selectedObjectColor = Color.WHITE;
        }
        return selectedObjectColor;
    }

    private void drawChartBackground() {
        int stepX = projection.getViewportWidth();
        int stepY = projection.getViewportHeight();
        int viewport_left = projection.getViewportPosX();
        int view_bottom = projection.getViewportPosY();

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glColor3fv(chartColor.getComponents(null), 0);
        projection.setOrthographicProjection();// set 2D coordinate
        glBegin(GL_QUADS);
        glTexCoord2s((short) 0, (short) 0);
        glVertex2i(viewport_left, view_bottom);// Bottom Left
        glTexCoord2s((short) 1, (short) 0);
        glVertex2i(viewport_left + stepX, view_bottom);// Bottom Right
        glTexCoord2s((short) 1, (short) 1);
        glVertex2i(viewport_left + stepX, view_bottom + stepY);// Top Right
        glTexCoord2s((short) 0, (short) 1);
        glVertex2i(viewport_left, view_bottom + stepY);// Top Left
        glEnd();
        projection.resetPerspectiveProjection();
    }

    private String getCursorPos(int x, int y) {
        Point3d v = new Point3d();
        projection.unprojectEarthEqu(x, y, v);
        StelUtility.Coords coords = StelUtility.rectToSphe(v);
        return "RA : " + StelUtility.printAngleHms(coords.getRA()) + "\n" +
                "DE : " + StelUtility.printAngleDms(coords.getDE());
    }

    /**
     * Set the projection type
     *
     * @param pType
     * @throws StellariumException
     */
    public void setProjectionType(Projector.TYPE pType) throws StellariumException {
        if (projection.getType() == pType) return;
        DefaultProjector ptemp = DefaultProjector.create(pType,
                projection.getViewport(),
                projection.getFieldOfView(), logger);
        ptemp.setMaskType(projection.getMaskType());
        ptemp.setFlagGravityLabels(projection.isGravityLabelsEnabled());
        projection = ptemp;
        glFrontFace(projection.needGlFrontFaceCW() ? GL_CW : GL_CCW);
    }

    public void setFlipHorz(boolean flip) {
        projection.setFlipHorz(flip);
        glFrontFace(projection.needGlFrontFaceCW() ? GL_CW : GL_CCW);
    }

    public void setFlipVert(boolean flip) {
        projection.setFlipVert(flip);
        glFrontFace(projection.needGlFrontFaceCW() ? GL_CW : GL_CCW);
    }

    public void turnRight(boolean s) {
        if (s && flagEnableMoveKeys) {
            azimuthDelta = 1;
            setTracking(false);
            setFlagLockSkyPosition(false);
        } else {
            azimuthDelta = 0;
        }
    }

    public void turnLeft(boolean s) {
        if (s && flagEnableMoveKeys) {
            azimuthDelta = -1;
            setTracking(false);
            setFlagLockSkyPosition(false);
        } else {
            azimuthDelta = 0;
        }
    }

    public void turnUp(boolean s) {
        if (s && flagEnableMoveKeys) {
            deltaAlt = 1;
            setTracking(false);
            setFlagLockSkyPosition(false);
        } else {
            deltaAlt = 0;
        }
    }

    public void turnDown(boolean s) {
        if (s && flagEnableMoveKeys) {
            deltaAlt = -1;
            setTracking(false);
            setFlagLockSkyPosition(false);
        } else {
            deltaAlt = 0;
        }
    }

    public void zoomIn(boolean s) {
        if (flagEnableZoomKeys) {
            deltaFov = -1 * (s ? 1 : 0);
        }
    }

    public void zoomOut(boolean s) {
        if (flagEnableZoomKeys) {
            deltaFov = (s ? 1 : 0);
        }
    }

    /**
     * Make the first screen position correspond to the second (useful for mouse dragging)
     */
    public void dragView(int x1, int y1, int x2, int y2) {
        Point3d tempvec1 = new Point3d();
        Point3d tempvec2 = new Point3d();
        int viewportHeight = getViewportHeight();
        if (navigation.getViewingMode() == Navigator.VIEWING_MODE_TYPE.HORIZON) {
            projection.unprojectLocal(x2, viewportHeight - y2, tempvec2);
            projection.unprojectLocal(x1, viewportHeight - y1, tempvec1);
        } else {
            projection.unprojectEarthEqu(x2, viewportHeight - y2, tempvec2);
            projection.unprojectEarthEqu(x1, viewportHeight - y1, tempvec1);
        }
        StelUtility.Coords coords1 = StelUtility.rectToSphe(tempvec1);
        StelUtility.Coords coords2 = StelUtility.rectToSphe(tempvec2);
        navigation.updateMove(coords2.getLongitude() - coords1.getLongitude(), coords1.getLatitude() - coords2.getLatitude());
        setTracking(false);
        setFlagLockSkyPosition(false);
    }

    /**
     * Increment/decrement smoothly the vision field and position
     *
     * @param deltaTime
     */
    public void updateMove(long deltaTime) {
        // the more it is zoomed, the more the mooving speed is low (in angle)
        double depl = moveSpeed * deltaTime * projection.getFieldOfView();
        double deplzoom = zoomSpeed * deltaTime * projection.getFieldOfView();
        if (azimuthDelta < 0) {
            azimuthDelta = -depl / 30;
            if (azimuthDelta < -0.2) {
                azimuthDelta = -0.2;
            }
        } else {
            if (azimuthDelta > 0) {
                azimuthDelta = (depl / 30);
                if (azimuthDelta > 0.2) {
                    azimuthDelta = 0.2;
                }
            }
        }
        if (deltaAlt < 0) {
            deltaAlt = -depl / 30;
            if (deltaAlt < -0.2) {
                deltaAlt = -0.2;
            }
        } else {
            if (deltaAlt > 0) {
                deltaAlt = depl / 30;
                if (deltaAlt > 0.2) {
                    deltaAlt = 0.2;
                }
            }
        }

        if (deltaFov < 0) {
            deltaFov = -deplzoom * 5;
            if (deltaFov < -0.15 * projection.getFieldOfView()) {
                deltaFov = -0.15f * projection.getFieldOfView();
            }
        } else {
            if (deltaFov > 0) {
                deltaFov = depl * 5;
                if (deltaFov > 20) {
                    deltaFov = 20;
                }
            }
        }

        //projection.changeFov(deltaFov);
        //navigation.updateMove(deltaAz, deltaAlt);

        if (deltaFov != 0) {
            projection.changeFov(deltaFov);
            applicationCallback.recordCommand("zoom delta_fov " + deltaFov);
        }

        if (azimuthDelta != 0 || deltaAlt != 0) {
            navigation.updateMove(azimuthDelta, deltaAlt);
            applicationCallback.recordCommand("look delta_az " + azimuthDelta + " delta_alt " + deltaAlt);
        } else {
            // must perform call anyway, but don't record!
            navigation.updateMove(azimuthDelta, deltaAlt);
        }
    }

    public boolean setHomePlanet(String planet) {
        // reset planet trails due to changed perspective
        solarSystem.startTrails(solarSystem.getFlagTrails());

        return observatory.setHomePlanet(planet);
    }

    /**
     * For use by TUI
     */
    public String getPlanetHashString() {
        return solarSystem.getPlanetHashString();
    }

    /**
     * Set stellarium time to current real world time
     */
    void setTimeNow() {
        navigation.setJDay(JulianDay.getJulianFromSys());
    }

    /**
     * Get wether the current stellarium time is the real world time
     */
    boolean getIsTimeNow() {
        // cache last time to prevent to much slow system call
        double lastJD = getJulianDay();
        boolean previousResult = (abs(getJulianDay() - JulianDay.getJulianFromSys()) < NavigatorIfc.JD_SECOND);
        if (abs(lastJD - getJulianDay()) > NavigatorIfc.JD_SECOND / 4) {
            lastJD = getJulianDay();
            previousResult = (abs(getJulianDay() - JulianDay.getJulianFromSys()) < NavigatorIfc.JD_SECOND);
        }
        return previousResult;
    }

    /**
     * Selects a given object.
     *
     * @param obj The object to select.
     * @return true if the object was selected (false if the same was already selected)
     */
    private boolean selectObject(StelObject obj) {
        boolean selectionChanged;
        // Unselect if it is the same object
        if (obj == null || selectedObject == obj) {
            unSelect();
            selectionChanged = true;
        } else if (obj.getType() == StelObject.TYPE.CONSTELLATION) {
            selectionChanged = selectObject(((Constellation) obj).getBrightestStarInConstellation());
        } else {
            selectedObject = obj;
            // Draw the pointer on the currently selected object
            // TODO: this would be improved if pointer was drawn at same time as object for correct depth in scene
            if (objectPointer) {
                startPointer(projection, navigation);
            }

            // If an object was selected keep the earth following
            if (getFlagTracking()) {
                navigation.setLockEquPos(true);
            }
            setTracking(false);

            switch (selectedObject.getType()) {
                case STAR:
                    asterisms.setSelected(selectedObject);
                    // potentially record this action
                    applicationCallback.recordCommand("select " + selectedObject.getEnglishName());
                    break;

                case PLANET:
                    solarSystem.setSelected(selectedObject);
                    // potentially record this action
                    applicationCallback.recordCommand("select planet " + selectedObject.getEnglishName());
                    break;

                case NEBULA:
                    // potentially record this action
                    applicationCallback.recordCommand("select nebula \"" + selectedObject.getEnglishName() + "\"");
                    break;

                default:
                    asterisms.setSelected(StelObject.getUninitializedObject());
            }
            selectionChanged = true;
        }
        return selectionChanged;
    }

    List<String> listMatchingObjectsI18n(String objPrefix) {
        return listMatchingObjectsI18n(objPrefix, 5);
    }

    /**
     * Find and return the list of at most maxNbItem objects auto-completing the passed object I18n name
     *
     * @param objPrefix the case insensitive first letters of the searched object
     * @param maxNbItem the maximum number of returned object names
     * @return a vector of matching object name by order of relevance, or an empty vector if nothing match
     */
    public List<String> listMatchingObjectsI18n(String objPrefix, int maxNbItem) {
        List<String> result = new ArrayList<String>();

        // Get matching planets
        List<String> matchingPlanets = solarSystem.listMatchingObjectsI18n(objPrefix, maxNbItem);
        result.addAll(matchingPlanets);
        maxNbItem -= matchingPlanets.size();

        // Get matching constellations
        List<String> matchingConstellations = asterisms.listMatchingObjectsI18n(objPrefix, maxNbItem);
        result.addAll(matchingConstellations);
        maxNbItem -= matchingConstellations.size();

        // Get matching nebulae
        List<String> matchingNebulae = nebulas.listMatchingObjectsI18n(objPrefix, maxNbItem);
        result.addAll(matchingNebulae);
        maxNbItem -= matchingNebulae.size();

        // Get matching stars
        List<String> matchingStars = stars.listMatchingObjectsI18n(objPrefix, maxNbItem);
        result.addAll(matchingStars);
        maxNbItem -= matchingStars.size();

        // Get matching telescopes
        List<String> matchingTelescopes = telescopeMgr.listMatchingObjectsI18n(objPrefix, maxNbItem);
        result.addAll(matchingTelescopes);
        maxNbItem -= matchingStars.size();

        Collections.sort(result);

        return result;
    }

    /**
     * Enable/disable selected object tracking
     *
     * @param track If the tracking must be enabled.
     */
    public void setTracking(boolean track) {
        if (!track || selectedObject == null) {
            navigation.setTracking(false);
        } else {
            navigation.moveTo(selectedObject.getEarthEquPos(navigation), getAutoMoveDuration());
            navigation.setTracking(true);
        }
    }

    public void setStars(boolean b) {
        stars.setStars(b);
    }

    public boolean isStarEnabled() {
        return stars.getFlagStars();
    }

    public void setStarNames(boolean b) {
        stars.setStarNames(b);
    }

    public boolean isStarNameEnabled() {
        return stars.getStarNames();
    }

    void setStarSciNames(boolean b) {
        stars.setStarSciNames(b);
    }

    boolean getStarSciNames() {
        return stars.getFlagStarSciName();
    }

    public void setStarTwinkle(boolean b) {
        stars.setStarTwinkle(b);
    }

    public boolean isStarTwinkleEnabled() {
        return stars.getStarTwinkle();
    }

    public void setPointStar(boolean b) {
        stars.setPointStar(b);
    }

    public boolean getPointStar() {
        return stars.getPointStar();
    }

    public void setMaxMagStarName(float f) {
        stars.setMaxMagStarName(f);
    }

    public float getMaxMagStarName() {
        return stars.getMaxMagStarName();
    }

    void setMaxMagStarSciName(float f) {
        stars.setMaxMagStarSciName(f);
    }

    float getMaxMagStarSciName() {
        return stars.getMaxMagStarSciName();
    }

    public void setStarScale(float f) {
        stars.setStarScale(f);
    }

    public float getStarScale() {
        return stars.getStarScale();
    }

    public void setStarMagScale(float f) {
        stars.setStarMagScale(f);
    }

    public float getStarMagScale() {
        return stars.getStarMagScale();
    }

    public void setStarTwinkleAmount(float f) {
        stars.setStarTwinkleAmount(f);
    }

    public float getStarTwinkleAmount() {
        return stars.getStarTwinkleAmount();
    }

    void setStarLimitingMag(float f) {
        stars.setStarLimitingMag(f);
    }

    float getStarLimitingMag() {
        return stars.getStarLimitingMag();
    }


    Color getColorStarNames() {
        return stars.getLabelColor();
    }

    Color getColorStarCircles() {
        return stars.getCircleColor();
    }

    void setColorStarNames(Color v) {
        stars.setLabelColor(v);
    }

    void setColorStarCircles(Color v) {
        stars.setCircleColor(v);
    }

    // END OF stel_core.cpp

    // TODO: fred I'm here

    // START OF stel_core.h public methods implemented

    public String getSkyCultureDir() {
        return skyCultureDir;
    }

    /**
     * Get the current sky culture I18 name
     */
    public String getSkyCulture() {
        return skyLocalizer.getSkyCultureName(skyCultureDir);
    }

    /**
     * Get the I18 available sky culture names
     */
    public List<String> getSkyCultureList() {
        return skyLocalizer.getSkyCultureList();
    }

    /** Get the current sky language used for sky object labels*/
    /**
     * @return The name of the locale (e.g fr)
     */
    public Locale getSkyLanguage() {
        return skyTranslator.getLocale();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Navigation

    /**
     * Set time speed in JDay/sec
     */
    public void setTimeSpeed(double ts) {
        navigation.setTimeSpeed(ts);
    }

    /**
     * Get time speed in JDay/sec
     */
    public double getTimeSpeed() {
        return navigation.getTimeSpeed();
    }

    /**
     * Set the current date in Julian Day
     */
    public void setJDay(double JD) {
        navigation.setJDay(JD);
    }

    /**
     * Get the current date in Julian Day
     */
    public double getJulianDay() {
        return navigation.getJulianDay();
    }

    /**
     * Get object tracking
     */
    public boolean getFlagTracking() {
        return navigation.getTracking();
    }

    /**
     * Set whether sky position is to be locked
     */
    public void setFlagLockSkyPosition(boolean b) {
        navigation.setLockEquPos(b);
    }

    /**
     * Set whether sky position is locked
     */
    public boolean getFlagLockSkyPosition() {
        return navigation.getLockEquPos();
    }

    /**
     * Set current mount type
     */
    public void setMountMode(MOUNT_MODE m) {
        navigation.setViewingMode((m == MOUNT_MODE.ALTAZIMUTAL) ? Navigator.VIEWING_MODE_TYPE.HORIZON : Navigator.VIEWING_MODE_TYPE.EQUATOR);
    }

    /**
     * Get current mount type
     */
    public MOUNT_MODE getMountMode() {
        return ((navigation.getViewingMode() == Navigator.VIEWING_MODE_TYPE.HORIZON) ? MOUNT_MODE.ALTAZIMUTAL : MOUNT_MODE.EQUATORIAL);
    }

    /**
     * Toggle current mount mode between equatorial and altazimutal
     */
    public void toggleMountMode() {
        if (getMountMode() == MOUNT_MODE.ALTAZIMUTAL) setMountMode(MOUNT_MODE.EQUATORIAL);
        else setMountMode(MOUNT_MODE.ALTAZIMUTAL);
    }


    /**
     * Go to the selected object
     */
    public void gotoSelectedObject() {
        if (selectedObject != null) {
            navigation.moveTo(selectedObject.getEarthEquPos(navigation), autoMoveDuration);
        }
    }

    /**
     * Move view in alt/az (or equatorial if in that mode) coordinates
     */
    public void panView(double deltaAz, double deltaAlt) {
        setTracking(false);
        navigation.updateMove(deltaAz, deltaAlt);
    }

    /**
     * Set automove duration.
     *
     * @param f The aimed automove duration, in seconds
     */
    public void setAutoMoveDuration(float f) {
        autoMoveDuration = f;
    }

    /**
     * Get automove duration in seconds
     */
    public float getAutoMoveDuration() {
        return autoMoveDuration;
    }

    /**
     * Zoom to the given Field Of View.
     *
     * @param aimFov The aimed FOV, in degrees.
     */
    public void zoomTo(double aimFov) {
        zoomTo(aimFov, 1);
    }

    public void zoomTo(double aimFov, float moveDuration) {
        projection.zoomTo(aimFov, moveDuration);
    }

    /**
     * Get current Field Of View
     *
     * @return The current FOV, in degrees
     */
    public double getFieldOfView() {
        return projection.getFieldOfView();
    }

    /**
     * If is currently zooming, return the target FOV, otherwise return current FOV
     *
     * @return
     */
    public double getAimFov() {
        return projection.getAimFov();
    }

    /**
     * Set the current FOV (in degree)
     *
     * @param f
     */
    public void setFov(double f) {
        projection.setFieldOfView(f);
    }

    /**
     * Set the maximum FOV (in degree)
     *
     * @param f
     */
    public void setMaxFov(double f) {
        projection.setMaxFov(f);
    }

    /**
     * Set whether auto zoom can go further than normal
     */
    public void setFlagManualAutoZoom(boolean b) {
        flagManualZoom = b;
    }

    /**
     * Get whether auto zoom can go further than normal
     */
    public boolean getFlagManualAutoZoom() {
        return flagManualZoom;
    }

    /**
     * Return whether an object is currently selected
     */
    public boolean getFlagHasSelected() {
        return selectedObject != null;
    }

    /** Deselect selected object if any*/
    /**
     * Does not deselect selected constellation
     */
    public void unSelect() {
        stopPointer();
        selectedObject = null;
        //asterisms.setSelected((StelObject)null);
        solarSystem.setSelected(StelObject.getUninitializedObject());
    }

    /**
     * Set whether a pointer is to be drawn over selected object.
     *
     * @param selected If the object pointer should be visible.
     */
    public void setObjectPointer(boolean selected) {
        objectPointer = selected;
    }

    /**
     * Get selected object description.
     *
     * @return A multiline String describing the currently selected object.
     */
    public String getSelectedObjectInfo() {
        return selectedObject.getInfoString(navigation);
    }

    /**
     * Get a 1 line String briefly describing the currently selected object
     */
    public String getSelectedObjectShortInfo() {
        return selectedObject.getShortInfoString(navigation);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Constellations methods

    /**
     * Set display flag of constellation lines
     */
    public void setFlagConstellationLines(boolean b) {
        asterisms.setLinesEnabled(b);
    }

    /**
     * Get display flag of constellation lines
     */
    public boolean isConstellationLinesEnabled() {
        return asterisms.isLinesEnabled();
    }

    /**
     * Set display flag of constellation art
     */
    public void setFlagConstellationArt(boolean b) {
        asterisms.setArtEnabled(b);
    }

    /**
     * Get display flag of constellation art
     */
    public boolean getFlagConstellationArt() {
        return asterisms.isArtEnabled();
    }

    /**
     * Set display flag of constellation names
     *
     * @param b If constellation names must be displayed.
     */
    public void setFlagConstellationNames(boolean b) {
        asterisms.setNamesEnabled(b);
    }

    /**
     * Get display flag of constellation names.
     *
     * @return If constellation names must be displayed.
     */
    public boolean getFlagConstellationNames() {
        return asterisms.getNamesEnabled();
    }

    /**
     * Set display flag of constellation boundaries
     */
    public void setFlagConstellationBoundaries(boolean b) {
        asterisms.setBoundariesEnabled(b);
    }

    /**
     * Get display flag of constellation boundaries
     */
    public boolean getFlagConstellationBoundaries() {
        return asterisms.getBoundariesEnabled();
    }

    public Color getColorConstellationBoundaries() {
        return asterisms.getBoundaryColor();
    }

    /**
     * Set constellation art intensity
     */
    public void setConstellationArtIntensity(float f) {
        asterisms.setArtIntensity(f);
    }

    /**
     * Get constellation art intensity
     */
    public double getConstellationArtIntensity() {
        return asterisms.getArtIntensity();
    }

    /**
     * Set constellation art intensity
     */
    public void setConstellationArtFadeDuration(float f) {
        asterisms.setArtFadeDuration(f);
    }

    /**
     * Get constellation art intensity
     */
    public double getConstellationArtFadeDuration() {
        return asterisms.getArtFadeDuration();
    }

    /**
     * Set whether selected constellation is drawn alone
     */
    public void setFlagConstellationIsolateSelected(boolean b) {
        asterisms.setFlagIsolateSelected(b);
    }

    /**
     * Get whether selected constellation is drawn alone
     */
    public boolean getFlagConstellationIsolateSelected() {
        return asterisms.getFlagIsolateSelected();
    }

    /**
     * Get constellation line color
     */
    public Color getColorConstellationLine() {
        return asterisms.getLineColor();
    }

    /**
     * Set constellation line color
     */
    public void setColorConstellationLine(Color v) {
        asterisms.setLineColor(v);
    }

    /**
     * Get constellation names color
     */
    public Color getColorConstellationNames() {
        return asterisms.getLabelColor();
    }

    /**
     * Set constellation names color
     */
    public void setColorConstellationNames(Color v) {
        asterisms.setLabelColor(v);
    }

    /**
     * Set flag for displaying Planets
     */
    public void setPlanets(boolean b) {
        solarSystem.setFlagPlanets(b);
    }

    /**
     * Get flag for displaying Planets
     */
    public boolean isPlanetsEnabled() {
        return solarSystem.getFlagPlanets();
    }

    /**
     * Set flag for displaying Planets Trails
     */
    public void setPlanetsTrails(boolean b) {
        solarSystem.setFlagTrails(b);
    }

    /**
     * Get flag for displaying Planets Trails
     */
    public boolean getFlagPlanetsTrails() {
        return solarSystem.getFlagTrails();
    }

    /**
     * Set flag for displaying Planets Hints
     */
    public void setPlanetsHints(boolean b) {
        solarSystem.setFlagHints(b);
    }

    /**
     * Get flag for displaying Planets Hints
     */
    public boolean isPlanetsHintsEnabled() {
        return solarSystem.getFlagHints();
    }

    /**
     * Set flag for displaying Planets Orbits
     */
    public void setPlanetsOrbits(boolean b) {
        solarSystem.setFlagOrbits(b);
    }

    /**
     * Get flag for displaying Planets Orbits
     */
    public boolean getFlagPlanetsOrbits() {
        return solarSystem.getFlagOrbits();
    }

    public void setLightTravelTime(boolean b) {
        solarSystem.setFlagLightTravelTime(b);
    }

    public boolean getFlagLightTravelTime() {
        return solarSystem.getFlagLightTravelTime();
    }

    public Color getColorPlanetsOrbits() {
        return solarSystem.getOrbitColor();
    }

    public Color getColorPlanetsNames() {
        return solarSystem.getLabelColor();
    }

    /**
     * Start/stop displaying planets Trails
     */
    public void startPlanetsTrails(boolean b) {
        solarSystem.startTrails(b);
    }

    public Color getColorPlanetsTrails() {
        return solarSystem.getTrailColor();
    }

    /**
     * Set base planets display scaling factor
     */
    public void setPlanetsScale(double f) {
        solarSystem.setScale(f);
    }

    /**
     * Get base planets display scaling factor
     */
    public double getPlanetsScale() {
        return solarSystem.getScale();
    }

    /** Set selected planets by englishName*/
    /**
     * @param englishName The planet name or "" to select no planet
     */
    public void setPlanetsSelected(String englishName) {
        solarSystem.setSelected(englishName);
    }

    /**
     * Set flag for displaying a scaled Moon
     */
    public void setMoonScaled(boolean b) {
        solarSystem.setFlagMoonScale(b);
    }

    /**
     * Get flag for displaying a scaled Moon
     */
    public boolean isMoonScaled() {
        return solarSystem.getFlagMoonScale();
    }

    /**
     * Set Moon scale
     */
    public void setMoonScale(float f) {
        if (f < 0) solarSystem.setMoonScale(1.f);
        else solarSystem.setMoonScale(f);
    }

    /**
     * Get Moon scale
     */
    public float getMoonScale() {
        return solarSystem.getMoonScale();
    }

    /**
     * Set flag for displaying Azimutal Grid
     */
    public void setAzimutalGrid(boolean b) {
        azimutalGrid.show(b);
    }

    /**
     * Get flag for displaying Azimutal Grid
     */
    public boolean isAzimutalGridEnabled() {
        return azimutalGrid.isShown();
    }

    public Color getColorAzimutalGrid() {
        return azimutalGrid.getColor();
    }

    /**
     * Set flag for displaying Equatorial Grid
     */
    public void setEquatorGrid(boolean b) {
        equatorialGrid.show(b);
    }

    /**
     * Get flag for displaying Equatorial Grid
     */
    public boolean isEquatorGridEnabled() {
        return equatorialGrid.isShown();
    }

    public Color getColorEquatorGrid() {
        return equatorialGrid.getColor();
    }

    /**
     * Set flag for displaying Equatorial Line
     */
    public void setEquatorLine(boolean b) {
        equatorLine.setFlagshow(b);
    }

    /**
     * Get flag for displaying Equatorial Line
     */
    public boolean isEquatorLineEnabled() {
        return equatorLine.getFlagshow();
    }

    public Color getColorEquatorLine() {
        return equatorLine.getColor();
    }

    /**
     * Set flag for displaying Ecliptic Line
     */
    public void setEclipticLine(boolean b) {
        eclipticLine.setFlagshow(b);
    }

    /**
     * Get flag for displaying Ecliptic Line
     */
    public boolean isEclipticLineEnabled() {
        return eclipticLine.getFlagshow();
    }

    public Color getColorEclipticLine() {
        return eclipticLine.getColor();
    }


    /**
     * Set flag for displaying Meridian Line
     */
    public void setMeridianLine(boolean b) {
        meridianLine.setFlagshow(b);
    }

    /**
     * Get flag for displaying Meridian Line
     */
    public boolean getFlagMeridianLine() {
        return meridianLine.getFlagshow();
    }

    public Color getColorMeridianLine() {
        return meridianLine.getColor();
    }

    /**
     * Set flag for displaying Cardinals Points
     */
    public void setCardinalsPointsEnabled(boolean b) {
        cardinalsPoints.setFlagShow(b);
    }

    /**
     * Get flag for displaying Cardinals Points
     */
    public boolean isCardinalsPointsEnabled() {
        return cardinalsPoints.getFlagShow();
    }

    /**
     * Set Cardinals Points color
     */
    public void setColorCardinalPoints(Color v) {
        cardinalsPoints.setColor(v);
    }

    /**
     * Get Cardinals Points color
     */
    public Color getColorCardinalPoints() {
        return cardinalsPoints.getColor();
    }

    public void setColorConstellationBoundaries(Color v) {
        asterisms.setBoundaryColor(v);
    }

    public void setColorPlanetsOrbits(Color v) {
        solarSystem.setOrbitColor(v);
    }

    public void setColorPlanetsNames(Color v) {
        solarSystem.setLabelColor(v);
    }

    public void setColorPlanetsTrails(Color v) {
        solarSystem.setTrailColor(v);
    }

    public void setColorAzimutalGrid(Color v) {
        azimutalGrid.setColor(v);
    }

    public void setColorEquatorGrid(Color v) {
        equatorialGrid.setColor(v);
    }

    public void setColorEquatorLine(Color v) {
        equatorLine.setColor(v);
    }

    public void setColorEclipticLine(Color v) {
        eclipticLine.setColor(v);
    }

    public void setColorMeridianLine(Color v) {
        meridianLine.setColor(v);
    }

    public void setColorNebulaLabels(Color v) {
        nebulas.setLabelColor(v);
    }

    public void setColorNebulaCircle(Color v) {
        nebulas.setCircleColor(v);
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    // Projection

    /**
     * Set the horizontal viewport offset in pixels
     */
    public void setViewportHorizontalOffset(int hoff) {
        projection.setViewportPosX(hoff);
    }

    /**
     * Get the horizontal viewport offset in pixels
     */
    public int getViewportHorizontalOffset() {
        return projection.getViewportPosX();
    }

    /**
     * Set the vertical viewport offset in pixels
     */
    public void setViewportVerticalOffset(int voff) {
        projection.setViewportPosY(voff);
    }

    /**
     * Get the vertical viewport offset in pixels
     */
    public int getViewportVerticalOffset() {
        return projection.getViewportPosY();
    }

    /**
     * Maximize viewport according to passed screen values
     */
    public void setMaximizedViewport(int screenW, int screenH) {
        projection.setViewport(0, 0, screenW, screenH);
    }

    /**
     * Set a centered squared viewport with passed vertical and horizontal offset
     */
    public void setSquareViewport(int screenW, int screenH, int hoffset, int voffset) {
        int m = min(screenW, screenH);
        projection.setViewport((screenW - m) / 2 + hoffset, (screenH - m) / 2 + voffset, m, m);
    }

    /**
     * Set whether a disk mask must be drawn over the viewport
     */
    public void setViewportMaskDisk() {
        projection.setMaskType(DefaultProjector.PROJECTOR_MASK_TYPE.DISK);
    }

    /**
     * Get whether a disk mask must be drawn over the viewport
     */
    public boolean getViewportMaskDisk() {
        return projection.getMaskType() == DefaultProjector.PROJECTOR_MASK_TYPE.DISK;
    }

    /**
     * Set whether no mask must be drawn over the viewport
     */
    public void setViewportMaskNone() {
        projection.setMaskType(DefaultProjector.PROJECTOR_MASK_TYPE.NONE);
    }

    /**
     * Get the projection type
     */
    public Projector.TYPE getProjectionType() throws StellariumException {
        return projection.getType();
    }

    /**
     * get/set horizontal/vertical image flip
     */
    public boolean getFlipHorz() {
        return projection.getFlipHorz();
    }

    public boolean getFlipVert() {
        return projection.getFlipVert();
    }

    /**
     * Set flag for enabling gravity labels
     */
    public void setGravityLabels(boolean b) {
        projection.setFlagGravityLabels(b);
    }

    /**
     * Get flag for enabling gravity labels
     */
    public boolean getFlagGravityLabels() {
        return projection.isGravityLabelsEnabled();
    }

    /**
     * Get viewport width
     */
    public int getViewportWidth() {
        return projection.getViewportWidth();
    }

    /**
     * Get viewport height
     *
     * @return
     */
    public int getViewportHeight() {
        return projection.getViewportHeight();
    }

    /**
     * Get viewport X position
     */
    public int getViewportPosX() {
        return projection.getViewportPosX();
    }

    /**
     * Get viewport Y position
     */
    public int getViewportPosY() {
        return projection.getViewportPosY();
    }

    /**
     * Print the passed String so that it is oriented in the drection of the gravity
     */
    public void printGravity(SFontIfc font, float x, float y, String str) {
        printGravity(font, x, y, str, true, 0f, 0f);
    }

    public void printGravity(SFontIfc font, float x, float y, String str, boolean speedOptimize,
                             float xshift, float yshift) {
        projection.printGravity180(font, x, y, str, speedOptimize, xshift, yshift);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Landscape

    /**
     * Set flag for displaying Landscape
     */
    public void setLandscapeEnabled(boolean b) {
        landscape.setVisible(b);
        landscape.setFlagShowFog(b);
    }

    /**
     * Get flag for displaying Landscape
     */
    public boolean isLandscapeEnabled() {
        return landscape.isVisible();
    }

    /**
     * Set flag for displaying Fog
     */
    public void setFlagFog(boolean b) {
        landscape.setFlagShowFog(b);
    }

    /**
     * Get flag for displaying Fog
     */
    public boolean isFogEnabled() {
        return landscape.getFlagShowFog();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Atmosphere

    /**
     * Set flag for displaying Atmosphere
     */
    public void setAtmosphere(boolean b) {
        atmosphere.setVisible(b);
    }

    /**
     * Get flag for displaying Atmosphere
     */
    public boolean isAtmosphereEnabled() {
        return atmosphere.isVisible();
    }

    /**
     * Set atmosphere fade duration in s
     */
    public void setAtmosphereFadeDuration(float f) {
        atmosphere.setFadeDuration(f);
    }

    /**
     * Get atmosphere fade duration in s
     */
    public float getAtmosphereFadeDuration() {
        return atmosphere.getFadeDuration();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Milky Way

    /**
     * Set flag for displaying Milky Way
     */
    public void setMilkyWay(boolean b) {
        milkyWay.setFlagShow(b);
    }

    /**
     * Get flag for displaying Milky Way
     */
    public boolean getFlagMilkyWay() {
        return milkyWay.getFlagShow();
    }

    /**
     * Set Milky Way intensity
     */
    public void setMilkyWayIntensity(float f) {
        milkyWay.setIntensity(f);
    }

    /**
     * Get Milky Way intensity
     */
    public float getMilkyWayIntensity() {
        return milkyWay.getIntensity();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Nebulae

    /**
     * Set flag for displaying Nebulae
     */
    public void setNebula(boolean b) {
        nebulas.setFlagShow(b);
    }

    /**
     * Get flag for displaying Nebulae
     */
    public boolean getFlagNebula() {
        return nebulas.getFlagShow();
    }

    /**
     * Set flag for displaying Nebulae Hints
     */
    public void setNebulaHints(boolean b) {
        nebulas.setFlagHints(b);
    }

    /**
     * Get flag for displaying Nebulae Hints
     */
    public boolean isNebulaHintEnabled() {
        return nebulas.getFlagHints();
    }

    /**
     * Set Nebulae Hints circle scale
     */
    public void setNebulaCircleScale(float f) {
        nebulas.setNebulaCircleScale(f);
    }

    /**
     * Get Nebulae Hints circle scale
     */
    public double getNebulaCircleScale() {
        return nebulas.getNebulaCircleScale();
    }

    /**
     * Set flag for displaying Nebulae as bright
     */
    public void setBrightNebulae(boolean b) {
        nebulas.setFlagBright(b);
    }

    /**
     * Get flag for displaying Nebulae as brigth
     */
    public boolean getFlagBrightNebulae() {
        return nebulas.getFlagBright();
    }

    /**
     * Set maximum magnitude at which nebulae hints are displayed
     */
    public void setNebulaMaxMagHints(float f) {
        nebulas.setMaxMagHints(f);
    }

    /**
     * Get maximum magnitude at which nebulae hints are displayed
     */
    public float getNebulaMaxMagHints() {
        return nebulas.getMaxMagHints();
    }

    /**
     * Set flag for displaying Nebulae even without textures
     */
    public void setFlagNebulaDisplayNoTexture(boolean b) {
        nebulas.setFlagDisplayNoTexture(b);
    }

    /**
     * Get flag for displaying Nebulae without textures
     */
    public boolean isNebulaDisplayNoTexture() {
        return nebulas.getFlagDisplayNoTexture();
    }

    public Color getColorNebulaLabels() {
        return nebulas.getLabelColor();
    }

    public Color getColorNebulaCircle() {
        return nebulas.getCircleColor();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Observator

    /**
     * Return the current observatory (as a  object)
     */
    public Observator getObservatory() {
        return observatory;
    }

    /**
     * Move to a new latitude and longitude on home planet
     */
    public void moveObserver(double lat, double lon, double alt, int delay, String name) {
        observatory.moveTo(lat, lon, alt, delay, name);
    }

    /**
     * Set Meteor Rate in number per hour
     */
    public void setMeteorsRate(int f) {
        meteors.setZHR(f);
    }

    /**
     * Get Meteor Rate in number per hour
     */
    public int getMeteorsRate() {
        return meteors.getZHR();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Others

    /**
     * Return the current image manager which display users images
     */
    public ImageMgr getImageMgr() {
        return scriptImages;
    }

    public double getZoomSpeed() {
        return zoomSpeed;
    }

    public String getLandscapeName() {
        return landscape.getName();
    }

    public String getLandscapeAuthorName() {
        return landscape.getAuthorName();
    }

    public String getLandscapeDescription() {
        return landscape.getDescription();
    }

    // END OF stel_core.h public methods implemented

    // TODO!

    void loadObservatory() {

    }

    public Navigator getNavigation() {
        return navigation;
    }

    public DefaultProjector getProjection() {
        return projection;
    }

    public StelObject getSelectedObject() {
        return selectedObject;
    }

// ---------------------------------------------------------------
    // Interfaces for external controls (gui, tui or script facility)
    // Only the function listed here should be called by them
    // ---------------------------------------------------------------

    public void zoomTo(double aimFOV, double moveDuration) {
        projection.zoomTo(aimFOV, moveDuration);
    }

    // Members from private section of stel_core.h

    private final ApplicationCallback applicationCallback;

    /**
     * The font file used by default during initialization
     */
    String baseFontName;

    /**
     * The directory containing data for the culture used for constellations, etc.
     */
    private String skyCultureDir = null;

    /**
     * The translator used for astronomical object naming
     */
    private Translator skyTranslator;


    // Main elements of the program
    /**
     * Manage all navigation parameters, coordinate transformations etc..
     */
    private Navigator navigation;

    /**
     * Manage observer position and locales for its country
     */
    private Observator observatory;

    /**
     * Manage the projection mode and matrix
     */
    private DefaultProjector projection;

    /**
     * The selected object in stellarium
     */
    private StelObject selectedObject;

    /**
     * Manage the Hipparcos stars
     */
    HipStarMgr stars;

    /**
     * Manage constellations (boundaries, names etc..)
     */
    private ConstellationMgr asterisms;

    /**
     * Manage the nebulas
     */
    NebulaMgr nebulas;

    /**
     * Manage the solar system
     */
    public SolarSystem solarSystem;

    /**
     * Atmosphere
     */
    StelAtmosphere atmosphere = new StelAtmosphere();

    /**
     * Equatorial grid
     */
    public SkyGrid equatorialGrid = new EquatorialSkyGrid();

    /**
     * Azimutal grid
     */
    public SkyGrid azimutalGrid = new AzimutalSkyGrid();

    /**
     * Celestial Equator line
     */
    public SkyLine equatorLine;

    /**
     * Eclptic line
     */
    public SkyLine eclipticLine;

    /**
     * Meridian line
     */
    public SkyLine meridianLine;

    /**
     * Cardinals points
     */
    public Cardinals cardinalsPoints = new Cardinals();

    /**
     * Our galaxy
     */
    public MilkyWay milkyWay;

    /**
     * Manage meteor showers
     */
    MeteorMgr meteors;

    /**
     * The landscape ie the fog, the ground and "decor"
     */
    Landscape landscape;

    /**
     * Tones conversion between stellarium world and display device
     */
    private ToneReproductor toneConverter = new ToneReproductor();

    /**
     * For sky cultures and locales
     */
    private SkyLocalizer skyLocalizer = new SkyLocalizer();

    /**
     * for script loaded image display
     */
    ImageMgr scriptImages;

    private PointerDisplayer pointer;

    public PointerDisplayer getPointer() {
        return pointer;
    }

    public synchronized void startPointer(DefaultProjector projection, Navigator navigation) {
        stopPointer();
        StelObject selectedObject = getSelectedObject();
        pointer = new PointerDisplayer(selectedObject, projection, navigation);
    }

    public void stopPointer() {
        if (pointer != null) {
            pointer = null;
        }
    }

    private class PointerDisplayer {
        private StelObject selectedObject;
        private DefaultProjector prj;
        private Navigator nav;
        private long localTime;

        /**
         * Pointer refresh rate, in milliseconds.
         */
        public PointerDisplayer(StelObject someObject, DefaultProjector projection, Navigator navigation) {
            selectedObject = someObject;
            prj = projection;
            nav = navigation;
        }

        /**
         * Draw a nice animated pointer around the object
         *
         * @param deltaTime The delta time
         */
        public void draw(long deltaTime) {
            localTime += deltaTime;
            selectedObject.drawPointer(nav, prj, localTime);
        }
    }

    /**
     * For managing connected telescopes
     */
    private TelescopeMgr telescopeMgr;

    /**
     * Current sky Brightness in ?
     */
    float skyBrightness;

    /**
     * Should selected object pointer be drawn
     */
    private boolean objectPointer = true;   // Allow object selection by default

    boolean flagEnableZoomKeys;

    boolean flagEnableMoveKeys;

    /**
     * View movement
     */
    double deltaFov, deltaAlt, azimuthDelta;

    /**
     * Speed of movement and zooming
     */
    double moveSpeed = 0.00025f, zoomSpeed;

    /**
     * Default viewing FOV
     */
    float initFov;

    /**
     * Default viewing direction
     */
    Point3d initViewPos;

    /**
     * Define whether auto zoom can go further
     */
    private boolean flagManualZoom;

    /**
     * ?
     */
    Color chartColor;

    /**
     * Duration of movement for the auto move to a selected object
     */
    private float autoMoveDuration;

    /**
     * For init to track if reload or first time setup
     */
    public boolean firstTime = true;

    /**
     * size for constellation labels
     */
    int constellationFontSize;
}
