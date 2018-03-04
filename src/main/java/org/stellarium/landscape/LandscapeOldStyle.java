package org.stellarium.landscape;

import org.stellarium.Navigator;
import org.stellarium.StellariumException;
import org.stellarium.ToneReproductor;
import org.stellarium.data.IniFileParser;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.ui.render.STexture;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static java.lang.StrictMath.*;
import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @version $revision$
 */
public class LandscapeOldStyle extends Landscape {

    /**
     * Default constructor with radius of 2
     */
    public LandscapeOldStyle(Logger parentLogger) {
        this(2f, parentLogger);
    }

    LandscapeOldStyle(float someRadius, Logger parentLogger) {
        super(someRadius, parentLogger);
    }

    protected void load(URL landscapeFile, String sectionName) throws StellariumException {
        IniFileParser pd = loadCommon(landscapeFile, sectionName);

        // TODO: put values into hash and call create method to consolidate code

        LandscapeType type = LandscapeType.valueOf(pd.getStr(sectionName, IniFileParser.TYPE));
        if (type != LandscapeType.old_style) {
            validLandscape = false;
            throw new StellariumException("Landscape type mismatch for landscape " + sectionName +
                    ", expected old_style, found " + type + ".  No landscape in use.");
        }

        // Load sides textures
        nbSideTexs = pd.getInt(sectionName, "nbsidetex", 0);
        sideTexs = new STexture[nbSideTexs];
        String tmp;
        for (int i = 0; i < nbSideTexs; ++i) {
            tmp = "tex" + i;
            sideTexs[i] = textureFactory.createTexture(pd.getStr(sectionName, tmp), STexture.TEX_LOAD_TYPE_PNG_ALPHA, false);
        }

        // Init sides parameters
        nbSide = pd.getInt(sectionName, "nbside", 0);
        sides = new LandscapeTexCoord[nbSide];
        for (int i = 0; i < sides.length; i++) {
            sides[i] = new LandscapeTexCoord();
        }
        String s;
        int texnum;
        float a, b, c, d;
        for (int i = 0; i < nbSide; ++i) {
            tmp = "side" + i;
            s = pd.getStr(sectionName, tmp);
            StringTokenizer st = new StringTokenizer(s.substring("tex".length()), ":");
            texnum = Integer.parseInt(st.nextToken());
            a = Float.parseFloat(st.nextToken());
            b = Float.parseFloat(st.nextToken());
            c = Float.parseFloat(st.nextToken());
            d = Float.parseFloat(st.nextToken());
            sides[i].tex = sideTexs[texnum];
            sides[i].texCoords[0] = a;
            sides[i].texCoords[1] = b;
            sides[i].texCoords[2] = c;
            sides[i].texCoords[3] = d;
            //printf("%f %f %f %f\n",a,b,c,d);
        }

        nbDecorRepeat = pd.getInt(sectionName, "nb_decor_repeat", 1);

        groundTex = textureFactory.createTexture(pd.getStr(sectionName, "groundtex"), STexture.TEX_LOAD_TYPE_PNG_SOLID, false);
        s = pd.getStr(sectionName, "ground");
        StringTokenizer st = new StringTokenizer(s.substring("groundtex".length()), ":");
        a = Float.parseFloat(st.nextToken());
        b = Float.parseFloat(st.nextToken());
        c = Float.parseFloat(st.nextToken());
        d = Float.parseFloat(st.nextToken());
        groundTexCoord.tex = groundTex;
        groundTexCoord.texCoords[0] = a;
        groundTexCoord.texCoords[1] = b;
        groundTexCoord.texCoords[2] = c;
        groundTexCoord.texCoords[3] = d;

        fogTex = textureFactory.createTexture(pd.getStr(sectionName, "fogtex"), STexture.TEX_LOAD_TYPE_PNG_SOLID_REPEAT, false);
        s = pd.getStr(sectionName, "fog");
        st = new StringTokenizer(s.substring("fogtex".length()), ":");
        a = Float.parseFloat(st.nextToken());
        b = Float.parseFloat(st.nextToken());
        c = Float.parseFloat(st.nextToken());
        d = Float.parseFloat(st.nextToken());
        fogTexCoord.tex = fogTex;
        fogTexCoord.texCoords[0] = a;
        fogTexCoord.texCoords[1] = b;
        fogTexCoord.texCoords[2] = c;
        fogTexCoord.texCoords[3] = d;

        fogAltAngle = pd.getDouble(sectionName, "fog_alt_angle", 0.);
        fogAngleShift = pd.getDouble(sectionName, "fog_angle_shift", 0.);
        decorAltAngle = pd.getDouble(sectionName, "decor_alt_angle", 0.);
        decorAngleShift = pd.getDouble(sectionName, "decor_angle_shift", 0.);
        decorAngleRotateZ = pd.getDouble(sectionName, "decor_angle_rotatez", 0.);
        groundAngleShift = pd.getDouble(sectionName, "groundAngleShift", 0.);
        groundAngleRotateZ = pd.getDouble(sectionName, "ground_angle_rotatez", 0.);
        drawGroundFirst = pd.getInt(sectionName, "draw_ground_first", 0) == 1;
    }

