package org.stellarium.ui;

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

import org.stellarium.*;
import org.stellarium.astro.JulianDay;
import org.stellarium.command.ScriptMgr;
import org.stellarium.command.StelCommandInterface;
import org.stellarium.data.IniFileParser;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.ui.components.*;
import org.stellarium.ui.dialog.ConfigDialog;
import org.stellarium.ui.dialog.SearchDialog;
import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontFactory;
import org.stellarium.ui.render.STexture;

import javax.media.opengl.GLCanvas;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Dimension2D;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Swing User Interface of S4J
 */
public class SwingUI extends StelUI implements KeyListener, MouseMotionListener, MouseWheelListener, MouseListener {

    private static Cursor BLANK_CURSOR;
    public JLabel topBar;
    public JFrame mainFrame;

    /**
     * The container for the button
     */
    public ExtendedToolBar toolBar;
    protected JToggleButton constellationDrawButton;
    protected JToggleButton constellationNameButton;
    protected JToggleButton constellationArtButton;
    protected JToggleButton azimuthGridButton;
    protected JToggleButton equatorGridButton;
    protected JToggleButton groundButton;
    protected JToggleButton cardinalsButton;
    protected JToggleButton atmosphereButton;
    protected JToggleButton nebulaNameButton;
    protected JToggleButton helpButton;
    protected JToggleButton equatorialModeButton;
    protected JToggleButton configButton;
    protected JToggleButton chartButton;
    protected JToggleButton nightModeButton;
    /**
     * Time control buttons
     */
    protected JToolBar timeControlButtons;
    /**
     * The window managing the configuration
     */
    protected ConfigDialog configWin;

    /**
     * The window managing the search - Tony
     */
    protected SearchDialog searchWin;

    protected int toolBarDockingSize;
    private GraphicsDevice device;
    private DisplayMode displayMode;

    /**
     * The container which contains everything
     */
    protected JPanel desktop;

    private JLabel statusBar;

    public SwingUI(StelCore someCore, StelApp someApp, Logger parentLogger) throws StellariumException {
        super(someCore, someApp, parentLogger);
    }

    public static void scaleIcon(ImageIcon icon, int size) {
        Image scaledImage = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        icon.setImage(scaledImage);
    }

