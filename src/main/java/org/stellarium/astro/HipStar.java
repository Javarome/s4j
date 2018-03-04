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
package org.stellarium.astro;

//import org.stellarium.projector.Projector;

import org.stellarium.*;
import org.stellarium.data.HipData;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.projector.Projector;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.media.opengl.GL;
import javax.vecmath.Point3d;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.StelUtility.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * @author Fred Simon
 * @version 0.8.2
 */
public class HipStar extends StelObjectBase implements Comparable<HipStar> {
    private static final char ENDL = '\n';

    private static STexture pointerStar;

    public HipStar(Logger parentLogger) {
        super(parentLogger);
        if (pointerStar == null) {
            pointerStar = new STextureFactory(logger).createTexture("pointeur2.png");
        }
    }

    public String getInfoString(NavigatorIfc nav) {
        Point3d equatorialPos = nav.j2000ToEarthEqu(XYZ);
        StelUtility.Coords coords = rectToSphe(equatorialPos);
        StringBuffer oss = new StringBuffer();
        boolean hasCommonNameI18n = !isEmpty(commonNameI18);
        if (hasCommonNameI18n)
            oss.append(commonNameI18).append(' ');
        if (!isEmpty(sciName)) {
            if (hasCommonNameI18n)
                oss.append('(');
            oss.append(sciName);
            if (hasCommonNameI18n)
                oss.append(')');
        }
        // If no name print HP number
        if (oss.length() == 0) {
            oss.append("HP ").append(hp);
        }
        if (doubleStar)
            oss.append(" **");
        oss.append(ENDL);

        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        oss.append("Magnitude: ").append(format.format(mag));
        if (variableStar)
            oss.append(" (Variable)");
        oss.append(ENDL);

        oss.append("RA/DE: ").append(printAngleHms(coords.getRA())).append("/")
                .append(printAngleDms(coords.getDE())).append(ENDL);

        // calculate alt az
        Point3d localPos = nav.earthEquToLocal(equatorialPos);
        coords = rectToSphe(localPos);
        coords.northToZero();
        oss.append("Az/Alt: ").append(printAngleDms(coords.getRA())).append("/")
                .append(printAngleDms(coords.getDE())).append(ENDL);

        oss.append("Distance: ");
        if (distance > 0) {
            oss.append(format.format(distance));
        } else {
            oss.append('-');
        }
        oss.append(" Light Years").append(ENDL);

        oss.append("Cat: HP ");
        if (hp > 0) {
            oss.append(hp);
        } else {
            oss.append('-');
        }
        oss.append(ENDL);

        oss.append("Spectral Type: ").append(getSpectralType()).append(ENDL);

        return oss.toString();
    }

    public String getNameI18n() {
        // If flagSciNames is true (set when culture is western),
        // fall back to scientific names and catalog numbers if no common name.
        // Otherwise only use common names from the culture being viewed.

        if (!isEmpty(commonNameI18)) return commonNameI18;
        if (flagSciNames) {
            if (!isEmpty(sciName)) return sciName;
            return "HP " + hp;
        }
        return "";
    }

    public String getEnglishName() {
        return "HP " + hp;
    }

    public String getShortInfoString(NavigatorIfc nav) {
        StringBuffer oss = new StringBuffer();

        oss.append(commonNameI18).append("  ");

        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        oss.append("Magnitude: ").append(format.format(mag));
        if (variableStar)
            oss.append(" (Variable)");

        if (distance > 0) {
            oss.append("  ");
            oss.append(format.format(distance));
            oss.append(" Light Years");
        }

        return oss.toString();
    }

    public char getSpectralType() {
        if (type == null || type == SpectralType.Default)
            return '?';
        return type.name().charAt(0);
    }

    public Color getRGB() {
        if (type == null)
            return SpectralType.Default.getColor();
        return type.getColor();
    }

    /**
     * Read datas in binary catalog and compute x,y,z;
     * The aliasing bug on some architecture has been fixed by Rainer Canavan on 26/11/2003
     * Really ? -- JB, 20060607
     *
     * @param data the raw data from the ctatlog file
     * @return If the read succeeded
     * @throws java.io.IOException
     */
    boolean read(HipData data) throws IOException {
        double RA = data.getRa() * Math.PI / 12;// Convert from hours to rad
        double DE = Math.toRadians(data.getDe());
        this.mag = data.getMagnitude();

        // Calc the Cartesian coord with RA and DE
        XYZ = new Point3d();
        StelUtility.spheToRect(RA, DE, XYZ);
        XYZ.scale(RADIUS_STAR);

        SpectralType[] spectralTypes = SpectralType.values();
        if (data.getType() < 0 || data.getType() > (spectralTypes.length - 1)) {
            this.type = SpectralType.X;
        } else {
            this.type = spectralTypes[data.getType()];
        }

        // Precomputation of a term used later
        term1 = Math.exp(-0.92103f * (this.mag + 12.12331f)) * 108064.73f;

        // distance
        distance = data.getDistance();

        if (data.getShortMag() == 0 && this.type == SpectralType.O) return false;

        // Hardcoded fix for bad data (because hp catalog isn't in cvs control)
        if (hp == 120412) {
            return false;
        }

        //	printf("%d\t%d\t%.4f\t%.4f\t%c\n", HP, mag, rao, deo, SpType);

        return true;
    }

