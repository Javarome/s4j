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
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.media.opengl.GL;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.text.NumberFormat;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static java.lang.StrictMath.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 */
public class Nebula extends StelObjectBase {
    private static final char endl = '\n';

    private static final String[] UNKNOWN_TYPE_NAMES = new String[]{" ", "-", "*", "D*", "***"};
    private STextureFactory textureFactory;

    public enum NebulaType {
        NEB_GX("Gx", "Galaxy"),
        NEB_OC("OC", "Open Cluster"),
        NEB_GC("Gb", "Globular Cluster"),
        NEB_N("Nb", "Nebula"),
        NEB_PN("Pl", "Planetary Nebula"),
        NEB_DN("D*", "Undocumented type"),
        NEB_IG("***", "Undocumented type"),
        NEB_CN("C+N", "Cluster associated with nebulosity"),
        NEB_UNKNOWN("?", "Unknown");

        String shortDesc;

        String description;

        NebulaType(String shortDesc, String description) {
            this.shortDesc = shortDesc;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public String getShortDesc() {
            return shortDesc;
        }
    }

    static final double RADIUS_NEB = 1;

    Nebula(Logger parentLogger) {
        super(parentLogger);
        textureFactory = new STextureFactory(logger);
        if (pointerNebula == null) {
            pointerNebula = textureFactory.createTexture("pointeur5.png");
        }
        messierNb = 0;
        ngcNb = 0;
        icNb = 0;
        nebTex = null;
        incLum = random() * PI;
        nameI18 = "";
    }

    public Point3d getEarthEquPos(NavigatorIfc nav) {
        return nav.j2000ToEarthEqu(XYZ);
    }

    protected static STexture pointerNebula;

    public STexture getPointer() {
        return pointerNebula;
    }

    protected void drawPointerTexture(Navigator nav, DefaultProjector prj, long localTime, Point3d screenPos) {
        glColor3f(0.4f, 0.5f, 0.8f);
        drawPointerTexture1(nav, prj, localTime, screenPos);
    }

    public String getInfoString(NavigatorIfc nav) {
        Point3d equPos = nav.j2000ToEarthEqu(XYZ);
        StelUtility.Coords tempCoords = StelUtility.rectToSphe(equPos);

        StringBuffer oss = new StringBuffer();
        if (!StelUtility.isEmpty(nameI18)) {
            oss.append(nameI18).append(" (");
        }
        if ((messierNb > 0) && (messierNb < 111)) {
            oss.append("M ").append(messierNb).append(" - ");
        }
        if (ngcNb > 0) {
            oss.append("NGC ").append(ngcNb);
        }
        if (icNb > 0) {
            oss.append("IC ").append(icNb);
        }
        /*if (ugcNb > 0)
        {
            oss.append("UGC ").append(ugcNb);
        }*/
        if (!StelUtility.isEmpty(nameI18)) {
            oss.append(")");
        }
        oss.append(endl);

        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        oss.append("Magnitude: ").append(format.format(mag)).append(endl);

        oss.append("RA/DE: ").append(StelUtility.printAngleHms(tempCoords.getRA())).append("/")
                .append(StelUtility.printAngleDms(tempCoords.getDE())).append(endl);

        // calculate alt az
        Point3d localPos = nav.earthEquToLocal(equPos);
        tempCoords = StelUtility.rectToSphe(localPos);
        tempCoords.northToZero();
        oss.append("Az/Alt: ").append(StelUtility.printAngleDms(tempCoords.getRA())).append("/")
                .append(StelUtility.printAngleDms(tempCoords.getDE())).append(endl);

        oss.append("Type: ").append(nType.name()).append(endl);
        oss.append("Size: ").append(StelUtility.printAngleDms(Math.toRadians(angularSize))).append(endl);

        return oss.toString();
    }

    public String getShortInfoString(NavigatorIfc nav) {
        if (!StelUtility.isEmpty(nameI18)) {

            StringBuffer oss = new StringBuffer(nameI18);
            oss.append("  ");
            if (mag < 99) oss.append("Magnitude: ").append(mag);

            return oss.toString();
        } else {
            if (messierNb > 0) {
                return "M " + messierNb;
            } else if (ngcNb > 0) {
                return "NGC " + ngcNb;
            } else if (icNb > 0) {
                return "IC " + icNb;
            }
        }

        // All nebula have at least an NGC or IC number
        assert (false);
        return "";
    }

    public double getCloseFOV(NavigatorIfc nav) {
        return (4 * angularSize * 180d) / PI;
    }

