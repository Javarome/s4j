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
package org.stellarium.ui;

import org.stellarium.StellariumException;
import org.stellarium.Translator;
import org.stellarium.projector.Projector;
import static org.stellarium.ui.SglAccess.glBlendFunc;
import static org.stellarium.ui.SglAccess.glEnable;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;

import static javax.media.opengl.GL.*;
import javax.vecmath.Point3d;
import java.awt.*;

/**
 * Cardinal points display manager
 *
 * @author <a href="mailto:rr0@rr0.org">J&eacute;r&ocirc;me Beau</a>
 * @author Fred Simon
 * @version 2.0.0
 * @since 1.0.0
 */
public class Cardinals {

    public Cardinals() {
        this(1.);
    }

    public Cardinals(double radius) {
        this.radius = radius;
        this.font = null;
        this.color = new Color(0.6f, 0.2f, 0.2f);
    }

    /**
     * Draw the cardinals points : N S E W
     * handles special cases at poles
     */
    public void draw(Projector prj, double latitude) {
        if (!fader.hasInterstate()) {
            return;
        }

        // direction text
        String d[] = new String[4];

        d[0] = sNorth;
        d[1] = sSouth;
        d[2] = sEast;
        d[3] = sWest;

        // fun polar special cases
        if (latitude == 90.0) {
            d[0] = d[1] = d[2] = d[3] = sSouth;
        } else if (latitude == -90.0) {
            d[0] = d[1] = d[2] = d[3] = sNorth;
        }

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        // Normal transparency mode
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Point3d pos = new Point3d();
        Point3d xy = new Point3d();

        prj.setOrthographicProjection();
        try {
            float[] colorComponents = color.getComponents(null);
            final int ALPHA_CHANNEL = 3;
            colorComponents[ALPHA_CHANNEL] = fader.getInterstate();
            color = new Color(colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[ALPHA_CHANNEL]);

            int shift = font.getStrLen(sNorth) / 2;

            if (prj.isGravityLabelsEnabled()) {
                printGravity(prj, d, pos, xy, shift);
            } else {
                // N for North
                pos.set(-1.f, 0, 0);
                if (prj.projectLocal(pos, xy)) {
                    font.print(((int) xy.x) - shift, ((int) xy.y) - shift, d[0], false, color);
                }

                // S for South
                pos.set(1.f, 0, 0);
                if (prj.projectLocal(pos, xy)) {
                    font.print(((int) xy.x) - shift, ((int) xy.y) - shift, d[1], false, color);
                }

                // E for East
                pos.set(0, 1.f, 0);
                if (prj.projectLocal(pos, xy)) {
                    font.print(((int) xy.x) - shift, ((int) xy.y) - shift, d[2], false, color);
                }

                // W for West
                pos.set(0, -1.f, 0);
                if (prj.projectLocal(pos, xy)) {
                    font.print(((int) xy.x) - shift, ((int) xy.y) - shift, d[3], false, color);
                }
            }
        } finally {
            prj.resetPerspectiveProjection();
        }
    }

    private void printGravity(Projector prj, String[] d, Point3d pos, Point3d xy, int shift) {
        // N for North
        pos.set(-1.f, 0, 0.22f);
        if (prj.projectLocal(pos, xy)) {
            prj.printGravity180(font, xy.x, xy.y, d[0], -shift, -shift);
        }
        // S for South
        pos.set(1.f, 0, 0.22f);
        if (prj.projectLocal(pos, xy)) {
            prj.printGravity180(font, xy.x, xy.y, d[1], -shift, -shift);
        }

        // E for East
        pos.set(0, 1.f, 0.22f);
        if (prj.projectLocal(pos, xy)) {
            prj.printGravity180(font, xy.x, xy.y, d[2], -shift, -shift);
        }

        // W for West
        pos.set(0, -1.f, 0.22f);
        if (prj.projectLocal(pos, xy)) {
            prj.printGravity180(font, xy.x, xy.y, d[3], -shift, -shift);
        }
    }

    public void setColor(Color c) {
        color = c;
    }

    public Color getColor() {
        return color;
    }

    public void setFont(SFontIfc someFont) throws StellariumException {
        font = someFont;
    }

    /**
     * Translate cardinal labels with gettext to current sky language.
     *
     * @param someTranslator The Translator responsible for providing the new labels.
     */
    public void translateLabels(Translator someTranslator) {
        sNorth = someTranslator.translate("N");
        sSouth = someTranslator.translate("S");
        sEast = someTranslator.translate("E");
        sWest = someTranslator.translate("W");
    }

    public void update(long deltaTime) {
//        fader.update(deltaTime);
    }

    void setFadeDuration(float duration) {
        fader.setDuration((int) (duration * 1000.f));
    }

    public void setFlagShow(boolean b) {
        fader.set(b);
    }

    public boolean getFlagShow() {
        return fader.getState();
    }

    public double getRadius() {
        return radius;
    }

    private double radius;

    private Color color;

    private SFontIfc font;

    /**
     * Cardinal labels' keys
     */
    private String sNorth = "N", sSouth = "S", sEast = "E", sWest = "W";

    private LinearFader fader = new LinearFader();
}
