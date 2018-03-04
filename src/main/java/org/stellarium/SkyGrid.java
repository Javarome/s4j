/*
 * This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau, Frederic Simon
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
package org.stellarium;

import org.stellarium.projector.DefaultProjector;
import org.stellarium.projector.Projector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;

import javax.vecmath.Point3d;
import java.awt.*;

/**
 * <p>Sky grid display management.</p>
 *
 * @author <a href="mailto:rr0@rr0.org"/>Jerome Beau</a>, Frederic Simon
 * @version 0.8.2
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/draw.cpp?view=markup&pathrev=stellarium-0-8-2">C++ version</a> of this file.
 */
public abstract class SkyGrid {
    private SFontIfc font;
    private boolean transparentTop = true;

    public SkyGrid() throws StellariumException {
        this(24, 17, 1., 18, 50);
    }

    public SkyGrid(int pnbMeridian, int pnbParallel, double pradius, int pnbAltSegment, int pnbAziSegment) throws StellariumException {
        nbMeridian = pnbMeridian;
        nbParallel = pnbParallel;
        radius = pradius;
        nbAltSegment = pnbAltSegment;
        nbAziSegment = pnbAziSegment;
        initArrays();
    }

    private void initArrays() {
        // Alt points are the points to draw along the meridian
        altPoints = new Point3d[nbMeridian][nbAltSegment + 1];
        for (int nm = 0; nm < nbMeridian; ++nm) {
            //altPoints[nm] = new Point3d[nbAltSegment + 1];
            for (int i = 0; i < nbAltSegment + 1; ++i) {
                altPoints[nm][i] = new Point3d();
                StelUtility.spheToRect(2 * nm * Math.PI / nbMeridian,
                        i * Math.PI / nbAltSegment - StelUtility.M_PI_2, altPoints[nm][i]);
                altPoints[nm][i].scale(this.radius);
            }
        }

        // Alt points are the points to draw along the meridian
        aziPoints = new Point3d[nbParallel][this.nbAziSegment + 1];
        for (int np = 0; np < nbParallel; ++np) {
            //aziPoints[np] = new Point3d[this.nbAziSegment + 1];
            for (int i = 0; i < nbAziSegment + 1; ++i) {
                aziPoints[np][i] = new Point3d();
                StelUtility.spheToRect(i * 2 * Math.PI / nbAziSegment,
                        (np + 1) * Math.PI / (nbParallel + 1) - StelUtility.M_PI_2, aziPoints[np][i]);
                aziPoints[np][i].scale(this.radius);
            }
        }
    }

    abstract void draw(Projector prj);

    public void setColor(Color c) {
        color = new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    public Color getColor() {
        return color;
    }

    public void show(boolean b) {
        fader.set(b);
    }

    public boolean isShown() {
        return fader.getState();
    }

    public void setTopTransparency(boolean b) {
        transparentTop = b;
    }

    private int nbMeridian;

    private int nbParallel;

    private double radius;

    private int nbAltSegment;

    private int nbAziSegment;

    private Color color = new Color(0.2f, 0.2f, 0.2f);

    private Point3d[][] altPoints;

    private Point3d[][] aziPoints;

    private LinearFader fader = new LinearFader();

    public DefaultProjector.ProjFunc getProjFunc(Projector prj) {
        return prj.getProjectEarthEquFunc();
    }

    public int getNbMeridian() {
        return nbMeridian;
    }

    protected int getNbAltSegment() {
        return nbAltSegment;
    }

    protected int getNbParallel() {
        return nbParallel;
    }

    protected int getNbAziSegment() {
        return nbAziSegment;
    }

    protected Point3d[] getAziPoint(int np) {
        return aziPoints[np];
    }

    protected Point3d[] getAltPoint(int nm) {
        return altPoints[nm];
    }

    public void setFont(SFontIfc someFont) throws StellariumException {
        font = someFont;
    }

    protected void print(int x, int y, String str, Color color, float angle, Projector prj) {
        font.print(x, y, str, false, color, angle);
    }

    protected LinearFader getFader() {
        return fader;
    }

    public boolean isTransparentTop() {
        return transparentTop;
    }
}