    /**
     * Read nebula data from file and compute x,y and z;
     *
     * @param record One line in the fab file representing one nebula entry
     * @throws org.stellarium.StellariumException
     *
     */
    void readTexture(String record) throws StellariumException {
        String texName;
        String name;
        double ra;
        double de;
        double texAngularSize;
        double texRotation;
        int ngc;

        // TODO: Throw a comprehensive exception when wrong parsing
        StringTokenizer tokenizer = new StringTokenizer(record);
        ngc = Integer.parseInt(tokenizer.nextToken());
        ra = Double.parseDouble(tokenizer.nextToken());
        de = Double.parseDouble(tokenizer.nextToken());
        mag = Double.parseDouble(tokenizer.nextToken());
        texAngularSize = Double.parseDouble(tokenizer.nextToken());
        texRotation = Double.parseDouble(tokenizer.nextToken());
        name = tokenizer.nextToken();
        texName = tokenizer.nextToken();
        credit = tokenizer.nextToken();

        if ("none".equals(credit))
            credit = "";
        else
            credit = "Credit: " + credit;

        credit = credit.replace('_', ' ');

        // Only set name if not already set from NGC data
        if (StelUtility.isEmpty(englishName)) {
            englishName = name.replace('_', ' ');
        }

        // Calc the RA and DE from the datas
        double rightAscensionInRadians = Math.toRadians(ra);
        double declinationInRadians = Math.toRadians(de);

        // Calc the Cartesian coord with RA and DE
        if (XYZ == null) {
            XYZ = new Point3d();
        }
        StelUtility.spheToRect(rightAscensionInRadians, declinationInRadians, XYZ);
        XYZ.scale(RADIUS_NEB);

        // Calc the angular size in radian : TODO this should be independant of texAngularSize
        angularSize = (texAngularSize * PI) / (2 * 60 * 180);

        nebTex = textureFactory.createTexture(texName, STexture.TEX_LOAD_TYPE_PNG_BLEND1, true);// use mipmaps

        //texAngularSize*texAngularSize*3600/4*M_PI
        //	luminance = mag_to_luminance(mag, texAngularSize*texAngularSize*3600) /	neb_tex->get_average_luminance() * 50;
        luminance = StelUtility.magToLuminance(mag, texAngularSize * texAngularSize * 3600);

        // To force a calculation on draw
        texAvgLuminance = Double.NaN;

        double texSize = RADIUS_NEB * sin((texAngularSize * PI) / (2 * 60 * 180));

        // Precomputation of the rotation/translation matrix
        Matrix4d matPrecomp = new Matrix4d();
        matPrecomp.setIdentity();
        matPrecomp.setTranslation(new Vector3d(XYZ));
        Matrix4d tmp = new Matrix4d();
        tmp.rotZ(rightAscensionInRadians);
        matPrecomp.mul(tmp);
        tmp.rotY(-declinationInRadians);
        matPrecomp.mul(tmp);
        tmp.rotX(Math.toRadians(texRotation));
        matPrecomp.mul(tmp);

        texQuadVertex = new Point3d[4];
        for (int i = 0; i < texQuadVertex.length; i++) {
            texQuadVertex[i] = new Point3d();
        }
        matPrecomp.transform(new Point3d(0, -texSize, -texSize), texQuadVertex[0]);// Bottom Right
        matPrecomp.transform(new Point3d(0, texSize, -texSize), texQuadVertex[1]);// Bottom Right
        matPrecomp.transform(new Point3d(0, -texSize, texSize), texQuadVertex[2]);// Bottom Right
        matPrecomp.transform(new Point3d(0, texSize, texSize), texQuadVertex[3]);// Bottom Right
    }

