/*
 * Copyright (C) 2006 Frederic Simon
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

import org.stellarium.astro.Planet;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;


public interface NavigatorIfc {
    Matrix4d getJ2000ToEyeMat();

    /**
     * Place openGL in local viewer coordinates (Usually somewhere on earth viewing in a specific direction)
     */
    void switchToLocal();

    Matrix4d getLocalToEyeMat();

    enum VIEWING_MODE_TYPE {
        HORIZON,
        EQUATOR
    }

    double JD_MINUTE = 0.00069444444444444444444;

    double JD_HOUR = 0.041666666666666666666;

    double JD_DAY = 1;

    /**
     * Conversion in standar Julian time format
     */
    double JD_SECOND = 0.000011574074074074074074;

    /**
     * Transform vector from equatorial coordinate to local
     */
    Point3d earthEquToLocal(Point3d v);

    Point3d j2000ToEarthEqu(Point3d v);

    /**
     * @return the observer heliocentric position
     */
    Point3d getObserverHelioPos();

    /**
     * Transform vector from heliocentric coordinate to false equatorial : equatorial
     * coordinate but centered on the observer position (usefull for objects close to earth)
     */
    Point3d helioToEarthPosEqu(Point3d v);

    /**
     * Return the modelview matrix for some coordinate systems
     */
    Matrix4d getHelioToEyeMat();

    Planet getHomePlanet();

    double getJulianDay();

    Matrix4d getMatVsop87ToJ2000();

    /**
     * Transform vector from heliocentric coordinate to earth equatorial
     */
    Vector3d helioToEarthEqu(Vector3d v);

    /**
     * Transform point from local coordinate to equatorial
     */
    Point3d localToEarthEqu(Point3d v);

    Point3d getPrecEquVision();
}