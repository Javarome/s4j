/*
* Stellarium
* Copyright (C) 2002 Fabien Chereau
*
* Stellarium for Java
* Copyright (C) 2008 Jerome Beau, Frederic Simon
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

import com.sun.opengl.util.Animator;
import org.stellarium.Navigator;
import org.stellarium.StelApp;
import org.stellarium.StelCore;
import org.stellarium.StellariumException;
import org.stellarium.astro.JulianDay;
import org.stellarium.data.IniFileParser;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.projector.Projector;
import org.stellarium.projector.ViewportDistorter;
import org.stellarium.ui.components.EditBox;
import org.stellarium.ui.components.ExtendedToolBar;
import org.stellarium.ui.components.StellariumComponent;
import org.stellarium.ui.dialog.SearchDialog;
import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * Stellarium User Interface
 *
 * @author <a href="mailto:rr0@rr0.org"/>Jérôme Beau</a>
 * @version 2.0
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_ui.h?view=markup&pathrev=stellarium-0-8-2">stel_ui.h</a>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_ui.cpp?view=markup&pathrev=stellarium-0-8-2">stel_ui.cpp</a>
 */
public abstract class StelUI implements GLEventListener {
    protected FontFactory fontFactory;

    public final DecimalFormat decimalFormat = new DecimalFormat();

    boolean waitOnLocation = true;

    protected boolean dragging;
    private boolean glinit;
    private boolean uiInit;
    protected final Logger logger;
    protected final STextureFactory textureFactory;

    /**
     * allow mouse at edge of screen to move view
     */
    protected boolean mouseMoveEnabled;

    /**
     * Flag for mouse movements
     */
    protected boolean isMouseMovingHoriz;

    /**
     * Flag for mouse movements
     */
    protected boolean isMouseMovingVert;
    protected StelUITuiConf tui;

    public StelUI(StelCore someCore, StelApp someApp, Logger parentLogger) throws StellariumException {
        assert someCore != null : "Core cannot be null in StelUI";

        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);

        core = someCore;
        app = someApp;

        decimalFormat.setMinimumFractionDigits(3);

        IniFileParser conf = app.getConf();

