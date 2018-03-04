/*
 * This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau
 * and is a Java version of the original Stellarium C++ version,
 * Author and Copyright of this file and of the stellarium telescope feature:
 * Johannes Gajdosik, 2006
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
package org.stellarium.telescope;

import org.stellarium.NavigatorIfc;
import org.stellarium.StelObject;
import org.stellarium.StelUtility;
import org.stellarium.StellariumException;
import org.stellarium.data.IniFileParser;
import org.stellarium.projector.Projector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.media.opengl.GL;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.stellarium.ui.SglAccess.*;

/**
 * @author Jerome Beau, Fred Simon
 * @version 27 oct. 2006 01:54:22
 */
public class TelescopeMgr {
    private Logger logger;
    private STextureFactory textureFactory;

    public TelescopeMgr(Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);
    }

    public void close() {
        if (telescopeFont != null) {
            //telescope_font.close();
            telescopeFont = null;
        }
        if (telescopeTexture != null) {
            // TODO: FRED There is a need for removing texture from OpenGL lib and other close related issue
            // Need to review close methods
            //telescope_texture.close();
            telescopeTexture = null;
        }
    }

    public void draw(Projector prj, NavigatorIfc nav) {
        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);
        prj.setOrthographicProjection();// set 2D coordinate
        glBindTexture(GL.GL_TEXTURE_2D, telescopeTexture.getID());
        glBlendFunc(GL.GL_ONE, GL.GL_ONE);
        for (Telescope telescope : telescopeMap.values()) {
            if (telescope.isConnected() && telescope.hasKnownPosition()) {
                Point3d xyPoint = new Point3d();
                if (prj.projectJ2000Check(telescope.getObsJ2000Pos(null), xyPoint)) {
                    if (telescopeFader.hasInterstate()) {
                        glColor4f(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(),
                                telescopeFader.getInterstate());
                        double radius = 15;
                        //          double radius = 0.5*prj.getRadPerPixel()*(M_PI/180)*0.5;
                        //          if (radius < 15) radius = 15;
                        //          if (radius > 0.5*prj.getViewportWidth())
                        //            radius = 0.5*prj.getViewportWidth();
                        //          if (radius > 0.5*prj.getViewportHeight())
                        //            radius = 0.5*prj.getViewportHeight();
                        glBegin(GL.GL_QUADS);
                        glTexCoord2i(0, 0);
                        glVertex2d(xyPoint.x - radius, xyPoint.y - radius);// Bottom left
                        glTexCoord2i(1, 0);
                        glVertex2d(xyPoint.x + radius, xyPoint.y - radius);// Bottom right
                        glTexCoord2i(1, 1);
                        glVertex2d(xyPoint.x + radius, xyPoint.y + radius);// Top right
                        glTexCoord2i(0, 1);
                        glVertex2d(xyPoint.x - radius, xyPoint.y + radius);// Top left
                        glEnd();
                    }
                    if (nameFader.hasInterstate()) {
                        glColor4f(labelColor.getRed(), labelColor.getGreen(), labelColor.getBlue(),
                                nameFader.getInterstate());
                        if (prj.isGravityLabelsEnabled()) {
                            prj.printGravity180(telescopeFont, xyPoint.x, xyPoint.y,
                                    telescope.getNameI18n(), true, 6f, -4f);
                        } else {
                            telescopeFont.print(((int) xyPoint.x) + 6, ((int) xyPoint.y) - 4, telescope.getNameI18n());
                        }
                        glBindTexture(GL.GL_TEXTURE_2D, telescopeTexture.getID());
                    }
                }
            }
        }
        prj.resetPerspectiveProjection();
    }

    public void init(IniFileParser conf) throws StellariumException {
        if (telescopeTexture != null) {
            telescopeTexture.close();
        }
        telescopeTexture = textureFactory.createTexture("telescope.png", STexture.TEX_LOAD_TYPE_PNG_SOLID);
        telescopeMap.clear();
        Preferences section = conf.getSection("telescopes");
        String[] allTelescopes;
        try {
            allTelescopes = section.childrenNames();
        } catch (BackingStoreException e) {
            throw new StellariumException(e);
        }
        for (String telKey : allTelescopes) {
            String url = section.get(telKey, "");
            if (!StelUtility.isEmpty(url)) {
                Telescope telescope = Telescope.create(url);
                telescopeMap.put(Integer.parseInt(telKey), telescope);
            }
        }
    }

    public void update(long deltaTime) {
//        nameFader.update(deltaTime);
//        telescopeFader.update(deltaTime);
    }

    public void communicate() {
        if (!telescopeMap.isEmpty()) {

            for (Telescope telescope : telescopeMap.values()) {
                telescope.prepareSelectFds();
            }
            //if (fd_max >= 0) {
            //int select_rc = select(fd_max+1,read_fds,write_fds,0,tv);
            //if (select_rc > 0) {
            for (Telescope telescope : telescopeMap.values()) {
                telescope.handleSelectFds();
            }
            //}
            //}
            //    t = GetNow() - t;
            //    cout << "TelescopeMgr::communicate: " << t << endl;
        }
    }

    void setNamesFadeDuration(float duration) {
        nameFader.setDuration((int) (duration * 1000.f));
    }

    public List<StelObject> searchAround(Point3d pos, double limFieldOfView) {
        List<StelObject> result = new ArrayList<StelObject>();
        Vector3d v = new Vector3d(pos);
        v.normalize();
        double cosLimFov = Math.cos(Math.toRadians(limFieldOfView));
        for (Telescope telescope : telescopeMap.values()) {
            if (StelUtility.dot(telescope.getObsJ2000Pos(null), v) >= cosLimFov) {
                result.add(telescope);
            }
        }
        return result;
    }

    public StelObject searchByNameI18n(String nameI18n) {
        for (Telescope telescope : telescopeMap.values()) {
            if (telescope.getNameI18n().equalsIgnoreCase(nameI18n)) {
                return telescope;
            }
        }
        return null;
    }

    public List<String> listMatchingObjectsI18n(String objPrefix, int maxNbItem) {
        List<String> result = new ArrayList<String>();
        String objw = objPrefix.toUpperCase();
        for (Telescope telescope : telescopeMap.values()) {
            String telName = telescope.getNameI18n();
            if (telName.toUpperCase().startsWith(objw)) {
                result.add(telName);
            }
        }
        Collections.sort(result);
        return result;
    }

    public void setLabelColor(Color c) {
        labelColor = c;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public void setCircleColor(Color c) {
        circleColor = c;
    }

    public Color getCircleColor() {
        return circleColor;
    }

    /**
     * Set display flag for Telescopes
     */
    public void setFlagTelescopes(boolean b) {
        telescopeFader.set(b);
    }

    /**
     * Get display flag for Telescopes
     */
    public boolean getFlagTelescopes() {
        return telescopeFader.getState();
    }

    /**
     * Set display flag for Telescope names
     */
    public void setFlagTelescopeName(boolean b) {
        nameFader.set(b);
    }

    /**
     * Get display flag for Telescope names
     */
    public boolean getFlagTelescopeName() {
        return nameFader.getState();
    }

    /**
     * Define font file name and size to use for telescope names display
     *
     * @param someFont
     * @throws StellariumException
     */
    public void setFont(SFontIfc someFont) throws StellariumException {
        telescopeFont = someFont;
    }

    /**
     * send a J2000-goto-command to the specified telescope
     *
     * @param telescopeNr
     * @param j2000Pos
     */
    public void telescopeGoto(int telescopeNr, Point3d j2000Pos) {
        Telescope foundTel = telescopeMap.get(telescopeNr);
        if (foundTel != null) {
            foundTel.telescopeGoto(j2000Pos);
        }
    }

    private LinearFader nameFader = new LinearFader();

    private LinearFader telescopeFader = new LinearFader();

    private Color circleColor;

    private Color labelColor;

    SFontIfc telescopeFont;

    STexture telescopeTexture;

    class TelescopeMap extends HashMap<Integer, Telescope> {
        public TelescopeMap() {
            clear();
        }

        public void clear() {
            for (Telescope telescope : values()) {
                telescope.close();
            }
        }
    }

    TelescopeMap telescopeMap = new TelescopeMap();
}