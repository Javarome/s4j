/*
* Copyright (C) 1999, 2000 Juan Carlos Remis
* Copyright (C) 2002 Liam Girdwood
* Copyright (C) 2003 Fabien Chereau
* Copyright (C) 2006 Jerome Beau
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
package org.stellarium.astro;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class Misc {
    /**
     * puts a large angle in the correct range 0 - 360 degrees
     */
    static double rangeDegrees(double d) {
        d %= 360;
        if (d < 0) d += 360;
        return d;
    }

    /**
     * puts a large angle in the correct range 0 - 2PI radians
     */
    static double rangeRadians(double r) {
        r %= 2 * Math.PI;
        if (r < 0) {
            r += 2. * Math.PI;
        }
        return r;
    }

    /**
     * The obliquity formula (and all the magic numbers below) come from Meeus,
     * Astro Algorithms.
     *
     * @param t Time in julian day. Valid range is the years -8000 to +12000 (t = -100 to 100).
     * @return Mean obliquity (epsilon sub 0) in degrees.
     */
    static double getMeanObliquity(double t) {
        double u, u0;
        double t0 = 30000.;
        double rval = 0.;
        final double rvalStart = 23. * 3600. + 26. * 60. + 21.448;
        final int OBLIQ_COEFFS = 10;
        final double coeffs[] = new double[]{
                -468093., -155., 199925., -5138., -24967.,
                -3905., 712., 2787., 579., 245.};
        int i;
        t = (t - JulianDay.J2000) / 36525.; // Convert time in centuries

        if (t0 != t) {
            t0 = t;
            u = u0 = t / 100.;     // u is in julian 10000's of years
            rval = rvalStart;
            for (i = 0; i < OBLIQ_COEFFS; i++) {
                rval += u * coeffs[i] / 100.;
                u *= u0;
            }
            // convert from seconds to degree
            rval /= 3600.;
        }
        return rval;
    }

    /**
     * Obtains a ln_date from 2 strings.
     * Uses the current date if s1 is "today" and current time if s2 is "now"
     *
     * @param s1 date with the form dd/mm/yyyy
     * @param s2 time with the form hh:mm:ss.s
     * @return null if s1 or s2 is not valid.
     */
    static Date strToDate(String s1, String s2) {
        Date date = new Date();
        if (s1 == null || s2 == null) return null;
        if (!"today".equals(s1)) {
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
            try {
                date = dateFormat.parse(s1);
            } catch (ParseException e) {
                return null;
            }
        }

        Date tempDate2;
        if ("now".equals(s2)) {
            tempDate2 = new Date();
        } else {
            DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.s\n");
            try {
                tempDate2 = dateFormat.parse(s2);
            } catch (ParseException e) {
                return null;
            }
        }
        date.setHours(tempDate2.getHours());
        date.setMinutes(tempDate2.getMinutes());
        date.setSeconds(tempDate2.getSeconds());

        // Java months start at 0
        if (date.getMonth() > 11 || date.getMonth() < 0 || date.getDate() < 1 || date.getDate() > 31 ||
                date.getHours() > 23 || date.getHours() < 0 || date.getMinutes() < 0 || date.getMinutes() > 59 ||
                date.getSeconds() < 0 || date.getSeconds() >= 60) {
            return null;
        }
        return date;
    }
}