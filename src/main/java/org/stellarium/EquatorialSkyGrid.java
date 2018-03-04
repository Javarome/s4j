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
package org.stellarium;

import org.stellarium.projector.DefaultProjector;
import org.stellarium.projector.Projector;
import static org.stellarium.ui.SglAccess.*;
import org.stellarium.ui.fader.LinearFader;

import static javax.media.opengl.GL.*;
import javax.vecmath.Point3d;
import java.awt.*;

/**
 * Sky grid.
 * <p/>
 * See <a href="http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/draw.cpp?rev=1.72&view=markup">C++ version</a> of this file.
 *
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 */
public class EquatorialSkyGrid extends SkyGrid {

    public EquatorialSkyGrid() throws StellariumException {
        super(24, 17, 1., 18, 50);
    }

    public EquatorialSkyGrid(int pnbMeridian, int pnbParallel, double pradius, int pnbAltSegment, int pnbAziSegment) throws StellariumException {
        super(pnbMeridian, pnbParallel, pradius, pnbAltSegment, pnbAziSegment);
    }

    void draw(Projector prj) {
        LinearFader fader = getFader();
        if (fader.hasInterstate()) {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            // Normal transparency mode
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            Point3d pt1 = new Point3d();
            Point3d pt2 = new Point3d();

            prj.setOrthographicProjection();    // set 2D coordinate
            try {
                Projector.ProjFunc projFunc = getProjFunc(prj);
                // Draw meridians
                final float interstate = fader.getInterstate();
                float[] colorComponents = getColor().getComponents(null);
                final int ALPHA_CHANNEL = 3;
                for (int nm = 0; nm < getNbMeridian(); ++nm) {
                    Point3d[] altPoint = getAltPoint(nm);
                    if (isTransparentTop()) {   // Transparency for the first and last points
                        if (projFunc.execute(altPoint[0], pt1) && projFunc.execute(altPoint[1], pt2)) {
                            double dx = pt1.x - pt2.x;
                            double dy = pt1.y - pt2.y;
                            double dq = dx * dx + dy * dy;
                            if (dq < 1024 * 1024) {
                                colorComponents[ALPHA_CHANNEL] = 0;
                                glColor4fv(colorComponents, 0);

                                glBegin(GL_LINES);
                                glVertex2d(pt1.x, pt1.y);
                                colorComponents[ALPHA_CHANNEL] = interstate;
                                glColor4fv(colorComponents, 0);
                                glVertex2d(pt2.x, pt2.y);
                                glEnd();
                            }
                        }

                        colorComponents[ALPHA_CHANNEL] = interstate;
                        glColor4fv(colorComponents, 0);

                        for (int i = 1; i < getNbAltSegment() - 1; ++i) {
                            if (projFunc.execute(altPoint[i], pt1) && projFunc.execute(altPoint[i + 1], pt2)) {
                                double dx = pt1.x - pt2.x;
                                double dy = pt1.y - pt2.y;
                                double dq = dx * dx + dy * dy;
                                if (dq < 1024 * 1024) {
                                    glBegin(GL_LINES);
                                    glVertex2d(pt1.x, pt1.y);
                                    glVertex2d(pt2.x, pt2.y);
                                    glEnd();

                                    double angle;

                                    // TODO: allow for other numbers of meridians and parallels without screwing up labels?
                                    if (i == 8) {
                                        glEnable(GL_TEXTURE_2D);
                                        // draw labels along equator for RA
                                        final double d = Math.sqrt(dq);

                                        angle = Math.acos((pt1.y - pt2.y) / d);
                                        if (pt1.x < pt2.x) {
                                            angle *= -1;
                                        }

                                        prj.setOrthographicProjection();
                                        try {
                                            //float angleF = (float) (90 + Math.toDegrees(angle));
                                            float angleF = 0;
                                            Color interColor = new Color(colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[ALPHA_CHANNEL]);
                                            String str = nm + "h";
                                            print(((int) pt2.x) + 2, ((int) pt2.y) + 2, str, interColor, angleF, prj);
                                        } finally {
                                            prj.resetPerspectiveProjection();
                                            glDisable(GL_TEXTURE_2D);
                                        }
                                    } else if (nm % 8 == 0 && i != 16) {
                                        glEnable(GL_TEXTURE_2D);

                                        final double d = Math.sqrt(dq);

                                        angle = (float) Math.acos((pt1.y - pt2.y) / d);
                                        if (pt1.x < pt2.x) {
                                            angle *= -1;
                                        }

                                        if (i > 8) {
                                            angle += Math.PI;
                                        }

                                        prj.setOrthographicProjection();
                                        try {
                                            //float angleF = (float) Math.toDegrees(angle);
                                            float angleF = 0;
                                            Color interColor = new Color(colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[ALPHA_CHANNEL]);
                                            String str = "" + (i - 8) * 10;
                                            print(((int) pt2.x) + 2, ((int) pt2.y) + 2, str, interColor, angleF, prj);
                                        } finally {
                                            prj.resetPerspectiveProjection();
                                        }
                                        glDisable(GL_TEXTURE_2D);
                                    }
                                }
                            }
                        }

                        if (projFunc.execute(altPoint[getNbAltSegment() - 1], pt1) && projFunc.execute(altPoint[getNbAltSegment()], pt2)) {
                            double dx = pt1.x - pt2.x;
                            double dy = pt1.y - pt2.y;
                            double dq = dx * dx + dy * dy;
                            if (dq < 1024 * 1024) {
                                colorComponents[ALPHA_CHANNEL] = interstate;
                                glColor4fv(colorComponents, 0);

                                glBegin(GL_LINES);
                                glVertex2d(pt1.x, pt1.y);
                                colorComponents[ALPHA_CHANNEL] = 0;
                                glColor4fv(colorComponents, 0);
                                glVertex2d(pt2.x, pt2.y);
                                glEnd();
                            }
                        }
                    } else {// No transparency
                        colorComponents[ALPHA_CHANNEL] = interstate;
                        glColor4fv(colorComponents, 0);

                        for (int i = 0; i < getNbAltSegment(); ++i) {
                            if (projFunc.execute(altPoint[i], pt1) && projFunc.execute(altPoint[i + 1], pt2)) {
                                double dx = pt1.x - pt2.x;
                                double dy = pt1.y - pt2.y;
                                double dq = dx * dx + dy * dy;
                                if (dq < 1024 * 1024) {
                                    glBegin(GL_LINES);
                                    glVertex2d(pt1.x, pt1.y);
                                    glVertex2d(pt2.x, pt2.y);
                                    glEnd();
                                }
                            }
                        }
                    }
                }

                // Draw parallels
                colorComponents[ALPHA_CHANNEL] = interstate;
                glColor4fv(colorComponents, 0);
                for (int np = 0; np < getNbParallel(); ++np) {
                    Point3d[] aziPoint = getAziPoint(np);
                    for (int i = 0; i < getNbAziSegment(); ++i) {
                        if (projFunc.execute(aziPoint[i], pt1) && projFunc.execute(aziPoint[i + 1], pt2)) {
                            double dx = pt1.x - pt2.x;
                            double dy = pt1.y - pt2.y;
                            double dq = dx * dx + dy * dy;
                            if (dq < 1024 * 1024) {
                                glBegin(GL_LINES);
                                glVertex2d(pt1.x, pt1.y);
                                glVertex2d(pt2.x, pt2.y);
                                glEnd();
                            }
                        }
                    }
                }
            } finally {
                prj.resetPerspectiveProjection();
            }
        }
    }

    public DefaultProjector.ProjFunc getProjFunc(Projector prj) {
        return prj.getProjectEarthEquFunc();
    }
}