    /**
     * create from a hash of parameters (no ini file needed)
     */
    void create(boolean fullPath, Map param) throws StellariumException {
        name = getStr(param, "name");
        validLandscape = true;// assume valid if got here

        // Load sides textures
        nbSideTexs = getInt(param, "nbsidetex");
        sideTexs = new STexture[nbSideTexs];
        String tmp;
        for (int i = 0; i < nbSideTexs; ++i) {
            tmp = "tex" + i;
            sideTexs[i] = textureFactory.createTexture(fullPath, (getStr(param, "path")) + getStr(param, tmp), STexture.TEX_LOAD_TYPE_PNG_ALPHA, false);
        }

        // Init sides parameters
        nbSide = getInt(param, "nbside");
        sides = new LandscapeTexCoord[nbSide];
        String s;
        int texnum;
        float a, b, c, d;
        for (int i = 0; i < nbSide; ++i) {
            tmp = "side" + i;
            s = getStr(param, tmp);
            StringTokenizer st = new StringTokenizer(s.substring("tex".length()), ":");
            texnum = Integer.parseInt(st.nextToken());
            a = Float.parseFloat(st.nextToken());
            b = Float.parseFloat(st.nextToken());
            c = Float.parseFloat(st.nextToken());
            d = Float.parseFloat(st.nextToken());
            sides[i].tex = sideTexs[texnum];
            sides[i].texCoords[0] = a;
            sides[i].texCoords[1] = b;
            sides[i].texCoords[2] = c;
            sides[i].texCoords[3] = d;
            //printf("%f %f %f %f\n",a,b,c,d);
        }

        nbDecorRepeat = getInt(param, "nb_decor_repeat", 1);

        groundTex = textureFactory.createTexture(getStr(param, "groundtex"), STexture.TEX_LOAD_TYPE_PNG_SOLID, false);
        s = getStr(param, "ground");
        StringTokenizer st = new StringTokenizer(s.substring("groundtex".length()), ":");
        a = Float.parseFloat(st.nextToken());
        b = Float.parseFloat(st.nextToken());
        c = Float.parseFloat(st.nextToken());
        d = Float.parseFloat(st.nextToken());
        groundTexCoord.tex = groundTex;
        groundTexCoord.texCoords[0] = a;
        groundTexCoord.texCoords[1] = b;
        groundTexCoord.texCoords[2] = c;
        groundTexCoord.texCoords[3] = d;

        fogTex = textureFactory.createTexture(getStr(param, "fogtex"), STexture.TEX_LOAD_TYPE_PNG_SOLID_REPEAT, false);
        s = getStr(param, "fog");
        st = new StringTokenizer(s.substring("fogtex".length()), ":");
        a = Float.parseFloat(st.nextToken());
        b = Float.parseFloat(st.nextToken());
        c = Float.parseFloat(st.nextToken());
        d = Float.parseFloat(st.nextToken());
        fogTexCoord.tex = fogTex;
        fogTexCoord.texCoords[0] = a;
        fogTexCoord.texCoords[1] = b;
        fogTexCoord.texCoords[2] = c;
        fogTexCoord.texCoords[3] = d;

        fogAltAngle = getDouble(param, "fog_alt_angle", 0.);
        fogAngleShift = getDouble(param, "fog_angle_shift", 0.);
        decorAltAngle = getDouble(param, "decor_alt_angle", 0.);
        decorAngleShift = getDouble(param, "decor_angle_shift", 0.);
        decorAngleRotateZ = getDouble(param, "decor_angle_rotatez", 0.);
        groundAngleShift = getDouble(param, "groundAngleShift", 0.);
        groundAngleRotateZ = getDouble(param, "ground_angle_rotatez", 0.);
        drawGroundFirst = getInt(param, "draw_ground_first", 0) == 1;
    }

    public void draw(ToneReproductor eye, DefaultProjector prj, Navigator nav) {
        if (!validLandscape) {
            return;
        }
        if (drawGroundFirst) {
            drawGround(eye, prj, nav);
        }
        drawDecor(eye, prj, nav);
        if (!drawGroundFirst) {
            drawGround(eye, prj, nav);
        }
        drawFog(eye, prj, nav);
    }

    /**
     * Draw the horizon fog
     *
     * @param eye
     * @param prj
     * @param nav
     */
    void drawFog(ToneReproductor eye, DefaultProjector prj, Navigator nav) {
        if (!fogFader.hasInterstate())
            return;
        glBlendFunc(GL_ONE, GL_ONE);
        glPushMatrix();
        // Should be converted to glColor4f
        glColor3f(fogFader.getInterstate() * (0.1f + 0.1f * skyBrightness),
                fogFader.getInterstate() * (0.1f + 0.1f * skyBrightness),
                fogFader.getInterstate() * (0.1f + 0.1f * skyBrightness));
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glBindTexture(GL_TEXTURE_2D, fogTex.getID());
        Matrix4d subMat = new Matrix4d(nav.getLocalToEyeMat());
        Matrix4d temp = new Matrix4d();
        temp.setIdentity();
        temp.setTranslation(new Vector3d(0, 0, radius * sin(Math.toRadians(fogAngleShift))));
        subMat.mul(temp);
        prj.sCylinder(radius, radius * sin(Math.toRadians(fogAltAngle)), 128, 1, subMat, true);
        glDisable(GL_CULL_FACE);
        glPopMatrix();
    }

