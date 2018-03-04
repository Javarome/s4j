package org.stellarium.projector;

/*
* Stellarium
* Copyright (C) 2003 Fabien Ch�reau
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

import org.stellarium.StelUtility;
import org.stellarium.vecmath.Rectangle4i;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import static java.lang.StrictMath.*;

/**
 * "Fish eye" projector.
 * <p/>
 * See <a href="http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/fisheye_projector.cpp?rev=1.20&view=markup">C++ version</a> of this file.
 *
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 */
public class FisheyeProjector extends CustomProjector {

    public FisheyeProjector(Rectangle4i viewport) {
        this(viewport, 175.);
    }

    public FisheyeProjector(Rectangle4i viewport, double _fov) {
        super(viewport, _fov);
        minFov = 0.0001;
        maxFov = 180.00001;
        setFieldOfView(_fov);
    }

    public TYPE getType() {
        return TYPE.fisheye;
    }

    public boolean projectCustom(final Point3d v,
                                 Tuple3d win,
                                 final Matrix4d mat) {
        // optimization by
        // 1) calling atan instead of asin (very good on Intel CPUs)
        // 2) calling sqrt only once
        // Interestingly on my Amd64 asin works slightly faster than atan
        // (although it is done in software!),
        // but the omitted sqrt is still worth it.
        // I think that for calculating win[2] we need no sqrt.
        // Johannes.
        // Fab) Removed one division
        // TODO: FRED make sure we get points here
        Point3d pwin;
        if (win instanceof Point3d) {
            pwin = (Point3d) win;
        } else {
            System.err.println("Called on project custom not point for win...");
            pwin = new Point3d(win);
        }
        mat.transform(v, pwin);
        if (win != pwin)
            win.set(pwin);
        final double oneoverh = 1. / sqrt(win.x * win.x + win.y * win.y);
        final double a = StelUtility.M_PI_2 + atan(win.z * oneoverh);
        //  modified fisheye
        //  if (a > 0.5*M_PI) a = 0.25*(M_PI*M_PI)/(M_PI-a);
        final double f = (a * viewScalingFactor) * oneoverh;
        win.x = center.x + (flipHorz * win.x * f);
        win.y = center.y + (flipVert * win.y * f);
        win.z = (abs(win.z) - zNear) / (zFar - zNear);
        return (a < (0.9 * PI));
    }

    static double length;

    Point3d unproject(double x, double y, Matrix4d m) {
        double d = min(vecViewport.getWidth(), vecViewport.getHeight()) / 2;
        // This one is really a vector
        Vector3d v = new Vector3d(
                flipHorz * (x - center.x),
                flipVert * (x - center.y),  // TODO(JBE): Was center.x in original code
                0);
        length = v.length();

        double angleCenter = (length / d) * (fieldOfView / 2) * (PI / 180);
        double r = sin(angleCenter);
        if (length != 0) {
            v.normalize();
            v.scale(r);
        } else {
            v.set(0, 0, 0);
        }

        v.z = sqrt(1 - (v.x * v.x + v.y * v.y));
        if (angleCenter > StelUtility.M_PI_2) {
            v.z = -v.z;
        }

        Point3d p = new Point3d(v);
        m.transform(p);
        return p;
    }
}