    void drawChart(DefaultProjector prj, Navigator nav) {
        boolean lastState = glIsEnabled(GL.GL_TEXTURE_2D);
        double r = (getOnScreenSize(prj, nav) / 2) * 1.2;// slightly bigger than actual!
        if (r < 5) {
            r = 5;
        }
        r *= circleScale;

        glDisable(GL.GL_TEXTURE_2D);
        glLineWidth(1.0f);

        glColor4d(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), circleColor.getAlpha());
        if (nType == NebulaType.NEB_UNKNOWN) {
            glCircle(XY, r);
        } else if (nType == NebulaType.NEB_N)// supernova reemnant
        {
            glCircle(XY, r);
        } else {
            double xy0 = XY.x;
            double xy1 = XY.y;
            if (nType == NebulaType.NEB_PN)// planetary nebula
            {
                glCircle(XY, 0.4 * r);

                glBegin(GL.GL_LINE_LOOP);
                glVertex3d(xy0 - r, xy1, 0);
                glVertex3d(xy0 - 0.4 * r, xy1, 0);
                glEnd();

                glBegin(GL.GL_LINE_LOOP);
                glVertex3d(xy0 + r, xy1, 0);
                glVertex3d(xy0 + 0.4 * r, xy1, 0);
                glEnd();

                glBegin(GL.GL_LINE_LOOP);
                glVertex3d(xy0, xy1 + r, 0);
                glVertex3d(xy0, xy1 + 0.4 * r, 0);
                glEnd();

                glBegin(GL.GL_LINE_LOOP);
                glVertex3d(xy0, xy1 - r, 0);
                glVertex3d(xy0, xy1 - 0.4 * r, 0);
                glEnd();
            } else if (nType == NebulaType.NEB_OC)// open cluster
            {
                glLineStipple(2, (short) 0x3333);
                glEnable(GL.GL_LINE_STIPPLE);
                glCircle(XY, r);
                glDisable(GL.GL_LINE_STIPPLE);
            } else if (nType == NebulaType.NEB_GC)// Globular cluster
            {
                glCircle(XY, r);

                glBegin(GL.GL_LINE_LOOP);
                glVertex3d(xy0 - r, xy1, 0);
                glVertex3d(xy0 + r, xy1, 0);
                glEnd();

                glBegin(GL.GL_LINE_LOOP);
                glVertex3d(xy0, xy1 - r, 0);
                glVertex3d(xy0, xy1 + r, 0);
                glEnd();
            } else if (nType == NebulaType.NEB_DN)// Diffuse Nebula
            {
                glLineStipple(1, (short) 0xAAAA);
                glEnable(GL.GL_LINE_STIPPLE);
                glCircle(XY, r);
                glDisable(GL.GL_LINE_STIPPLE);
            } else if (nType == NebulaType.NEB_IG)// Irregular
            {
                glEllipse(XY, r, 0.5);
            } else // not sure what type!!!
            {
                glCircle(XY, r);
            }
        }
        glLineWidth(1.0f);

        if (lastState) glEnable(GL.GL_TEXTURE_2D);
    }

    void drawTex(DefaultProjector prj, Navigator nav, ToneReproductor eye) {
        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);
        glBlendFunc(GL.GL_ONE, GL.GL_ONE);

        if (nebTex == null)
            return;

        // if start zooming in, turn up brightness to full for DSO images
        // gradual change might be better
        if (flagBright && getOnScreenSize(prj, nav) > 12.) {
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            double adLum = eye.adaptLuminance(luminance);

            // Calculate only if not done already
            if (texAvgLuminance == Double.NaN) {
                // this is a huge performance drag if called every frame, so cache here
                texAvgLuminance = nebTex.getAverageLuminance();
            }
            // TODO this should be revisited to be less ad hoc
            // 3 is a fudge factor since only about 1/3 of a texture is not black background
            double cmag = 3d * adLum / texAvgLuminance;
            glColor4d(cmag, cmag, cmag, 1.0d);
        }

        glBindTexture(GL.GL_TEXTURE_2D, nebTex.getID());

        Point3d v = new Point3d();
        double[] glIn = new double[3];

