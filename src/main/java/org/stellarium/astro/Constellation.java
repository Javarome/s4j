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
import org.stellarium.projector.DefaultProjector;
import org.stellarium.projector.Projector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;

import javax.vecmath.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * Constellation.
 * <p/>
 * See <a href="http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/constellation.cpp?rev=1.60&view=markup">C++ version</a> of this file.
 *
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>
 * @version Java
 */
public class Constellation extends StelObjectBase {

    static final double RADIUS_CONST = 1;

    static {
        lineColor = new Color(.4f, .4f, .8f);
        labelColor = new Color(.4f, .4f, .8f);
        boundaryColor = new Color(0.8f, 0.3f, 0.3f);
    }

    Constellation(Logger parentLogger) {
        this(new LinearFader(), new LinearFader(), new LinearFader(), new LinearFader(), parentLogger);
    }

    Constellation(LinearFader someArtFader, LinearFader someLineFader, LinearFader someNameFader, LinearFader someBoundaryFader, Logger parentLogger) {
        super(parentLogger);
        artFader = someArtFader;
        lineFader = someLineFader;
        nameFader = someNameFader;
        boundaryFader = someBoundaryFader;
    }

    /**
     * Read Constellation datas and grab cartesian positions of stars
     *
     * @param record
     * @param vouteCeleste
     * @throws StellariumException If an error occured while parsing the record.
     */
    void read(String record, HipStarMgr vouteCeleste) throws StellariumException {
        int hpNumber;

        StringTokenizer tokenizer = new StringTokenizer(record);
        abbreviation = tokenizer.nextToken();
        nbSegments = Integer.parseInt(tokenizer.nextToken());

        // TODO: Fred no need for Uppercase here since the Manager map has the key in uppercase
        // make abbreviation uppercase for case insensitive searches
        abbreviation = abbreviation.toUpperCase();

        asterism = new ArrayList<StelObject>(nbSegments);
        for (int i = 0; i < nbSegments * 2; ++i) {
            String token = tokenizer.nextToken();
            hpNumber = Integer.parseInt(token);
            if (hpNumber == 0) {
                throw new StellariumException("Error while reading constellation data: Hipparcos number is 0 for token \"" + token + "\"");
            }

            HipStar star = vouteCeleste.searchHP(hpNumber);
            if (star == null) {
                throw new StellariumException("Error while reading constellation data: Star not found for Hipparcos number " + hpNumber);
            }
            asterism.add(star);
        }

        xyzName = new Point3d();
        for (StelObject stelObject : asterism) {
            xyzName.add(stelObject.getObsJ2000Pos(null));
        }
        xyzName.scale(1. / (nbSegments * 2));
    }

