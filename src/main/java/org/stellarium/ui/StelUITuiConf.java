/*
 * Stellarium
 * Copyright (C) 2002 Fabien Chéreau
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
package org.stellarium.ui;

import org.stellarium.*;
import org.stellarium.data.IniFileParser;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.landscape.Landscape;
import org.stellarium.projector.Projector;
import org.stellarium.ui.render.SFontIfc;

import javax.media.opengl.GL;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * Text Configuration UI.
 *
 * @see <a href="http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/stel_ui_conf.cpp">C++ version of this file</>.
 */
public class StelUITuiConf {

    // 1. Location
    private STUI.DecimalItem tuiLocationLatitude;

    private STUI.DecimalItem tuiLocationLongitude;

    private STUI.IntegerItem tuiLocationAltitude;

    // 2. Time & Date
    private STUI.TimeItem tuiTimeSkyTime;

    private STUI.TimeZoneItem tuiTimeSetTmz;

    private STUI.TimeItem tuiTimePresetSkyTime;

    private STUI.MultiSetItem<String> tuiTimeStartupTime;

    private STUI.MultiSetItem<String> tuiTimeDisplayFormat;

    private STUI.MultiSetItem<String> tuiDateDisplayFormat;

    // 3. General
    private STUI.MultiSetItem<String> tuiGeneralSkyCulture;

    private STUI.MultiSetItem<String> tuiGeneralSkyLocale;

    // 4. Stars
    private STUI.BooleanItem tuiStarsShow;

    private STUI.DecimalItem tuiStarMagScale;

    private STUI.DecimalItem tuiStarLabelMaxMag;

    private STUI.DecimalItem tuiStarsTwinkle;

    // 5. Effects
    private STUI.MultiSetItem<String> tuiEffectLandscape;

    private STUI.BooleanItem tuiEffectPointObj;

    private STUI.DecimalItem tuiEffectZoomDuration;

    private STUI.BooleanItem tuiEffectManualZoom;

    // 6. Scripts
    private STUI.MultiSetItem<String> tuiScriptsLocal;

    private STUI.MultiSetItem<String> tuiScriptsRemoveable;

    // 7. Administration
    private STUI.ActionConfirmItem tuiAdminLoadDefault;

    private STUI.ActionConfirmItem tuiAdminSaveDefault;

    //MultiSetItem<String>* tui_admin_setlocal;
    private STUI.ActionItem tuiAdminUpdateMe;

    private STUI.IntegerItem tuiAdminVOffset;

    private STUI.IntegerItem tuiAdminHOffset;

    private static final String TUI_SCRIPT_MSG = "TUI_SCRIPT_MSG";

    private static final String SCRIPT_REMOVEABLE_DISK = "SCRIPT_REMOVEABLE_DISK";

    private boolean scriptDirectoryRead;

    private STUI.VectorItem tuiColorsConstLineColor;

    private STUI.VectorItem tuiColorsConstLabelColor;

    private STUI.VectorItem tuiColorscardinalColor;

    private STUI.VectorItem tuiColorsConstNoundaryColor;

    private STUI.VectorItem tuiColorsPlanetNamesColor;

    private STUI.VectorItem tuiColorsPlanetOrbitsColor;

    private STUI.VectorItem tuiColorsObjectTrailsColor;

    private STUI.VectorItem tuiColorsMeridianColor;

    private STUI.VectorItem tuiColorsAzimuthalColor;

    private STUI.VectorItem tuiColorsEquatorialColor;

    private STUI.VectorItem tuiColorsEquatorColor;

    private STUI.VectorItem tuiColorsEclipticColor;

    private STUI.VectorItem tuiColorsNebulaLabelColor;

    private STUI.VectorItem tuiColorsNebulaCircleColor;

    private STUI.DecimalItem tuiColorsConstArtIntensity;

    private STUI.DecimalItem tuiEffectObjectScale;

    private STUI.DecimalItem tuiEffectMilkywayIntensity;

    private STUI.DecimalItem tuiEffectCursorTimeout;

    private STUI.DecimalItem tuiEffectNebulaeLabelMagnitude;

    private STUI.MultiSetItem<String> tuiAdminSetLocale;

    private STUI.MultiSet2Item<String> tuiLocationPlanet;

    private STUI.MultiSetItem<String> tuiTimeDateFormat;

