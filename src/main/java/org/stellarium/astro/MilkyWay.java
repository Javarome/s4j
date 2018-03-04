/*
 * This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau
 * and is a Java version of the original Stellarium C++ version,
 * (draw.h, draw.cpp)
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

import org.stellarium.NavigatorIfc;
import org.stellarium.StelUtility;
import org.stellarium.StellariumException;
import org.stellarium.ToneReproductor;
import org.stellarium.projector.Projector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.vecmath.Matrix4d;
import javax.vecmath.Tuple4f;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * Class which manages the displaying of the Milky Way
 *
 * @author Jerome Beau, Fred Simon
 * @version 0.8.2
 */
public class MilkyWay {

    private Logger logger;
    private STextureFactory textureFactory;

    public MilkyWay(Logger parentLogger) throws StellariumException {
        this(1., parentLogger);
    }

    MilkyWay(double radius, Logger parentLogger) throws StellariumException {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);
        this.radius = radius;
    }

    public void close() {
        if (tex != null) {
            tex.close();
            tex = null;
        }
    }

    public void draw(ToneReproductor eye, Projector prj, NavigatorIfc nav) {
        assert tex != null;// A texture must be loaded before calling this

        // Scotopic color = 0.25, 0.25 in xyY mode. Global stars luminance ~= 0.001 cd/m^2
        color = new SColor(0.25f * fader.getInterstate(), 0.25f * fader.getInterstate(),
                intensity * 0.002f * fader.getInterstate() / texAvgLuminance);
        float[] c = StelUtility.toArray(color);
        eye.xyYToRGB(c);
        glColor3fv(c, 0);

        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, tex.getID());

        Matrix4d subMat = getSphereMat(nav);
        prj.sSphere(radius, 1.0, 20, 20, subMat, true);

        glDisable(GL_CULL_FACE);
    }

    public void drawChart(ToneReproductor eye, Projector prj, NavigatorIfc nav) {
        assert tex != null;// A texture must be loaded before calling this

        float[] c = StelUtility.toArray(color);
        glColor3fv(c, 0);
        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, tex.getID());

        Matrix4d subMat = getSphereMat(nav);
        prj.sSphere(radius, 1.0, 20, 20, subMat, true);

        glDisable(GL_CULL_FACE);

    }

    public void update(long deltaTime) {
        //        fader.update(deltaTime);
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setTexture(String texFile) throws StellariumException {
        setTexture(texFile, false);
    }

    public void setTexture(String texFile, boolean blend) throws StellariumException {
        if (tex != null) {
            tex.close();
        }
        tex = textureFactory.createTexture(texFile,
                (!blend) ? STexture.TEX_LOAD_TYPE_PNG_SOLID_REPEAT : STexture.TEX_LOAD_TYPE_PNG_BLEND3);

        // big performance improvement to cache this
        texAvgLuminance = tex.getAverageLuminance();
    }

    public void setColor(Tuple4f c) {
        color = new SColor(c);
    }

    public void setFlagShow(boolean b) {
        fader.set(b);
    }

    public boolean getFlagShow() {
        return fader.getState();
    }

    private double radius;

    private STexture tex;

    private SColor color = new SColor(1.f, 1.f, 1.f);

    private float intensity;

    private float texAvgLuminance;

    private LinearFader fader = new LinearFader();

    private Matrix4d getSphereMat(NavigatorIfc nav) {
        Matrix4d tmp = new Matrix4d();
        Matrix4d subMat = new Matrix4d();
        subMat.set(nav.getJ2000ToEyeMat());
        tmp.rotX(Math.toRadians(23));
        subMat.mul(tmp);
        tmp.rotY(Math.toRadians(120));
        subMat.mul(tmp);
        tmp.rotZ(Math.toRadians(7));
        subMat.mul(tmp);
        return subMat;
    }
}
