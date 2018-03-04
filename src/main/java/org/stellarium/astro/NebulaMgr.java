/*
 * Stellarium
 * Copyright (C) 2002 Fabien Ch�reau
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
import org.stellarium.projector.DefaultProjector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STextureFactory;

import javax.media.opengl.GL;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static org.stellarium.ui.SglAccess.glBlendFunc;
import static org.stellarium.ui.SglAccess.glEnable;

/**
 * class used to manage groups of Nebulas
 *
 * @author Jerome Beau, Fred Simon
 * @version 0.8.2
 */
public class NebulaMgr {

    protected final Logger logger;
    protected final STextureFactory textureFactory;

    public enum Catalogs {
        M, NGC, IC, UGC
    }

    public static final double RADIUS_NEB = 1;

    public void setLabelColor(Color c) {
        Nebula.labelColor = c;
    }

    public Color getLabelColor() {
        return Nebula.labelColor;
    }

    public void setCircleColor(Color c) {
        Nebula.circleColor = c;
    }

    public Color getCircleColor() {
        return Nebula.circleColor;
    }

    public void setNebulaCircleScale(float scale) {
        Nebula.circleScale = scale;
    }

    public double getNebulaCircleScale() {
        return Nebula.circleScale;
    }

    public void setFlagBright(boolean b) {
        Nebula.flagBright = b;
    }

    public boolean getFlagBright() {
        return Nebula.flagBright;
    }

    public NebulaMgr(Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);