    private StelCore core;
    private StelApp app;
    private Logger logger;
    private StelUI ui;

    public StelUITuiConf(StelUI ui) throws StellariumException {
        logger = Logger.getLogger(getClass().getName());
        if (ui.logger != null) {
            logger.setParent(ui.logger);
        }

        this.ui = ui;
        core = ui.core;
        app = ui.app;

        IniFileParser conf = app.getConf();
        showTuiDateTime = conf.getBoolean(IniFileParser.TEXT_UI_SECTION, "flag_show_tui_datetime");
        showTuiShortObjInfo = conf.getBoolean(IniFileParser.TEXT_UI_SECTION, "flag_show_tui_short_obj_info");
    }

    public void drawGravityUi() throws StellariumException {
        // Normal transparency mode
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);

        Projector projection = core.getProjection();
        int x = core.getViewportPosX() + core.getViewportWidth() / 2;
        int y = core.getViewportPosY() + core.getViewportHeight() / 2;
        int shift = Math.min(core.getViewportWidth() / 2, core.getViewportHeight() / 2);

        if (showTuiDateTime) {
            double julianDay = core.getJulianDay();
            String os = app.getPrintableDateLocal(julianDay) + " " + app.getPrintableTimeLocal(julianDay);

            // label location if not on earth
            if (!core.getObservatory().getHomePlanetEnglishName().equals("Earth")) {
                os += " " + core.getObservatory().getHomePlanetEnglishName();
            }

            if (ui.showFieldOfView) {
                os += " fov " + core.getFieldOfView();
            }
            if (ui.showFramesPerSecond) {
                os += "  FPS " + app.getFramesPerSecond();
            }

            glColor3f(0.5f, 1f, 0.5f);
            projection.printGravity180(tuiFont, x - shift + 38, y - shift + 38, os, false, 0, 0);
        }

