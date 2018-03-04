/*
 * Stellarium
 * Copyright (C) 2002 Fabien Chereau
 * Author 2006 Johannes Gajdosik
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

/**
 * @author Fred Simon
 * @version 0.8.2
 */
public class StereographicProjector extends CustomProjector {
    public StereographicProjector(Rectangle4i viewport, double fov) {
        super(viewport, fov);
        minFov = 0.001;
        maxFov = 270.00001;
        setFieldOfView(fov);
    }

    public TYPE getType() {
        return TYPE.stereographic;
    }

    public boolean projectCustom(Point3d v,
                                 Tuple3d win,
                                 final Matrix4d mat) {
        Point3d tmp = new Point3d(v);
        mat.transform(tmp);
        final double r = StelUtility.getLength(tmp);// sqrt(x*x+y*y+z*z);
        final double h = 0.5 * (r - tmp.z);
        if (h <= 0.0) return false;
        final double f = viewScalingFactor / h;
        win.set(
                center.x + flipHorz * tmp.x * f,
                center.y + flipVert * tmp.y * f,
                r / zFar //(r - zNear) / (zFar - zNear)
        );
        return true;
    }


    void unproject(double x, double y,
                   final Matrix4d m, Point3d v) {
        x = flipHorz * (x - center.x) / (viewScalingFactor * 2);
        y = flipVert * (y - center.y) / (viewScalingFactor * 2);
        final double lq = x * x + y * y;
        v.set(
                2.0 * x,
                2.0 * y,
                -(lq - 1.0)// why minus ?
        );
        v.scale(1.0 / (lq + 1.0));
        //cout << "StereographicProjector::unproject: before("
        //     << v[0] << ',' << v[1] << ',' << v[2] << ')' << endl;
        m.transform(v);
        //cout << "StereographicProjector::unproject: after ("
        //     << v[0] << ',' << v[1] << ',' << v[2] << ')' << endl;
    }
}
