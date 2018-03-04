/*
 * Stellarium
 * Copyright (C) 2003 Fabien Chereau
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

import org.stellarium.vecmath.Rectangle4i;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

/**
 * @author Fred Simon
 * @version 0.8.2
 */
public class SphericMirrorProjector extends CustomProjector {
    private SphericMirrorCalculator calc = new SphericMirrorCalculator();

    public SphericMirrorProjector(Rectangle4i viewport, double fov) {
        super(viewport, fov);
        minFov = 0.001;
        maxFov = 180.00001;
        setFieldOfView(fov);
    }

    public TYPE getType() {
        return TYPE.spheric_mirror;
    }

    public boolean needGlFrontFaceCW() {
        return !super.needGlFrontFaceCW();
    }

    public void setViewport(int x, int y, int w, int h) {
        super.setViewport(x, y, w, h);
        center.set(vecViewport.x + vecViewport.w / 2,
                vecViewport.y + 4 * vecViewport.z / 8, 0);
    }

    public boolean projectCustom(final Point3d v, Tuple3d win, Matrix4d mat) {
        Point3d S = new Point3d(v);
        mat.transform(S);

        final double z = S.z;
        S.z = S.y;
        S.y = -z;

        Point3d tmp = new Point3d();
        final boolean rval = calc.transform(S, tmp);

        win.set(center.x - flipHorz * tmp.x * (5 * viewScalingFactor),
                center.y + flipVert * tmp.y * (5 * viewScalingFactor),
                rval ? ((-z - zNear) / (zFar - zNear)) : -1000
        );
        return rval;
    }


    Point3d unproject(double x, double y, Matrix4d m) {
        x = -flipHorz * (x - center.x) / (5 * viewScalingFactor);
        y = flipVert * (y - center.y) / (5 * viewScalingFactor);

        Point3d v = new Point3d();
        calc.retransform(x, y, v);

        v.z = -v.z;  // why

        double tmpy = v.y;
        v.y = -v.z;
        v.z = tmpy;

        m.transform(v);
        return v;
    }
}