        if (core.getFlagHasSelected() && showTuiShortObjInfo) {
            String info = core.getSelectedObjectShortInfo();
            glColor3fv(core.getSelectedObjectInfoColor().getComponents(null), 0);
            core.printGravity(tuiFont, x + shift - 38, y + shift - 38, info, false, 0, 0);
        }
    }

    /**
     * Create all the components of the text user interface.
     * <p/>
     * Should be safe to call more than once but not recommended
     * since lose states - try localizeTui() instead
     *
     * @throws org.stellarium.StellariumException
     *
     */
    void initTui() throws StellariumException {
        // Menu root branch
        scriptDirectoryRead = false;

        // If already initialized before, delete existing objects
        tuiRoot = null;

        // Load standard font based on app locale
//        StelCore.FontParam font = new StelCore.FontParam();
//        StelCore.FontParam tmp = new StelCore.FontParam();

//        core.getFontForLocale(app.getAppLanguage(), font, tmp);

        tuiFont = ui.getFontFactory().create(ui.baseFontSize, null);

        // Menu root branch
        tuiRoot = new STUI.Branch();

        // Submenus
        STUI.MenuBranch tuiMenuLocation = new STUI.MenuBranch("1. Set Location ");
        STUI.MenuBranch tuiMenuTime = new STUI.MenuBranch("2. Set Time ");
        STUI.MenuBranch tuiMenuGeneral = new STUI.MenuBranch("3. General ");
        STUI.MenuBranch tuiMenuStars = new STUI.MenuBranch("4. Stars ");
        STUI.MenuBranch tuiMenuEffects = new STUI.MenuBranch("5. Effects ");
        STUI.MenuBranch tuiMenuScripts = new STUI.MenuBranch("6. Scripts ");
        STUI.MenuBranch tuiMenuAdministration = new STUI.MenuBranch("7. Administration ");

        tuiRoot.addComponent(tuiMenuLocation);
        tuiRoot.addComponent(tuiMenuTime);
        tuiRoot.addComponent(tuiMenuGeneral);
        tuiRoot.addComponent(tuiMenuStars);
        tuiRoot.addComponent(tuiMenuEffects);
        tuiRoot.addComponent(tuiMenuScripts);
        tuiRoot.addComponent(tuiMenuAdministration);

        // 1. Location
        tuiLocationLatitude = new STUI.DecimalItem(-90, 90, 0, "1.1 Latitude: ");
        STUI.Callback cblCallback = new STUI.Callback() {
            public void execute() {
                tuiCb1();
            }
        };
        tuiLocationLatitude.setOnChangeCallback(cblCallback);
        tuiLocationLongitude = new STUI.DecimalItem(-180, 180, 0, "1.2 Longitude: ");
        tuiLocationLongitude.setOnChangeCallback(cblCallback);
        tuiLocationAltitude = new STUI.IntegerItem(-500, 10000, 0, "1.3 Altitude (m): ");
        tuiLocationAltitude.setOnChangeCallback(cblCallback);

        // Home planet only changed if hit enter to accept because
        // switching planet instantaneously as select is hard on a planetarium audience
        tuiLocationPlanet = new STUI.MultiSet2Item<String>("1.4 ");
        tuiLocationPlanet.addItemList(core.getPlanetHashString());
        //	tui_location_planet->set_OnChangeCallback(callback<void>(this, &StelUI::tuiCbLocationChangePlanet));
        tuiLocationPlanet.set_OnTriggerCallback(new STUI.Callback() {
            public void execute() throws StellariumException {
                tuiCbLocationChangePlanet();
            }
        });

        tuiMenuLocation.addComponent(tuiLocationLatitude);
        tuiMenuLocation.addComponent(tuiLocationLongitude);
        tuiMenuLocation.addComponent(tuiLocationAltitude);
        tuiMenuLocation.addComponent(tuiLocationPlanet);

        // 2. Time
        tuiTimeSkyTime = new STUI.TimeItem("2.1 Sky Time: ");
        tuiTimeSkyTime.setOnChangeCallback(cblCallback);
        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        tuiTimeSetTmz = new STUI.TimeZoneItem(locatorUtil.getDataFile("zone.tab"), "2.2 Set Time Zone: ");
        tuiTimeSetTmz.setOnChangeCallback(new STUI.Callback() {
            public void execute() {
                tuiCbSetTimeZone();
            }
        });
        tuiTimeSetTmz.settz(app.getCustomTzName());
        tuiTimePresetSkyTime = new STUI.TimeItem("2.3 Preset Sky Time: ");
        tuiTimePresetSkyTime.setOnChangeCallback(cblCallback);
        tuiTimeStartupTime = new STUI.MultiSetItem<String>("2.4 Sky Time At Start-up: ");
        tuiTimeStartupTime.addItem("Actual");
        tuiTimeStartupTime.addItem("Preset");
        tuiTimeStartupTime.setOnChangeCallback(cblCallback);
        tuiTimeDisplayFormat = new STUI.MultiSetItem<String>("2.5 Time Display Format: ");
        tuiTimeDisplayFormat.addItem("24h");
        tuiTimeDisplayFormat.addItem("12h");
        tuiTimeDisplayFormat.addItem("system_default");
        tuiTimeDisplayFormat.setOnChangeCallback(new STUI.Callback() {
            public void execute() {
                tuiCbSetTimeDisplayFormat();
            }
        });

        tuiMenuTime.addComponent(tuiTimeSkyTime);
        tuiMenuTime.addComponent(tuiTimeSetTmz);
        tuiMenuTime.addComponent(tuiTimePresetSkyTime);
        tuiMenuTime.addComponent(tuiTimeStartupTime);
        tuiMenuTime.addComponent(tuiTimeDisplayFormat);

        // 3. General settings

        // sky culture goes here
        tuiGeneralSkyCulture = new STUI.MultiSetItem("3.1 Sky Culture: ");
        for (String skyCulture : core.getSkyCultureList()) {
            // human readable names
            tuiGeneralSkyCulture.addItem(skyCulture);
        }
        tuiGeneralSkyCulture.setOnChangeCallback(new STUI.Callback() {
            public void execute() {
                tuiCbTuiGeneralChangeSkyCulture();
            }
        });
        tuiMenuGeneral.addComponent(tuiGeneralSkyCulture);

        tuiGeneralSkyLocale = new STUI.MultiSetItem<String>("3.2 Sky Language: ");
        for (String language : Translator.getNamesOfAvailableLanguages()) {
            // human readable names
            tuiGeneralSkyLocale.addItemList(language);
        }

        tuiGeneralSkyLocale.setOnChangeCallback(new STUI.Callback() {
            public void execute() throws StellariumException {
                tuiCbTuiGeneralChangeSkyLocale();
            }
        });
        tuiMenuGeneral.addComponent(tuiGeneralSkyLocale);

        // 4. Stars
        tuiStarsShow = new STUI.BooleanItem(false, "4.1 Show: ", "Yes", "No");
        tuiStarsShow.setOnChangeCallback(cblCallback);
        tuiStarMagScale = new STUI.DecimalItem(1, 30, 1, "4.2 Star Magnitude Multiplier: ");
        tuiStarMagScale.setOnChangeCallback(cblCallback);
        tuiStarLabelMaxMag = new STUI.DecimalItem(-1.5, 10, 2, "4.3 Maximum Magnitude to Label: ");
        tuiStarLabelMaxMag.setOnChangeCallback(cblCallback);
        tuiStarsTwinkle = new STUI.DecimalItem(0., 1., 0.3, "4.4 Twinkling: ", 0.1);
        tuiStarsTwinkle.setOnChangeCallback(cblCallback);

        tuiMenuStars.addComponent(tuiStarsShow);
        tuiMenuStars.addComponent(tuiStarMagScale);
        tuiMenuStars.addComponent(tuiStarLabelMaxMag);
        tuiMenuStars.addComponent(tuiStarsTwinkle);

        // 5. Effects
        tuiEffectLandscape = new STUI.MultiSetItem<String>("5.1 Landscape: ");
        tuiEffectLandscape.addItemList(Landscape.getFileContent(locatorUtil.getDataFile("landscapes.ini")));
        tuiEffectLandscape.setOnChangeCallback(new STUI.Callback() {
            public void execute() throws StellariumException {
                tuiCbTuiEffectChangeLandscape();
            }
        });
        tuiMenuEffects.addComponent(tuiEffectLandscape);

        tuiEffectPointObj = new STUI.BooleanItem(false, "5.2 Object Sizing Rule: ", "Point", "Magnitude");
        tuiEffectPointObj.setOnChangeCallback(cblCallback);
        tuiMenuEffects.addComponent(tuiEffectPointObj);

        tuiEffectZoomDuration = new STUI.DecimalItem(1, 10, 2, "5.3 Zoom duration: ");
        tuiEffectZoomDuration.setOnChangeCallback(cblCallback);
        tuiMenuEffects.addComponent(tuiEffectZoomDuration);

        tuiEffectManualZoom = new STUI.BooleanItem(false, "5.4 Manual zoom: ", "Yes", "No");
        tuiEffectManualZoom.setOnChangeCallback(cblCallback);
        tuiMenuEffects.addComponent(tuiEffectManualZoom);

        // 6. Scripts
        tuiScriptsLocal = new STUI.MultiSetItem<String>("6.1 Local Script: ");
        tuiScriptsLocal.addItemList(TUI_SCRIPT_MSG + "\n" + app.getScripts().getScriptList(locatorUtil.getScriptsDir().getPath()));
        tuiScriptsLocal.setOnChangeCallback(new STUI.Callback() {
            public void execute() {
                tuiCbScriptsLocal();
            }
        });
        tuiMenuScripts.addComponent(tuiScriptsLocal);

        tuiScriptsRemoveable = new STUI.MultiSetItem<String>("6.2 CD/DVD Script: ");
        //	tuiScriptsRemoveable.addItem("Arrow down to load list.");
        tuiScriptsRemoveable.addItem(TUI_SCRIPT_MSG);
        tuiScriptsRemoveable.setOnChangeCallback(new STUI.Callback() {
            public void execute() {
                tuiCbScriptsRemoveable();
            }
        });
        tuiMenuScripts.addComponent(tuiScriptsRemoveable);

        // 7. Administration
        tuiAdminLoadDefault = new STUI.ActionConfirmItem("7.1 Load Default Configuration: ");
        tuiAdminLoadDefault.setOnChangeCallback(new STUI.Callback() {
            public void execute() throws StellariumException {
                tuiCbAdminLoadDefault();
            }
        });
        tuiAdminSaveDefault = new STUI.ActionConfirmItem("7.2 Save Current Configuration as Default: ");
        tuiAdminSaveDefault.setOnChangeCallback(new STUI.Callback() {
            public void execute() throws StellariumException {
                tuiCbAdminSaveDefault();
            }
        });
        /*tui_admin_setlocal = new MultiSetItem<String>("7.3 Set Locale: ");
        tui_admin_setlocal.addItem("fr_FR");
        tui_admin_setlocal.addItem("en_EN");
        tui_admin_setlocal.addItem("en_US");
        tui_admin_setlocal.set_OnChangeCallback(Callback(this, &StelUI::tui_cb_admin_set_locale));*/
        tuiAdminUpdateMe = new STUI.ActionItem("7.3 Update me via Internet: ");
        tuiAdminUpdateMe.setOnChangeCallback(new STUI.Callback() {
            public void execute() throws StellariumException {
                tuiCbAdminUpdateMe();
            }
        });
        tuiMenuAdministration.addComponent(tuiAdminLoadDefault);
        tuiMenuAdministration.addComponent(tuiAdminSaveDefault);
        //tui_menu_administration.addComponent(tui_admin_setlocal);
        tuiMenuAdministration.addComponent(tuiAdminUpdateMe);

        tuiAdminVOffset = new STUI.IntegerItem(-10, 10, 0, "7.4 N-S Centering Offset: ");
        /*
        tuiAdminVOffset.setOnChangeCallback(new STUI.Callback() {
            public void execute() {
                tuiCbTuiAdminChangeViewport();
            }
        });
        */
        tuiMenuAdministration.addComponent(tuiAdminVOffset);

        tuiAdminHOffset = new STUI.IntegerItem(-10, 10, 0, "7.5 E-W Centering Offset: ");
        /*
        tuiAdminHOffset.setOnChangeCallback(new STUI.Callback() {
            public void execute() {
                tuiCbTuiAdminChangeViewport();
            }
        });
        */
        tuiMenuAdministration.addComponent(tuiAdminHOffset);
    }

    /**
     * Display the Text UI
     */
    public void drawTUI() {
        // Normal transparency mode
        glBlendFunc(GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);

        int x = core.getViewportPosX() + core.getViewportWidth() / 2;
        int y = core.getViewportPosY() + core.getViewportHeight() / 2;
        int shift = (int) (Math.sqrt(2) / 2 * Math.min(core.getViewportWidth() / 2, core.getViewportHeight() / 2));

        if (!core.getFlagGravityLabels()) {
            // for horizontal tui move to left edge of screen kludge
            shift = 0;
            x = core.getViewportPosX() + (int) (0.1 * core.getViewportWidth());
            y = core.getViewportPosY() + (int) (0.1 * core.getViewportHeight());
        }

        if (tuiRoot != null) {
            glColor3f(0.5f, 1, 0.5f);
            core.printGravity(tuiFont, x + shift - 30, y - shift + 38,
                    STUI.STOP_ACTIVE + tuiRoot.getString(), false, 0, 0);
        }
    }