        glBegin(GL.GL_TRIANGLE_STRIP);
        glTexCoord2i(1, 0);// Bottom Right
        prj.projectJ2000(texQuadVertex[0], v);
        v.get(glIn);
        glVertex3dv(glIn, 0);
        glTexCoord2i(0, 0);// Bottom Left
        prj.projectJ2000(texQuadVertex[1], v);
        v.get(glIn);
        glVertex3dv(glIn, 0);
        glTexCoord2i(1, 1);// Top Right
        prj.projectJ2000(texQuadVertex[2], v);
        v.get(glIn);
        glVertex3dv(glIn, 0);
        glTexCoord2i(0, 1);// Top Left
        prj.projectJ2000(texQuadVertex[3], v);
        v.get(glIn);
        glVertex3dv(glIn, 0);
        glEnd();
    }

    void drawCircle(DefaultProjector prj, Navigator nav) {
        if (2.f / getOnScreenSize(prj, nav) < 0.1) {
            return;
        }
        incLum++;
        double lum = min(1, 2.f / getOnScreenSize(prj, nav)) * (0.8 + 0.2 * sin(incLum / 10));

        double xy0 = XY.x;
        double xy1 = XY.y;
        glColor4d(circleColor.getRed(), circleColor.getGreen(), circleColor.getBlue(), lum * hintsBrightness);
        glBindTexture(GL.GL_TEXTURE_2D, texCircle.getID());
        glBegin(GL.GL_TRIANGLE_STRIP);
        glTexCoord2i(1, 0);// Bottom Right
        glVertex3d(xy0 + 4, xy1 - 4, 0.0f);
        glTexCoord2i(0, 0);// Bottom Left
        glVertex3d(xy0 - 4, xy1 - 4, 0.0f);
        glTexCoord2i(1, 1);// Top Right
        glVertex3d(xy0 + 4, xy1 + 4, 0.0f);
        glTexCoord2i(0, 1);// Top Left
        glVertex3d(xy0 - 4, xy1 + 4, 0.0f);
        glEnd();
    }

    void drawNoTex(DefaultProjector prj, Navigator nav, ToneReproductor eye) {
        double r = (getOnScreenSize(prj, nav) / 2);
        double cmag = 0.20 * hintsBrightness;

        glColor3d(cmag, cmag, cmag);
        glBindTexture(GL.GL_TEXTURE_2D, texCircle.getID());
        glBegin(GL.GL_QUADS);
        double xy0 = XY.x;
        double xy1 = XY.y;
        glTexCoord2i(0, 0);
        glVertex2d(xy0 - r, xy1 - r);// Bottom left
        glTexCoord2i(1, 0);
        glVertex2d(xy0 + r, xy1 - r);// Bottom right
        glTexCoord2i(1, 1);
        glVertex2d(xy0 + r, xy1 + r);// Top right
        glTexCoord2i(0, 1);
        glVertex2d(xy0 - r, xy1 + r);// Top left
        glEnd();
    }

    /**
     * Return the radius of a circle containing the object on screen
     */
    public double getOnScreenSize(Projector prj, NavigatorIfc nav) {
        return Math.toDegrees(angularSize) * (prj.getViewportHeight() / prj.getFieldOfView());
    }

    void drawName(DefaultProjector prj) {
        glColor4d(labelColor.getRed(), labelColor.getGreen(), labelColor.getBlue(), hintsBrightness);

        double size = getOnScreenSize(prj, null);
        double shift = 8 + size / 2;

        String nebulaName = getNameI18n();

        double xy0 = XY.x;
        double xy1 = XY.y;
        if (prj.isGravityLabelsEnabled()) {
            prj.printGravity180(nebulaFont, xy0 + shift, xy1 + shift, nebulaName, true, 0, 0);
        } else {
            nebulaFont.print((int) (xy0 + shift), (int) (xy1 + shift), nebulaName);
        }

        // draw image credit, if it fits easily
        if (!StelUtility.isEmpty(credit) && size > nebulaFont.getStrLen(credit)) {
            if (prj.isGravityLabelsEnabled()) {
                prj.printGravity180(nebulaFont, xy0 - shift - 40f, xy1 + -shift - 40f, credit, true, 0f, 0f);
            } else {
                nebulaFont.print((int) (xy0 - shift), (int) (xy1 - shift - 60), credit);
            }
        }
    }

    boolean hasTex() {
        return nebTex != null;
    }

    /**
     * Read the record entry and fill the members
     *
     * @param recordstr
     * @return true if known nebula type, false otherwise
     */
    boolean readNGC(String recordstr) {
        int rahr;
        double ramin;
        int dedeg;
        double demin;
        double texAngularSize;
        int nb;

        String afterI = recordstr.substring(1).trim();
        StringTokenizer tokenizer = new StringTokenizer(afterI);

        nb = Integer.parseInt(tokenizer.nextToken());

        if (recordstr.charAt(0) == 'I') {
            icNb = nb;
        } else {
            ngcNb = nb;
        }
        String typeName = tokenizer.nextToken();
        // If the first char is a number it means the type name was empty and the token is rahr
        if (Character.isDigit(typeName.charAt(0))) {
            rahr = Integer.parseInt(typeName);
            typeName = " ";
        } else {
            rahr = Integer.parseInt(tokenizer.nextToken());
        }

        ramin = Double.parseDouble(tokenizer.nextToken());
        String sdegdeg = tokenizer.nextToken();
        if (sdegdeg.charAt(0) == '+')
            sdegdeg = sdegdeg.substring(1);
        dedeg = Integer.parseInt(sdegdeg);
        demin = Double.parseDouble(tokenizer.nextToken());

        char pos = tokenizer.nextToken().charAt(0);
        String constellation = tokenizer.nextToken();

        // Calc the angular size in radian : TODO this should be independant of texAngularSize
        String sTexAngularSize = tokenizer.nextToken();
        if (sTexAngularSize.charAt(0) == '<')
            sTexAngularSize = tokenizer.nextToken();
        texAngularSize = Double.parseDouble(sTexAngularSize);
        mag = Double.parseDouble(tokenizer.nextToken());

        double raRad = rahr + ramin / 60;
        double decRad = dedeg + demin / 60;
        //if (recordstr[21] == '-') decRad *= -1.;

        raRad *= PI / 12.;// Convert from hours to rad
        decRad = Math.toRadians(decRad);

        // Calc the Cartesian coord with RA and DE
        if (XYZ == null) {
            XYZ = new Point3d();
        }
        StelUtility.spheToRect(raRad, decRad, XYZ);
        XYZ.scale(RADIUS_NEB);

        if (mag < 1) {
            mag = 99;
        }

        if (texAngularSize < 0)
            texAngularSize = 1;
        if (texAngularSize > 150)
            texAngularSize = 150;

        angularSize = (texAngularSize * PI) / (2 * 60 * 180);

        luminance = StelUtility.magToLuminance(mag, texAngularSize * texAngularSize * 3600);
        if (luminance < 0)
            luminance = .0075;

        // this is a huge performance drag if called every frame, so cache here
        //if (neb_tex) delete neb_tex;
        nebTex = null;

        nType = NebulaType.NEB_UNKNOWN;
        for (String unknownTypeName : UNKNOWN_TYPE_NAMES) {
            if (unknownTypeName.equals(typeName)) {
                return false;
            }
        }
        for (NebulaType nebulaType : NebulaType.values()) {
            if (nebulaType.getShortDesc().equals(typeName)) {
                nType = nebulaType;
            }
        }

        return true;
    }

    public String getTypeString() {
        return nType.getDescription();
    }

    /**
     * Translate nebula name using the passed translator
     *
     * @param trans
     */
    public void translateName(Translator trans) {
        nameI18 = trans.translate(englishName);
    }

    public StelObject.TYPE getType() {
        return StelObject.TYPE.NEBULA;
    }

    void setLabelColor(Color v) {
        labelColor = v;
    }

    void setCircleColor(Color v) {
        circleColor = v;
    }

    public double getAngularSize() {
        return angularSize;
    }

    public int getMessierNb() {
        return messierNb;
    }

    public void setMessierNb(int mNb) {
        this.messierNb = mNb;
    }

    public int getNgcNb() {
        return ngcNb;
    }

    public int getIcNb() {
        return icNb;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    /**
     * Messier Catalog number
     */
    private int messierNb;

    /**
     * New General Catalog number
     */
    private int ngcNb;

    /**
     * Index Catalog number
     */
    private int icNb;

    /** Uppsala General Catalog number */
    //private int ugcNb;

    /**
     * English name
     */
    String englishName;

    /**
     * Nebula name
     */
    String nameI18;

    /**
     * Nebula image credit
     */
    private String credit;

    /**
     * Apparent magnitude
     */
    double mag;

    /**
     * Angular size, in radians
     */
    private double angularSize;

    /**
     * Cartesian equatorial position
     */
    Point3d XYZ;

    /**
     * Store temporary 2D position
     */
    Point3d XY = new Point3d();

    /**
     * Nebula Type
     */
    NebulaType nType;

    /**
     * Texture
     */
    private STexture nebTex;

    /**
     * The 4 vertex used to draw the nebula texture
     */
    private Point3d[] texQuadVertex = new Point3d[4];

    /**
     * Object luminance to use (value computed to compensate
     * the texture avergae luminosity)
     */
    private double luminance;

    /**
     * avg luminance of the texture (saved here for performance)
     */
    private double texAvgLuminance;

    /**
     * Local counter for symbol animation
     */
    private double incLum;

    /**
     * The symbolic circle texture
     */
    static STexture texCircle;

    /**
     * Font used for names printing
     */
    static SFontIfc nebulaFont;

    static double hintsBrightness;

    static Color labelColor, circleColor;

    /**
     * Define the sclaing of the hints circle
     */
    static double circleScale;

    /**
     * Define if nebulae must be drawn in bright mode
     */
    static boolean flagBright;
}