    void draw(final Point3d XY) {
        // Compute the equivalent star luminance for a 5 arc min circle and convert it
        // in function of the eye adaptation
        double rmag = eye.adaptLuminance(term1) * Math.pow(proj.getFieldOfView(), -0.85) * 70;
        double cmag = 1;

        // if size of star is too small (blink) we put its size to 1.2 -. no more blink
        // And we compensate the difference of brighteness with cmag
        if (rmag < 1.2) {
            cmag = rmag * rmag / 1.44;
            if (rmag / starScale < 0.1 || cmag < 0.1 / starMagScale) {
                return;
            }
            rmag = 1.2;
        } else {
            if (rmag > 5) {
                rmag = 5;
            }
        }

        // Calculation of the luminosity
        // Random coef for star twinkling
        cmag *= (1 - twinkleAmount * Math.random());

        // Global scaling
        rmag *= starScale;
        cmag *= starMagScale;

        Color typeColor = type.getColor();
        Color rgb = new Color(Math.min((int) (typeColor.getRed() * cmag), 255),
                Math.min((int) (typeColor.getGreen() * cmag), 255),
                Math.min((int) (typeColor.getBlue() * cmag), 255));  // scale
        glColor3fv(rgb.getComponents(null), 0);

        glBlendFunc(GL_ONE, GL_ONE);

        glBegin(GL_QUADS);
        glTexCoord2i(0, 0);
        glVertex2d(XY.x - rmag, XY.y - rmag);// Bottom left
        glTexCoord2i(1, 0);
        glVertex2d(XY.x + rmag, XY.y - rmag);// Bottom right
        glTexCoord2i(1, 1);
        glVertex2d(XY.x + rmag, XY.y + rmag);// Top right
        glTexCoord2i(0, 1);
        glVertex2d(XY.x - rmag, XY.y + rmag);// Top left
        glEnd();
    }

    void drawPoint(final Point3d XY) {
        // Compute the equivalent star luminance for a 5 arc min circle and convert it
        // in function of the eye adaptation
        double rmag = eye.adaptLuminance(term1) * Math.pow(proj.getFieldOfView(), 0.85f) * 50.f;

        // if size of star is too small (blink) we put its size to 1.2 -. no more blink
        // And we compensate the difference of brighteness with cmag
        double cmag = rmag * rmag / 1.44f;

        if (rmag / starScale < 0.05f || cmag < 0.05 / starMagScale) return;

        // Calculation of the luminosity
        // Random coef for star twinkling
        cmag *= (1. - twinkleAmount * Math.random());
        cmag *= starMagScale;

        Color typeColor = type.getColor();
        Color rgb = new Color(Math.min((int) (typeColor.getRed() * cmag), 255),
                Math.min((int) (typeColor.getGreen() * cmag), 255),
                Math.min((int) (typeColor.getBlue() * cmag), 255));  // scale
        glColor3fv(rgb.getComponents(null), 0);

        glBlendFunc(GL_ONE, GL_ONE);

        // rms - one pixel stars
        glDisable(GL_TEXTURE_2D);
        glPointSize(0.1f);
        glBegin(GL_POINTS);
        glVertex3d(XY.x, XY.y, 0);
        glEnd();
        glEnable(GL_TEXTURE_2D);// required for star labels to work
    }

    boolean drawName(final Point3d XY) {
        String starName = getNameI18n();
        if (StelUtility.isEmpty(starName)) {
            return false;
        }
        // if (draw_mode == DM_NORMAL) {
        Color typeColor = type.getColor();
        glColor4f(typeColor.getRed() * 0.75f,
                typeColor.getGreen() * 0.75f,
                typeColor.getBlue() * 0.75f,
                (float) namesBrightness);
        // }
        //else glColor3fv(label_color);

        if (proj.isGravityLabelsEnabled()) {
            proj.printGravity180(starFont, XY.x, XY.y, starName, true, 6, -4);
        } else {
            int x = (int) (XY.x + 6);
            int y = (int) (XY.y - 4);
//            glutBitmapString(x, y, starName);
            starFont.print(x, y, starName);
        }

        return true;
    }

    static final double RADIUS_STAR = 1;

    // Init Static variables
    static double twinkleAmount = 10;

    static double starScale = 10;

    static double starMagScale = 10;

    static double namesBrightness = 1;

    static ToneReproductor eye;

    static Projector proj;

    static boolean gravityLabel = false;

    static Color circleColor = new Color(0, 0, 0);

    static Color labelColor = new Color(0.8f, 0.8f, 0.8f);