//    public void keyPressed(KeyEvent e) {
//        StelUI.tuiRoot.keyPressed(e);
//    }

    /**
     * Update all the core parameters with values taken from the tui widgets
     */
    void tuiCb1() {
        // 2. Date & Time
        app.setPresetSkyTime(tuiTimePresetSkyTime.getJDay());
        app.setStartupTimeMode(tuiTimeStartupTime.getCurrent());
    }

    /**
     * Update all the tui widgets with values taken from the core parameters
     */
    void tuiUpdateWidgets() {
        // 1. Location
        Observator observatory = core.getObservatory();
        tuiLocationLatitude.setValue(observatory.getLatitude());
        tuiLocationLongitude.setValue(observatory.getLongitude());
        tuiLocationAltitude.setValue(observatory.getAltitude());

        // 2. Date & Time
        Navigator navigation = core.getNavigation();
        tuiTimeSkyTime.setJDay(navigation.getJulianDay() + app.getGMTShift(navigation.getJulianDay()) * Navigator.JD_HOUR);
        tuiTimeSetTmz.settz(app.getCustomTzName());
        tuiTimePresetSkyTime.setJDay(app.getPresetSkyTime());
        tuiTimeStartupTime.setCurrent(app.getStartupTimeMode());
        tuiTimeDisplayFormat.setCurrent(app.getTimeFormatStr());
        tuiTimeDateFormat.setCurrent(app.getDateFormatStr());

        // 3. general
        // TODO: Fred is it used?
        tuiGeneralSkyCulture.setValue(core.getSkyCultureDir());
        final Locale skyLanguage = core.getSkyLanguage();
        tuiGeneralSkyLocale.setValue(skyLanguage.getLanguage() + " " + skyLanguage.getDisplayLanguage());// human readable names

        // 4. Stars
        tuiStarsShow.setValue(core.isStarEnabled());
        tuiStarLabelMaxMag.setValue(core.getMaxMagStarName());
        tuiStarsTwinkle.setValue(core.getStarTwinkleAmount());
        tuiStarMagScale.setValue(core.getStarMagScale());

        // 5. Colors
        tuiColorsConstLineColor.setVector(core.getColorConstellationLine());
        tuiColorsConstLabelColor.setVector(core.getColorConstellationNames());
        tuiColorscardinalColor.setVector(core.getColorCardinalPoints());
        tuiColorsConstArtIntensity.setValue(core.getConstellationArtIntensity());
        tuiColorsConstNoundaryColor.setVector(core.getColorConstellationBoundaries());
        tuiColorsPlanetNamesColor.setVector(core.getColorPlanetsNames());
        tuiColorsPlanetOrbitsColor.setVector(core.getColorPlanetsOrbits());
        tuiColorsObjectTrailsColor.setVector(core.getColorPlanetsTrails());
        tuiColorsMeridianColor.setVector(core.getColorMeridianLine());
        tuiColorsAzimuthalColor.setVector(core.getColorAzimutalGrid());
        tuiColorsEquatorialColor.setVector(core.getColorEquatorGrid());
        tuiColorsEquatorColor.setVector(core.getColorEquatorLine());
        tuiColorsEclipticColor.setVector(core.getColorEclipticLine());
        tuiColorsNebulaLabelColor.setVector(core.getColorNebulaLabels());
        tuiColorsNebulaCircleColor.setVector(core.getColorNebulaCircle());

        // 6. effects
        tuiEffectLandscape.setValue(observatory.getLandscapeName());
        tuiEffectPointObj.setValue(core.getPointStar());
        tuiEffectZoomDuration.setValue(core.getAutoMoveDuration());
        tuiEffectManualZoom.setValue(core.getFlagManualAutoZoom());
        tuiEffectObjectScale.setValue(core.getStarScale());
        tuiEffectMilkywayIntensity.setValue(core.getMilkyWayIntensity());
        tuiEffectCursorTimeout.setValue(app.getMouseCursorTimeout());
        tuiEffectNebulaeLabelMagnitude.setValue(core.getNebulaMaxMagHints());

        // 7. Scripts
        // each fresh time enter needs to reset to select message
        if (app.getSelectedScript().equals("")) {
            tuiScriptsLocal.setCurrent(TUI_SCRIPT_MSG);

            if (scriptDirectoryRead) {
                tuiScriptsRemoveable.setCurrent(TUI_SCRIPT_MSG);
            } else {
                // no directory mounted, so put up message
                tuiScriptsRemoveable.replaceItemList("Arrow down to load list.", 0);
            }
        }

        // 8. admin
        tuiAdminSetLocale.setValue(app.getAppLanguage());
    }

    /**
     * Launch script to set time zone in the system locales
     */
    // TODO : this works only if the system manages the TZ environment
    // variables of the form "Europe/Paris". This is not the case on windows
    // so everything migth have to be re-done internaly :(
    void tuiCbSetTimeZone() {
        // Don't call the script anymore coz it's pointless
        // system( ( core.dataDir + "script_set_time_zone " + tuiTimeSetTmz.getCurrent() ).c_str() );
        app.setCustomTzName(tuiTimeSetTmz.gettz());
    }

    /**
     * Set time format mode
     */
    void tuiCbSetTimeDisplayFormat() {
        app.setDateFormatStr(tuiDateDisplayFormat.getCurrent(), tuiTimeDisplayFormat.getCurrent());
    }

    // 7. Administration actions functions

    /**
     * Load default configuration
     *
     * @throws StellariumException If an I/O error occured
     */
    void tuiCbAdminLoadDefault() throws StellariumException {
        app.init();
        tuiUpdateIndependentWidgets();
    }

    /**
     * Save to default configuration
     *
     * @throws StellariumException If an I/O error occured
     */
    void tuiCbAdminSaveDefault() throws StellariumException {
        app.saveCurrentConfig(app.getConf());
        ResourceLocatorUtil.getInstance().execScript("script_save_config");
    }

    /**
     * Launch script for internet update
     *
     * @throws StellariumException If an I/O error occured
     */
    void tuiCbAdminUpdateMe() throws StellariumException {
        ResourceLocatorUtil.getInstance().execScript("script_internet_update");
    }

    /**
     * Set a new landscape skin
     *
     * @throws StellariumException
     */
    void tuiCbTuiEffectChangeLandscape() throws StellariumException {
        app.getCommander().executeCommand("set landscape_name " + tuiEffectLandscape.getCurrent());
    }

    /**
     * Set a new sky culture
     */
    void tuiCbTuiGeneralChangeSkyCulture() throws StellariumException {
        app.getCommander().executeCommand("set sky_culture " + tuiGeneralSkyCulture.getCurrent());
    }

    /**
     * Set a new sky locale
     *
     * @throws StellariumException
     */
    void tuiCbTuiGeneralChangeSkyLocale() throws StellariumException {
        app.getCommander().executeCommand("set sky_locale " + tuiGeneralSkyLocale.getCurrent());
    }

    /**
     * Callback for changing scripts from removeable media
     */
    void tuiCbScriptsRemoveable() throws StellariumException {
        if (!scriptDirectoryRead) {
            // read scripts from mounted disk
            String scriptList = app.getScripts().getScriptList(SCRIPT_REMOVEABLE_DISK);
            tuiScriptsRemoveable.replaceItemList(TUI_SCRIPT_MSG + "\n" + scriptList, 0);
            scriptDirectoryRead = true;
        }

        if (tuiScriptsRemoveable.getCurrent().equals(TUI_SCRIPT_MSG)) {
            app.setSelectedScript("");
        } else {
            app.setSelectedScript(tuiScriptsRemoveable.getCurrent());
            app.setSelectedScriptDirectory(SCRIPT_REMOVEABLE_DISK);
            // to avoid confusing user, clear out local script selection as well
            tuiScriptsLocal.setCurrent(TUI_SCRIPT_MSG);
        }
    }

    /**
     * Callback for changing scripts from local directory
     */
    void tuiCbScriptsLocal() {
        if (tuiScriptsLocal.getCurrent() != TUI_SCRIPT_MSG) {
            app.setSelectedScript((String) tuiScriptsLocal.getCurrent());
            ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
            app.setSelectedScriptDirectory(locatorUtil.getScriptsDir().getPath());
            // to reduce confusion for user, clear out removeable script selection as well
            if (scriptDirectoryRead) {
                tuiScriptsRemoveable.setCurrent(TUI_SCRIPT_MSG);
            }
        } else {
            app.setSelectedScript("");
        }
    }

    void tui_cb_effects_milkyway_intensity() throws StellariumException {
        String oss = "set milky_way_intensity " + tuiEffectMilkywayIntensity.getValue();
        app.getCommander().executeCommand(oss);
    }

    void tui_cb_setlocation() throws StellariumException {

        String oss = "moveto lat " + tuiLocationLatitude.getValue()
                + " lon " + tuiLocationLongitude.getValue()
                + " alt " + tuiLocationAltitude.getValue();
        app.getCommander().executeCommand(oss);
    }


    void tui_cb_stars() throws StellariumException {
        // 4. Stars
        String oss = "flag stars " + tuiStarsShow.getValue();
        app.getCommander().executeCommand(oss);

        oss = "set max_mag_star_name " + tuiStarLabelMaxMag.getValue();
        app.getCommander().executeCommand(oss);

        oss = "set star_twinkle_amount " + tuiStarsTwinkle.getValue();
        app.getCommander().executeCommand(oss);

        oss = "set star_mag_scale " + tuiStarMagScale.getValue();
        app.getCommander().executeCommand(oss);

    }

    void tui_cb_effects() throws StellariumException {
        // 5. effects
        String oss = "flag point_star " + tuiEffectPointObj.getValue();
        app.getCommander().executeCommand(oss);

        oss = "set auto_move_duration " + tuiEffectZoomDuration.getValue();
        app.getCommander().executeCommand(oss);

        oss = "flag manual_zoom " + tuiEffectManualZoom.getValue();
        app.getCommander().executeCommand(oss);

        oss = "set star_scale " + tuiEffectObjectScale.getValue();
        app.getCommander().executeCommand(oss);

        ui.mouseCursorTimeout = tuiEffectCursorTimeout.getValue();// never recorded
    }

    /**
     * Set sky time
     */
    void tuiCbSkyTime() throws StellariumException {
        String oss = "date local ";// + tuiTimeSkyTime.getDateString();
        app.getCommander().executeCommand(oss);
    }

    /**
     * Set nebula label limit
     */
    void tuiCbEffectsNebulaeLabelMagnitude() throws StellariumException {
        String oss = "set max_mag_nebula_name " + tuiEffectNebulaeLabelMagnitude.getValue();
        app.getCommander().executeCommand(oss);
    }


    void tuiCbChangeColor() {
        core.setColorConstellationLine(tuiColorsConstLineColor.getColor());
        core.setColorConstellationNames(tuiColorsConstLabelColor.getColor());
        core.setColorCardinalPoints(tuiColorscardinalColor.getColor());
        core.setConstellationArtIntensity((float) tuiColorsConstArtIntensity.getValue());
        //core.setColorConstellationBoundaries(tuiColorsConstBoundaryColor.getColor());
        // core.setColorStarNames(
        // core.setColorStarCircles(
        core.setColorPlanetsOrbits(tuiColorsPlanetOrbitsColor.getColor());
        core.setColorPlanetsNames(tuiColorsPlanetNamesColor.getColor());
        core.setColorPlanetsTrails(tuiColorsObjectTrailsColor.getColor());
        core.setColorAzimutalGrid(tuiColorsAzimuthalColor.getColor());
        core.setColorEquatorGrid(tuiColorsEquatorialColor.getColor());
        core.setColorEquatorLine(tuiColorsEquatorColor.getColor());
        core.setColorEclipticLine(tuiColorsEclipticColor.getColor());
        core.setColorMeridianLine(tuiColorsMeridianColor.getColor());
        core.setColorNebulaLabels(tuiColorsNebulaLabelColor.getColor());
        core.setColorNebulaCircle(tuiColorsNebulaCircleColor.getColor());
    }

    void tuiCbLocationChangePlanet() throws StellariumException {
        //	core.setHomePlanet( StelUtility::wstringToString( tui_location_planet.getCurrent() ) );
        //	wcout << "set home planet " << tui_location_planet.getCurrent() << endl;
        app.getCommander().executeCommand("set home_planet \"" + tuiLocationPlanet.getCurrent() + "\"");
    }

    /**
     * Update widgets that don't always match current settings with current settings
     */
    void tuiUpdateIndependentWidgets() {

        // Since some tui options don't immediately affect actual settings
        // reset those options to the current values now
        // (can not do this in tui_update_widgets)

        tuiLocationPlanet.setValue(core.getObservatory().getHomePlanetEnglishName());
    }

    /**
     * Handle key events when Text UI is enabled.
     *
     * @param keyEvent
     */
    public void tuiKeypressed(KeyEvent keyEvent) {
        int key = keyEvent.getKeyCode();
        if (key == KeyEvent.VK_M) {
            // leave tui menu
            ui.setShowTextUiMenu(false);

            // If selected a script in tui, run that now
            if (!app.getSelectedScript().equals("")) {
                app.getCommander().executeCommand("script action play filename " + app.getSelectedScriptDirectory() + app.getSelectedScript() + " path " + app.getSelectedScriptDirectory());
            }
            // clear out now
            app.setSelectedScriptDirectory("");
            app.setSelectedScript("");
            keyEvent.consume();
        }
    }

    public void setTuiFont(SFontIfc sFontIfc) {
        tuiFont = sFontIfc;
    }

    protected boolean showTuiDateTime;

    protected boolean showTuiShortObjInfo;

    public boolean isShowTuiDateTime() {
        return showTuiDateTime;
    }

    public void setShowTuiDateTime(boolean showTuiDateTime) {
        this.showTuiDateTime = showTuiDateTime;
    }

    public boolean isShowTuiShortObjInfo() {
        return showTuiShortObjInfo;
    }

    public void setShowTuiShortObjInfo(boolean showTuiShortObjInfo) {
        this.showTuiShortObjInfo = showTuiShortObjInfo;
    }

    public SFontIfc getTuiFont() {
        return tuiFont;
    }

    /**
     * The standard tui font - separate from gui so can reload on the fly
     */
    protected SFontIfc tuiFont;

    static STUI.Branch tuiRoot;
}