    /**
     * Draw the lines for the Constellation using the coords of the stars
     * (optimized for use thru the class ConstellationMgr only)
     *
     * @param prj
     */
    void drawOptim(Projector prj) {
        if (lineFader.hasInterstate()) {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);// Normal transparency mode

            float[] colorComponents = lineColor.getComponents(null);
            final float interstate = lineFader.getInterstate();
            int ALPHA_CHANNEL = 3;
            colorComponents[ALPHA_CHANNEL] = interstate;
            glColor4fv(colorComponents, 0);

            Point3d star1 = new Point3d();
            Point3d star2 = new Point3d();
            for (int i = 0; i < nbSegments; ++i) {
                final int index = 2 * i;
                Point3d v1 = asterism.get(index).getObsJ2000Pos(null);
                Point3d v2 = asterism.get(index + 1).getObsJ2000Pos(null);
                if (prj.projectJ2000LineCheck(v1, star1, v2, star2)) {
                    glBegin(GL_LINES);
                    glVertex2d(star1.x, star1.y);
                    glVertex2d(star2.x, star2.y);
                    glEnd();
                }
            }
        }
    }

    /**
     * Draw the name
     *
     * @param constFont
     * @param prj
     */
    void drawName(SFontIfc constFont, Projector prj) {
        if (!nameFader.hasInterstate()) {
            return;
        }
        float[] colorComponents = labelColor.getComponents(null);
        final int ALPHA_CHANNEL = 3;
        colorComponents[ALPHA_CHANNEL] = nameFader.getInterstate();
        Color color = new Color(colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[ALPHA_CHANNEL]);

        if (prj.isGravityLabelsEnabled()) {
            prj.printGravity180(constFont, XYName.x, XYName.y, nameI18, 1f, -constFont.getStrLen(nameI18) / 2);
        } else {
            constFont.print((int) (XYName.x - ((float) constFont.getStrLen(nameI18)) / 2), (int) XYName.y, nameI18, false, color);
        }
    }

    /**
     * Draw the art texture, optimized function to be called thru a constellation manager only
     */
    void drawArtOptim(Projector prj, NavigatorIfc nav) {
        float intensity = artFader.getInterstate();
        if (artTexture != null && intensity != 0) {
            glColor3f(intensity, intensity, intensity);

            // TODO: FRED create arrays or wrapping class
            Point3d v0 = new Point3d(),
                    v1 = new Point3d(),
                    v2 = new Point3d(),
                    v3 = new Point3d(),
                    v4 = new Point3d(),
                    v5 = new Point3d(),
                    v6 = new Point3d(),
                    v7 = new Point3d(),
                    v8 = new Point3d();
            boolean b0, b1, b2, b3, b4, b5, b6, b7, b8;

            // If one of the point is in the screen
            Point3d equVision = nav.getPrecEquVision();
            b0 = isInScreen(prj, 0, v0, equVision);
            b1 = isInScreen(prj, 1, v1, equVision);
            b2 = isInScreen(prj, 2, v2, equVision);
            b3 = isInScreen(prj, 3, v3, equVision);
            b4 = isInScreen(prj, 4, v4, equVision);
            b5 = isInScreen(prj, 5, v5, equVision);
            b6 = isInScreen(prj, 6, v6, equVision);
            b7 = isInScreen(prj, 7, v7, equVision);
            b8 = isInScreen(prj, 8, v8, equVision);

            if (b0 || b1 || b2 || b3 || b4 || b5 || b6 || b7 || b8) {
                glBindTexture(GL_TEXTURE_2D, artTexture.getID());

                if ((b0 || b1 || b2 || b3) && (v0.z < 1 && v1.z < 1 && v2.z < 1 && v3.z < 1)) {
                    glBegin(GL_QUADS);
                    glTexCoord2d(0, 1);
                    glVertex2d(v0.x, v0.y);
                    glTexCoord2d(0.5, 1);
                    glVertex2d(v1.x, v1.y);
                    glTexCoord2d(0.5, 0.5);
                    glVertex2d(v2.x, v2.y);
                    glTexCoord2d(0, 0.5);
                    glVertex2d(v3.x, v3.y);
                    glEnd();
                }
                if ((b1 || b4 || b5 || b2) && (v1.z < 1 && v4.z < 1 && v5.z < 1 && v2.z < 1)) {
                    glBegin(GL_QUADS);
                    glTexCoord2d(0.5, 1);
                    glVertex2d(v1.x, v1.y);
                    glTexCoord2d(1, 1);
                    glVertex2d(v4.x, v4.y);
                    glTexCoord2d(1, 0.5);
                    glVertex2d(v5.x, v5.y);
                    glTexCoord2d(0.5, 0.5);
                    glVertex2d(v2.x, v2.y);
                    glEnd();
                }
                if ((b2 || b5 || b6 || b7) && (v2.z < 1 && v5.z < 1 && v6.z < 1 && v7.z < 1)) {
                    glBegin(GL_QUADS);
                    glTexCoord2d(0.5, 0.5);
                    glVertex2d(v2.x, v2.y);
                    glTexCoord2d(1, 0.5);
                    glVertex2d(v5.x, v5.y);
                    glTexCoord2d(1, 0);
                    glVertex2d(v6.x, v6.y);
                    glTexCoord2d(0.5, 0);
                    glVertex2d(v7.x, v7.y);
                    glEnd();
                }
                if ((b3 || b2 || b7 || b8) && (v3.z < 1 && v2.z < 1 && v7.z < 1 && v8.z < 1)) {
                    glBegin(GL_QUADS);
                    glTexCoord2d(0, 0.5);
                    glVertex2d(v3.x, v3.y);
                    glTexCoord2d(0.5, 0.5);
                    glVertex2d(v2.x, v2.y);
                    glTexCoord2d(0.5, 0);
                    glVertex2d(v7.x, v7.y);
                    glTexCoord2d(0, 0);
                    glVertex2d(v8.x, v8.y);
                    glEnd();
                }
            }
        }
    }

    private boolean isInScreen(Projector prj, int i, Point3d win, Tuple3d equVision) {
        Point3d[] artVertex = getArtVertex();
        return prj.projectJ2000Check(artVertex[i], win) || (StelUtility.dot(equVision, artVertex[i]) > 0.9);
    }

    public Point3d[] getArtVertex() {
        if (artVertex == null) {
            artVertex = new Point3d[9];
            if (artTexture != null) {
                int texSize = artTexture.getWidth();

                // To transform from texture coordinate to 2d coordinate we need to find X with XA = B
                // A formed of 4 points in texture coordinate, B formed with 4 points in 3d coordinate
                // We need 3 stars and the 4th point is deduced from the other to get an normal base
                // X = B inv(A)
                // Vec3f s4 = s1 + (s2 - s1) ^ (s3 - s1);
                Vector3d s2_1 = new Vector3d();
                s2_1.sub(s2, s1);
                Vector3d s3_1 = new Vector3d();
                s3_1.sub(s3, s1);
                Vector3d s4 = new Vector3d();
                s4.cross(s2_1, s3_1);
                s4.add(s1);
                Matrix4d B = new Matrix4d(
                        s1.x, s2.x, s3.x, s4.x,
                        s1.y, s2.y, s3.y, s4.y,
                        s1.z, s2.z, s3.z, s4.z,
                        1, 1, 1, 1);
                Matrix4d A = new Matrix4d(
                        p1.x, p2.x, p3.x, p1.x,
                        texSize - p1.y, texSize - p2.y, texSize - p3.y, texSize - p1.y,
                        0, 0, 0, texSize,
                        1, 1, 1, 1);
                A.invert();
                Matrix4d X = new Matrix4d();
                X.mul(B, A);

                // TODO: FRED accessing directly artVertex of Constellation should be replaced
                Point3d[] ds = artVertex;
                for (int i = 0; i < ds.length; i++) {
                    ds[i] = new Point3d();
                }
                X.transform(new Point3d(0, 0, 0), ds[0]);
                X.transform(new Point3d(texSize / 2, 0, 0), ds[1]);
                X.transform(new Point3d(texSize / 2, texSize / 2, 0), ds[2]);
                X.transform(new Point3d(0, texSize / 2, 0), ds[3]);
                X.transform(new Point3d(texSize / 2 + texSize / 2, 0, 0), ds[4]);
                X.transform(new Point3d(texSize / 2 + texSize / 2, texSize / 2, 0), ds[5]);
                X.transform(new Point3d(texSize / 2 + texSize / 2, texSize / 2 + texSize / 2, 0), ds[6]);
                X.transform(new Point3d(texSize / 2 + 0, texSize / 2 + texSize / 2, 0), ds[7]);
                X.transform(new Point3d(0, texSize / 2 + texSize / 2, 0), ds[8]);
            }
        }
        return artVertex;
    }

    /**
     * Draw the art texture
     *
     * @param prj
     * @param nav
     */
    void drawArt(DefaultProjector prj, Navigator nav) {
        glBlendFunc(GL_ONE, GL_ONE);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_CULL_FACE);

        prj.setOrthographicProjection();

        drawArtOptim(prj, nav);

        prj.resetPerspectiveProjection();

        glDisable(GL_CULL_FACE);
    }

    Constellation isStarIn(StelObject s) {
        for (int i = 0; i < nbSegments * 2; ++i) {
            if (asterism.get(i) == s) {
                return this;
            }
        }
        return null;
    }

    public STexture getPointer() {
        return null;
    }

    /*public void update(long deltaTime) {
        lineFader.update(deltaTime);
        nameFader.update(deltaTime);
        artFader.update(deltaTime);
        boundaryFader.update(deltaTime);
    }*/

    /**
     * Draw the Constellation lines
     *
     * @param prj
     */
    void drawBoundaryOptim(Projector prj) {
        if (!boundaryFader.hasInterstate()) {
            return;
        }

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);// Normal transparency mode

        float interstate = boundaryFader.getInterstate();
        glColor4f(boundaryColor.getRed(), boundaryColor.getGreen(), boundaryColor.getBlue(), interstate);

        int i, j, size;
        Point3d pt1 = new Point3d(), pt2 = new Point3d();
        List<Point3d> points;

        if (singleSelected) {
            size = isolatedBoundarySegments.size();
        } else {
            size = sharedBoundarySegments.size();
        }

        for (i = 0; i < size; i++) {
            if (singleSelected) {
                points = isolatedBoundarySegments.get(i);
            } else {
                points = sharedBoundarySegments.get(i);
            }

            for (j = 0; j < points.size() - 1; j++) {
                if (prj.projectJ2000LineCheck(points.get(j), pt1, points.get(j + 1), pt2)) {
                    glBegin(GL_LINES);
                    glVertex2d(pt1.x, pt1.y);
                    glVertex2d(pt2.x, pt2.y);
                    glEnd();
                }
            }
        }
    }

    public StelObject getBrightestStarInConstellation() {
        float maxMag = 99.f;
        StelObject brightest = null;
        // maybe the brightest star has always odd index,
        // so check all segment endpoints:
        for (int i = 2 * nbSegments - 1; i >= 0; i--) {
            StelObject star = asterism.get(i);
            float mag = star.getMag(null);
            if (mag < maxMag) {
                brightest = star;
                maxMag = mag;
            }
        }
        return brightest;
    }

    // StelObject method to override

    /**
     * Write I18n information about the object in String.
     */
    public String getInfoString(NavigatorIfc nav) {
        return getNameI18n() + "(" + getShortName() + "°";
    }

    /**
     * The returned String can typically be used for object labeling in the sky
     */
    public String getShortInfoString(NavigatorIfc nav) {
        return getNameI18n();
    }

    /**
     * Return object's type
     */
    public StelObject.TYPE getType() {
        return StelObject.TYPE.CONSTELLATION;
    }

    /**
     * Get position in earth equatorial frame
     */
    public Point3d getEarthEquPos(NavigatorIfc nav) {
        return xyzName;
    }

    /**
     * observer centered J2000 coordinates
     */
    public Point3d getObsJ2000Pos(NavigatorIfc nav) {
        return xyzName;
    }

    /**
     * Return object's magnitude
     */
    public float getMag(NavigatorIfc nav) {
        return 0.f;
    }

    public String getNameI18n() {
        return nameI18;
    }

    public String getEnglishName() {
        return abbreviation;
    }

    public String getShortName() {
        return abbreviation;
    }

    void setFlagLines(boolean b) {
        lineFader.set(b);
    }

    void setFlagBoundaries(boolean b) {
        boundaryFader.set(b);
    }

    void setNameEnabled(boolean b) {
        nameFader.set(b);
    }

    void setArtEnabled(boolean b) {
        artFader.set(b);
    }

    boolean getFlagLines() {
        return lineFader.getState();
    }

    boolean getFlagBoundaries() {
        return boundaryFader.getState();
    }

    boolean getFlagName() {
        return nameFader.getState();
    }

    boolean getFlagArt() {
        return artFader.getState();
    }

    /**
     * International name (translated using gettext)
     */
    String nameI18;

    /**
     * Name in english
     */
    String englishName;

    /**
     * Abbreviation (of the latin name for western constellations)
     */
    String abbreviation;

    /**
     * Direction vector pointing on constellation name drawing position
     */
    protected Point3d xyzName = new Point3d();

    protected Point3d XYName = new Point3d();

    /**
     * Number of segments in the lines
     */
    private int nbSegments;

    private List<StelObject> asterism;

    protected STexture artTexture;

    Point3d artVertex[] = null;

    // The 3 stars coordinate in space 3D and their mapping on the texture 2I
    Point2i p1, p2, p3;
    Point3d s1, s2, s3;

    /**
     * Define whether art, lines, names and boundary must be drawn
     */
    final LinearFader artFader;

    final LinearFader lineFader;

    final LinearFader nameFader;

    final LinearFader boundaryFader;

    List<List<Point3d>> isolatedBoundarySegments = new ArrayList<List<Point3d>>();

    List<List<Point3d>> sharedBoundarySegments = new ArrayList<List<Point3d>>();

    // Currently we only need one color for all constellations, this may change at some point
    static Color lineColor;

    static Color labelColor;

    static Color boundaryColor;

    /**
     * Whether labels are to be printed with gravity
     */
    static boolean gravityLabel;

    static boolean singleSelected;

    public void setAllFlags(Constellation cc) {
        if (cc == null) {
            setFlagLines(false);
            setNameEnabled(false);
            setArtEnabled(false);
            setFlagBoundaries(false);
        } else {
            setFlagLines(cc.getFlagLines());
            setNameEnabled(cc.getFlagName());
            setArtEnabled(cc.getFlagArt());
            setFlagBoundaries(cc.getFlagBoundaries());
        }
    }
}