    public enum SpectralType {
        O(0.8 / 1.3, 1.0 / 1.3, 1.3 / 1.3),
        B(0.9 / 1.2, 1.0 / 1.2, 1.2 / 1.2),
        A(0.95 / 1.15, 1.0 / 1.15, 1.15 / 1.15),
        F(1.05 / 1.05, 1.0 / 1.05, 1.05 / 1.05),
        G(1.3 / 1.3, 1.0 / 1.3, 0.9 / 1.3),
        K(1.15 / 1.15, 0.95 / 1.15, 0.8 / 1.15),
        M(1.15 / 1.15, 0.85 / 1.15, 0.8 / 1.15),
        R(1.3 / 1.3, 0.85 / 1.3, 0.6 / 1.3),
        S(1.5 / 1.5, 0.8 / 1.5, 0.2 / 1.5),
        N(1.5 / 1.5, 0.8 / 1.5, 0.2 / 1.5),
        W(1.5 / 1.5, 0.8 / 1.5, 0.2 / 1.5),
        X(1.0, 1.0, 1.0),
        Default(1.0, 1.0, 1.0);

        private final Color color;

        SpectralType(double x, double y, double z) {
            this.color = new Color((float) x, (float) y, (float) z);
        }

        public Color getColor() {
            return color;
        }
    }

    static SFontIfc starFont;

    static boolean flagSciNames;

    /**
     * Hipparcos number
     */
    int hp;

    /**
     * Apparent magnitude
     */
    double mag;

    /**
     * double star flag
     */
    boolean doubleStar;

    /**
     * not implemented yet
     */
    boolean variableStar;

    /**
     * Cartesian position
     */
    Point3d XYZ;

    /**
     * Optimization term
     */
    double term1;

    /**
     * English Common Name of the star
     */
    String englishCommonName;

    /**
     * Common Name of the star
     */
    String commonNameI18;

    /**
     * Scientific name
     */
    String sciName;

    /**
     * Spectral type coded as number in [0..12]
     */
    SpectralType type = SpectralType.Default;

    /**
     * Distance from Earth in light years
     */
    double distance;

    public STexture getPointer() {
        return pointerStar;
    }

    protected void drawPointerTexture(Navigator nav, DefaultProjector prj, long localTime, Point3d screenPos) {
        float[] rgbValues = getRGB().getComponents(null);
        glColor3fv(rgbValues, 0);
        STexture pointerTexture = getPointer();
        glBindTexture(GL.GL_TEXTURE_2D, pointerTexture.getID());
        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);
        glTranslated(screenPos.x, screenPos.y, 0);
        glRotatef(localTime / 20.0f, 0, 0, 1.0f);
        pointerTexture.displayTexture(-13, -13, 26, 26);
    }

    public StelObject.TYPE getType() {
        return StelObject.TYPE.STAR;
    }

    public Point3d getEarthEquPos(NavigatorIfc nav) {
        return nav.j2000ToEarthEqu(XYZ);
    }

    public Point3d getObsJ2000Pos(NavigatorIfc nav) {
        return XYZ;
    }

    public float getMag(NavigatorIfc nav) {
        return (float) mag;
    }

    public int getHPNumber() {
        return hp;
    }

    public void setLabelColor(Color v) {
        labelColor = v;
    }

    public void setCircleColor(Color v) {
        circleColor = v;
    }

    public void translateNames(Translator trans) {
        commonNameI18 = trans.translate(englishCommonName);
    }

    // Natural Order per hp number
    public int compareTo(HipStar o) {
        return hp - o.hp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HipStar hipStar = (HipStar) o;

        return hp == hipStar.hp;
    }

    public int hashCode() {
        return hp;
    }

    static interface NamesManager {
        void clean(HipStar star);

        void set(HipStar star, String name);
    }

    static class CommonNamesManager implements NamesManager {
        public void clean(HipStar star) {
            star.englishCommonName = "";
            star.commonNameI18 = "";
        }

        public void set(HipStar star, String name) {
            star.englishCommonName = name;
            star.commonNameI18 = Translator.getCurrentTranslator().translate(name);
        }
    }

    static class ScientificNamesManager implements NamesManager {
        public void clean(HipStar star) {
            star.sciName = "";
        }

        public void set(HipStar star, String name) {
            star.sciName = name;
        }
    }

    static interface FlagManager {
        void set(HipStar star);
    }

    static class DoubleFlagManager implements FlagManager {
        public void set(HipStar star) {
            star.doubleStar = true;
        }
    }

    static class VariableFlagManager implements FlagManager {
        public void set(HipStar star) {
            star.variableStar = true;
        }
    }

    public static class MagnitudeComparator implements Comparator<HipStar> {
        public int compare(HipStar o1, HipStar o2) {
            if (o1.hp == o2.hp)
                return 0;
            if (o1.mag > o2.mag)
                return 1;
            if (o2.mag > o1.mag)
                return -1;
            return o1.hp - o2.hp;
        }
    }

    public static class EnglishNameComparator implements Comparator<HipStar> {
        public int compare(HipStar o1, HipStar o2) {
            return o1.englishCommonName.compareTo(o2.englishCommonName);
        }
    }

    public static class ScientificNameComparator implements Comparator<HipStar> {
        public int compare(HipStar o1, HipStar o2) {
            return o1.sciName.compareTo(o2.sciName);
        }
    }

}

