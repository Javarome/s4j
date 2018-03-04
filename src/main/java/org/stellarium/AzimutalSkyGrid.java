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
 * <p>Azimutal sky grid display management</p>
 *
 * @author <a href="mailto:rr0@rr0.org"/>Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 * @see <a href="http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/draw.cpp?rev=1.72&view=markup">C++ version</a> of this file.
 */
public class AzimutalSkyGrid extends SkyGrid {

    public AzimutalSkyGrid() throws StellariumException {
        this(24, 17, 1., 18, 50);
    }

    public AzimutalSkyGrid(int pnbMeridian, int pnbParallel, double pradius, int pnbAltSegment, int pnbAziSegment) throws StellariumException {
        super(pnbMeridian, pnbParallel, pradius, pnbAltSegment, pnbAziSegment);
    }

    void draw(Projector prj) {
        LinearFader fader = getFader();
        if (fader.hasInterstate()) {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);  // Normal transparency mode

            Point3d point1 = new Point3d();
            Point3d point2 = new Point3d();

            prj.setOrthographicProjection();    // set 2D coordinate

            Projector.ProjFunc projFunc = getProjFunc(prj);
            // Draw meridians
            final float interstate = fader.getInterstate();
            float[] colorComponents = getColor().getComponents(null);
            int ALPHA_CHANNEL = 3;
            for (int meridianIndex = 0; meridianIndex < getNbMeridian(); ++meridianIndex) {
                if (isTransparentTop()) {   // Transparency for the first and last points
                    Point3d[] altPoint = getAltPoint(meridianIndex);
                    if (projFunc.execute(altPoint[0], point1) && projFunc.execute(altPoint[1], point2)) {
                        double dx = point1.x - point2.x;
                        double dy = point1.y - point2.y;
                        double dq = dx * dx + dy * dy;
                        if (dq < 1024 * 1024) {
                            colorComponents[ALPHA_CHANNEL] = 0.f;
                            glColor4fv(colorComponents, 0);

                            glBegin(GL_LINES);
                            glVertex2d(point1.x, point1.y);
                            colorComponents[ALPHA_CHANNEL] = interstate;
                            glColor4fv(colorComponents, 0);
                            glVertex2d(point2.x, point2.y);
                            glEnd();
                        }
                    }

                    colorComponents[ALPHA_CHANNEL] = interstate;
                    glColor4fv(colorComponents, 0);

                    int nbAltSegment = getNbAltSegment();
                    for (int altSegmentIndex = 1; altSegmentIndex < nbAltSegment - 1; ++altSegmentIndex) {
                        if (projFunc.execute(altPoint[altSegmentIndex], point1) && projFunc.execute(altPoint[altSegmentIndex + 1], point2)) {
                            double deltaX = point1.x - point2.x;
                            double deltaY = point1.y - point2.y;
                            double dq = deltaX * deltaX + deltaY * deltaY;
                            if (dq < 1024 * 1024) {
                                glBegin(GL_LINES);
                                glVertex2d(point1.x, point1.y);
                                glVertex2d(point2.x, point2.y);
                                glEnd();

                                double angle;

                                // TODO: allow for other numbers of meridians and parallels without
                                // screwing up labels?
                                if (altSegmentIndex != 16 && meridianIndex % 8 == 0) {
                                    glEnable(GL_TEXTURE_2D);

                                    final double d = Math.sqrt(dq);

                                    angle = Math.acos((point1.y - point2.y) / d);
                                    if (point1.x < point2.x) {
                                        angle *= -1;
                                    }

                                    angle += Math.PI;

                                    prj.setOrthographicProjection();
                                    try {
                                        //float angleF = (float) Math.toDegrees(angle);
                                        float angleF = 0;
                                        Color interColor = new Color(colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[ALPHA_CHANNEL]);
                                        print(((int) point2.x) + 2, ((int) point2.y) + 2, "" + (altSegmentIndex - 8) * 10, interColor, angleF, prj);
                                    } finally {
                                        prj.resetPerspectiveProjection();
                                    }
                                    glDisable(GL_TEXTURE_2D);
                                }
                            }
                        }
                    }

                    if (projFunc.execute(altPoint[nbAltSegment - 1], point1) && projFunc.execute(altPoint[nbAltSegment], point2)) {
                        double dx = point1.x - point2.x;
                        double dy = point1.y - point2.y;
                        double dq = dx * dx + dy * dy;
                        if (dq < 1024 * 1024) {
                            colorComponents[ALPHA_CHANNEL] = interstate;
                            glColor4fv(colorComponents, 0);

                            glBegin(GL_LINES);
                            glVertex2d(point1.x, point1.y);
                            colorComponents[ALPHA_CHANNEL] = 0;
                            glColor4fv(colorComponents, 0);
                            glVertex2d(point2.x, point2.y);
                            glEnd();
                        }
                    }
                } else {// No transparency
                    colorComponents[ALPHA_CHANNEL] = interstate;
                    glColor4fv(colorComponents, 0);

                    for (int i = 0; i < getNbAltSegment(); ++i) {
                        Point3d[] altPoint = getAltPoint(meridianIndex);
                        if (projFunc.execute(altPoint[i], point1) && projFunc.execute(altPoint[i + 1], point2)) {
                            double dx = point1.x - point2.x;
                            double dy = point1.y - point2.y;
                            double dq = dx * dx + dy * dy;
                            if (dq < 1024 * 1024) {
                                glBegin(GL_LINES);
                                glVertex2d(point1.x, point1.y);
                                glVertex2d(point2.x, point2.y);
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
                for (int i = 0; i < getNbAziSegment(); ++i) {
                    Point3d[] aziPoint = getAziPoint(np);
                    if (projFunc.execute(aziPoint[i], point1) && projFunc.execute(aziPoint[i + 1], point2)) {
                        double dx = point1.x - point2.x;
                        double dy = point1.y - point2.y;
                        double dq = dx * dx + dy * dy;
                        if (dq < 1024 * 1024) {
                            glBegin(GL_LINES);
                            glVertex2d(point1.x, point1.y);
                            glVertex2d(point2.x, point2.y);
                            glEnd();
                        }
                    }
                }
            }

            prj.resetPerspectiveProjection();
        }
    }

    public DefaultProjector.ProjFunc getProjFunc(Projector prj) {
        return prj.getProjectLocalFunc();
    }
}