        // Video Section
        core.setViewportSize(conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.SCREEN_WIDTH), conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.SCREEN_HEIGHT));
        core.setViewportHorizontalOffset(conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.HORIZONTAL_OFFSET));
        core.setViewportVerticalOffset(conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.VERTICAL_OFFSET));

        // Projector
        String tmpstr = conf.getStr(IniFileParser.PROJECTION_SECTION, IniFileParser.TYPE);
        core.setProjectionType(Projector.TYPE.valueOf(tmpstr));

        tmpstr = conf.getStr(IniFileParser.PROJECTION_SECTION, IniFileParser.VIEWPORT);
        DefaultProjector.PROJECTOR_MASK_TYPE projMaskType = DefaultProjector.stringToMaskType(tmpstr);
        core.getProjection().setMaskType(projMaskType);

        int bbpMode = conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.BBP_MODE);
        fullScreen = conf.getBoolean(IniFileParser.VIDEO_SECTION, IniFileParser.FULLSCREEN);

        screenW = conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.SCREEN_WIDTH);
        screenH = conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.SCREEN_HEIGHT);

        mouseMoveEnabled = conf.getBoolean(IniFileParser.NAVIGATION_SECTION, IniFileParser.MOVE_MOUSE_ENABLED, true);
        setupMainFrame(conf);
    }

    protected abstract void setupMainFrame(IniFileParser conf);

    protected GLCanvas glCanvas = new GLCanvas();

    public abstract void done();

    /**
     * @return a list of working fullscreen hardware video modes (one per line)
     */
    public abstract String[] getVideoModeList();

    public synchronized void startMainLoop() {
        done();

        Animator animator = new Animator(glCanvas);
        animator.start();
    }

    void resizeGL(int w, int h) {
        if (h == 0 || w == 0) {
            return;
        }
        //        core.setScreenSize(w, h);
    }

    public void init(GLAutoDrawable drawable) {
        logger.entering(getClass().getName(), "init");
        //DebugGL glDebug = new DebugGL(drawable.getGL());
        GL gl = drawable.getGL();
        gl.setSwapInterval(0);
        setGl(gl, logger);
        glClear(GL_COLOR_BUFFER_BIT);
        logger.exiting(getClass().getName(), "init");
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        //setGl(drawable.getGL());
        //glMatrixMode(GL_PROJECTION);
        //glLoadIdentity();
        //gluOrtho2D(0, 1, 0, 1);
        //glMatrixMode(GL_MODELVIEW);
        //glLoadIdentity();
        //        GL gl = drawable.getGL();
        //        System.out.println("Canvas w=" + width + " h=" + height);
    }

    private long didIt;

    public void display(GLAutoDrawable drawable) {
        if (!glinit) {
            initFromGLInit();
            resize();
        } else {
            if (!uiInit) {
                initAsDisplayable();
                draw(1);
                uiInit = true;
            } else {
                draw();
            }
        }
    }

    private void draw() {
        long curTime = System.currentTimeMillis();
        if (didIt != 0) {
            int deltaTime = (int) (curTime - didIt);
            app.update(deltaTime);
            //glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            draw(deltaTime);
        }
        didIt = curTime;
    }

    public void draw(int deltaTime) {
        internalDraw();
        updateTopBar();
        updateWidgets(deltaTime);
        app.draw(deltaTime);
        if (core.getPointer() != null) {
            updateInfoSelectString();
        }
    }

    public int getScreenH() {
        return screenH;
    }

    public int getScreenW() {
        return screenW;
    }

    protected boolean showGravityUi;

    public void internalDraw() {
        // draw first as windows should cover these up also problem after 2dfullscreen with square viewport
        if (showGravityUi) {
            tui.drawGravityUi();
        }
        if (isShowTextUiMenu()) {
            tui.drawTUI();
        }

        // Special cool text transparency mode
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_BLEND);

        set2DfullscreenProjection();// 2D coordinate
        StellariumComponent.enableScissor();

        glScalef(1, -1, 1);// invert the y axis, down is positive
        glTranslatef(0, -core.getViewportHeight(), 0);// move the origin from the bottom left corner to the upper left corner

        StellariumComponent.disableScissor();
        restoreFrom2DfullscreenProjection();// Restore the other coordinate
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {

    }

    protected abstract void resize();

    public abstract void init(IniFileParser conf) throws StellariumException;

    protected abstract void setShowTopBar(boolean conf);

    public abstract void initAsDisplayable();

    /**
     * draws a message window to display a message to user
     * if timeout is zero, won't time out
     * otherwise use miliseconds
     *
     * @param title       Message window title
     * @param someMessage Message text.
     */
    public abstract void showError(String title, String someMessage);

    public abstract void showMessage(String someMessage, int someTimeOut);

    protected abstract void updateTopBar() throws StellariumException;

    static final int UI_PADDING = 3;

    static final int UI_BT = 25;

    static final int UI_SCRIPT_BAR = 300;

    public String getBaseFontName() {
        return baseFontName;
    }

    public FontFactory getFontFactory() {
        return fontFactory;
    }

    /**
     * Set the drawing mode in 2D for drawing in the full screen
     */
    public void set2DfullscreenProjection() {
        glViewport(0, 0, screenW, screenH);
        glMatrixMode(GL_PROJECTION);// projection matrix mode
        glPushMatrix();// store previous matrix
        glLoadIdentity();
        gluOrtho2D(0, screenW, 0, screenH);// set a 2D orthographic projection
        glMatrixMode(GL_MODELVIEW);// modelview matrix mode
        glPushMatrix();
        glLoadIdentity();
    }

    /**
     * Restore previous projection mode
     */
    public void restoreFrom2DfullscreenProjection() {
        glMatrixMode(GL_PROJECTION);// Restore previous matrix
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    public void initFromGLInit() {
        // Init fonts : should be moved into the constructor
        SFontIfc equatorialGridFont = getFontFactory().create(core.generalFontSize, baseFontName);
        core.equatorialGrid.setFont(equatorialGridFont);
        SFontIfc azimutalGridFont = getFontFactory().create(core.generalFontSize, baseFontName);
        core.azimutalGrid.setFont(azimutalGridFont);
        SFontIfc equatorLineFont = getFontFactory().create(core.generalFontSize, baseFontName);
        core.equatorLine.setFont(equatorLineFont);
        SFontIfc eclipticLineFont = getFontFactory().create(core.generalFontSize, baseFontName);
        core.eclipticLine.setFont(eclipticLineFont);
        SFontIfc meridianLineFont = getFontFactory().create(core.generalFontSize, baseFontName);
        core.meridianLine.setFont(meridianLineFont);
        SFontIfc cardinalPointsFont = getFontFactory().create(core.cardinalPointsFontSize, baseFontName);
        core.cardinalsPoints.setFont(cardinalPointsFont);

        // Init milky way
        if (core.firstTime) {
            core.milkyWay.setTexture("milkyway.png");
        }

        core.setLandscape(core.getObservatory().getLandscapeName());
        core.firstTime = false;

        IniFileParser conf = app.getConf();
        String tmpstr = conf.getStr(IniFileParser.PROJECTION_SECTION, IniFileParser.VIEWPORT);
        if ("maximized".equals(tmpstr)) {
            core.setMaximizedViewport(getScreenW(), getScreenH());
        } else if ("square".equals(tmpstr) || "disk".equals(tmpstr)) {
            core.setSquareViewport(getScreenW(), getScreenH(), conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.HORIZONTAL_OFFSET), conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.HORIZONTAL_OFFSET));
            if ("disk".equals(tmpstr)) {
                core.setViewportMaskDisk();
            }
        } else {
            throw new StellariumException("Unknown viewport type : " + tmpstr);
        }

        // initialisation of the User Interface

        // TODO: Need way to update settings from config without reinitializing whole gui
        //        ui.init(conf);

        if (!initialized) {
            initTui();// don't reinit tui since probably called from there
        } else {
            //            ui.localizeTui();// update translations/fonts as needed

            // Initialisation of the color scheme
            app.drawMode = StelApp.DRAWMODE.NONE;// fool caching
        }

        app.setVisionModeNormal();
        if (conf.getBoolean(IniFileParser.VIEWING_SECTION, IniFileParser.CHART_ENABLED)) {
            app.setVisionModeChart();
        }
        if (conf.getBoolean(IniFileParser.VIEWING_SECTION, IniFileParser.NIGHT_MODE)) {
            app.setVisionModeNight();
        }
        if (app.distorter == null) {
            app.setViewPortDistorterType(ViewportDistorter.TYPE.valueOf(conf.getStr(IniFileParser.VIDEO_SECTION, IniFileParser.DISTORTER, ViewportDistorter.TYPE.none.name())));
        }

        // play startup script, if available
        if (app.scripts != null) {
            //            scripts.play_startup_script();
        }

        glinit = true;
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
        StellariumComponent.deleteScissor();
    }

    /**
     * Create the button panel in the lower left corner
     *
     * @param conf The configuration
     * @return The created tool bar, populated with its buttons
     * @throws org.stellarium.StellariumException
     *
     */
    abstract ExtendedToolBar createFlagButtons(IniFileParser conf) throws StellariumException;

    void btDecTimeSpeedCb() {
        double s = core.getTimeSpeed();
        if (s > Navigator.JD_SECOND) {
            s /= 10;
        } else if (s <= -Navigator.JD_SECOND) {
            s *= 10;
        } else if (s > -Navigator.JD_SECOND && s <= 0) {
            s = -Navigator.JD_SECOND;
        } else if (s > 0 && s <= Navigator.JD_SECOND) {
            s = 0;
        }
        app.setTimeSpeed(s);
    }

    void btIncTimeSpeedCb() {
        double s = core.getTimeSpeed();
        if (s >= Navigator.JD_SECOND) {
            s *= 10;
        } else if (s < -Navigator.JD_SECOND) {
            s /= 10;
        } else if (s >= 0 && s < Navigator.JD_SECOND) {
            s = Navigator.JD_SECOND;
        } else if (s >= -Navigator.JD_SECOND && s < 0) {
            s = 0;
        }
        app.setTimeSpeed(s);
    }

    void btRealTimeSpeedCb() {
        app.setTimeSpeed(Navigator.JD_SECOND);
    }

    void btTimeNowCb() {
        core.setJDay(JulianDay.getJulianFromSys());
    }

    void cbEditScriptExecute() throws StellariumException {
        String commandString = scriptButton.getText();
        logger.fine("Executing command: " + commandString);

        scriptButton.clearText();
        scriptButton.setEditing(false);

        app.getCommander().executeCommand(commandString);
        //        btFlagHelpLbl.setLabel("Invalid Script command");
    }

    double lastJD = 0;

    /**
     * Update changing values
     *
     * @param deltaTime
     */
    protected abstract void updateWidgets(int deltaTime);

    protected abstract void updateTimeControls();

    /**
     * Update the infos about the selected object in the TextLabel widget
     */
    public abstract void updateInfoSelectString();

    public void setTitleObservatoryName(String name) {
        topBarTitle = name;
        /*
                if (name.equals(""))
                    topBarAppNameLbl.setLabel(Main.APP_NAME);
                else {
                    topBarAppNameLbl.setLabel(Main.APP_NAME + " (" + name + ")");
                }
                topBarAppNameLbl.setPos(core.getViewportWidth() / 2 - topBarAppNameLbl.getSizeX() / 2, 1);
        */
    }

    public String getTitleWithAltitude() {
        return core.getObservatory().getHomePlanetNameI18n() +
                ", " + core.getObservatory().getName() +
                " @ " + core.getObservatory().getAltitude() + "m";
    }

    abstract void setColorScheme(File skinFile, String section) throws StellariumException;

    public void setShowTextUiMenu(boolean flag) {
        if (flag && !showTuiMenu) {
            tuiUpdateIndependentWidgets();
        }

        showTuiMenu = flag;
    }

    public boolean isShowTextUiMenu() {
        return showTuiMenu;
    }

    protected boolean showTuiMenu;

    /**
     * The Main core can be accessed because StelUI is a friend class
     */
    protected StelCore core;

    /**
     * The main application instance
     */
    protected final StelApp app;

    boolean initialized;

    /**
     * The standard font
     */
    protected SFontIfc baseFont;

    /**
     * The standard fixed size font
     */
    protected SFontIfc courierFont;


    /**
     * The standard fill texture
     */
    protected STexture baseTex;

    /**
     * The standard fill texture
     */
    protected STexture flipBaseTex;

    /**
     * Up arrow texture
     */
    protected STexture texUp;

    /**
     * Down arrow texture
     */
    protected STexture texDown;


    // Gui
    boolean showTopBar;

    protected boolean showFramesPerSecond;

    boolean showTime;

    boolean showDate;

    boolean showApplicationName;

    boolean showScriptBar;

    protected boolean showFieldOfView;

    boolean toolBarEnabled;

    boolean showSelectedObjectInfo;

    SColor guiBaseColor;

    SColor guiTextColor;

    int baseFontSize;

    String baseFontName;

    int baseCFontSize;

    String baseCFontName;

    boolean opaqueGUI = true;

    protected String topBarTitle;

    // Flags buttons (the buttons in the bottom left corner)

    interface ButtonAction {

    }

    protected ButtonAction constellationDrawAction;

    protected ButtonAction constellationNameAction;

    protected ButtonAction constellationArtAction;

    protected ButtonAction azimuthGridAction;

    protected ButtonAction equatorGridAction;

    protected ButtonAction groundAction;

    protected ButtonAction cardinalsAction;

    protected ButtonAction atmosphereAction;

    protected ButtonAction nebulaNameAction;

    protected ButtonAction helpAction;

    protected ButtonAction equatorialModeAction;

    protected ButtonAction configAction;

    protected ButtonAction quitAction;

    protected ButtonAction searchAction;

    protected EditBox scriptButton;

    protected ButtonAction gotoAction;

    protected ButtonAction horizontalSplitAction;

    protected ButtonAction verticalSplitAction;

    protected ButtonAction chartVisionAction;

    protected ButtonAction nightModeAction;

    protected ButtonAction timeDecreaseAction;

    protected ButtonAction realTimeAction;

    protected ButtonAction timeIncreaseAction;

    protected ButtonAction timeNowAction;

    protected abstract SearchDialog createSearchWindow();

    void searchDialogHide() {

    }

    public abstract void autoCompleteSearchedObject(String objectName);

    public abstract void gotoSearchedObject(String objectName);

    int previousX = -1, previousY = -1;

    /**
     * Is the removeable disk for scripts mounted?
     */
    boolean flagScriptsRemoveableDiskMounted;

    boolean scriptDirectoryRead;

    /**
     * for cursor timeout (seconds)
     */
    double mouseTimeLeft;

    public void initTui() {
        tui = new StelUITuiConf(this);
    }

    public void drawGravityUI() {
        tui.drawGravityUi();
    }

    public double getMouseCursorTimeout() {
        return mouseCursorTimeout;
    }

    /**
     * Seconds to hide cursor when not used.  0 means no timeout
     */
    protected double mouseCursorTimeout;

    protected boolean tuiMenuEnabled;

    public boolean isShowSelectedObjectInfo() {
        return showSelectedObjectInfo;
    }

    public void setShowSelectedObjectInfo(boolean showSelectedObjectInfo) {
        this.showSelectedObjectInfo = showSelectedObjectInfo;
    }

    public boolean isShowScriptBar() {
        return showScriptBar;
    }

    public void setShowScriptBar(boolean showScriptBar) {
        this.showScriptBar = showScriptBar;
    }

    // implemented in stel_ui_tuiconf.cpp

    /**
     * For widgets that aren't tied directly to current settings
     */
    void tuiUpdateIndependentWidgets() {
        tui.tuiUpdateIndependentWidgets();
    }

    /**
     * Display the tui
     */
    void drawTUI() {
        tui.drawTUI();
    }

    abstract void showCursor(boolean visible);

    public boolean isWaitOnLocation() {
        return waitOnLocation;
    }

    public void setWaitOnLocation(boolean waitOnLocation) {
        this.waitOnLocation = waitOnLocation;
    }

    public StelUITuiConf getTui() {
        return tui;
    }

    /**
     * Screen size
     */
    protected int screenW;
    protected int screenH;

    protected boolean fullScreen;

}