    protected ConfigDialog createConfigWindow() throws StellariumException {
        ConfigDialog configWin = new ConfigDialog(mainFrame, app, logger);
        configWin.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                configDialogHide();
            }
        });
        configWin.pack();
        return configWin;
    }

    /**
     * Create Search window widgets
     */
    protected SearchDialog createSearchWindow() throws StellariumException {
        SearchDialog searchWin = new SearchDialog(mainFrame, app);
        searchWin.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                searchDialogHide();
            }
        });
        searchWin.pack();
        return searchWin;
    }

    void configDialogHide() {
        configWin.setVisible(false);
//        configWin.setVisible(false);
        // for MapPicture - when the dialog appears, this tells the system
        // not to show the city until MapPicture has located the name
        // from the lat and long.
        waitOnLocation = true;
        configButton.setSelected(false);
    }

    void searchDialogHide() {
        searchWin.setVisible(false);
        searchButton.setSelected(false);
    }

    public void done() {
        if (fullScreen) {
            if (device.isFullScreenSupported()) {
                if (device.isDisplayChangeSupported()) {
                    getFrame().setUndecorated(true);
                    getFrame().setVisible(true);
                    device.setFullScreenWindow(getFrame());
                    device.setDisplayMode(displayMode);
                    fullScreen = true;
                } else {
                    logger.warning("Display change is not supported for device " + device);
                    // Not much point in having a full-screen window in this case
                    device.setFullScreenWindow(null);
                    final Frame f2 = getFrame();
                    try {
                        EventQueue.invokeAndWait(new Runnable() {
                            public void run() {
                                f2.setUndecorated(false);
                                f2.setVisible(false);
                                f2.setVisible(true);
                                f2.setSize(screenW, screenH);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logger.warning("Was not able to change display mode; full-screen disabled");
                }
            } else {
                logger.warning("Full-screen mode not supported; running in window instead");
            }
        } else {
            getFrame().setVisible(true);
        }

        // Use the main thread (This method is called from main)
        // As the calculation thread separated from display
        glCanvas.addKeyListener(this);
        glCanvas.addMouseListener(this);
        glCanvas.addMouseMotionListener(this);
        glCanvas.addMouseWheelListener(this);


        toolBar.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (ExtendedToolBar.FLOATING.equals(evt.getPropertyName()) || "orientation".equals(evt.getPropertyName())) {
                    SwingUI.this.resize();
                }
            }
        });
        toolBarDockingSize = toolBar.getHeight();
        getFrame().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                resize();
            }
        });
    }

    public String[] getVideoModeList() {
        GraphicsDevice device = getFrame().getGraphicsConfiguration().getDevice();
        DisplayMode[] displayModes = device.getDisplayModes();
        String[] result = new String[displayModes.length];
        for (int i = 0; i < displayModes.length; i++) {
            result[i] = getDisplayModeString(displayModes[i]);
        }
        return result;
    }

    protected void resize() {
        final Insets insets = getFrame().getInsets();
        final int topBarHeight = topBar.isVisible() ? topBar.getHeight() : 0;
        boolean floating = toolBar.isFloating();
        int orientation = toolBar.getOrientation();
        int toolBarHeight = floating ? 0 : orientation == JToolBar.HORIZONTAL ? toolBarDockingSize : 0;
        int toolBarWidth = floating ? 0 : orientation == JToolBar.VERTICAL ? toolBarDockingSize : 0;
        final int left = 0;
        final int bottom = 0;
        final int height = getFrame().getHeight() - topBarHeight - insets.top - insets.bottom - toolBarHeight;
        final int width = getFrame().getWidth() - insets.right - insets.left - toolBarWidth;
        core.getProjection().setViewport(left, bottom, width, height);
    }

    public void init(IniFileParser conf) throws StellariumException {

        // Ui section
        showFramesPerSecond = conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_FRAMES_PER_SECOND);
        toolBarEnabled = conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_MENU);
        //flagHelp = conf.getBoolean("gui", "flag_help");   // JBE: What's the point of displaying help at startup.
        //flagInfos = conf.getBoolean("gui", "flag_infos");
        setShowTopBar(conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_TOPBAR));
        showTime = conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_TIME);
        showDate = conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_DATE);
        showApplicationName = conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_APPLICATION_NAME);
        showFieldOfView = conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_FIELD_OF_VIEW);
        showSelectedObjectInfo = conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_SELECTION_INFO);
        baseFontSize = conf.getInt(IniFileParser.GUI_SECTION, IniFileParser.BASE_FONT_SIZE, 15);
        baseFontName = conf.getStr(IniFileParser.GUI_SECTION, IniFileParser.BASE_FONT_NAME, "DejaVuSans.ttf");
        showScriptBar = conf.getBoolean(IniFileParser.GUI_SECTION, "flag_show_script_bar", false);
        mouseCursorTimeout = conf.getDouble(IniFileParser.GUI_SECTION, IniFileParser.MOUSE_CURSOR_TIMEOUT, 0);

        // Text ui section
        tuiMenuEnabled = conf.getBoolean(IniFileParser.TEXT_UI_SECTION, IniFileParser.SHOW_TEXT_MENU);
        showGravityUi = conf.getBoolean(IniFileParser.TEXT_UI_SECTION, "flag_show_gravity_ui");

        // TODO: can we get rid of this second font requirement?
        baseCFontSize = conf.getInt(IniFileParser.GUI_SECTION, "base_cfont_size", 12);
        baseCFontName = conf.getStr(IniFileParser.GUI_SECTION, "base_cfont_name", "DejaVuSansMono.ttf");

        fontFactory = new SFontFactory(new Dimension2D() {   // Dynamic dimension

            public double getWidth() {
                return core.getViewportWidth();
            }

            public double getHeight() {
                return core.getViewportHeight();
            }

            public void setSize(double width, double height) {
                // Not allowed from font factory
            }
        });

        // Load standard font
        baseFont = fontFactory.create(baseFontSize, baseFontName);

        courierFont = fontFactory.create(baseCFontSize, baseCFontName);

        // set up mouse cursor timeout
        mouseTimeLeft = mouseCursorTimeout * 1000;

        // Create standard texture
        baseTex = textureFactory.createTexture("backmenu.png", STexture.TEX_LOAD_TYPE_PNG_ALPHA);
        flipBaseTex = textureFactory.createTexture("backmenu_flip.png", STexture.TEX_LOAD_TYPE_PNG_ALPHA);

        texUp = textureFactory.createTexture("up.png");
        texDown = textureFactory.createTexture("down.png");

        // Set default Painter
        Painter p = new Painter(baseTex, baseFont, new SColor(0.5f, 0.5f, 0.5f), new SColor(1, 1, 1));
        StellariumComponent.setDefaultPainter(p);

        StellariumComponent.initScissor(core.getViewportWidth(), core.getViewportHeight());

        initialized = true;

        setTitleObservatoryName(getTitleWithAltitude());
    }

    protected void setShowTopBar(boolean conf) {
        showTopBar = conf;
        if (desktop != null) {
            topBar.setVisible(showTopBar);
        }
    }

    protected JPanel createDesktop(IniFileParser conf, GLCanvas someGlCanvas) {
        init(conf);

        desktop = (JPanel) mainFrame.getContentPane();
        desktop.setLayout(new AKDockLayout());

        topBar = createTopBar();
        desktop.add(topBar, BorderLayout.NORTH);

        glCanvas = someGlCanvas;
        desktop.add(glCanvas, BorderLayout.CENTER);
        showCursor(true);

        //statusBar = new JLabel(" ");
        toolBar = createFlagButtons(conf);
        toolBar.setVisible(false);
        desktop.add(toolBar, BorderLayout.SOUTH);

        timeControlButtons = createTimeControlButtons();
        desktop.add(timeControlButtons, BorderLayout.SOUTH);

        return desktop;
    }

    public void initAsDisplayable() {
        configWin = createConfigWindow();
        configWin.addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                configButton.setSelected(false);
            }
        });

        searchWin = createSearchWindow();
        searchWin.addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                searchButton.setSelected(false);
            }
        });

        toolBar.setVisible(toolBarEnabled);
        constellationDrawButton.setSelected(core.isConstellationLinesEnabled());
        constellationNameButton.setSelected(core.getFlagConstellationNames());
        constellationArtButton.setSelected(core.getFlagConstellationArt());
        azimuthGridButton.setSelected(core.isAzimutalGridEnabled());
        equatorGridButton.setSelected(core.isEquatorGridEnabled());
        groundButton.setSelected(core.isLandscapeEnabled());
        cardinalsButton.setSelected(core.isCardinalsPointsEnabled());
        atmosphereButton.setSelected(core.isAtmosphereEnabled());
        nebulaNameButton.setSelected(core.isNebulaHintEnabled());
        equatorialModeButton.setSelected(core.getNavigation().getViewingMode() == Navigator.VIEWING_MODE_TYPE.EQUATOR);
        configButton.setSelected(configWin.isVisible());
        //btFlagChart.setSelected(app.getVisionModeChart());
        nightModeButton.setSelected(app.getVisionModeNight());
        searchButton.setSelected(searchWin.isVisible());
        gotoButton.setSelected(false);
        if (horizontalFlipButton != null) {
            horizontalFlipButton.setSelected(core.getFlipHorz());
        }
        if (verticalFlipButton != null) {
            verticalFlipButton.setSelected(core.getFlipVert());
        }

        glCanvas.requestFocus();
    }

    public void showError(String title, String someMessage) {
        JOptionPane.showMessageDialog(getFrame(), someMessage, title, JOptionPane.ERROR_MESSAGE);
    }

    public void showMessage(String someMessage, int someTimeOut) {
        JOptionPane.showMessageDialog(getFrame(), someMessage);
    }

    JLabel createTopBar() {
        JLabel topBar = new JLabel();
        topBarTitle = Main.APP_NAME;
        return topBar;
    }

    Cursor createCursor(final char image[][]) {
        int i;
        final int width = 32;
        final int height = 32;
        int data[] = new int[4 * 32];
        int mask[] = new int[4 * 32];
        int hotX, hotY;

        i = -1;
        int row;
        for (row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                if (col % 8 != 0) {
                    data[i] <<= 1;
                    mask[i] <<= 1;
                } else {
                    ++i;
                    data[i] = mask[i] = 0;
                }
                switch (image[4 + row][col]) {
                    case 'X':
                        data[i] |= 0x01;
                        mask[i] |= 0x01;
                        break;
                    case '.':
                        mask[i] |= 0x01;
                        break;
                    case ' ':
                        break;
                }
            }
        }
        String hotString = new String(image[4 + row], 0, 5);
        int commaPos = hotString.indexOf(',');
        hotX = Integer.parseInt(hotString.substring(0, commaPos));
        hotY = Integer.parseInt(hotString.substring(commaPos + 1));
        // TODO(JBE): What about mask ?
        return createCursor(width, height, data, hotX, hotY, width, "customCursor");
    }

    private Cursor createCursor(int width, int height, int[] pix, int hotX, int hotY, int scan, String name) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(new MemoryImageSource(width, height, pix, 0, scan));
        return toolkit.createCustomCursor(image, new Point(hotX, hotY), name);
    }

    void showCursor(boolean visible) {
        glCanvas.setCursor(visible ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : getBlankCursor());
    }

    private Cursor getBlankCursor() {
        if (BLANK_CURSOR == null) {
            // Initialize "hidden" cursor
            final int[] pixels = new int[16 * 16];
            SwingUI.BLANK_CURSOR = createCursor(16, 16, pixels, 0, 0, 16, "invisibleCursor");
        }
        return BLANK_CURSOR;
    }

    void setColorScheme(File skinFile, String section) throws StellariumException {
        IniFileParser conf = new IniFileParser(getClass(), skinFile);

        Color guiBaseColor = StelUtility.stringToColor(conf.getStr(section, "gui_base_color", "0.3,0.4,0.7"));
        Color guiTextColor = StelUtility.stringToColor(conf.getStr(section, "gui_text_color", "0.7,0.8,0.9"));

        // TODO(JBE): Manage this
        //        desktop.setColorScheme(guiBaseColor, guiTextColor);
    }

    public String getDisplayModeString(DisplayMode displayMode) {
        return displayMode.getWidth() + "x" + displayMode.getHeight() + ", " + displayMode.getBitDepth() + " bits/pixel (" + displayMode.getRefreshRate() + " Hz)";
    }

    protected void scriptKeyPressed(KeyEvent e, int key) {
        ScriptMgr scripts = app.getScripts();
        StelCommandInterface commander = app.getCommander();
        // here reusing time control keys to control the script playback
        if (key == KeyEvent.VK_6) {
            // pause/unpause script
            commander.executeCommand("script action pause");
            app.setTimeMultiplier(1);// don't allow resumption of ffwd this way (confusing for audio)
            e.consume();
        } else if (key == KeyEvent.VK_K) {
            commander.executeCommand("script action resume");
            app.setTimeMultiplier(1);
            e.consume();
        } else if (key == KeyEvent.VK_7 || (key == KeyEvent.VK_C && e.isControlDown()) && (key == KeyEvent.VK_M && tuiMenuEnabled)) {
            // TODO: should double check with user here...
            commander.executeCommand("script action end");
            if (key == KeyEvent.VK_M) {
                setShowTextUiMenu(true);
            }
            // TODO n is bad key if ui allowed
            e.consume();
        } else if (key == KeyEvent.VK_GREATER || key == KeyEvent.VK_N) {
            commander.executeCommand("audio volume increment");
            e.consume();
        }
        // TODO d is bad key if ui allowed
        else if (key == KeyEvent.VK_LESS || key == KeyEvent.VK_D) {
            commander.executeCommand("audio volume decrement");
            e.consume();
        } else if (key == KeyEvent.VK_J) {
            if (app.getTimeMultiplier() == 2) {
                app.setTimeMultiplier(1);

                // restart audio in correct place
                commander.executeCommand("audio action sync");
            } else if (app.getTimeMultiplier() > 1) {
                app.setTimeMultiplier(app.getTimeMultiplier() / 2);
            }
            e.consume();
        } else if (key == KeyEvent.VK_1) {
            // stop audio since won't play at higher speeds
            commander.executeCommand("audio action pause");
            app.setTimeMultiplier(app.getTimeMultiplier() * 2);
            if (app.getTimeMultiplier() > 8) {
                app.setTimeMultiplier(8);
            }
            e.consume();
        } else if (!scripts.isAllowUI()) {
            logger.info("Playing a script.  Press ctrl-C (or 7) to stop.");
            e.consume();
        }
    }

    void cbEditScriptKey() {
        if (scriptButton.getLastKey() == KeyEvent.VK_SPACE || scriptButton.getLastKey() == KeyEvent.VK_TAB) {
            String command = scriptButton.getText().toLowerCase();
            if (scriptButton.getLastKey() == KeyEvent.VK_SPACE) {
                command = command.substring(0, command.length() - 1);
            }
        } else if (scriptButton.getLastKey() == KeyEvent.VK_ESCAPE) {
            scriptButton.clearText();
        }
    }

    static class SButton extends JToggleButton {
        public SButton(SwingButtonAction a) {
            super(a);
            setText(null);
            setPreferredSize(new Dimension(UI_BT, UI_BT));
            ImageIcon icon = (ImageIcon) a.getValue(Action.SMALL_ICON);
            scaleIcon(icon, UI_BT);
            setBorder(new LineBorder(Color.GRAY));
        }
    }

    abstract class SwingButtonAction extends AbstractAction implements ButtonAction {
        SwingButtonAction(String description, String iconFileName, KeyStroke accelerator) {
            super(description, getImageIcon(iconFileName, core.getTranslator().translate(description)));
            putValue(SHORT_DESCRIPTION, core.getTranslator().translate(description));
            putValue(MNEMONIC_KEY, accelerator.getKeyCode());
            putValue(ACCELERATOR_KEY, accelerator);
        }
    }

    private static ImageIcon getImageIcon(String iconFileName, String description) {
        URL textureURL = ResourceLocatorUtil.getInstance().getTextureURL(iconFileName);
        Image image = Toolkit.getDefaultToolkit().getImage(textureURL);
        return new ImageIcon(image, description);
    }

    protected void updateTopBar() throws StellariumException {
        if (showTopBar) {
            StringBuffer topBarText = new StringBuffer();
            String SEP = "";

            double jd = core.getJulianDay();
            if (showDate) {
                topBarText.append(app.getPrintableDateUTC(jd));
                SEP = "    ";
            }

            if (showApplicationName) {
                topBarText.append(SEP).append(topBarTitle);
                SEP = "    ";
            }

            if (showFieldOfView) {
                topBarText.append(SEP).append("FOV=").append(core.getFieldOfView()).append('\u00b0');
                SEP = "    ";
            }

            if (showFramesPerSecond) {
                topBarText.append(SEP).append("FPS=").append(decimalFormat.format(app.getFramesPerSecond()));
            }
            topBar.setText(topBarText.toString());
        }
    }

    ExtendedToolBar createFlagButtons(IniFileParser conf) throws StellariumException {

        ExtendedToolBar toggleButtonsContainer = new ExtendedToolBar();

        constellationDrawAction = new SwingButtonAction("DrawingOfTheConstellationsC", "bt_constellations.png", KeyStroke.getKeyStroke(KeyEvent.VK_C, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.isConstellationLinesEnabled();
                core.setFlagConstellationLines(newState);
                constellationDrawButton.setSelected(newState);
            }
        };
        constellationDrawButton = new SButton((SwingButtonAction) constellationDrawAction);
        constellationDrawButton.setSelected(core.isConstellationLinesEnabled());

        constellationNameAction = new SwingButtonAction("NamesOfTheConstellationsV", "bt_const_names.png", KeyStroke.getKeyStroke(KeyEvent.VK_V, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.getFlagConstellationNames();
                core.setFlagConstellationNames(newState);
                constellationNameButton.setSelected(newState);
            }
        };
        constellationNameButton = new SButton((SwingButtonAction) constellationNameAction);
        constellationDrawButton.putClientProperty("hideActionText", Boolean.TRUE);
        constellationNameButton.setSelected(core.getFlagConstellationNames());

        constellationArtAction = new SwingButtonAction("ConstellationsArtR", "bt_constart.png", KeyStroke.getKeyStroke(KeyEvent.VK_R, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.getFlagConstellationArt();
                core.setFlagConstellationArt(newState);
                constellationArtButton.setSelected(newState);
            }
        };
        constellationArtButton = new SButton((SwingButtonAction) constellationArtAction);
        constellationArtButton.setSelected(core.getFlagConstellationArt());

        azimuthGridAction = new SwingButtonAction("AzimuthalGridZ", "bt_azgrid.png", KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.isAzimutalGridEnabled();
                core.setAzimutalGrid(newState);
                azimuthGridButton.setSelected(newState);
            }
        };
        azimuthGridButton = new SButton((SwingButtonAction) azimuthGridAction);
        azimuthGridButton.setSelected(core.isAzimutalGridEnabled());

        equatorGridAction = new SwingButtonAction("EquatorialGridE", "bt_eqgrid.png", KeyStroke.getKeyStroke(KeyEvent.VK_E, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.isEquatorGridEnabled();
                core.setEquatorGrid(newState);
                equatorGridButton.setSelected(newState);
            }
        };
        equatorGridButton = new SButton((SwingButtonAction) equatorGridAction);
        equatorGridButton.setSelected(core.isEquatorGridEnabled());

        groundAction = new SwingButtonAction("GroundG", "bt_ground.png", KeyStroke.getKeyStroke(KeyEvent.VK_G, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.isLandscapeEnabled();
                core.setLandscapeEnabled(newState);
                groundButton.setSelected(newState);
            }
        };
        groundButton = new SButton((SwingButtonAction) groundAction);
        groundButton.setSelected(core.isLandscapeEnabled());

        cardinalsAction = new SwingButtonAction("CardinalPointsQ", "bt_cardinal.png", KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0)) {
            public void actionPerformed(ActionEvent e1) {
                core.setCardinalsPointsEnabled(!core.isCardinalsPointsEnabled());
            }
        };
        cardinalsButton = new SButton((SwingButtonAction) cardinalsAction);
        cardinalsButton.setSelected(core.isCardinalsPointsEnabled());

        atmosphereAction = new SwingButtonAction("AtmosphereA", "bt_atmosphere.png", KeyStroke.getKeyStroke(KeyEvent.VK_A, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.isAtmosphereEnabled();
                core.setAtmosphere(newState);
                atmosphereButton.setSelected(newState);
            }
        };
        atmosphereButton = new SButton((SwingButtonAction) atmosphereAction);
        atmosphereButton.setSelected(core.isAtmosphereEnabled());

        nebulaNameAction = new SwingButtonAction("NebulasN", "bt_nebula.png", KeyStroke.getKeyStroke(KeyEvent.VK_N, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !core.isNebulaHintEnabled();
                core.setNebulaHints(newState);
                nebulaNameButton.setSelected(newState);
            }
        };
        nebulaNameButton = new SButton((SwingButtonAction) nebulaNameAction);
        nebulaNameButton.setSelected(core.isNebulaHintEnabled());

        helpAction = new SwingButtonAction("HelpH", "bt_help.png", KeyStroke.getKeyStroke(KeyEvent.VK_H, 0)) {
            public void actionPerformed(ActionEvent e1) {
                Translator translator = core.getTranslator();
                JOptionPane.showMessageDialog(mainFrame, translator.translate("Movement & selection:\n"
                        + "Arrow Keys       : Change viewing RA/DE\n" +
                        "Page Up/Down     : Zoom\n" +
                        "CTRL+Up/Down     : Zoom\n" +
                        "Left Click       : Select Star\n" +
                        "Right Click      : Clear Pointer\n" +
                        "CTRL+Left Click  : Clear Pointer\n" +
                        "SPACE : Center On Selected Object\n" +
                        "ENTER : Equatorial/Altazimuthal mount\n" +
                        "CTRL + S : Take a Screenshot\n" +
                        "C   : Drawing of the Constellations\n" +
                        "V   : Names of the Constellations\n" +
                        "R   : Constellation Art\n" +
                        "E   : Equatorial Grid\n" +
                        "Z   : Azimuthal Grid\n" +
                        "N   : Nebulas\n" +
                        "P   : Planet Finder\n" +
                        "G   : Ground\n" +
                        "F   : Fog\n" +
                        "Q   : Cardinal Points\n" +
                        "A   : Atmosphere\n" +
                        "H   : Help\n" +
                        "4   : Ecliptic Line\n" +
                        "5   : Equator Line\n" +
                        "T   : Object Tracking\n" +
                        "S   : Stars\n" +
                        "I   : About Stellarium\n" +
                        "F1  : Toggle fullscreen if possible.\n" +
                        "CTRL+Q : Quit\n"), translator.translate("Help"), JOptionPane.INFORMATION_MESSAGE);
                helpButton.setSelected(false);  // Action is performed, so reset the button state
            }
        };
        helpButton = new SButton((SwingButtonAction) helpAction);

        equatorialModeAction = new SwingButtonAction("EquatorialAltazimuthalMountENTER", "bt_follow.png", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
            public void actionPerformed(ActionEvent e1) {
                core.getNavigation().switchViewingMode();
                equatorialModeButton.setSelected(core.getMountMode() == StelCore.MOUNT_MODE.EQUATORIAL);
            }
        };
        equatorialModeButton = new SButton((SwingButtonAction) equatorialModeAction);
        boolean isEquatorial = core.getMountMode() == StelCore.MOUNT_MODE.EQUATORIAL;
        equatorialModeButton.setSelected(isEquatorial);

        configAction = new SwingButtonAction("ConfigurationWindow", "bt_config.png", KeyStroke.getKeyStroke(KeyEvent.VK_1, 0)) {
            public void actionPerformed(ActionEvent e1) {
                boolean newState = !configWin.isVisible();
                configWin.setVisible(newState);
                configButton.setSelected(newState);
            }
        };
        configButton = new SButton((SwingButtonAction) configAction);

        chartVisionAction = new SwingButtonAction("Chart vision mode", "bt_chart.png", KeyStroke.getKeyStroke(KeyEvent.VK_2, 0)) {
            public void actionPerformed(ActionEvent e1) {
                if (app.getVisionModeChart() != chartButton.isSelected()) {
                    if (nightModeButton.isSelected()) {
                        app.setVisionModeChart();
                    } else {
                        app.setVisionModeNormal();
                    }
                }
            }
        };
        chartButton = new SButton((SwingButtonAction) chartVisionAction);
        chartButton.setSelected(app.getVisionModeChart());

        nightModeAction = new SwingButtonAction("NightRedMode", "bt_night.png", KeyStroke.getKeyStroke(KeyEvent.VK_3, 0)) {
            public void actionPerformed(ActionEvent e1) {
                if (app.getVisionModeNight() != nightModeButton.isSelected()) {
                    if (nightModeButton.isSelected()) {
                        app.setVisionModeNight();
                    } else {
                        app.setVisionModeNormal();
                    }
                }
            }
        };
        nightModeButton = new SButton((SwingButtonAction) nightModeAction);
        nightModeButton.setSelected(app.getVisionModeNight());

        quitAction = new SwingButtonAction(ResourceLocatorUtil.isMacOSX() ? "QuitCMDQ" : "QuitCTRLQ", "bt_quit.png", KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK)) {
            public void actionPerformed(ActionEvent e1) {
                app.quit();
            }
        };
        quitButton = new SButton((SwingButtonAction) quitAction);

        searchAction = new SwingButtonAction("SearchForObject", "bt_search.png", KeyStroke.getKeyStroke(KeyEvent.VK_S, 0)) {
            public void actionPerformed(ActionEvent e1) {
                searchWin.setVisible(!searchWin.isVisible());
            }
        };
        searchButton = new SButton((SwingButtonAction) searchAction);

        scriptButton = new EditBox();
        scriptButton.setAutoFocus(false);
        scriptButton.setSize(299, 24);
        StelCallback cbEditScriptKeyCallback = new StelCallback() {
            public void execute() {
                cbEditScriptKey();
            }
        };
        scriptButton.setOnKeyCallback(cbEditScriptKeyCallback);
        StelCallback cbEditScriptExecuteCallback = new StelCallback() {
            public void execute() throws StellariumException {
                cbEditScriptExecute();
            }
        };
        scriptButton.setOnReturnKeyCallback(cbEditScriptExecuteCallback);

        gotoAction = new SwingButtonAction("GotoSelectedObjectSPACE", "bt_goto.png", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)) {
            public void actionPerformed(ActionEvent e1) {
                core.gotoSelectedObject();
                core.setTracking(true);
                gotoButton.setSelected(false);  // Goto action is now performed, reset button state
            }
        };
        gotoButton = new SButton((SwingButtonAction) gotoAction);

        toggleButtonsContainer.add(constellationDrawButton);
        toggleButtonsContainer.add(constellationNameButton);
        toggleButtonsContainer.add(constellationArtButton);
        toggleButtonsContainer.add(azimuthGridButton);
        toggleButtonsContainer.add(equatorGridButton);
        toggleButtonsContainer.add(groundButton);
        toggleButtonsContainer.add(cardinalsButton);
        toggleButtonsContainer.add(atmosphereButton);
        toggleButtonsContainer.add(nebulaNameButton);
        toggleButtonsContainer.add(equatorialModeButton);
        toggleButtonsContainer.add(gotoButton);

        if (conf.getBoolean(IniFileParser.GUI_SECTION, IniFileParser.SHOW_FLIP_BUTTONS, false)) {
            horizontalSplitAction = new SwingButtonAction("FlipHorizontally", "bt_flip_horz.png", KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0)) {
                public void actionPerformed(ActionEvent e1) {
                    core.setFlipHorz(horizontalFlipButton.isSelected());
                }
            };
            horizontalFlipButton = new SButton((SwingButtonAction) horizontalSplitAction);
            toggleButtonsContainer.add(horizontalFlipButton);

            verticalSplitAction = new SwingButtonAction("FlipVertically", "bt_flip_vert.png", KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0)) {
                public void actionPerformed(ActionEvent e1) {
                    core.setFlipVert(verticalFlipButton.isSelected());
                }
            };
            verticalFlipButton = new SButton((SwingButtonAction) verticalSplitAction);
            toggleButtonsContainer.add(verticalFlipButton);
        }

        // TODO(JBE): Add EditBox as JButton
        /*  btFlagCtr.add(btScript);
        if (!flagShowScriptBar) {
            btScript.setVisible(false);
        } else {
            x += UI_SCRIPT_BAR;
            x += UI_PADDING;
        }*/

        toggleButtonsContainer.add(searchButton);
        toggleButtonsContainer.add(configButton);
        toggleButtonsContainer.add(chartButton);
        toggleButtonsContainer.add(nightModeButton);
        toggleButtonsContainer.add(helpButton);
        toggleButtonsContainer.add(quitButton);

        return toggleButtonsContainer;
    }

    /**
     * Create the button panel in the lower right corner
     *
     * @return The toolbar for containing time control buttons.
     */
    private JToolBar createTimeControlButtons() throws StellariumException {
        JToolBar timeControlToolBar = new ExtendedToolBar();

        timeDecreaseAction = new SwingButtonAction("DecTimeAction", "bt_rwd.png", KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0)) {
            public void actionPerformed(ActionEvent e1) {
                btDecTimeSpeedCb();
            }
        };
        timeSpeedDecreaseButton = new SButton((SwingButtonAction) timeDecreaseAction);

        realTimeAction = new SwingButtonAction("RealTimeAction", "bt_realtime.png", KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0)) {
            public void actionPerformed(ActionEvent e1) {
                btRealTimeSpeedCb();
            }
        };
        realTimeButton = new SButton((SwingButtonAction) realTimeAction);

        timeIncreaseAction = new SwingButtonAction("IncTimeAction", "bt_fwd.png", KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0)) {
            public void actionPerformed(ActionEvent e1) {
                btIncTimeSpeedCb();
            }
        };
        timeSpeedIncreaseButton = new SButton((SwingButtonAction) timeIncreaseAction);

        timeNowAction = new SwingButtonAction("Now", "bt_now.png", KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0)) {
            public void actionPerformed(ActionEvent e1) {
                btTimeNowCb();
            }
        };
        timeNowButton = new SButton((SwingButtonAction) timeNowAction);

        timeControlToolBar.add(timeSpeedDecreaseButton);
        timeControlToolBar.add(realTimeButton);
        timeControlToolBar.add(timeSpeedIncreaseButton);
        timeControlToolBar.add(timeNowButton);

        return timeControlToolBar;
    }

    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        app.distorter.distortXY(x, y);
        // Turn if the mouse is at the edge of the screen.
        // unless config asks otherwise
        if (mouseMoveEnabled) {
            if (x == 0) {
                core.turnLeft(true);
                isMouseMovingHoriz = true;
            } else if (x == getScreenW() - 1) {
                core.turnRight(true);
                isMouseMovingHoriz = true;
            } else if (isMouseMovingHoriz) {
                core.turnLeft(false);
                isMouseMovingHoriz = false;
            }

            if (y == 0) {
                core.turnUp(true);
                isMouseMovingVert = true;
            } else if (y == getScreenH() - 1) {
                core.turnDown(true);
                isMouseMovingVert = true;
            } else if (isMouseMovingVert) {
                core.turnUp(false);
                isMouseMovingVert = false;
            }
            e.consume();
        }

        // Do not allow use of mouse while script is playing
        // otherwise script can get confused
        if (app.getScripts().isPlaying()) {
            return;
        }

        showCursor(true);
        mouseTimeLeft = mouseCursorTimeout * 1000;
    }

    /**
     *
     */
    public void mouseClicked(MouseEvent e) {
        // Do not allow use of mouse while script is playing
        // otherwise script can get confused
        if (!app.getScripts().isPlaying()) {
            // Make sure object pointer is turned on (script may have turned off)
            core.setObjectPointer(true);

            showCursor(true);
            mouseTimeLeft = mouseCursorTimeout * 1000;
        }

        if (!e.isConsumed()) {
            core.findAndSelect(e.getX(), e.getY());
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        double aimedFOV = core.getAimFov();
        double delta = app.getMouseZoom() * aimedFOV / 60.0;
        if (e.getWheelRotation() < 0) {
            delta = -delta;
        }
        core.zoomTo(aimedFOV + delta, 0.1);
        e.consume();
    }

    /**
     *
     */
    public void keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.getKeyCode();

        if (isShowTextUiMenu()) {
            tui.tuiKeypressed(keyEvent);
            if (!keyEvent.isConsumed()) {
                uiKeyPressed(keyEvent);
            }
        } else {
            uiKeyPressed(keyEvent);
            if (!keyEvent.isConsumed()) {
                // Direction and zoom deplacements
                switch (key) {
                    case KeyEvent.VK_LEFT:
                        core.turnLeft(true);
                        keyEvent.consume();
                        break;
                    case KeyEvent.VK_RIGHT:
                        core.turnRight(true);
                        keyEvent.consume();
                        break;
                    case KeyEvent.VK_UP:
                        if ((keyEvent.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            core.zoomIn(true);
                        } else {
                            core.turnUp(true);
                            keyEvent.consume();
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if ((keyEvent.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            core.zoomOut(true);
                            keyEvent.consume();
                        } else {
                            core.turnDown(true);
                            keyEvent.consume();
                        }
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        core.zoomIn(true);
                        keyEvent.consume();
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        core.zoomOut(true);
                        keyEvent.consume();
                        break;
                    case KeyEvent.VK_Q:
                        if ((keyEvent.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                            keyEvent.consume();
                            app.quit();
                        }
                        break;
                }
            }
        }
    }

    private void uiKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        //        if (e.isConsumed()) {
        //            return;
        //        }

        if (key == KeyEvent.VK_Q) {
            if (e.isControlDown()) {
                e.consume();
                app.quit();
            }
        } else
            try {
                StelCommandInterface commander = app.getCommander();
                // if script is running, only script control keys are accessible
                // to pause/resume/cancel the script
                // (otherwise script could get very confused by user interaction)
                ScriptMgr scripts = app.getScripts();
                if (scripts.isPlaying()) {
                    scriptKeyPressed(e, key);

                    if (!scripts.isAllowUI()) {
                        return;//false;  // only limited user interaction allowed with script
                    }
                } else {
                    app.setTimeMultiplier(1);// if no script in progress always real time

                    // normal time controls here (taken over for script control above if playing a script)
                    switch (key) {
                        case KeyEvent.VK_K:
                            commander.executeCommand("timerate rate 1");
                            e.consume();
                            break;
                        case KeyEvent.VK_1:
                            commander.executeCommand("timerate action increment");
                            e.consume();
                            break;
                        case KeyEvent.VK_J:
                            commander.executeCommand("timerate action decrement");
                            e.consume();
                            break;
                        case KeyEvent.VK_6:
                            commander.executeCommand("timerate action pause");
                            e.consume();
                            break;
                        case KeyEvent.VK_7:
                            commander.executeCommand("timerate rate 0");
                            e.consume();
                            break;
                        case KeyEvent.VK_8:
                            commander.executeCommand("date load preset");
                            e.consume();
                            break;
                    }
                }

                if (key == KeyEvent.VK_R && e.isControlDown()) {
                    if (scripts.isRecording()) {
                        commander.executeCommand("script action cancelrecord");
                        showMessage("Command recording stopped.", 3000);
                        e.consume();
                    } else {
                        commander.executeCommand("script action record");

                        if (scripts.isRecording()) {
                            showMessage("Recording commands to script file:\n"
                                    + scripts.getRecordFileName() + "\n\n"
                                    + "Hit CTRL-R again to stop.\n", 4000);
                        } else {
                            showMessage("Error: Unable to open script file to record commands.", 3000);
                        }
                        e.consume();
                    }
                    return;
                }

                switch (key) {

                    case KeyEvent.VK_ESCAPE:
                        // RFE 1310384, ESC closes dialogs
                        // close search mode
                        searchWin.setVisible(false);

                        // close config dialog
                        configWin.setVisible(false);

                        // close information dialog
                        showMessage("<html><p><font size=\"+3\">" + Main.APP_NAME + "</font></p><br>"
                                + "<p>" + Main.VERSION + ", " + Main.BUILD_DATE + "</p><br>"
                                + "<p>Copyright (c) 2000-2006 Fabien Chéreau for the orginal C++ version<br>"
                                + "Copyright (c) 2006-2009 Jerome Beau, Fred Simon for the Java version</p><br>"
                                + "<p>Please check last version and send bug report & comments <br>"
                                + "on <a href=\"http://stellarium4java.sf.net\">SJ4 web page</a><p><br>"
                                + "<p>This program is free software; you can redistribute it and/or <br>"
                                + "modify it under the terms of the GNU General Public License <br>"
                                + "as published by the Free Software Foundation; either version 2 <br>"
                                + "of the License, or (at your option) any later version.</p><br>"
                                + "<p>This program is distributed in the hope that it will be useful, but <br>"
                                + "WITHOUT ANY WARRANTY; without even the implied <br>"
                                + "warranty of MERCHANTABILITY or FITNESS FOR A <br>"
                                + "PARTICULAR PURPOSE.  See the GNU General Public <br>"
                                + "License for more details.<p><br>"
                                + "<p>You should have received a copy of the GNU General Public <br>"
                                + "License along with this program; if not, write to the <br>"
                                + "Free Software Foundation, Inc., 59 Temple Place - Suite 330 <br>"
                                + "Boston, MA  02111-1307, USA.</p></html>", 10000);
                        // END RFE 1310384
                        e.consume();
                        break;

                    case KeyEvent.VK_0:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(0);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_1:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(1);
                        } else {
                            configWin.setVisible(!configWin.isVisible());
                        }
                        e.consume();
                        break;

                    case KeyEvent.VK_2:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(2);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_3:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(3);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_4:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(4);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_COMMA:
                        if (!core.isEclipticLineEnabled()) {
                            commander.executeCommand("flag ecliptic_line on");
                        } else if (!core.getFlagPlanetsTrails()) {
                            commander.executeCommand("flag object_trails on");
                        } else {
                            commander.executeCommand("flag object_trails off");
                            commander.executeCommand("flag ecliptic_line off");
                        }
                        e.consume();
                        break;

                    case KeyEvent.VK_5:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(5);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_6:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(6);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_7:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(7);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_8:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(8);
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_9:
                        if (!e.isControlDown()) {
                            core.telescopeGoto(9);
                            e.consume();
                        } else {
                            final int zhr = core.getMeteorsRate();
                            if (zhr <= 10) {
                                commander.executeCommand("meteors zhr 80");// standard Perseids rate
                            } else if (zhr <= 80) {
                                commander.executeCommand("meteors zhr 10000");// exceptional Leonid rate
                            } else if (zhr <= 10000) {
                                commander.executeCommand("meteors zhr 144000");// highest ever recorded ZHR (1966 Leonids)
                            } else {
                                commander.executeCommand("meteors zhr 10");// set to ***default base rate (10 is normal, 0 would be none)
                            }
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_LEFT_PARENTHESIS:
                        commander.executeCommand("date relative -7");
                        e.consume();
                        break;
                    case KeyEvent.VK_RIGHT_PARENTHESIS:
                        commander.executeCommand("date relative 7");
                        e.consume();
                        break;
                    case KeyEvent.VK_SLASH:
                        if (e.isControlDown()) {
                            commander.executeCommand("autozoom direction out");
                        } else {
                            commander.executeCommand("autozoom direction in");
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_BACK_SLASH:
                        commander.executeCommand("autozoom direction out");
                        e.consume();
                        break;
                    case KeyEvent.VK_X:
                        commander.executeCommand("flag show_tui_datetime toggle");
                        commander.executeCommand("flag show_tui_short_obj_info toggle");
                        e.consume();
                        break;
                }
            } catch (StellariumException e1) {
                logger.severe("Could not execute select command:");
                e1.printStackTrace();// TODO(JBE): Implement better error propagation
            }
        if (!e.isConsumed()) {
            //e.setModifiers(e.getModifiers() | KeyEvent.ALT_MASK);
            toolBar.keyPressed(e);
        }
    }

    public void keyReleased(KeyEvent keyEvent) {
        int key = keyEvent.getKeyCode();
        if (!keyEvent.isConsumed()) {
            // When a deplacement key is released stop mooving
            switch (key) {
                case KeyEvent.VK_LEFT:
                    core.turnLeft(false);
                    keyEvent.consume();
                    break;
                case KeyEvent.VK_RIGHT:
                    core.turnRight(false);
                    keyEvent.consume();
                    break;
                case KeyEvent.VK_PAGE_UP:
                    core.zoomIn(false);
                    keyEvent.consume();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    core.zoomOut(false);
                    keyEvent.consume();
                    break;
                case KeyEvent.VK_UP:
                    if ((keyEvent.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                        core.zoomIn(false);
                    } else {
                        core.turnUp(false);
                    }
                    keyEvent.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    if ((keyEvent.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
                        core.zoomOut(false);
                    } else {
                        core.turnDown(false);
                    }
                    keyEvent.consume();
                    break;
            }
        }
    }

    public void keyTyped(KeyEvent e) {
        // See keyPressed
    }

    public void mouseReleased(MouseEvent e) {
        dragging = false;
    }

    public void mousePressed(MouseEvent mouseEvent) {
        // Not handled here
    }

    public void mouseEntered(MouseEvent mouseEvent) {
        // Not handled here
    }

    public void mouseExited(MouseEvent mouseEvent) {
        // Not handled here
    }

    public void mouseDragged(MouseEvent e) {
        //System.out.println("mouseDragged");
        int x = e.getX();
        int y = e.getY();
        if (dragging && (x != previousX || y != previousY)) {
            //core.setFlagTracking(false);
            core.dragView(previousX, previousY, x, y);
        } else {
            dragging = true;
        }
        previousX = x;
        previousY = y;
    }

    protected void updateWidgets(int deltaTime) {
        // handle mouse cursor timeout
        if (mouseCursorTimeout > 0) {
            if (mouseTimeLeft > deltaTime) {
                mouseTimeLeft -= deltaTime;
            } else {
                mouseTimeLeft = 0;
                showCursor(false);
            }
        }

        // update message win
        //        messageWin.update(deltaTime);

        updateTimeControls();

        if (configWin.isVisible()) {
            configWin.updateValues();
        }
    }

    protected void updateTimeControls() {
        timeControlButtons.setVisible(toolBarEnabled);
        realTimeButton.setSelected(Math.abs(core.getTimeSpeed() - NavigatorIfc.JD_SECOND) < 0.000001);
        timeSpeedIncreaseButton.setSelected(core.getTimeSpeed() - NavigatorIfc.JD_SECOND > 0.0001);
        timeSpeedDecreaseButton.setSelected(core.getTimeSpeed() - NavigatorIfc.JD_SECOND < -0.0001);
        // cache last time to prevent to much slow system call
        double julianDay = core.getJulianDay();
        if (Math.abs(lastJD - julianDay) > NavigatorIfc.JD_SECOND / 4) {
            timeNowButton.setSelected(Math.abs(julianDay - JulianDay.getJulianFromSys()) < NavigatorIfc.JD_SECOND);
            lastJD = julianDay;
        }
    }

    public void updateInfoSelectString() {
        if (showSelectedObjectInfo && core.getFlagHasSelected()) {
            Color infoColor;
            if (app.getVisionModeNight()) {
                infoColor = new Color(1.0f, 0.2f, 0.2f);
            } else {
                infoColor = core.getSelectedObjectInfoColor();
            }
            int topBarHeight = topBar.isVisible() ? topBar.getHeight() : 0;
            baseFont.print(7, glCanvas.getHeight() - topBarHeight - 7, core.getSelectedObjectInfo(), true, infoColor);
        }
    }

    public void autoCompleteSearchedObject(String objectName) {
        searchWin.setAutoCompleteOptions(core.listMatchingObjectsI18n(objectName, 5));
    }

    public void gotoSearchedObject(String objectName) {
        if (core.findAndSelectI18n(objectName)) {
            core.gotoSelectedObject();
            core.setTracking(true);
        } else {
            JOptionPane.showMessageDialog(searchWin, objectName + " is unknown!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void setupMainFrame(IniFileParser conf) {
        device = getFrame().getGraphicsConfiguration().getDevice();

        if (fullScreen) {
            DisplayMode[] displayModes = device.getDisplayModes();
            for (DisplayMode displayMode1 : displayModes) {
                if (displayMode1.getWidth() == screenW && displayMode1.getHeight() == screenH) {
                    displayMode = displayMode1;
                    break;
                }
            }
            if (displayMode == null) {
                displayMode = device.getDisplayMode();
            }

            screenW = displayMode.getWidth();
            screenH = displayMode.getHeight();
        }

        getFrame().addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // TODO(JBE): Stop UI component
                //stop();
                app.quit();
            }
        });
        Toolkit.getDefaultToolkit().setDynamicLayout(true);

        getFrame().getContentPane().setLayout(new BorderLayout());

        glCanvas.addGLEventListener(this);

        if (conf.getBoolean(IniFileParser.VIDEO_SECTION, IniFileParser.FULLSCREEN)) {
            getFrame().setUndecorated(true);
        }

        //        jFrame.getContentPane().add(statusBar, BorderLayout.SOUTH);
        createDesktop(app.getConf(), glCanvas);

        getFrame().setSize(conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.SCREEN_WIDTH), conf.getInt(IniFileParser.VIDEO_SECTION, IniFileParser.SCREEN_HEIGHT));
    }

    private JFrame getFrame() {
        if (mainFrame == null) {
            mainFrame = new JFrame(Main.APP_NAME);
        }
        return mainFrame;
    }

    public static LoadingBar getLoadingBar() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = graphicsEnvironment.getDefaultScreenDevice();
        DisplayMode displayMode = device.getDisplayMode();
        return new LoadingBar("newlogo.png", displayMode.getWidth(), displayMode.getHeight(), Main.VERSION, 15, 320, 140);
    }

    protected SButton timeNowButton;

    protected SButton timeSpeedIncreaseButton;

    /**
     * Button to set real time speed
     */
    protected SButton realTimeButton;

    protected SButton quitButton;

    protected SButton searchButton;

    protected SButton gotoButton;

    protected SButton horizontalFlipButton;

    protected SButton verticalFlipButton;

    protected SButton timeSpeedDecreaseButton;
}