        int nbZones = nebGrid.getNbPoints();
        nebZones = new ArrayList<List<Nebula>>(nbZones);
        for (int i = 0; i < nbZones; i++)
            nebZones.add(new ArrayList<Nebula>());
    }

    /**
     * read from stream
     */
    public void read(SFontIfc someFont, URL catNGC, URL catNGCNames, URL catTextures) throws StellariumException {
        loadNGC(catNGC);
        loadNGCNames(catNGCNames);
        loadTextures(catTextures);

        Nebula.nebulaFont = someFont;

        if (Nebula.texCircle == null)
            Nebula.texCircle = textureFactory.createTexture("neb.png");// Load circle texture
    }

    /**
     * Draw all the Nebulaes
     *
     * @param prj
     * @param eye
     * @param nav
     * @throws StellariumException
     */
    public void draw(DefaultProjector prj, Navigator nav, ToneReproductor eye) throws StellariumException {
        Nebula.hintsBrightness = hintsFader.getInterstate() * flagShow.getInterstate();

        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);

        glBlendFunc(GL.GL_ONE, GL.GL_ONE);
        // if (draw_mode == DM_NORMAL) glBlendFunc(GL_ONE, GL_ONE);
        // else glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // charting

        Vector3d pXYZ;

        // Find the star zones which are in the screen
        int nbZones = 0;
        // FOV is currently measured vertically, so need to adjust for wide screens
        // TODO: projector should probably use largest measurement itself
        double maxFieldOfView = Math.max(prj.getFieldOfView(), prj.getFieldOfView() * prj.getViewportWidth() / prj.getViewportHeight());
        nbZones = nebGrid.intersect(nav.getPrecEquVision(), 1.2d * Math.toRadians(maxFieldOfView));
        // static
        int[] zoneList = nebGrid.getResult();

        prj.setOrthographicProjection();// set 2D coordinate
        try {
// speed up the computation of n->get_on_screen_size(prj, nav)>5:
            double sizeLimit = Math.toRadians(5.0) * (prj.getFieldOfView() / prj.getViewportHeight());

            for (int i = 0; i < nbZones; ++i) {
                List<Nebula> nebulasInZone = nebZones.get(zoneList[i]);
                for (Nebula n : nebulasInZone) {
                    if (!displayNoTexture && !n.hasTex()) {
                        continue;
                    }

                    // improve performance by skipping if too small to see
                    // TODO: skip if too faint to see
                    if (n.getAngularSize() > sizeLimit || (hintsFader.getInterstate() > 0.0001 && n.mag <= getMaxMagHints())) {
                        if (!prj.projectJ2000Check(n.XYZ, n.XY)) {
                            continue;
                        }

                        if (n.hasTex()) {
                            n.drawTex(prj, nav, eye);
                        } else {
                            n.drawNoTex(prj, nav, eye);
                        }

                        if (hintsFader.getInterstate() > 0.00001 && n.mag <= getMaxMagHints()) {
                            n.drawName(prj);
                            n.drawCircle(prj, nav);
                        }
                    }
                }
            }
        } finally {
            prj.resetPerspectiveProjection();
        }
    }

    public void update(long deltaTime) {
        //        hintsFader.update(deltaTime);
        //        flagShow.update(deltaTime);
    }

    /**
     * Search a Nebula by its name.
     * The name format should follow [catalog abbrev: M NGC IC UGC][ _]number
     *
     * @param name The name of the nebula to lookup.
     * @return The found Nebula, or null if not found.
     */
    public StelObject search(String name) {
        if (name == null || name.length() == 0)
            return null;

        name = name.trim().toUpperCase();
        for (Nebula nebula : nebArray) {
            if (nebula.getEnglishName().equalsIgnoreCase(name)) {
                return nebula;
            }
        }

        // If no match found, try search by catalog reference
        Catalogs cat = null;

        Catalogs[] catalogs = Catalogs.values();
        for (Catalogs catalog : catalogs) {
            if (name.startsWith(catalog.name())) {
                cat = catalog;
                break;
            }
        }
        // No catalog found
        if (cat == null) {
            return null;
        }

        int num = -1;
        // read the number from first digit found
        for (int pos = cat.name().length(); pos < name.length(); pos++) {
            if (Character.isDigit(name.charAt(pos))) {
                num = Integer.parseInt(name.substring(pos));
                break;
            }
        }
        // No number found
        if (num == -1) {
            return null;
        }

        switch (cat) {
            case M:
                return searchM(num);
            case NGC:
                return searchNGC(num);
            case IC:
                return searchIC(num);
            /*if (cat == "UGC") return searchUGC(num);*/
        }

        return null;
    }

    /**
     * Look for a nebulae by XYZ coords
     *
     * @param pos
     * @return
     */
    public StelObject search(Vector3d pos) {
        pos.normalize();
        Nebula plusProche = null;
        double anglePlusProche = 0;
        for (Nebula iter : nebArray) {
            if (iter.XYZ.x * pos.x + iter.XYZ.y * pos.y + iter.XYZ.z * pos.z > anglePlusProche) {
                anglePlusProche = iter.XYZ.x * pos.x + iter.XYZ.y * pos.y + iter.XYZ.z * pos.z;
                plusProche = iter;
            }
        }
        if (anglePlusProche > RADIUS_NEB * 0.999) {
            return plusProche;
        } else {
            return null;
        }
    }

    /**
     * Return a stl vector containing the nebulas located inside the lim_fov circle around position v
     *
     * @param p
     * @param limFieldOfView
     * @return
     */
    public List<StelObject> searchAround(Tuple3d p, double limFieldOfView) {
        List<StelObject> result = new ArrayList<StelObject>();
        Vector3d v = new Vector3d(p);
        v.normalize();
        double cosLimFOV = Math.cos(Math.toRadians(limFieldOfView));
        Vector3d equPos;

        for (Nebula iter : nebArray) {
            equPos = new Vector3d(iter.XYZ);
            equPos.normalize();
            if (equPos.x * v.x + equPos.y * v.y + equPos.z * v.z >= cosLimFOV) {
                result.add(iter);
            }
        }
        return result;
    }

    public Nebula searchM(int mNum) {
        for (Nebula iter : nebArray) {
            if (iter.getMessierNb() == mNum)
                return iter;
        }
        return null;
    }

    Nebula searchNGC(int ngcNum) {
        for (Nebula iter : nebArray) {
            if (iter.getNgcNb() == ngcNum)
                return iter;
        }
        return null;
    }

    Nebula searchIC(int icNum) {
        for (Nebula iter : nebArray) {
            if (iter.getIcNb() == icNum)
                return iter;
        }
        return null;
    }

    /*
    StelObject searchUGC(int ugcNum)
    {
        for (Nebula iter : nebArray) {
            if (iter.getUgcNb() == ugcNum)
                return iter;
        }

        return null;
    }*/

    /**
     * read from stream
     *
     * @param catNGC
     * @return
     * @throws StellariumException
     */
    boolean loadNGC(URL catNGC) throws StellariumException {
        logger.fine("Loading NGC data from " + catNGC);

        LineNumberReader ngcFile;
        try {
            // The line size of NGC dat file is 107 bytes
            // int catalogSize = (int) (catNGC.length() / 107);
            try {
                ngcFile = new LineNumberReader(new InputStreamReader(catNGC.openStream()));
            } catch (FileNotFoundException e) {
                logger.severe("NGC data file " + catNGC + " not found");
                return false;
            }

            // Read the NGC entries
            int i = 0;
            int data_drop = 0;
            String recordLine;
            int catalogSize = 120000;
            while ((recordLine = ngcFile.readLine()) != null) {
                i = ngcFile.getLineNumber();

                // Notify progress ?
                if ((i % 200 == 0) || (i == catalogSize - 1)) {
                    float percentage = (float) (i / catalogSize);
                    propertyChangeSupport.firePropertyChange("Loading NGC catalog of " + catalogSize + " objects", -1f, percentage);
                }

                Nebula e = new Nebula(logger);
                boolean readResult = false;
                try {
                    readResult = e.readNGC(recordLine);
                } catch (Exception e1) {
                    logger.severe("Error reading nebula line " + i + " value " + recordLine +
                            "\n Error:" + e1.getMessage());
                    e1.printStackTrace();
                }
                if (!readResult) {
                    // reading error
                    data_drop++;
                } else {
                    nebArray.add(e);
                    nebZones.get(nebGrid.getNearest(e.XYZ)).add(e);
                }
            }
            ngcFile.close();
            logger.fine("NGC data (" + i + " items loaded, " + data_drop + " dropped]");
        } catch (IOException e) {
            throw new StellariumException(e);
        }

        return true;
    }


    boolean loadNGCNames(URL catNGCNames) throws StellariumException {
        logger.fine("Loading NGC name data...");
        LineNumberReader ngcNameFile;
        try {
            try {
                ngcNameFile = new LineNumberReader(new InputStreamReader(catNGCNames.openStream()));
            } catch (FileNotFoundException e) {
                logger.severe("NGC name data file " + catNGCNames + " not found.");
                return false;
            }

            // Read the names of the NGC objects
            int i = 0;
            String n;
            int nb;
            Nebula e;
            String recordLine;

            while ((recordLine = ngcNameFile.readLine()) != null) {
                nb = Integer.parseInt(recordLine.substring(38, 43).trim());
                if (recordLine.charAt(37) == 'I') {
                    e = searchIC(nb);
                } else {
                    e = searchNGC(nb);
                }

                if (e != null) {
                    // trim the white spaces at the back
                    n = recordLine.substring(0, 37).trim();

                    // If the name is not a messier number perhaps one is already
                    // defined for this object
                    boolean isMessier = n.startsWith("M ");
                    if (!isMessier) {
                        e.setEnglishName(n);
                    }

                    // If it's a messiernumber, we will call it a messier if there is no better name
                    if (isMessier) {
                        int num = Integer.parseInt(n.substring(1).trim());

                        // Let us keep the right number in the Messier catalog
                        e.setMessierNb(num);

                        e.setEnglishName("M" + num);
                    }
                    i++;
                } else {
                    logger.warning("...no position data for " + recordLine);
                }
            }
            ngcNameFile.close();
            logger.fine(i + " NGC names loaded");
        } catch (IOException e1) {
            throw new StellariumException(e1);
        }

        return true;
    }

    boolean loadTextures(URL file) throws StellariumException {
        logger.fine("Loading Nebula Textures from " + file);

        String dataDescription = "nebula catalog";

        List<String> recordLines = DataFileUtil.getLines(file, dataDescription, false);

        if (recordLines == null)
            return false;

        int total = recordLines.size();
        logger.fine(total + " Nebula textures loaded");

        int current = 0;
        int NGC;
        for (String line : recordLines) {
            ++current;

            // Notify progress
            float percentage = (float) (current / total);
            propertyChangeSupport.firePropertyChange("Loading " + total + " nebula textures" + " objects", -1f, percentage);

            StringTokenizer tokenizer = new StringTokenizer(line);
            NGC = Integer.parseInt(tokenizer.nextToken());

            Nebula e = searchNGC(NGC);
            if (e != null) {
                try {
                    e.readTexture(line);
                } catch (Exception e1) {
                    throw new StellariumException("Error while reading texture for nebula " + e.getEnglishName()
                            + ": " + e1.getMessage(), e1);
                }
            } else {
                // Allow non NGC nebulas/textures!

                // System.out.print("Nebula with unrecognized NGC number " + NGC + endl;
                e = new Nebula(logger);
                try {
                    e.readTexture(line);
                } catch (Exception e1) {
                    throw new StellariumException("Error while reading texture for nebula " + e.getEnglishName()
                            + ": " + e1.getMessage(), e1);
                }

                nebArray.add(e);
                nebZones.get(nebGrid.getNearest(e.XYZ)).add(e);
            }

        }
        return true;
    }

    /**
     * Update i18 names from english names according to passed translator
     * The translation is done using gettext with translated strings defined in translations.h
     *
     * @param trans
     */
    public void translateNames(Translator trans) {
        for (Nebula nebula : nebArray) {
            nebula.translateName(trans);
        }
    }


    /**
     * Return the matching Nebula object's pointer if exists or null
     *
     * @param nameI18n
     * @return
     */
    public StelObject searchByNameI18n(String nameI18n) {
        String objw = nameI18n.toUpperCase();

        // Search by NGC numbers (possible formats are "NGC31" or "NGC 31")
        if (objw.startsWith("NGC")) {
            // TODO: Throw a nice exception (or return null) if no number found
            int ngcNb = Integer.parseInt(objw.substring(3).trim());
            for (Nebula nebula : nebArray) {
                if (ngcNb == nebula.getNgcNb())
                    return nebula;
            }
            return null;
        }

        // Search by common names
        for (Nebula nebula : nebArray) {
            if (objw.equalsIgnoreCase(nebula.getNameI18n())) {
                return nebula;
            }
        }

        // Search by Messier numbers (possible formats are "M31" or "M 31")
        if (objw.startsWith("M")) {
            // TODO: Throw a nice exception (or return null) if no number found
            int messierNb = Integer.parseInt(objw.substring(1).trim());
            for (Nebula nebula : nebArray) {
                if (messierNb == nebula.getMessierNb())
                    return nebula;
            }
            return null;
        }

        return null;
    }

    //! Find and return the list of at most maxNbItem objects auto-completing the passed object I18n name
    public List<String> listMatchingObjectsI18n(String objPrefix, int maxNbItem) {
        SortedSet<String> result = new TreeSet<String>();

        if (maxNbItem == 0)
            return new ArrayList<String>();

        String objw = objPrefix.toUpperCase();

        // Search by messier objects number (possible formats are "M31" or "M 31")
        if (objw.length() >= 1 && objw.startsWith("M")) {
            for (Nebula nebula : nebArray) {
                if (nebula.getMessierNb() == 0)
                    continue;
                String constw = "M" + nebula.getMessierNb();
                if (constw.startsWith(objw)) {
                    result.add(constw);
                    continue;// Prevent adding both forms for name
                }
                constw = "M " + nebula.getMessierNb();
                if (constw.startsWith(objw)) {
                    result.add(constw);
                }
            }
        }

        // Search by NGC numbers (possible formats are "NGC31" or "NGC 31")
        for (Nebula nebula : nebArray) {
            if (nebula.getNgcNb() == 0)
                continue;
            String constw = "NGC" + nebula.getNgcNb();
            if (constw.startsWith(objw)) {
                result.add(constw);
                continue;
            }
            constw = "NGC " + nebula.getNgcNb();
            if (constw.startsWith(objw)) {
                result.add(constw);
            }
        }

        // Search by common names
        for (Nebula nebula : nebArray) {
            if (nebula.getNameI18n().startsWith(objw)) {
                result.add(nebula.getNameI18n());
            }
        }

        List<String> finalResults;
        if (result.size() > maxNbItem) {
            finalResults = new ArrayList<String>(maxNbItem);
            int i = 0;
            for (String s : result) {
                finalResults.add(s);
                i++;
                if (i >= maxNbItem)
                    break;
            }
        } else {
            finalResults = new ArrayList<String>(result);
        }

        return finalResults;
    }

    public void setMaxMagHints(float maxMagHints) {
        this.maxMagHints = maxMagHints;
    }

    public float getMaxMagHints() {
        return maxMagHints;
    }

    void setHintsFadeDuration(float duration) {
        hintsFader.setDuration((int) (duration * 1000.f));
    }

    public void setFlagHints(boolean b) {
        hintsFader.set(b);
    }

    public boolean getFlagHints() {
        return hintsFader.getState();
    }

    public void setFlagShow(boolean b) {
        flagShow.set(b);
    }

    public boolean getFlagShow() {
        return flagShow.getState();
    }

    public boolean getFlagDisplayNoTexture() {
        return displayNoTexture;
    }

    public void setFlagDisplayNoTexture(boolean displayNoTexture) {
        this.displayNoTexture = displayNoTexture;
    }

    private BufferedReader nebulaFic;

    /**
     * The nebulas list
     */
    private List<Nebula> nebArray = new ArrayList<Nebula>();

    private LinearFader hintsFader = new LinearFader();

    private LinearFader flagShow = new LinearFader();

    /**
     * Grid for opimisation
     */
    Grid nebGrid = new Grid();

    /**
     * array of nebula vector with the grid id as array rank
     */
    private List<List<Nebula>> nebZones;

    /**
     * Define maximum magnitude at which nebulae hints are displayed
     */
    float maxMagHints;

    /**
     * Define if nebulas without textures are to be displayed
     */
    boolean displayNoTexture;

    public PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
}