/*
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Library General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. 

Copyright (C) 2000 Liam Girdwood <liam@nova-ioe.org>

*/
package org.stellarium.astro;

import static java.lang.StrictMath.cos;

/**
 * Calculate the mean sidereal time at the meridian of Greenwich of a given date.
 * returns apparent sidereal time (degree).
 * Formula 11.1, 11.4 pg 83
 */
public class SideralTime {
    public static double getMeanSiderealTime(double JD) {
        double sidereal;
        double T;

        T = (JD - 2451545.0) / 36525.0;

        /* calc mean angle */
        sidereal = 280.46061837 + (360.98564736629 * (JD - 2451545.0)) + (0.000387933 * T * T) - (T * T * T / 38710000.0);

        /* add a convenient multiple of 360 degrees */
        sidereal = Misc.rangeDegrees(sidereal);

        return sidereal;
    }

    /**
     * Calculate the apparent sidereal time at the meridian of Greenwich of a given date.
     * returns apparent sidereal time (degree).
     * Formula 11.1, 11.4 pg 83
     */
    public static double getApparentSiderealTime(double JD) {
        double correction, sidereal;

        /* get the mean sidereal time */
        sidereal = getMeanSiderealTime(JD);

        /* add corrections for nutation in longitude and for the true obliquity of
        the ecliptic */
        Nutation.lnNutation nutation = Nutation.getNutation(JD);

        correction = (nutation.longitude * cos(Math.toRadians(nutation.obliquity)));

        sidereal += correction;

        return (sidereal);
    }

}