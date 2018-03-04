/*
 * Stellarium
 * Copyright (C) 2003 Fabien Chereau
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
package org.stellarium.projector;

import org.stellarium.StelUtility;
import org.stellarium.vecmath.Rectangle4i;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import static java.lang.StrictMath.*;

/**
 * @author Fred Simon
 * @version 0.8.2
 */
public class CylinderProjector extends CustomProjector {
    public CylinderProjector(Rectangle4i viewport, double fov) {
        super(viewport, fov);
        minFov = 0.001;
        maxFov = 500.00001;
        setFieldOfView(fov);
    }

    public TYPE getType() {
        return TYPE.cylinder;
    }

    public boolean projectCustom(final Point3d v,
                                 Tuple3d win,
                                 final Matrix4d mat) {
        Point3d tmp = new Point3d(v);
        mat.transform(tmp);
        final double tmpLength = StelUtility.getLength(tmp);
        final double alpha = asin(tmp.y / tmpLength);
        final double delta = atan2(tmp.z, tmp.x);
        win.set(
                center.x + (flipHorz * delta * viewScalingFactor),
                center.y + (flipVert * alpha * viewScalingFactor),
                (tmpLength - zNear) / (zFar - zNear))
        ;
        return true;
    }

    /**
     * @param x
     * @param y
     * @param m
     * @return The 3D coordinates
     */
    Point3d unproject(double x, double y, Matrix4d m) {
        final double d = flipHorz * (x - center.x) / viewScalingFactor;
        final double a = flipVert * (y - center.y) / viewScalingFactor;
        Point3d v = new Point3d(
                cos(a) * cos(d),
                sin(a),
                -cos(a) * sin(d)// why minus ?
        );
        m.transform(v);
        return v;
    }
}
