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
package org.stellarium.astro;

import org.stellarium.*;
import org.stellarium.data.DataFileUtil;
import org.stellarium.projector.Projector;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STextureFactory;

import javax.vecmath.Point2i;
import javax.vecmath.Point3d;
import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * <p>Manage group of constellations.</p>
 * <p/>
 * See <a href="http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/constellation_mgr.cpp?rev=1.53&view=markup">C++ version</a> of this file.
 *
 * @author <a href="mailto:rr0@rr0.org"/>Jerome Beau</a>
 * @author Fred Simon
 * @version 2.0.0
 * @since 1.0.0
 */
public class ConstellationMgr {

    private Logger logger;
    private STextureFactory textureFactory;

    /**
     * loads all data from appropriate files
     *
     * @param hipStars
     * @param parentLogger
     * @throws org.stellarium.StellariumException
     *
     */
    public ConstellationMgr(HipStarMgr hipStars, Logger parentLogger) throws StellariumException {
        assert hipStars != null : "Hiparcos stars catalog cannot be null";
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);
        hipStarMgr = hipStars;
    }

    public void setFlagGravityLabel(boolean g) {
        Constellation.gravityLabel = g;
    }

    public void setLineColor(Color c) {
        Constellation.lineColor = c;
    }

    public Color getLineColor() {
        return Constellation.lineColor;
    }

    public void setBoundaryColor(Color c) {
        Constellation.boundaryColor = c;
    }

    public Color getBoundaryColor() {
        return Constellation.boundaryColor;
    }

    public void setLabelColor(Color c) {
        Constellation.labelColor = c;
    }

    public Color getLabelColor() {
        return Constellation.labelColor;
    }

    public void setFont(SFontIfc someFont) throws StellariumException {
        asterFont = someFont;
    }

    public boolean isEmpty() {
        return asterisms.isEmpty();
    }

    /**
     * Load from file
     */
    public void loadLinesAndArt(URL consDataFile, URL artConsFile, URL boundaryFile) throws StellariumException {
        selected = null;

        logger.fine("Loading Constellation data from " + consDataFile);
        List<String> lines = DataFileUtil.getLines(consDataFile, "constellation data", true);

        // Delete existing data, if any
        asterisms.clear();

        int lineNumber = 0;
        int total = lines.size();

        for (String record : lines) {
            if (record.length() == 0 || record.charAt(0) == '#') {
                continue;
            }
            lineNumber++;
            Constellation cons = new Constellation(logger);
            cons.read(record, hipStarMgr);
            asterisms.put(cons.getShortName().toUpperCase(), cons);

            // Notify progress
            float percentage = (float) (lineNumber / total);
            propertyChangeSupport.firePropertyChange("Loading " + total + " constellations", -1f, percentage);
        }
        logger.fine(lineNumber + " constellations loaded");

        // Set current states
        setArtEnabled(artEnabled);
        setLinesEnabled(linesEnabled);
        setNamesEnabled(namesEnabled);
        setBoundariesEnabled(boundariesEnabled);

        loadBoundaries(boundaryFile);

        lines = DataFileUtil.getLines(artConsFile, "art file",
                false);// no art, but still loaded constellation data

        // Read the constellation art file with the following format :
        // ShortName texture_file x1 y1 hp1 x2 y2 hp2
        // Where :
        // shortname is the international short name (i.e "Lep" for Lepus)
        // texture_file is the graphic file of the art texture
        // x1 y1 are the x and y texture coordinates in pixels of the star of hipparcos number hp1
        // x2 y2 are the x and y texture coordinates in pixels of the star of hipparcos number hp2
        // The coordinate are taken with (0,0) at the top left corner of the image file
        String shortname;
        String texfile;
        int hp1, hp2, hp3;
        Point2i p1, p2, p3;
        total = lines.size();

        int current = 0;
        glDisable(GL_BLEND);

        for (String line : lines) {
            if (!StelUtility.isEmpty(line)) {
                try {
                    StringTokenizer st = new StringTokenizer(line);
                    shortname = st.nextToken();
                    texfile = st.nextToken();
                    p1 = new Point2i(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
                    hp1 = Integer.parseInt(st.nextToken());
                    p2 = new Point2i(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
                    hp2 = Integer.parseInt(st.nextToken());
                    p3 = new Point2i(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
                    hp3 = Integer.parseInt(st.nextToken());
                } catch (Exception e) {
                    throw new StellariumException("Error while loading art for constellation " + line, e);
                }

                // Notify progress
                float percentage = (float) ((current + 1) / total);
                propertyChangeSupport.firePropertyChange("Loading " + total + " constellation art", -1f, percentage);

                Constellation cons = findFromAbbreviation(shortname);
                if (cons == null) {
                    logger.severe("Can't find constellation called : " + shortname);
                } else {
                    cons.artTexture = textureFactory.createTexture(texfile);
                    cons.s1 = hipStarMgr.searchHP(hp1).getObsJ2000Pos(null);
                    cons.p1 = p1;
                    cons.s2 = hipStarMgr.searchHP(hp2).getObsJ2000Pos(null);
                    cons.p2 = p2;
                    cons.s3 = hipStarMgr.searchHP(hp3).getObsJ2000Pos(null);
                    cons.p3 = p3;
                }
                current++;
            }
        }
    }

    /**
     * Draw all the constellations in the vector
     */
    public void draw(Projector prj, NavigatorIfc nav) {
        prj.setOrthographicProjection();
        try {
            drawLines(prj);
            drawNames(prj);
            drawArt(prj, nav);
            drawBoundaries(prj);
        } finally {
            prj.resetPerspectiveProjection();
        }
    }

    public void drawArt(Projector prj, NavigatorIfc nav) {
        glBlendFunc(GL_ONE, GL_ONE);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_CULL_FACE);

        for (Constellation iter : asterisms.values()) {
            iter.drawArtOptim(prj, nav);
        }

        glDisable(GL_CULL_FACE);
    }

    public void drawLines(Projector someProjector) {
        for (Constellation iter : asterisms.values()) {
            iter.drawOptim(someProjector);
        }
    }

    /**
     * Draw the names of all the constellations
     */
    public void drawNames(Projector prj) {
        if (asterisms.values().iterator().next().nameFader.hasInterstate()) {
            glEnable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);

            glBlendFunc(GL_ONE, GL_ONE);
            // if (draw_mode == DM_NORMAL) glBlendFunc(GL_ONE, GL_ONE);
            // else glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // charting

            for (Constellation constellation : asterisms.values()) {
                // Check if in the field of view
                if (prj.projectJ2000Check(constellation.xyzName, constellation.XYName))
                    constellation.drawName(asterFont, prj);
            }
        }
    }

    public Constellation isStarIn(StelObject s) {
        for (Constellation iter : asterisms.values()) {
            // Check if the star is in one of the constellation
            if (iter.isStarIn(s) != null) return iter;
        }
        return null;
    }

    public Constellation findFromAbbreviation(String shortname) {
        // searchByEnglishName in uppercase only
        return asterisms.get(shortname.toUpperCase());
    }

    /**
     * Read constellation names from the given file.
     *
     * @param namesFile Name of the file containing the constellation names in english
     */
    public void loadNames(URL namesFile) throws StellariumException {
        // Constellation not loaded yet
        if (asterisms.isEmpty()) return;

        // clear previous names
        for (Constellation constellation : asterisms.values()) {
            constellation.englishName = null;
        }

        // read in translated common names from file
        List<String> lines = DataFileUtil.getLines(namesFile, "constellations names", false);

        if (lines == null) {
            return;
        }

        // find matching constellation and update name
        String tmpShortName;
        Constellation aster;

        for (String record : lines) {
            if (!StelUtility.isEmpty(record)) {
                StringTokenizer tokenizer = new StringTokenizer(record);
                tmpShortName = tokenizer.nextToken();

                //System.out.println("working on short name " + tmpShortName);

                aster = findFromAbbreviation(tmpShortName);
                if (aster != null) {
                    // Read the names in english
                    aster.englishName = tokenizer.nextToken();
                }
            }
        }
    }

    /**
     * Update i18 names from english names according to current locale
     * The translation is done using gettext with translated strings defined in translations.h
     */
    public void translateNames(Translator trans) {
        for (Constellation constellation : asterisms.values()) {
            constellation.nameI18 = trans.translate(constellation.englishName);
        }
    }

    /*public void update(long deltaTime) {
        for (Constellation constellation : asterisms.values()) {
            constellation.update(deltaTime);
        }
    }*/

    /**
     * Set constellation maximum art intensity
     *
     * @param max
     */
    public void setArtIntensity(float max) {
        artMaxIntensity = max;
        for (Constellation constellation : asterisms.values()) {
            constellation.artFader.setMaxValue(max);
        }
    }

    /**
     * Set constellation maximum art intensity
     *
     * @return
     */
    public double getArtIntensity() {
        return artMaxIntensity;
    }

    /**
     * Set constellation art fade duration
     *
     * @param duration
     */
    public void setArtFadeDuration(float duration) {
        artFadeDuration = duration;
        for (Constellation constellation : asterisms.values()) {
            constellation.artFader.setDuration((int) (duration * 1000.f));
        }
    }

    /**
     * Get constellation art fade duration
     *
     * @return
     */
    public double getArtFadeDuration() {
        return artFadeDuration;
    }

    /**
     * Set whether constellation path lines will be displayed
     *
     * @param enabled If the constellation lines must be displayed
     */
    public void setLinesEnabled(boolean enabled) {
        linesEnabled = enabled;
        if (selected != null && isolateSelected) {
            selected.setFlagLines(enabled);
        } else {
            for (Constellation constellation : asterisms.values()) {
                constellation.setFlagLines(enabled);
            }
        }
    }

    /**
     * Get whether constellation path lines are displayed
     *
     * @return
     */
    public boolean isLinesEnabled() {
        return linesEnabled;
    }

    /**
     * Set whether constellation boundaries lines will be displayed
     *
     * @param b
     */
    public void setBoundariesEnabled(boolean b) {
        boundariesEnabled = b;
        if (selected != null && isolateSelected) {
            selected.setFlagBoundaries(b);
        } else {
            for (Constellation constellation : asterisms.values()) {
                constellation.setFlagBoundaries(b);
            }
        }
    }

    /**
     * Get whether constellation boundaries lines are displayed
     *
     * @return
     */
    public boolean getBoundariesEnabled() {
        return boundariesEnabled;
    }

    /**
     * Set whether constellation art will be displayed
     *
     * @param b
     */
    public void setArtEnabled(boolean b) {
        artEnabled = b;
        if (selected != null && isolateSelected) {
            selected.setArtEnabled(b);
        } else {
            for (Constellation constellation : asterisms.values()) {
                constellation.setArtEnabled(b);
            }
        }
    }

    /**
     * Get whether constellation art is displayed
     *
     * @return
     */
    public boolean isArtEnabled() {
        return artEnabled;
    }

    /**
     * Set whether constellation names will be displayed
     *
     * @param enabled If the names must be displayed.
     */
    public void setNamesEnabled(boolean enabled) {
        namesEnabled = enabled;
        if (selected != null && isolateSelected) {
            selected.setNameEnabled(enabled);
        } else {
            for (Constellation constellation : asterisms.values()) {
                constellation.setNameEnabled(enabled);
            }
        }
    }

    /**
     * Get whether constellation names are displayed
     *
     * @return
     */
    public boolean getNamesEnabled() {
        return namesEnabled;
    }

    /**
     * Set whether selected constellation must be displayed alone
     */
    public void setFlagIsolateSelected(boolean s) {
        isolateSelected = s;
        setSelectedConst(selected);
    }

    /**
     * Get whether selected constellation is displayed alone
     */
    public boolean getFlagIsolateSelected() {
        return isolateSelected;
    }

    /**
     * Define which constellation is selected from its abbreviation
     */
    public void setSelected(String abbreviation) {
        setSelectedConst(findFromAbbreviation(abbreviation));
    }

    /**
     * Define which constellation is selected from a star number
     */
    public void setSelected(StelObject s) {
        if (s == null) setSelectedConst(null);
        else setSelectedConst(isStarIn(s));
    }

    public StelObject getSelected() {
        return selected;
    }

    void setSelectedConst(Constellation c) {
        // update states for other constellations to fade them out
        if (c != null) {
            Constellation cc;
            // Propagate old parameters new newly selected constellation
            if (selected != null) cc = selected;
            else cc = asterisms.values().iterator().next();
            c.setAllFlags(cc);
            selected = c;

            if (isolateSelected) {
                for (Constellation iter : asterisms.values()) {
                    if (iter != selected) {
                        iter.setAllFlags(null);
                    }
                }
                Constellation.singleSelected = true;
            } else {
                for (Constellation iter : asterisms.values()) {
                    iter.setAllFlags(c);
                }
                Constellation.singleSelected = false;
            }
        } else {
            if (selected == null) return;
            for (Constellation iter : asterisms.values()) {
                if (iter != selected) {
                    iter.setAllFlags(selected);
                }
            }
            selected = null;
        }
    }

    // Load from file
    boolean loadBoundaries(URL boundaryFile) throws StellariumException {
        Constellation cons = null;

        allBoundarySegments.clear();

        logger.fine("Loading Constellation boundary data from " + boundaryFile + " ...");
        // Modified boundary file by Torsten Bronger with permission
        // http://pp3.sourceforge.net

        List<String> lines = DataFileUtil.getLines(boundaryFile, "Constellation boundary data", false);

        if (lines == null) {
            return false;
        }

        double DE, RA;
        Point3d XYZ;
        int num, numc;
        List<Point3d> points = null;
        String consname;
        int nbSegments = 0;
        int nbSegmentsDrop = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (StelUtility.isEmpty(line))
                continue;

            try {
                points = new ArrayList<Point3d>();

                num = 0;
                StringTokenizer tokenizer = new StringTokenizer(line);
                num = Integer.parseInt(tokenizer.nextToken());
                if (num == 0)
                    continue;// empty line

                for (int j = 0; j < num; j++) {
                    if (!tokenizer.hasMoreTokens()) {
                        i++;
                        line = lines.get(i);
                        tokenizer = new StringTokenizer(line);
                    }
                    RA = Double.parseDouble(tokenizer.nextToken());
                    DE = Double.parseDouble(tokenizer.nextToken());

                    RA *= Math.PI / 12.;// Convert from hours to rad
                    DE = Math.toRadians(DE);

                    // Calc the Cartesian coord with RA and DE
                    XYZ = new Point3d();
                    StelUtility.spheToRect(RA, DE, XYZ);
                    points.add(XYZ);
                }

                // this list is for the de-allocation
                allBoundarySegments.add(points);

                numc = Integer.parseInt(tokenizer.nextToken());
                // there are 2 constellations per boundary

                for (int j = 0; j < numc; j++) {
                    consname = tokenizer.nextToken();
                    // not used?
                    if (consname.equalsIgnoreCase("SER1") || consname.equalsIgnoreCase("SER2"))
                        consname = "SER";

                    cons = findFromAbbreviation(consname);
                    if (cons == null)
                        logger.warning("Can't find constellation called : " + consname);
                    else
                        cons.isolatedBoundarySegments.add(points);
                }

                if (cons != null) {
                    cons.sharedBoundarySegments.add(points);
                    nbSegments++;
                }

            } catch (NumberFormatException e) {
                logger.severe("Error while parsing line number " + i + " a number is unreadable in " + line);
                e.printStackTrace();
                nbSegmentsDrop++;
            } catch (NoSuchElementException e) {
                logger.severe("Error parsing line number " + i + " the number of elements is incorrect in " + line);
                e.printStackTrace();
                nbSegmentsDrop++;
            }
        }

        logger.fine(nbSegments + " constellation boundaries segments loaded, " + nbSegmentsDrop + " data drop]");

        return true;
    }

    // Draw constellations lines
    void drawBoundaries(Projector prj) {
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);

        glLineStipple(2, (short) 0x3333);
        glEnable(GL_LINE_STIPPLE);

        for (Constellation iter : asterisms.values()) {
            iter.drawBoundaryOptim(prj);
        }
        glDisable(GL_LINE_STIPPLE);
    }

    ///unsigned int ConstellationMgr::getFirstSelectedHP(void) {
    ///  if (selected) return selected->asterism[0]->get_hp_number();
    ///  return 0;
    ///}

    /**
     * Return the matching constellation object's pointer if exists or NULL
     *
     * @param nameI18n The case sensistive constellation name
     */
    public StelObject searchByNameI18n(String nameI18n) {
        for (Constellation iter : asterisms.values()) {
            if (iter.nameI18.equalsIgnoreCase(nameI18n))
                return iter;
        }
        return null;
    }

    public List<String> listMatchingObjectsI18n(String objPrefix) {
        return listMatchingObjectsI18n(objPrefix, 5);
    }

    /**
     * Find and return the list of at most maxNbItem objects auto-completing the passed object I18n name
     */
    public List<String> listMatchingObjectsI18n(String objPrefix, int maxNbItem) {
        List<String> result = new ArrayList<String>();
        if (maxNbItem == 0) return result;

        String objw = objPrefix.toUpperCase();

        for (Constellation iter : asterisms.values()) {
            if (iter.getNameI18n().toUpperCase().startsWith(objw)) {
                result.add(iter.getNameI18n());
                if (result.size() == maxNbItem)
                    return result;
            }
        }
        return result;
    }

    private Map<String, Constellation> asterisms = new HashMap<String, Constellation>();

    private SFontIfc asterFont;

    private final HipStarMgr hipStarMgr;

    private Constellation selected;

    private boolean isolateSelected;

    // TODO: Fred never used even in the C++ version...
    private List<List<Point3d>> allBoundarySegments = new ArrayList<List<Point3d>>();

    // These are THE master settings - individual constellation settings can vary based on selection status
    private boolean namesEnabled;

    private boolean linesEnabled;

    private boolean artEnabled;

    private boolean boundariesEnabled;

    private double artFadeDuration;

    private double artMaxIntensity;

    public PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
}