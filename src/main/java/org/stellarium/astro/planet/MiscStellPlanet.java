/*
* Copyright (C) 1999, 2000 Juan Carlos Remis
* Copyright (C) 2002 Liam Girdwood
* Copyright (C) 2003 Fabien Chereau
* Copyright (C) 2005 Jerome Beau
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;

/**
 * @author <a href="mailto:javarome@javarome.net">Jerome Beau</a>
 * @version Java
 */
public abstract class MiscStellPlanet {
    /**
     * Spherical to rectangular coordinates conversion
     *
     * @param lng Longitude spherical coordinate
     * @param lat Latitude spherical coordinate
     * @param r   Radius spherical coordinate
     * @param v   The rectangular coordinates
     */
    public static void spheToRect(double lng, double lat, double r, Tuple3d v) {
        final double cosLat = cos(lat);
        v.set(Math.cos(lng) * cosLat * r, sin(lng) * cosLat * r, sin(lat) * r);
    }

    static public class ZeroFunc implements PosFunc {
        /**
         * Return 0
         *
         * @param jd
         * @param eclipticPos The rect coords of ecliptic pos, as a result
         */
        public void compute(double jd, Tuple3d eclipticPos) {
            eclipticPos.set(0, 0, 0);
        }
    }

    /**
     * @return 0 of course...
     */
    public static PosFunc createSunHelioFunc() {
        return new ZeroFunc();
    }
}
