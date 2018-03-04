package org.stellarium.ui.dialog;

/*import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;*/

import org.stellarium.*;
import org.stellarium.data.DataFileUtil;
import org.stellarium.data.IniFileParser;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.landscape.Landscape;
import org.stellarium.projector.Projector;
import org.stellarium.projector.ViewportDistorter;
import org.stellarium.ui.StelUI;
import org.stellarium.ui.SwingUI;
import org.stellarium.ui.components.*;
import org.stellarium.ui.render.STexture;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author <a href="mailto:javarome@javarome.net">J&eacute;r&ocirc;me Beau</a>
 * @version 10 dec. 2006 23:43:22
 */
public class ConfigDialog extends JDialog {
    private final StelApp app;

    // Rendering options
    protected JCheckBox starsCbx;

    protected JCheckBox starNamesCbx;

    protected JSpinner maxMagStarName;

    protected JCheckBox starTwinkleCbx;

    protected JSpinner starTwinkleAmount;

    protected JCheckBox constellationCbx;

    protected JCheckBox constellationNameCbx;

    protected JCheckBox selConstellationCbx;

    protected CheckBox nebulasCbx;

    protected JCheckBox nebulasNamesCbx;

    protected JCheckBox nebulasNoTextureCbx;

    protected JSpinner maxMagNebulaName;

    protected JCheckBox planetsCbx;

    protected JCheckBox planetsHintsCbx;

    protected JCheckBox moonX4Cbx;

    protected JCheckBox equatorGridCbx;

    protected JCheckBox azimuthGridCbx;

    protected JCheckBox equatorCbx;

    protected JCheckBox eclipticCbx;

    protected JCheckBox groundCbx;

    protected JCheckBox cardinalCbx;

    protected JCheckBox atmosphereCbx;

    protected JCheckBox fogCbx;

    protected MapPicture earthMap;

    JLabel labelMapLocation;

    JLabel labelMapPointer;

    protected JSpinner latIncDec, longIncDec;

    JSpinner altIncDec;

    // Date & Time options
    protected SpinnerDateModel timeCurrent;

    CheckBox systemTzCbx;

    TimeZoneItem tzSelector;

    protected JLabel systemTimeZoneLabel2;

    protected JLabel timeSpeedLabel2;

    // Video Options
    protected LabeledCheckBox fisheyeProjectionCbx;

    protected JCheckBox diskViewportCbx;

    protected JList screenSizeList;

    private JCheckBox constellationBoundariesCbx;

    private JLabel meteorlbl;

    JRadioButton meteorRate10;

    JRadioButton meteorRate80;

    JRadioButton meteorRate10000;

    JRadioButton meteorRate144000;

    LabeledCheckBox meteorRatePerseids;

    private JList projectionSl;
    private JSpinner.DateEditor dateEditor;
    private Calendar currentDate = Calendar.getInstance();
    protected final Logger logger;


    public ConfigDialog(JFrame owner, StelApp app, Logger parentLogger) throws HeadlessException {
        super(owner, app.getCore().getTranslator().translate("Configuration"));
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }

        this.app = app;
        buildUI();
        pack();
    }

    protected void buildUI() {
        //configWin.setClosable(true);
        //configWin.setOpaque(opaqueGUI);

        JTabbedPane configTabCtr = new JTabbedPane();

        // Rendering options
        JPanel renderTab = createRenderConfigTab();

        // Date & Time options
        JPanel timeTab = createTimeConfigTab();

        // Location options
        JPanel locationTab = createLocationConfigTab();

        // Video Options
        JPanel videoTab = createVideoConfigTab();

        // Landscape options
        JPanel landscapeTab = createLandscapeConfigTab();

        // Language options
        JPanel languageTab = createLanguageConfigTab();

        Translator translator = getTranslator();
        configTabCtr.addTab(translator.translate("Language"), languageTab);
        configTabCtr.addTab(translator.translate("DateTime"), timeTab);
        configTabCtr.addTab(translator.translate("Location"), locationTab);
        configTabCtr.addTab(translator.translate("Landscapes"), landscapeTab);
        configTabCtr.addTab(translator.translate("Video"), videoTab);
        configTabCtr.addTab(translator.translate("Rendering"), renderTab);
        add(configTabCtr);
    }

    public void pack() {
        super.pack();
        earthMap.setSize(getWidth() - 10, 250);
    }

    private Translator getTranslator() {
        return getCore().getTranslator();
    }

    private JPanel createRenderConfigTab() {
        JPanel renderTab = new JPanel(false);
        renderTab.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.weighty = 0.5;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridheight = 3;
        StelPicture pstar = new StelPicture("halo.png", 32, 32);
        renderTab.add(pstar, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        starsCbx = new JCheckBox(getTranslator().translate("Stars"), getCore().isStarEnabled());
        starsCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // TODO(JBE): Encapsulate this (and other similar) in a common Action
                synchronized (app) {
                    app.getCommander().executeCommand("flag stars ", starsCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(starsCbx, constraints);

        constraints.gridy++;
        starNamesCbx = new JCheckBox(getTranslator().translate("StarNamesUpToMag"), getCore().isStarNameEnabled());
        starNamesCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean selected = starNamesCbx.isSelected();
                synchronized (app) {
                    app.getCommander().executeCommand("flag star_names ", selected);
                    app.notify();
                }
                maxMagStarName.setEnabled(selected);
            }
        });
        renderTab.add(starNamesCbx, constraints);

        constraints.gridx = 2;
        SpinnerNumberModel magnitudeSpinerModel = new SpinnerNumberModel(getCore().getMaxMagStarName(), -1.5, 9, 0.5);
        maxMagStarName = new JSpinner(magnitudeSpinerModel);
        maxMagStarName.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("set max_mag_star_name ", maxMagStarName.getValue().toString());
                    app.notify();
                }
            }
        });
        renderTab.add(maxMagStarName, constraints);

        constraints.gridx = 1;
        constraints.gridy++;
        starTwinkleCbx = new JCheckBox(getTranslator().translate("StarTwinkleAmount"), getCore().isStarTwinkleEnabled());
        starTwinkleCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean selected = starTwinkleCbx.isSelected();
                synchronized (app) {
                    app.getCommander().executeCommand("flag star_twinkle ", selected);
                    app.notify();
                }
                starTwinkleAmount.setEnabled(selected);
            }
        });
        renderTab.add(starTwinkleCbx, constraints);

        constraints.gridx = 2;
        SpinnerNumberModel twinkleSpinerModel = new SpinnerNumberModel(getCore().getStarTwinkleAmount(), 0, 0.6, 0.1);
        starTwinkleAmount = new JSpinner(twinkleSpinerModel);
        starTwinkleAmount.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("set star_twinkle_amount ", starTwinkleAmount.getValue().toString());
                    app.notify();
                }
            }
        });
        renderTab.add(starTwinkleAmount, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridheight = 4;
        StelPicture pconstell = new StelPicture("bt_constellations.png", 32, 32);
        renderTab.add(pconstell, constraints);

        constraints.gridx = 1;
        constraints.gridheight = 1;
        constellationCbx = new JCheckBox(getTranslator().translate("ConstellationsLines"), false);
        constellationCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag constellation_drawing ", constellationCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(constellationCbx, constraints);

        constraints.gridy++;
        constellationNameCbx = new JCheckBox(getTranslator().translate("ConstellationsNames"), false);
        constellationNameCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag constellation_names ", constellationNameCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(constellationNameCbx, constraints);

        constraints.gridy++;
        constellationBoundariesCbx = new JCheckBox(getTranslator().translate("ConstellationsBoundaries"), false);
        constellationBoundariesCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag constellation_boundaries ", constellationBoundariesCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(constellationBoundariesCbx, constraints);

        constraints.gridy++;
        selConstellationCbx = new JCheckBox(getTranslator().translate("SelectedConstellationOnly"), false);
        selConstellationCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag constellation_pick ", selConstellationCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(selConstellationCbx, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridheight = 2;
        StelPicture pneb = new StelPicture("bt_nebula.png", 32, 32);
        pneb.setBackground(Color.GREEN);
        renderTab.add(pneb, constraints);

        constraints.gridx = 1;
        constraints.gridheight = 1;
        nebulasNamesCbx = new JCheckBox(getTranslator().translate("NebulasNamesUpToMag"), false);
        nebulasNamesCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean selected = nebulasNamesCbx.isSelected();
                synchronized (app) {
                    app.getCommander().executeCommand("flag nebula_names ", selected);
                    app.notify();
                }
                maxMagNebulaName.setEnabled(selected);
            }
        });
        renderTab.add(nebulasNamesCbx, constraints);

        constraints.gridx = 2;
        SpinnerNumberModel nebulaMagnitudeSpinerModel = new SpinnerNumberModel(getCore().getNebulaMaxMagHints(), 0, 12, 0.5);
        maxMagNebulaName = new JSpinner(nebulaMagnitudeSpinerModel);
        maxMagNebulaName.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("set max_mag_nebula_name ", maxMagNebulaName.getValue().toString());
                    app.notify();
                }
            }
        });
        renderTab.add(maxMagNebulaName, constraints);

        constraints.gridx = 1;
        constraints.gridy++;
        nebulasNoTextureCbx = new JCheckBox(getTranslator().translate("AlsoDisplayNebulasWithoutTextures"), false);
        nebulasNoTextureCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    getCore().setFlagNebulaDisplayNoTexture(nebulasNoTextureCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(nebulasNoTextureCbx, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridheight = 2;
        StelPicture pplan = new StelPicture("bt_planet.png", 32, 32);
        renderTab.add(pplan, constraints);

        constraints.gridx = 1;
        constraints.gridheight = 1;
        planetsCbx = new JCheckBox(getTranslator().translate("Planets"), false);
        planetsCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag planets ", planetsCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(planetsCbx, constraints);

        constraints.gridx = 2;
        moonX4Cbx = new JCheckBox(getTranslator().translate("MoonScale"), false);
        moonX4Cbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag moon_scaled ", moonX4Cbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(moonX4Cbx, constraints);

        constraints.gridx = 1;
        constraints.gridy++;
        planetsHintsCbx = new JCheckBox(getTranslator().translate("PlanetsHints"), false);
        planetsHintsCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag planet_names ", planetsHintsCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(planetsHintsCbx, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridheight = 2;
        StelPicture pgrid = new StelPicture("bt_eqgrid.png", 32, 32);
        renderTab.add(pgrid, constraints);

        constraints.gridheight = 1;
        constraints.gridx = 1;
        equatorGridCbx = new JCheckBox(getTranslator().translate("EquatorialGrid"), false);
        equatorGridCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag equatorial_grid ", equatorGridCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(equatorGridCbx, constraints);

        constraints.gridx = 2;
        azimuthGridCbx = new JCheckBox(getTranslator().translate("AzimuthalGrid"), false);
        azimuthGridCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag azimuthal_grid ", azimuthGridCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(azimuthGridCbx, constraints);

        constraints.gridy++;
        constraints.gridx = 1;
        equatorCbx = new JCheckBox(getTranslator().translate("EquatorLine"), false);
        equatorCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag equator_line ", equatorCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(equatorCbx, constraints);

        constraints.gridx = 2;
        eclipticCbx = new JCheckBox(getTranslator().translate("EclipticLine"), false);
        eclipticCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag ecliptic_line ", eclipticCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(eclipticCbx, constraints);

        constraints.gridx = 0;
        constraints.gridheight = 2;
        constraints.gridy++;
        StelPicture pground = new StelPicture("bt_ground.png", 32, 32);
        renderTab.add(pground, constraints);

        constraints.gridheight = 1;
        constraints.gridx = 1;
        groundCbx = new JCheckBox(getTranslator().translate("Ground"), false);
        groundCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag landscape ", groundCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(groundCbx, constraints);

        constraints.gridx = 2;
        cardinalCbx = new JCheckBox(getTranslator().translate("CardinalPoints"), false);
        cardinalCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag cardinal_points ", cardinalCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(cardinalCbx, constraints);

        constraints.gridy++;
        constraints.gridx = 1;
        atmosphereCbx = new JCheckBox(getTranslator().translate("Atmosphere"), false);
        atmosphereCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag atmosphere ", atmosphereCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(atmosphereCbx, constraints);

        constraints.gridx = 2;
        fogCbx = new JCheckBox(getTranslator().translate("Fog"), false);
        fogCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                synchronized (app) {
                    app.getCommander().executeCommand("flag fog ", fogCbx.isSelected());
                    app.notify();
                }
            }
        });
        renderTab.add(fogCbx, constraints);

        constraints.gridy++;
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        JPanel meteorPanel = new JPanel();
        {
            meteorlbl = new JLabel("-");
            meteorPanel.add(meteorlbl);

            ButtonGroup buttonGroup = new ButtonGroup();
            meteorRate10 = new JRadioButton("10", false);
            buttonGroup.add(meteorRate10);
            meteorRate10.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (meteorRate10.isSelected() && getCore().getMeteorsRate() != 10) {
                        synchronized (app) {
                            app.getCommander().executeCommand("meteors zhr 10");
                            app.notify();
                        }
                    }
                }
            });
            meteorPanel.add(meteorRate10);

            meteorRate80 = new JRadioButton("80", false);
            buttonGroup.add(meteorRate80);
            meteorRate80.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (meteorRate80.isSelected() && getCore().getMeteorsRate() != 80) {
                        synchronized (app) {
                            app.getCommander().executeCommand("meteors zhr 80");
                            app.notify();
                        }
                    }
                }
            });
            meteorPanel.add(meteorRate80);

            meteorRate10000 = new JRadioButton("10000", false);
            buttonGroup.add(meteorRate10000);
            meteorRate10000.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (meteorRate10000.isSelected() && getCore().getMeteorsRate() != 10000) {
                        synchronized (app) {
                            app.getCommander().executeCommand("meteors zhr 10000");
                            app.notify();
                        }
                    }
                }
            });
            meteorPanel.add(meteorRate10000);

            meteorRate144000 = new JRadioButton("144000", false);
            buttonGroup.add(meteorRate144000);
            meteorRate144000.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (meteorRate144000.isSelected() && getCore().getMeteorsRate() != 144000) {
                        synchronized (app) {
                            app.getCommander().executeCommand("meteors zhr 144000");
                            app.notify();
                        }
                    }
                }
            });
            meteorPanel.add(meteorRate144000);
        }
        renderTab.add(meteorPanel, constraints);

        constraints.gridy++;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.CENTER;
        JButton saveButton = new JButton(getTranslator().translate("SaveAsDefault"));
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.finer("Saving rendering options in config file");

                IniFileParser conf = app.getConf();

                Preferences astroSec = conf.getSection("astro");
                Preferences starsSec = conf.getSection("stars");
                Preferences viewingSec = conf.getSection("viewing");
                Preferences landscapeSec = conf.getSection("landscape");

                astroSec.putBoolean("flag_stars", getCore().isStarEnabled());
                astroSec.putBoolean("flag_star_name", getCore().isStarNameEnabled());
                starsSec.putDouble("max_mag_star_name", getCore().getMaxMagStarName());
                starsSec.putBoolean("flag_star_twinkle", getCore().isStarTwinkleEnabled());
                starsSec.putDouble("star_twinkle_amount", getCore().getStarTwinkleAmount());
                viewingSec.putBoolean("flag_constellation_drawing", getCore().isConstellationLinesEnabled());
                viewingSec.putBoolean("flag_constellation_name", getCore().getFlagConstellationNames());
                viewingSec.putBoolean("flag_constellation_boundaries", getCore().getFlagConstellationBoundaries());
                viewingSec.putBoolean("flag_constellation_pick", getCore().getFlagConstellationIsolateSelected());
                astroSec.putBoolean("flag_nebula", getCore().getFlagNebula());
                astroSec.putBoolean("flag_nebula_name", getCore().isNebulaHintEnabled());
                astroSec.putDouble("max_mag_nebula_name", getCore().getNebulaMaxMagHints());
                astroSec.putBoolean("flag_nebula_display_no_texture", getCore().isNebulaDisplayNoTexture());
                astroSec.putBoolean("flag_planets", getCore().isPlanetsEnabled());
                astroSec.putBoolean("flag_planets_hints", getCore().isPlanetsHintsEnabled());
                viewingSec.putDouble("moon_scale", getCore().getMoonScale());
                viewingSec.putBoolean("flag_moon_scaled", getCore().isMoonScaled());
                viewingSec.putBoolean("flag_chart", app.getVisionModeChart());
                viewingSec.putBoolean("flag_night", app.getVisionModeNight());
                viewingSec.putBoolean("flag_equatorial_grid", getCore().isEquatorGridEnabled());
                viewingSec.putBoolean("flag_azimutal_grid", getCore().isAzimutalGridEnabled());
                viewingSec.putBoolean("flag_equator_line", getCore().isEquatorLineEnabled());
                viewingSec.putBoolean("flag_ecliptic_line", getCore().isEclipticLineEnabled());
                landscapeSec.putBoolean("flag_landscape", getCore().isLandscapeEnabled());
                viewingSec.putBoolean("flag_cardinal_points", getCore().isCardinalsPointsEnabled());
                landscapeSec.putBoolean("flag_atmosphere", getCore().isAtmosphereEnabled());
                landscapeSec.putBoolean("flag_fog", getCore().isFogEnabled());
                astroSec.putInt("meteor_rate", getCore().getMeteorsRate());

                conf.flush();
            }
        });
        renderTab.add(saveButton, constraints);
        return renderTab;
    }

    private StelCore getCore() {
        return app.getCore();
    }

    protected JPanel createLocationConfigTab() {
        JPanel locationTab = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(2, 5, 2, 5);

        earthMap = createOldEarthMap();
        //earthMap = createWorldwindEarthMap();
        locationTab.add((Component) earthMap, constraints);

        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        loadCities(locatorUtil.getDataFile("cities.fab"));

        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.5;

        constraints.gridy++;
        locationTab.add(new JLabel(getTranslator().translate("Cursor")), constraints);
        labelMapLocation = new JLabel();
        constraints.gridx++;
        locationTab.add(labelMapLocation, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        locationTab.add(new JLabel(getTranslator().translate("Selected")), constraints);
        constraints.gridx++;
        labelMapPointer = new JLabel();
        locationTab.add(labelMapPointer, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        locationTab.add(new JLabel(getTranslator().translate("Longitude")), constraints);
        SpinnerNumberModel longitudeSpinerModel = new SpinnerNumberModel(getCore().getNebulaMaxMagHints(), -180, 180, 1d / 60d);
        longIncDec = new JSpinner(longitudeSpinerModel);
        //TODO
        //longIncDec.setFormat(FORMAT_LONGITUDE);
        longIncDec.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setObserverPositionFromIncDec();
            }
        });
        constraints.gridx++;
        locationTab.add(longIncDec, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        locationTab.add(new JLabel(getTranslator().translate("Latitude")), constraints);
        SpinnerNumberModel latitudeSpinerModel = new SpinnerNumberModel(getCore().getNebulaMaxMagHints(), -90, 90, 1d / 60d);
        latIncDec = new JSpinner(latitudeSpinerModel);
        //TODO
        //latIncDec.setFormat(FORMAT_LATITUDE);
        latIncDec.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setObserverPositionFromIncDec();
            }
        });
        constraints.gridx++;
        locationTab.add(latIncDec, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        locationTab.add(new JLabel(getTranslator().translate("Altitude")), constraints);
        SpinnerNumberModel altitudeSpinerModel = new SpinnerNumberModel(getCore().getNebulaMaxMagHints(), 0, 2000, 10);
        constraints.gridx++;
        altIncDec = new JSpinner(altitudeSpinerModel);
        altIncDec.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setObserverPositionFromIncDec();
            }
        });
        locationTab.add(altIncDec, constraints);

        constraints.gridy++;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridwidth = 2;
        constraints.gridx = 0;
        JButton locationSaveButton = new JButton(getTranslator().translate("SaveLocation"));
        locationSaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String location = earthMap.getPositionString();
                if (location.equals(SGUI.UNKNOWN_OBSERVATORY)) {
                    //dialogWin.InputBox("Stellarium", "Enter observatory name", "observatory name");
                } else {
                    doSaveObserverPosition(location);
                }
            }
        });
        locationTab.add(locationSaveButton, constraints);

        return locationTab;
    }

    /*private MapPicture createWorldwindEarthMap() {
        WorldWindMapPicture wwMap = new WorldWindMapPicture() {
            protected void onPressCallback() {
                setObserverPositionFromMap();
            }
        };
        wwMap.setOnNearestCityListener(new OldMapPicture.NearestCityListener() {
            public void execute() {
                setCityFromMap();
            }
        });
        return wwMap;
    } */

    private OldMapPicture createOldEarthMap() {
        STexture earth = getCore().getObservatory().getHomePlanet().getMapTexture();
        OldMapPicture earthMap = new OldMapPicture(earth.getTextureName(), "pointeur1.png", "city.png") {
            protected void onPressCallback() {
                setObserverPositionFromMap();
            }
        };
        earthMap.setOnNearestCityListener(new OldMapPicture.NearestCityListener() {
            public void execute() {
                setCityFromMap();
            }
        });
        earthMap.setFont(9.5f, getUi().getBaseFontName());
        return earthMap;
    }

    private JPanel createTimeConfigTab() {
        JPanel timeTab = new JPanel();
        timeTab.setLayout(new BoxLayout(timeTab, BoxLayout.Y_AXIS));

        JPanel currentTimePanel = new JPanel();
        {
            currentTimePanel.setBorder(new TitledBorder("Time"));
            JLabel label = new JLabel("Displayed time is", JLabel.LEFT);

            Calendar calendar = Calendar.getInstance();
            Date initDate = calendar.getTime();
            timeCurrent = new SpinnerDateModel(initDate, null, null, Calendar.YEAR);//ignored for user input
            final JSpinner spinner = new JSpinner(timeCurrent);
            label.setLabelFor(spinner);
            dateEditor = new JSpinner.DateEditor(spinner, "EEE, dd/MM/yyyy HH:mm:ss z");
            spinner.setEditor(dateEditor);
            spinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (dateEditor.getTextField().isFocusOwner()) {
                        Date date = ((SpinnerDateModel) spinner.getModel()).getDate();
                        setCurrentTimeFromConfig(date);
                    }
                }
            });
            currentTimePanel.add(spinner);
        }
        timeTab.add(currentTimePanel);

        JPanel timeZonePanel = new JPanel();
        {
            timeZonePanel.setBorder(new TitledBorder("Time Zone"));
            JLabel systemTimeZoneLabel = new JLabel("Using System Default Time Zone", JLabel.LEFT);
            timeZonePanel.add(systemTimeZoneLabel);

            String tmpl = "(" + StelUtility.getTimeZoneNameFromSystem(getCore().getNavigation().getJulianDay()) + ")";
            StringBuffer tmplRes = new StringBuffer(tmpl.length());

            for (int i = 0; i < tmpl.length(); i++) {
                char c = tmpl.charAt(i);
                if ((int) c < 0 || (int) c > 255)
                    tmplRes.append('*');
                else
                    tmplRes.append(c);
            }

            systemTimeZoneLabel2 = new JLabel(tmplRes.toString(), JLabel.LEFT);
            timeZonePanel.add(systemTimeZoneLabel2);
        }
        timeTab.add(timeZonePanel);

        JPanel timeSpeedPanel = new JPanel();
        {
            timeSpeedPanel.setBorder(new TitledBorder("Time speed "));
            timeSpeedPanel.add(new JLabel("Current time speed is x"));
            timeSpeedLabel2 = new JLabel("", JLabel.LEFT);
            timeSpeedPanel.add(timeSpeedLabel2);

            timeSpeedPanel.add(new JLabel("Use key J and L to decrease and increase time speed. Use key K to return to real time speed."));
        }
        timeTab.add(timeSpeedPanel);

        return timeTab;
    }

    /**
     * DateFormatSymbols returns an extra, empty value at the
     * end of the array of months.  Remove it.
     */
    static protected String[] getMonthStrings() {
        String[] months = new java.text.DateFormatSymbols().getMonths();
        int lastIndex = months.length - 1;

        if (months[lastIndex] == null || months[lastIndex].length() <= 0) { //last item empty
            String[] monthStrings = new String[lastIndex];
            System.arraycopy(months, 0, monthStrings, 0, lastIndex);
            return monthStrings;
        } else { //last item not empty
            return months;
        }
    }

    private JPanel createVideoConfigTab() {

        JPanel videoTab = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTH;

        JPanel projectionPanel = createProjectionPanel();
        videoTab.add(projectionPanel, constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.VERTICAL;
        JPanel resolutionPanel = createResolutionPanel();
        videoTab.add(resolutionPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        final JComboBox lookAndFeelsCombo = createLookAndFeelCombo();
        videoTab.add(lookAndFeelsCombo, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        JButton videoSaveButton = createVideoSaveButton();
        videoTab.add(videoSaveButton, constraints);

        return videoTab;
    }

    private JButton createVideoSaveButton() {
        JButton videoSaveButton = new JButton(getTranslator().translate("SaveAsDefault"));
        videoSaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVideoOption();
            }
        });
        return videoSaveButton;
    }

    private JComboBox createLookAndFeelCombo() {
        final UIManager.LookAndFeelInfo[] lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
        String[] lookAndFeelNames = new String[lookAndFeelInfos.length];
        for (int i = 0; i < lookAndFeelInfos.length; i++) {
            UIManager.LookAndFeelInfo lookAndFeelInfo = lookAndFeelInfos[i];
            lookAndFeelNames[i] = lookAndFeelInfo.getName();
        }
        final JComboBox lookAndFeelsCombo = new JComboBox(lookAndFeelNames);
        lookAndFeelsCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    try {
                        for (UIManager.LookAndFeelInfo lookAndFeelInfo : lookAndFeelInfos) {
                            if (lookAndFeelInfo.getName().equals(lookAndFeelsCombo.getSelectedItem())) {
                                UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                                SwingUtilities.updateComponentTreeUI(ConfigDialog.this);
                                SwingUtilities.updateComponentTreeUI(getOwner());
                                break;
                            }
                        }
                    } catch (Exception e1) {
                        throw new StellariumException("Error while changing look & feel", e1);
                    }
            }
        });
        return lookAndFeelsCombo;
    }

    private JPanel createResolutionPanel() {
        JPanel resolutionPanel = new JPanel();
        resolutionPanel.setBorder(new TitledBorder(getTranslator().translate("ScreenResolution")));
        resolutionPanel.setLayout(new BoxLayout(resolutionPanel, BoxLayout.Y_AXIS));
        {
            screenSizeList = new JList(getUi().getVideoModeList());
            String vs = ((SwingUI) getUi()).getDisplayModeString(getOwner().getGraphicsConfiguration().getDevice().getDisplayMode());
            screenSizeList.setSelectedValue(vs, true);
            JScrollPane scrollPane = new JScrollPane(screenSizeList);
            resolutionPanel.add(scrollPane);
            resolutionPanel.add(new JLabel("Restart program for change to apply."));
        }
        return resolutionPanel;
    }

    private JPanel createProjectionPanel() {
        JPanel projectionPanel = new JPanel();
        projectionPanel.setBorder(new TitledBorder(getTranslator().translate("Projection")));
        projectionPanel.setLayout(new BoxLayout(projectionPanel, BoxLayout.Y_AXIS));
        StelCore core = getCore();
        {
            projectionSl = new JList(new String[]{"perspective", "fisheye", "stereographic", "spheric_mirror"});
            projectionSl.setSelectedValue(calculateProjectionSlValue(core.getProjectionType(), app.getViewPortDistorterType()), true);
            projectionSl.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateVideoVariables();
                }
            });
            projectionPanel.add(projectionSl);

            diskViewportCbx = new JCheckBox("Disk Viewport", false);
            diskViewportCbx.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    boolean selected = diskViewportCbx.isSelected();
                    firePropertyChange("diskViewport", Boolean.valueOf(selected), Boolean.valueOf(!selected));
                    updateVideoVariables();
                }
            });
            projectionPanel.add(diskViewportCbx);
        }
        return projectionPanel;
    }

    private JPanel createLandscapeConfigTab() {
        JPanel landscapeTab = new JPanel();

        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        final JLabel landscapeAuthorLabel = new JLabel();
        final JLabel landscapeDescriptionLabel = new JLabel();
        {
            final IniFileParser pd = Landscape.getLandscapeIniParser(locatorUtil.getDataFile("landscapes.ini"));
            final JList landscapeList = new JList(Landscape.getLandscapeNames(pd));
            landscapeList.setBorder(new TitledBorder(getTranslator().translate("ChooseLandscapes")));
            landscapeList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    StelCore core = getCore();
                    core.setLandscape(Landscape.nameToKey(pd,
                            (String) landscapeList.getSelectedValue()));
                    landscapeAuthorLabel.setText("Author: " + core.getLandscapeAuthorName());
                    String landscapeDescription = core.getLandscapeDescription();
                    landscapeDescriptionLabel.setText("<html>Info: " + landscapeDescription.replace("\n", "<br>") + "</html>");
                }

            });
            landscapeList.setSelectedValue(getCore().getLandscapeName(), true);
            landscapeTab.add(landscapeList);
        }
        landscapeTab.add(landscapeAuthorLabel);
        landscapeTab.add(landscapeDescriptionLabel);
        return landscapeTab;
    }

    private JPanel createLanguageConfigTab() {
        final SupportedLocalesListModel availLocaleList = new SupportedLocalesListModel(logger);

        JPanel languageTab = new JPanel();
        {
            final JList languageList = new JList(availLocaleList);
            languageList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    app.setAppLanguage(availLocaleList.getLocale(languageList.getSelectedIndex()));
                }
            });
            languageList.setSelectedValue(app.getAppLanguage(), true);
            JScrollPane scrollPane = new JScrollPane(languageList);
            scrollPane.setBorder(new TitledBorder(getTranslator().translate("ProgramLanguage")));
            languageTab.add(scrollPane);
        }
        {
            final JList languageSky = new JList(availLocaleList);
            languageSky.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    getCore().setSkyLocale(availLocaleList.getLocale(languageSky.getSelectedIndex()));
                }
            });
            languageSky.setSelectedValue(getCore().getSkyLanguage(), true);
            JScrollPane scrollPane = new JScrollPane(languageSky);
            scrollPane.setBorder(new TitledBorder(getTranslator().translate("SkyLanguage")));
            languageTab.add(scrollPane);
        }
        {
            final SkyLocalizer skyLocalizer = SkyLocalizer.getInstance();
            final List<String> skyCultures = skyLocalizer.getSkyCultureList();
            final AbstractListModel skyCultureLM = new AbstractListModel() {
                public int getSize() {
                    return skyCultures.size();
                }

                public Object getElementAt(int index) {
                    return skyCultures.get(index);
                }
            };
            final JList skyCulture = new JList(skyCultureLM);
            skyCulture.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    getCore().setSkyCulture(skyCultures.get(skyCulture.getSelectedIndex()));
                }
            });
            skyCulture.setSelectedValue(getCore().getSkyCulture(), true);
            JScrollPane scrollPane = new JScrollPane(skyCulture);
            scrollPane.setBorder(new TitledBorder(getTranslator().translate("SkyCulture")));
            languageTab.add(scrollPane);
        }
        return languageTab;
    }

    public void updateVideoVariables() {
        if ("spheric_mirror".equals(projectionSl.getSelectedValue())) {
            getCore().setProjectionType(Projector.TYPE.fisheye);
            app.setViewPortDistorterType(ViewportDistorter.TYPE.fisheye_to_spheric_mirror);
        } else {
            getCore().setProjectionType(Projector.TYPE.valueOf((String) projectionSl.getSelectedValue()));
            app.setViewPortDistorterType(ViewportDistorter.TYPE.none);
        }

        if (diskViewportCbx.isSelected() && !getCore().getViewportMaskDisk()) {
            getCore().setViewportMaskDisk();
        }
        if (!diskViewportCbx.isSelected() && getCore().getViewportMaskDisk()) {
            getCore().setViewportMaskNone();
        }
    }

    public void setCityFromMap() {
        getUi().setWaitOnLocation(false);
        labelMapLocation.setText(earthMap.getCursorString());
        labelMapPointer.setText(earthMap.getPositionString());
    }

    void setCurrentTimeFromConfig(Date someDate) {
        StringBuffer os = new StringBuffer();
        os.append(1900 + someDate.getYear()).append(":")
                .append(1 + someDate.getMonth()).append(":")
                .append(someDate.getDate()).append("T")
                .append(someDate.getHours()).append(":")
                .append(someDate.getMinutes()).append(":")
                .append(someDate.getSeconds());
        app.getCommander().executeCommand("date local " + os.toString());
    }

    void setObserverPositionFromMap() {
        StringBuffer oss = new StringBuffer();
        oss.append("moveto lat ").append(earthMap.getPointerLatitude())
                .append(" lon ").append(earthMap.getPointerLongitude())
                .append(" alt ").append(earthMap.getPointerAltitude());
        app.getCommander().executeCommand(oss.toString());
    }

    void setObserverPositionFromIncDec() {
        StringBuffer oss = new StringBuffer();
        oss.append("moveto lat ").append(latIncDec.getValue())
                .append(" lon ").append(longIncDec.getValue())
                .append(" alt ").append(altIncDec.getValue());
        app.getCommander().executeCommand(oss.toString());
    }

    public void doSaveObserverPosition(String name) {
        String location = name.replace(' ', '_');

        StringBuffer oss = new StringBuffer();
        oss.append("moveto lat ").append(latIncDec.getValue()).append(" lon ").append(longIncDec.getValue())
                .append(" name ").append(location);
        app.getCommander().executeCommand(oss.toString());

        getCore().getObservatory().save(app.getConf(), "init_location");
        getUi().setTitleObservatoryName(getUi().getTitleWithAltitude());
    }

    void saveLandscapeOptions() {
        logger.finer("Saving landscape name in config file");
        IniFileParser conf = app.getConf();
        Preferences locationSec = conf.getSection("init_location");
        locationSec.put("landscape_name", getCore().getObservatory().getLandscapeName());
        conf.flush();
    }

    void saveLanguageOptions() {
        logger.finer("Saving language in config file");
        IniFileParser conf = app.getConf();
        Preferences localizationSec = conf.getSection("localization");
        localizationSec.put("sky_locale", getCore().getSkyLanguage().toString());
        localizationSec.put("app_locale", app.getAppLanguage());
        localizationSec.put("sky_culture", getCore().getSkyCultureDir());
        conf.flush();
    }

    void setVideoOption() throws StellariumException {
        String s = (String) screenSizeList.getSelectedValue();
        int i = s.indexOf("x");
        int w = java.lang.Integer.parseInt(s.substring(0, i));
        int h = java.lang.Integer.parseInt(s.substring(i + 1/*, s.length()*/));

        logger.finer("Saving video settings: projection=" + getCore().getProjectionType() +
                ", distorter=" + app.getViewPortDistorterType() +
                " res=" + w + "x" + h + " in config file");

        IniFileParser conf = app.getConf();

        Preferences projectionSec = conf.getSection(IniFileParser.PROJECTION_SECTION);
        projectionSec.put(IniFileParser.TYPE, getCore().getProjectionType().name());
        Preferences videoSec = conf.getSection(IniFileParser.VIDEO_SECTION);
        projectionSec.put(IniFileParser.DISTORTER, app.getViewPortDistorterType().name());

        projectionSec.put(IniFileParser.VIEWPORT, getCore().getViewportMaskDisk() ? IniFileParser.DISK_VIEWPORT : IniFileParser.MAXIMIZED_VIEWPORT);

        if (w != 0 && h != 0) {
            videoSec.putInt(IniFileParser.SCREEN_WIDTH, w);
            videoSec.putInt(IniFileParser.SCREEN_HEIGHT, h);
        }

        conf.flush();
    }

    static Projector.TYPE calculateProjectionSlValue(Projector.TYPE projectionType, ViewportDistorter.TYPE viewportDistorterType) {
        if (viewportDistorterType == ViewportDistorter.TYPE.fisheye_to_spheric_mirror) {
            return Projector.TYPE.spheric_mirror;
        }
        return projectionType;
    }

    public void updateValues() {
        StelCore core = getCore();
        starsCbx.setSelected(core.isStarEnabled());
        starNamesCbx.setSelected(core.isStarNameEnabled());
        maxMagStarName.setValue((double) core.getMaxMagStarName());
        starTwinkleCbx.setSelected(core.isStarTwinkleEnabled());
        starTwinkleAmount.setValue(core.getStarTwinkleAmount());
        constellationCbx.setSelected(core.isConstellationLinesEnabled());
        constellationNameCbx.setSelected(core.getFlagConstellationNames());
        constellationBoundariesCbx.setSelected(core.getFlagConstellationBoundaries());
        selConstellationCbx.setSelected(core.getFlagConstellationIsolateSelected());
        nebulasNamesCbx.setSelected(core.isNebulaHintEnabled());
        maxMagNebulaName.setValue(core.getNebulaMaxMagHints());
        nebulasNoTextureCbx.setSelected(core.isNebulaDisplayNoTexture());
        planetsCbx.setSelected(core.isPlanetsEnabled());
        planetsHintsCbx.setSelected(core.isPlanetsHintsEnabled());
        moonX4Cbx.setSelected(core.isMoonScaled());
        equatorGridCbx.setSelected(core.isEquatorGridEnabled());
        azimuthGridCbx.setSelected(core.isAzimutalGridEnabled());
        equatorCbx.setSelected(core.isEquatorLineEnabled());
        eclipticCbx.setSelected(core.isEclipticLineEnabled());
        groundCbx.setSelected(core.isLandscapeEnabled());
        cardinalCbx.setSelected(core.isCardinalsPointsEnabled());
        atmosphereCbx.setSelected(core.isAtmosphereEnabled());
        fogCbx.setSelected(core.isFogEnabled());

        String meteorRate = "";
        if (core.getMeteorsRate() == 10) {
            meteorRate = ": Normal rate";
            meteorRate10.setSelected(true);
        } else {
            meteorRate10.setSelected(false);
        }
        if (core.getMeteorsRate() == 80) {
            meteorRate = ": Standard Perseids rate";
            meteorRate80.setSelected(true);
        } else {
            meteorRate80.setSelected(false);
        }
        if (core.getMeteorsRate() == 10000) {
            meteorRate = ": Exceptional Leonid rate";
            meteorRate10000.setSelected(true);
        } else {
            meteorRate10000.setSelected(false);
        }
        if (core.getMeteorsRate() == 144000) {
            meteorRate = ": Highest rate ever (1966 Leonids)";
            meteorRate144000.setSelected(true);
        } else {
            meteorRate144000.setSelected(false);
        }
        meteorlbl.setText("Meteor Rate per minute" + meteorRate);

        Observator observatory = core.getObservatory();
        earthMap.setPointerPosition(observatory.getLongitude(), observatory.getLatitude());
        longIncDec.setValue((float) observatory.getLongitude());
        latIncDec.setValue((float) observatory.getLatitude());
        altIncDec.setValue(observatory.getAltitude());
        labelMapLocation.setText(earthMap.getCursorString());
        if (!getUi().isWaitOnLocation())
            labelMapPointer.setText(earthMap.getPositionString());
        else {
            earthMap.findPosition(observatory.getLongitude(), observatory.getLatitude());
            labelMapPointer.setText(earthMap.getPositionString());
            getUi().setWaitOnLocation(false);
        }

        setJDay(core.getJulianDay() + app.getGMTShift(core.getJulianDay()) * NavigatorIfc.JD_HOUR);
        systemTimeZoneLabel2.setText("(" + StelUtility.getTimeZoneNameFromSystem(core.getJulianDay()) + ")");

        timeSpeedLabel2.setText(String.valueOf(core.getTimeSpeed() / Navigator.JD_SECOND));

        projectionSl.setSelectedValue(calculateProjectionSlValue(core.getProjectionType(), app.getViewPortDistorterType()), true);
        diskViewportCbx.setSelected(core.getViewportMaskDisk());
    }

    public void setJDay(double JD) {
        if (!dateEditor.getTextField().isFocusOwner()) {
            int iy, im, id, ih, imn, is;

            int a = (int) (JD + 0.5);
            double c;
            if (a < 2299161) {
                c = a + 1524;
            } else {
                double b = (int) ((a - 1867216.25) / 36524.25);
                c = a + b - (int) (b / 4) + 1525;
            }

            int dd = (int) ((c - 122.1) / 365.25);
            int e = (int) (365.25 * dd);
            int f = (int) ((c - e) / 30.6001);

            double dday = c - e - (int) (30.6001 * f) + ((JD + 0.5) - (int) (JD + 0.5));

            im = f - 1 - 12 * (f / 14);
            iy = dd - 4715 - (int) ((7.0 + im) / 10.0);
            id = (int) dday;

            double dhour = (dday - id) * 24;
            ih = (int) dhour;

            double dminute = (dhour - ih) * 60;
            imn = (int) dminute;

            is = (int) Math.round((dminute - imn) * 60);

            currentDate.set(Calendar.YEAR, iy);
            currentDate.set(Calendar.MONTH, im - 1);
            currentDate.set(Calendar.DATE, id);
            currentDate.set(Calendar.HOUR_OF_DAY, ih);
            currentDate.set(Calendar.MINUTE, imn);
            currentDate.set(Calendar.SECOND, is);
            timeCurrent.setValue(currentDate.getTime());
        }
    }

    private StelUI getUi() {
        return app.getUi();
    }

    void loadCities(URL file) {
        logger.finer("Loading cities from " + file + "...");
        java.util.List<String> lines = DataFileUtil.getLines(file, "cities data", false);
        if (lines == null || lines.isEmpty()) {
            return;// nothing to do
        }

        // determine total number to be loaded for percent complete display
        int lineNb = 1;
        for (String line : lines) {
            if (line.charAt(0) != '#') {
                try {
                    StringTokenizer tokenizer = new StringTokenizer(line);

                    String cname = tokenizer.nextToken();
                    String cstate = tokenizer.nextToken();
                    String ccountry = tokenizer.nextToken();
                    String clat = tokenizer.nextToken();
                    String clon = tokenizer.nextToken();
                    int alt = Integer.parseInt(tokenizer.nextToken());
                    String ctime = tokenizer.nextToken();
                    int showatzoom = Integer.parseInt(tokenizer.nextToken());

                    String name = cname.replace('_', ' ');
                    String state = cstate.replace('_', ' ');
                    String country = ccountry.replace('_', ' ');
                    float time;
                    if (ctime.charAt(0) == 'x') {
                        time = 0;
                    } else {
                        time = Float.parseFloat(ctime);
                    }
                    earthMap.addCity(name, state, country, StelUtility.getDecAngle(clon), StelUtility.getDecAngle(clat), time, showatzoom, alt);
                } catch (Throwable e) {
                    throw new StellariumException("Error while loading city from file " + file + " at line " + lineNb, e);
                }
            }
            lineNb++;
        }
        logger.finer(lineNb + " cities loaded.");
    }

    private static class SupportedLocalesListModel extends AbstractListModel {
        private final List<Locale> supportedLocales = new ArrayList<Locale>(Translator.getSupportedLocales());
        protected final Logger logger;

        public SupportedLocalesListModel(Logger parentLogger) {
            logger = Logger.getLogger(getClass().getName());
            if (parentLogger != null) {
                logger.setParent(parentLogger);
            }

            Collections.sort(supportedLocales, new Comparator<Locale>() {
                public int compare(Locale o1, Locale o2) {
                    final int langCompare = o1.getLanguage().compareTo(o2.getLanguage());
                    if (langCompare == 0) {
                        return o1.getCountry().compareTo(o2.getCountry());
                    }
                    return langCompare;
                }
            });
        }

        public int getSize() {
            return supportedLocales.size();
        }

        public Object getElementAt(int index) {
            final Locale myLocale = supportedLocales.get(index);
            return myLocale.toString() + " " + myLocale.getDisplayName(myLocale);
        }

        public Locale getLocale(int index) {
            if (index < 0 || index >= supportedLocales.size()) {
                logger.warning("Trying to retrieve unexistent index");
                return Locale.getDefault();
            }
            return supportedLocales.get(index);
        }
    }

    /*private class WorldWindMapPicture extends WorldWindowGLCanvas implements MapPicture {
        private double exactLatitude, exactLongitude;
        private int exactAltitude;

        private WorldWindMapPicture() {
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            setModel(m);
            addMouseMotionListener(new MouseMotionListener() {
                public void mouseDragged(MouseEvent e) {
                }

                public void mouseMoved(MouseEvent e) {
                    Position currentPosition = getCurrentPosition();
                    if (currentPosition != null) {
                        exactLongitude = currentPosition.getLongitude().getDegrees();
                        exactLatitude = currentPosition.getLatitude().getDegrees();
                        exactAltitude = (int) currentPosition.getElevation();
                        onPressCallback();
                    }
                }
            });
/*            addSelectListener(new SelectListener() {
                public void selected(SelectEvent event) {
                    if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                        if (event.hasObjects()) {
                            System.out.println();
                            PickedObjectList objectList = event.getObjects();
                            for (Object anObjectList : objectList) {
                                PickedObject pickedObject = (PickedObject) anObjectList;
                                Position position = pickedObject.getPosition();
                                exactLatitude = position.getLatitude().getDegrees();
                                exactLongitude = position.getLongitude().getDegrees();
                                exactAltitude = (int) position.getElevation();
                                onPressCallback();
                            }
                        }
                    }
                }
            });*
        }

        private NearestCityListener onNearestCityListener;

        public void setOnNearestCityListener(NearestCityListener c) {
            onNearestCityListener = c;
        }

        protected void onPressCallback() {
            // To be redefined
        }

        public String getPositionString() {
            return null;
        }

        public String getCursorString() {
            return null;
        }

        public double getPointerLatitude() {
            return exactLatitude;
        }

        public double getPointerLongitude() {
            return exactLongitude;
        }

        public int getPointerAltitude() {
            return exactAltitude;
        }

        public void setPointerPosition(double longitude, double latitude) {
            exactLongitude = longitude;
            exactLatitude = latitude;
        }

        public void findPosition(double longitude, double latitude) {
        }

        public void addCity(String name, String state, String country, double decAngle, double decAngle1, float time, int showatzoom, int alt) {
        }
    } */
}