    /**
     * Draw the mountains with a few pieces of texture
     *
     * @param eye
     * @param prj
     * @param nav
     */
    void drawDecor(ToneReproductor eye, DefaultProjector prj, Navigator nav) {
        if (!landFader.hasInterstate()) {
            return;
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);

        glColor4f(skyBrightness, skyBrightness, skyBrightness, landFader.getInterstate());

        int subdiv = 128 / (nbDecorRepeat * nbSide);
        if (subdiv <= 0) {
            subdiv = 1;
        }
        double da = (2 * PI) / (nbSide * subdiv * nbDecorRepeat);
        double dz = radius * sin(Math.toRadians(decorAltAngle));
        double z;// Unused = radius * sin(Math.toRadians(groundAngleShift));
        float x, y;
        double a;

        Matrix4d mat = new Matrix4d(nav.getLocalToEyeMat());
        //mat.rotZ(Math.toRadians(decorAngleRotateZ));
        glPushMatrix();
        glLoadMatrixd(mat);

        z = radius * sin(Math.toRadians(decorAngleShift));
        glEnable(GL_BLEND);
        glEnable(GL_CULL_FACE);

        for (int n = 0; n < nbDecorRepeat; ++n) {
            a = 2 * PI * n / nbDecorRepeat;
            for (int i = 0; i < nbSide; ++i) {
                glBindTexture(GL_TEXTURE_2D, sides[i].tex.getID());
                glBegin(GL_QUAD_STRIP);
                for (int j = 0; j <= subdiv; ++j) {
                    double decorAngleRotateZInRadians = Math.toRadians(decorAngleRotateZ);
                    x = (float) (radius * sin(a + da * j + da * subdiv * i + decorAngleRotateZInRadians));
                    y = (float) (radius * cos(a + da * j + da * subdiv * i + decorAngleRotateZInRadians));
                    glNormal3f(-x, -y, 0f);
                    glTexCoord2f(sides[i].texCoords[0] + j * (sides[i].texCoords[2] - sides[i].texCoords[0]) / subdiv, sides[i].texCoords[1]);
                    prj.sVertex3(x, y, z + dz * (sides[i].texCoords[3] - sides[i].texCoords[1]), mat);
                    glTexCoord2f(sides[i].texCoords[0] + j * (sides[i].texCoords[2] - sides[i].texCoords[0]) / subdiv, sides[i].texCoords[3]);
                    prj.sVertex3(x, y, z, mat);
                }
                glEnd();
            }
        }
        glDisable(GL_CULL_FACE);
        glPopMatrix();
    }

    /**
     * Draw the ground
     *
     * @param eye
     * @param prj
     * @param nav
     */
    void drawGround(ToneReproductor eye, DefaultProjector prj, Navigator nav) {
        if (!landFader.hasInterstate()) {
            return;
        }
        Matrix4d mat = new Matrix4d(nav.getLocalToEyeMat());
        Matrix4d temp = new Matrix4d();
        temp.rotZ(Math.toRadians(groundAngleRotateZ));
        mat.mul(temp);
        temp.setIdentity();
        temp.setTranslation(new Vector3d(0, 0, radius * sin(Math.toRadians(groundAngleShift))));
        mat.mul(temp);
        glColor3f(skyBrightness, skyBrightness, skyBrightness);
        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, groundTex.getID());

        int subdiv = 32 / (nbDecorRepeat * nbSide);
        if (subdiv <= 0) {
            subdiv = 1;
        }
        prj.sDisk(radius, nbSide * subdiv * nbDecorRepeat, 5, mat, true);
        // FRED: Someone missed it: Isn't it equivcalent...
        //prj.sDisk(radius, 32, 5, mat, true);
        glDisable(GL_CULL_FACE);
    }

    STexture[] sideTexs;

    int nbSideTexs;

    int nbSide;

    LandscapeTexCoord[] sides;

    STexture fogTex;

    LandscapeTexCoord fogTexCoord = new LandscapeTexCoord();

    STexture groundTex;

    LandscapeTexCoord groundTexCoord = new LandscapeTexCoord();

    int nbDecorRepeat;

    /**
     * Fog alt angle, in degrees
     */
    double fogAltAngle;

    /**
     * Fog angle shift, in degrees
     */
    double fogAngleShift;

    /**
     * Decor alt angle, in degrees
     */
    double decorAltAngle;

    /**
     * Decor angle shift, in degrees
     */
    double decorAngleShift;

    /**
     * Decor angle rotate Z, in degrees
     */
    double decorAngleRotateZ;

    /**
     * Ground angle shift, in degrees
     */
    double groundAngleShift;

    /**
     * Ground angle rotate Z, in degrees
     */
    double groundAngleRotateZ;

    boolean drawGroundFirst;
}
