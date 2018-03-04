/*
 * Stellarium
 * Copyright (C) 2002 Fabien Ch�eau
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
package org.stellarium.astro;

import org.stellarium.*;
import org.stellarium.data.HipData;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.data.ReverseDataInputStream;
import org.stellarium.projector.Projector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * Manage groups of Stars
 */
public class HipStarMgr {

    protected final Logger logger;
    private STextureFactory textureFactory;

    /**
     * construct and load all data
     *
     * @param parentLogger
     * @throws org.stellarium.StellariumException
     *
     */
    public HipStarMgr(Logger parentLogger) throws StellariumException {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);

        int nbZones = hipGrid.getNbPoints();
        starZones = new ArrayList<List<HipStar>>(nbZones);
        for (int i = 0; i < nbZones; i++) {
            starZones.add(new ArrayList<HipStar>());
        }
    }

    public Color getCircleColor() {
        return HipStar.circleColor;
    }

    public void setCircleColor(Color circleColor) {
        HipStar.circleColor = circleColor;
    }

    public Color getLabelColor() {
        return HipStar.labelColor;
    }

    public void setLabelColor(Color labelColor) {
        HipStar.labelColor = labelColor;
    }

    public void init(SFontIfc someFont, URL file,
                     URL commonNameFile, URL sciNameFile) throws StellariumException {
        loadData(file);
        loadDouble(file);
        loadVariable(file);
        loadCommonNames(commonNameFile);
        loadSciNames(sciNameFile);

        starTexture = textureFactory.createTexture("star16x16.png", STexture.TEX_LOAD_TYPE_PNG_SOLID, false);// Load star texture
        HipStar.starFont = someFont;
    }

    /**
     * Load from file ( create the stream and call the Read function )
     *
     * @param hipCatFile Hipparcos catalog file
     * @throws StellariumException If the Hipparcos catalog file is not found or if a problem occured while reading it.
     */
    void loadData(URL hipCatFile) throws StellariumException {
        logger.fine("Loading Hipparcos star data");

        try {
            ReverseDataInputStream inputStream = new ReverseDataInputStream(hipCatFile.openStream());
            int starArraySize = 0;
            int dataDrop = 0;
            try {
                // Read number of stars in the Hipparcos catalog
                starArraySize = inputStream.readInt();//120417
                starFlatArray = new HashMap<Integer, HipStar>(starArraySize);

                // Read binary file Hipparcos catalog
                HipStar e;
                HipData d = new HipData();
                for (int i = 0; i < starArraySize; i++) {
                    if (i % 2000 == 0 || i == starArraySize - 1) {
                        float percentage = (float) i / starArraySize;
                        propertyChangeSupport.firePropertyChange("Loading Hipparcos catalog of " + starArraySize + " stars", -1f, percentage);
                    }

                    e = new HipStar(logger);
                    e.hp = i;
                    d.fillData(i, inputStream);
                    if (!e.read(d)) {
                        dataDrop++;
                        continue;
                    }
                    starZones.get(hipGrid.getNearest(e.XYZ)).add(e);
                    starFlatArray.put(e.hp, e);
                }
            } finally {
                inputStream.close();
            }

            logger.fine((starArraySize - dataDrop) + " Hipparcos stars loaded, " + dataDrop + " dropped.");

            // sort stars by magnitude for faster rendering
            HipStar.MagnitudeComparator magComparator = new HipStar.MagnitudeComparator();
            for (List<HipStar> starZone : starZones) {
                Collections.sort(starZone, magComparator);
            }
        } catch (FileNotFoundException e) {
            throw new StellariumException(hipCatFile + " NOT FOUND", e);
        } catch (IOException e) {
            throw new StellariumException("Error while reading " + hipCatFile, e);
        }
    }


    /**
     * Load common names from file
     *
     * @param commonNameFile The common names file
     */
    public boolean loadCommonNames(URL commonNameFile) throws StellariumException {
        HipStar.NamesManager namesMgr = new HipStar.CommonNamesManager();
        return fillNames(commonNameFile, namesMgr, commonNamesMap);
    }

    /**
     * Load scientific names from file.
     *
     * @param sciNameFile
     * @throws org.stellarium.StellariumException
     *
     */
    public boolean loadSciNames(URL sciNameFile) throws StellariumException {
        HipStar.NamesManager namesMgr = new HipStar.ScientificNamesManager();
        return fillNames(sciNameFile, namesMgr, sciNamesMap);
    }

    private boolean fillNames(URL cnFile,
                              HipStar.NamesManager namesMgr,
                              Set<HipStar> starWithNames) throws StellariumException {
        logger.fine("Loading star names from " + cnFile);

        // clear existing names (would be faster if they were in separate array
        // since relatively few are named)
        Collection<HipStar> stars = starFlatArray.values();
        for (HipStar star : stars) {
            namesMgr.clean(star);
        }

        starWithNames.clear();
        // Assign names to the matching stars, now support spaces in names
        LineNumberReader cnReader = null;
        try {
            cnReader = new LineNumberReader(new InputStreamReader(cnFile.openStream()));
            int hipNumber;
            HipStar star;
            String line;
            while ((line = cnReader.readLine()) != null) {
                int pipePos = line.indexOf('|');
                if (pipePos < 0 || pipePos >= line.length()) {
                    logger.warning("The pipe character in file " + cnFile +
                            " line " + cnReader.getLineNumber() + " is not found or at the end.\n" +
                            "The line is: " + line);
                } else {
                    hipNumber = Integer.parseInt(line.substring(0, pipePos).trim());
                    star = searchHP(hipNumber);
                    if (star != null) {
                        // remove underscores
                        String name = line.substring(pipePos + 1).replace('_', ' ');
                        namesMgr.set(star, name);
                        starWithNames.add(star);
                    }
                }
            }
        } catch (IOException e) {
            throw new StellariumException("Could not load common hipstar names", e);
        } finally {
            if (cnReader != null) {
                try {
                    cnReader.close();
                } catch (IOException e) {
                    // TODO: Find a better execption managment
                    logger.warning("Ignoring exception on close file " + e.getMessage());
                }
            }
        }

        return true;
    }

    /**
     * Draw all the stars
     */
    public void draw(Tuple3d equVision, ToneReproductor someEye, Projector prj) {
        // If stars are turned off don't waste time below
        // projecting all stars just to draw disembodied labels
        if (!starsFader.hasInterstate()) {
            return;
        }

        // Set temporary static variable for optimization
        if (flagStarTwinkle) {
            HipStar.twinkleAmount = twinkleAmount;
        } else {
            HipStar.twinkleAmount = 0;
        }
        HipStar.starScale = starScale * starsFader.getInterstate();
        HipStar.starMagScale = starMagScale;
        HipStar.gravityLabel = gravityLabel;
        HipStar.namesBrightness = namesFader.getInterstate() * starsFader.getInterstate();
        HipStar.eye = someEye;
        HipStar.proj = prj;

        if (flagPointStar) {
            // TODO: fade on/off with starsFader
            // FRED: incoherent test with above...
            if (starsFader.hasInterstate()) {
                drawPoint(equVision, someEye, prj);
            }
            return;
        }

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);

        // Find the star zones which are in the screen
        int nbZones;

        // FOV is currently measured vertically, so need to adjust for wide screens
        // TODO: projector should probably use largest measurement itself
        double maxFov = Math.max(prj.getFieldOfView(), prj.getFieldOfView() * prj.getViewportWidth() / prj.getViewportHeight());

        nbZones = hipGrid.intersect(equVision, (1.2 * maxFov * Math.PI) / 180d);
        /*static*/
        int[] zoneList = hipGrid.getResult();
        double maxMag = limitingMag - 1 + 60.f / maxFov;

        prj.setOrthographicProjection();// set 2D coordinate

        // Bind the star texture
        //if (draw_mode == DM_NORMAL)
        glBindTexture(GL_TEXTURE_2D, starTexture.getID());
        //else glBindTexture (GL_TEXTURE_2D, starcTexture.getID());

        // Set the draw mode
        //if (draw_mode == DM_NORMAL)
        glBlendFunc(GL_ONE, GL_ONE);
        //else glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // charting

        // Print all the stars of all the selected zones
        Point3d XY = new Point3d();
        for (int i = 0; i < nbZones; ++i) {
            List<HipStar> subList = starZones.get(zoneList[i]);

            for (HipStar h : subList) {
                // If too small, skip and Compute the 2D position and check if in screen
                if (h.mag > maxMag) {
                    // Break since zone stars ordered by magnitude
                    break;
                }
                if (!prj.projectJ2000Check(h.XYZ, XY)) {
                    continue;
                }
                h.draw(XY);
                if (namesFader.hasInterstate() && h.mag < maxMagStarName) {
                    if (h.drawName(XY)) {
                        glBindTexture(GL_TEXTURE_2D, starTexture.getID());
                    }
                }
            }
        }

        prj.resetPerspectiveProjection();
    }

    /**
     * Draw all the stars
     */
    public void drawPoint(Tuple3d equVision, ToneReproductor someEye, Projector prj) {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, starTexture.getID());
        glBlendFunc(GL_ONE, GL_ONE);

        // Find the star zones which are in the screen
        int nbZones = hipGrid.intersect(equVision, prj.getFieldOfView() * 1.2d * Math.PI / 180.d);
        int[] zoneList = hipGrid.getResult();
        float maxMag = (float) (5.5f + 60f / prj.getFieldOfView());

        prj.setOrthographicProjection();// set 2D coordinate

        // Print all the stars of all the selected zones
        Point3d XY = new Point3d();
        for (int i = 0; i < nbZones; ++i) {
            List<HipStar> zone = starZones.get(zoneList[i]);
            for (HipStar h : zone) {
                // If too small, skip and Compute the 2D position and check if in screen
                if (h.mag > maxMag) break;
                if (!prj.projectJ2000Check(h.XYZ, XY)) continue;
                h.drawPoint(XY);
                if (!StelUtility.isEmpty(h.commonNameI18) && namesFader.hasInterstate() && h.mag < maxMagStarName) {
                    h.drawName(XY);
                    //glBindTexture(GL_TEXTURE_2D, starTexture.getID());
                }
            }
        }

        prj.resetPerspectiveProjection();
    }

    /**
     * Look for a star by XYZ coords
     *
     * @param pos
     */
    public HipStar search(Vector3d pos) {
        pos.normalize();
        HipStar nearest = null;
        double angleNearest = 0;

        Collection<HipStar> stars = starFlatArray.values();
        for (HipStar star : stars) {
            if (star.XYZ.x * pos.x + star.XYZ.y * pos.y + star.XYZ.z * pos.z > angleNearest) {
                angleNearest = star.XYZ.x * pos.x + star.XYZ.y * pos.y + star.XYZ.z * pos.z;
                nearest = star;
            }
        }
        if (angleNearest <= RADIUS_STAR * 0.9999) {
            nearest = null;
        }
        return nearest;
    }

    /**
     * Return a stl vector containing the nebulas located inside the limFov circle around position v
     *
     * @param p
     * @param limFieldOfView
     * @return
     */
    public List<StelObject> searchAround(Tuple3d p, double limFieldOfView) {
        List<StelObject> result = new ArrayList<StelObject>();
        Vector3d v = new Vector3d(p);
        v.normalize();
        double cosLimFov = Math.cos(Math.toRadians(limFieldOfView));
        //	static Vec3d equPos;

        Collection<HipStar> stars = starFlatArray.values();
        for (HipStar star : stars) {
            if (star.XYZ.x * v.x + star.XYZ.y * v.y + star.XYZ.z * v.z >= cosLimFov) {
                result.add(star);
            }
        }
        return result;
    }

    /**
     * Load the double stars from the double star file
     */
    boolean loadDouble(URL hipCatFile) throws StellariumException {
        return load(hipCatFile, "Loading Hipparcos double stars...", "double_txt.dat");
    }

    /**
     * Load the variable stars from the raw hip file
     */
    boolean loadVariable(URL hipCatFile) throws StellariumException {
        return load(hipCatFile, "Loading Hipparcos periodic variable stars...", "variable_txt.dat");
    }

    private boolean load(URL hipCatFile, String message, String fileName) {
        // The flag file is in the same folder than hipparcos file
        try {
            String s = hipCatFile.toExternalForm();
            URL doubleFile = ResourceLocatorUtil.getInstance().getOrLoadFile(new URL(s.substring(0, s.lastIndexOf("/"))), fileName);

            logger.fine(message);
            loadStarFlag(doubleFile, new HipStar.DoubleFlagManager());

            return true;
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }
    }

    private void loadStarFlag(URL flagFile, HipStar.FlagManager flagManager) throws StellariumException {
        int nbLoaded = 0;
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(flagFile.openStream()));
            String line;
            int hp;
            while ((line = reader.readLine()) != null) {
                hp = Integer.parseInt(line.trim());
                HipStar hipStar = searchHP(hp);
                if (hipStar != null) {
                    flagManager.set(hipStar);
                    nbLoaded++;
                } else {
                    logger.warning("Star number " + hp + " does not exists when loading " + flagFile + " line " + reader.getLineNumber() + ".");
                }
            }
            reader.close();
            reader = null;
        } catch (IOException e) {
            throw new StellariumException("Hipparcos star flag file " + flagFile + " error " + e.getMessage(), e);
        } finally {
            if (reader != null) {
                // Forcing and ignoring closure
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }

        logger.warning(nbLoaded + " Hipparcos stars loaded");
    }

    /**
     * Update i18 names from english names according to passed translator
     * The translation is done using gettext with translated strings defined in translations.h
     */
    public void translateNames(Translator trans) {
        // TODO: separate common names vector would be more efficient - DONE
        for (HipStar star : commonNamesMap) {
            star.translateNames(trans);
        }
    }

    static final String[] catalogs = new String[]{"HP", "HD", "SAO"};

    public StelObject search(String name) throws StellariumException {
        String n = name.replace('_', ' ').trim().toUpperCase();
        int nb = -1;
        int catalogNb = -1;

        // check if a valid catalog reference
        for (int i = 0; i < catalogs.length; i++) {
            String catalog = catalogs[i];
            if (n.startsWith(catalog)) {
                try {
                    nb = Integer.parseInt(n.substring(catalog.length()).trim());
                    catalogNb = i;
                } catch (NumberFormatException e) {
                    logger.severe("Parsing error searching for star " + name + "\n" + e.getMessage());
                }
                break;
            }
        }
        if (catalogNb == -1) {
            return null;
        }

        if (catalogNb == 0) {
            // Support for HP only
            return searchHP(nb);
        }
        throw new StellariumException("Catalog " + catalogs[catalogNb] + " not supported.");
    }

    /**
     * Search the star by HP number.
     *
     * @param hpNumber The HP number
     * @return The Hip star
     */
    public HipStar searchHP(int hpNumber) {
        return starFlatArray.get(hpNumber);
    }

    public StelObject searchByNameI18n(String nameI18n) {
        String objw = nameI18n.toUpperCase();

        // Search by HP number if it's an HP formated number
        // Please help, if you know a better way to do this:
        if (objw.length() >= 2 && objw.charAt(0) == 'H' && objw.charAt(1) == 'P') {
            try {
                // ignore spaces
                // parse the number
                int hp = Integer.parseInt(objw.substring(2).trim());
                return searchHP(hp);
            } catch (NumberFormatException e) {
                // Ignore and continue search...
            }
        }

        // Search by I18n common name
        for (HipStar star : commonNamesMap) {
            if (star.commonNameI18.toUpperCase().startsWith(objw) ||
                    star.englishCommonName.toUpperCase().startsWith(objw)) {
                return star;
            }
        }

        // Search by sci name
        for (HipStar star : sciNamesMap) {
            if (star.sciName.toUpperCase().startsWith(objw)) {
                return star;
            }
        }

        return null;
    }

    /**
     * Find and return the list of at most maxNbItem objects auto-completing the passed object I18n name
     */
    public List<String> listMatchingObjectsI18n(String objPrefix, int maxNbItem) {
        List<String> finalResult = new ArrayList<String>();
        if (maxNbItem == 0) return finalResult;

        String objw = objPrefix.toUpperCase();

        // avoid duplicates, and order with a TreeSet
        TreeSet<String> result = new TreeSet<String>();
        // Search by common names
        for (HipStar star : commonNamesMap) {
            if (star.commonNameI18.toUpperCase().startsWith(objw)) {
                result.add(star.commonNameI18);
                if (result.size() > maxNbItem)
                    break;
            }
            if (star.englishCommonName.toUpperCase().startsWith(objw)) {
                result.add(star.englishCommonName);
                if (result.size() > maxNbItem)
                    break;
            }
        }

        // Search by sci names
        for (HipStar star : sciNamesMap) {
            if (star.sciName.toUpperCase().startsWith(objw)) {
                result.add(star.sciName);
                if (result.size() > maxNbItem)
                    break;
            }
        }

        finalResult.addAll(result);

        return finalResult;
    }


    /**
     * Define font file name and size to use for star names display
     */
    public void setFont(SFontIfc someFont) throws StellariumException {
        HipStar.starFont = someFont;
    }

    public void setFlagSciNames(boolean f) {
        HipStar.flagSciNames = f;
    }

    public void update(long deltaTime) {
        //        namesFader.update(deltaTime);
        //        starsFader.update(deltaTime);
    }

    void setNamesFadeDuration(float duration) {
        namesFader.setDuration((int) (duration * 1000));
    }

    /**
     * Set display flag for Stars
     */
    public void setStars(boolean b) {
        starsFader.set(b);
    }

    /**
     * Get display flag for Stars
     */
    public boolean getFlagStars() {
        return starsFader.getState();
    }

    /**
     * Set display flag for Star names
     */
    public void setStarNames(boolean b) {
        namesFader.set(b);
    }

    /**
     * Get display flag for Star names
     */
    public boolean getStarNames() {
        return namesFader.getState();
    }

    /**
     * Set display flag for Star Scientific names
     */
    public void setStarSciNames(boolean b) {
        flagStarSciName = b;
    }

    /**
     * Get display flag for Star Scientific names
     */
    public boolean getFlagStarSciName() {
        return flagStarSciName;
    }

    /**
     * Set flag for Star twinkling
     */
    public void setStarTwinkle(boolean b) {
        flagStarTwinkle = b;
    }

    /**
     * Get flag for Star twinkling
     */
    public boolean getStarTwinkle() {
        return flagStarTwinkle;
    }

    /**
     * Set flag for displaying Star as GLpoints (faster but not so nice)
     */
    public void setPointStar(boolean b) {
        flagPointStar = b;
    }

    /**
     * Get flag for displaying Star as GLpoints (faster but not so nice)
     */
    public boolean getPointStar() {
        return flagPointStar;
    }

    /**
     * Set maximum magnitude at which stars names are displayed
     */
    public void setMaxMagStarName(float b) {
        maxMagStarName = b;
    }

    /**
     * Get maximum magnitude at which stars names are displayed
     */
    public float getMaxMagStarName() {
        return maxMagStarName;
    }

    /**
     * Set maximum magnitude at which stars scientific names are displayed
     */
    public void setMaxMagStarSciName(float b) {
        maxMagStarSciName = b;
    }

    /**
     * Get maximum magnitude at which stars scientific names are displayed
     */
    public float getMaxMagStarSciName() {
        return maxMagStarSciName;
    }

    /**
     * Set base stars display scaling factor
     */
    public void setStarScale(float b) {
        starScale = b;
    }

    /**
     * Get base stars display scaling factor
     */
    public float getStarScale() {
        return starScale;
    }

    /**
     * Set stars display scaling factor wrt magnitude
     */
    public void setStarMagScale(float b) {
        starMagScale = b;
    }

    /**
     * Get base stars display scaling factor wrt magnitude
     */
    public float getStarMagScale() {
        return starMagScale;
    }

    /**
     * Set stars twinkle amount
     */
    public void setStarTwinkleAmount(float b) {
        twinkleAmount = b;
    }

    /**
     * Get stars twinkle amount
     */
    public float getStarTwinkleAmount() {
        return twinkleAmount;
    }

    /**
     * Set stars limiting display magnitude
     */
    public void setStarLimitingMag(float f) {
        limitingMag = f;
    }

    /**
     * Get stars limiting display magnitude
     */
    public float getStarLimitingMag() {
        return limitingMag;
    }

    LinearFader namesFader = new LinearFader();

    LinearFader starsFader = new LinearFader();

    float starScale;

    float starMagScale;

    boolean flagStarName;

    boolean flagStarSciName;

    float maxMagStarName;

    float maxMagStarSciName;

    boolean flagStarTwinkle;

    float twinkleAmount;

    boolean flagPointStar;

    boolean gravityLabel;

    float limitingMag = 6.5f;// limiting magnitude at 60 degree fov

    /**
     * array of star vector with the grid id as array rank
     */
    private List<List<HipStar>> starZones;

    /**
     * Grid for opimisation
     */
    private Grid hipGrid = new Grid();

    /**
     * The map of Hipparcos Stars per hp number
     * The simple array of the star for sequential research
     */
    private Map<Integer, HipStar> starFlatArray;

    private STexture starTexture;

    // TODO: FRED We need to use lucene and remove all this very painful names
    // management and search. From java point-of-view all the string manaipulation and
    // search done by original stellarium needs to be removed
    private Set<HipStar> commonNamesMap = new TreeSet<HipStar>(new HipStar.EnglishNameComparator());

    private Set<HipStar> sciNamesMap = new TreeSet<HipStar>(new HipStar.ScientificNameComparator());

    public PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public static final double RADIUS_STAR = 1;
}