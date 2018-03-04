/*
Copyright (C) 2001 Liam Girdwood <liam@nova-ioe.org>
Copyright (C) 2003 Fabien Ch�reau

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Libary General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/
package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;

/**
 * Pluto coordinates.
 *
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>
 * @version Java
 */
public abstract class Pluto {

    static class PlutoArgument {
        double J, S, P;

        public PlutoArgument(double j, double s, double p) {
            J = j;
            S = s;
            P = p;
        }
    }

    static class PlutoLongitude {
        double A, B;

        public PlutoLongitude(double a, double b) {
            A = a;
            B = b;
        }
    }

    static class PlutoLatitude {
        double A, B;

        public PlutoLatitude(double a, double b) {
            A = a;
            B = b;
        }
    }

    static class PlutoRadius {
        double A, B;

        public PlutoRadius(double a, double b) {
            A = a;
            B = b;
        }
    }

    static final PlutoArgument[] argument = {
            new PlutoArgument(0, 0, 1),
            new PlutoArgument(0, 0, 2),
            new PlutoArgument(0, 0, 3),
            new PlutoArgument(0, 0, 4),
            new PlutoArgument(0, 0, 5),
            new PlutoArgument(0, 0, 6),
            new PlutoArgument(0, 1, -1),
            new PlutoArgument(0, 1, 0),
            new PlutoArgument(0, 1, 1),
            new PlutoArgument(0, 1, 2),
            new PlutoArgument(0, 1, 3),
            new PlutoArgument(0, 2, -2),
            new PlutoArgument(0, 2, -1),
            new PlutoArgument(0, 2, 0),
            new PlutoArgument(1, -1, 0),
            new PlutoArgument(1, -1, 1),
            new PlutoArgument(1, 0, -3),
            new PlutoArgument(1, 0, -2),
            new PlutoArgument(1, 0, -1),
            new PlutoArgument(1, 0, 0),
            new PlutoArgument(1, 0, 1),
            new PlutoArgument(1, 0, 2),
            new PlutoArgument(1, 0, 3),
            new PlutoArgument(1, 0, 4),
            new PlutoArgument(1, 1, -3),
            new PlutoArgument(1, 1, -2),
            new PlutoArgument(1, 1, -1),
            new PlutoArgument(1, 1, 0),
            new PlutoArgument(1, 1, 1),
            new PlutoArgument(1, 1, 3),
            new PlutoArgument(2, 0, -6),
            new PlutoArgument(2, 0, -5),
            new PlutoArgument(2, 0, -4),
            new PlutoArgument(2, 0, -3),
            new PlutoArgument(2, 0, -2),
            new PlutoArgument(2, 0, -1),
            new PlutoArgument(2, 0, 0),
            new PlutoArgument(2, 0, 1),
            new PlutoArgument(2, 0, 2),
            new PlutoArgument(2, 0, 3),
            new PlutoArgument(3, 0, -2),
            new PlutoArgument(3, 0, -1),
            new PlutoArgument(3, 0, 0)
    };

    static final PlutoLongitude[] longitude = {
            new PlutoLongitude(-19799805, 19850055),
            new PlutoLongitude(897144, -4954829),
            new PlutoLongitude(611149, 1211027),
            new PlutoLongitude(-341243, -189585),
            new PlutoLongitude(129287, -34992),
            new PlutoLongitude(-38164, 30893),
            new PlutoLongitude(20442, -9987),
            new PlutoLongitude(-4063, -5071),
            new PlutoLongitude(-6016, -3336),
            new PlutoLongitude(-3956, 3039),
            new PlutoLongitude(-667, 3572),
            new PlutoLongitude(1276, 501),
            new PlutoLongitude(1152, -917),
            new PlutoLongitude(630, -1277),
            new PlutoLongitude(2571, -459),
            new PlutoLongitude(899, -1449),
            new PlutoLongitude(-1016, 1043),
            new PlutoLongitude(-2343, -1012),
            new PlutoLongitude(7042, 788),
            new PlutoLongitude(1199, -338),
            new PlutoLongitude(418, -67),
            new PlutoLongitude(120, -274),
            new PlutoLongitude(-60, -159),
            new PlutoLongitude(-82, -29),
            new PlutoLongitude(-36, -20),
            new PlutoLongitude(-40, 7),
            new PlutoLongitude(-14, 22),
            new PlutoLongitude(4, 13),
            new PlutoLongitude(5, 2),
            new PlutoLongitude(-1, 0),
            new PlutoLongitude(2, 0),
            new PlutoLongitude(-4, 5),
            new PlutoLongitude(4, -7),
            new PlutoLongitude(14, 24),
            new PlutoLongitude(-49, -34),
            new PlutoLongitude(163, -48),
            new PlutoLongitude(9, 24),
            new PlutoLongitude(-4, 1),
            new PlutoLongitude(-3, 1),
            new PlutoLongitude(1, 3),
            new PlutoLongitude(-3, -1),
            new PlutoLongitude(5, -3),
            new PlutoLongitude(0, 0)
    };

    static final PlutoLatitude[] latitude = {
            new PlutoLatitude(-5452852, -14974862),
            new PlutoLatitude(3527812, 1672790),
            new PlutoLatitude(-1050748, 327647),
            new PlutoLatitude(178690, -292153),
            new PlutoLatitude(18650, 100340),
            new PlutoLatitude(-30697, -25823),
            new PlutoLatitude(4878, 11248),
            new PlutoLatitude(226, -64),
            new PlutoLatitude(2030, -836),
            new PlutoLatitude(69, -604),
            new PlutoLatitude(-247, -567),
            new PlutoLatitude(-57, 1),
            new PlutoLatitude(-122, 175),
            new PlutoLatitude(-49, -164),
            new PlutoLatitude(-197, 199),
            new PlutoLatitude(-25, 217),
            new PlutoLatitude(589, -248),
            new PlutoLatitude(-269, 711),
            new PlutoLatitude(185, 193),
            new PlutoLatitude(315, 807),
            new PlutoLatitude(-130, -43),
            new PlutoLatitude(5, 3),
            new PlutoLatitude(2, 17),
            new PlutoLatitude(2, 5),
            new PlutoLatitude(2, 3),
            new PlutoLatitude(3, 1),
            new PlutoLatitude(2, -1),
            new PlutoLatitude(1, -1),
            new PlutoLatitude(0, -1),
            new PlutoLatitude(0, 0),
            new PlutoLatitude(0, -2),
            new PlutoLatitude(2, 2),
            new PlutoLatitude(-7, 0),
            new PlutoLatitude(10, -8),
            new PlutoLatitude(-3, 20),
            new PlutoLatitude(6, 5),
            new PlutoLatitude(14, 17),
            new PlutoLatitude(-2, 0),
            new PlutoLatitude(0, 0),
            new PlutoLatitude(0, 0),
            new PlutoLatitude(0, 1),
            new PlutoLatitude(0, 0),
            new PlutoLatitude(1, 0)
    };

    static final PlutoRadius[] radius = {
            new PlutoRadius(66865439, 68951812),
            new PlutoRadius(-11827535, -332538),
            new PlutoRadius(1593179, -1438890),
            new PlutoRadius(-18444, 483220),
            new PlutoRadius(-65977, -85431),
            new PlutoRadius(31174, -6032),
            new PlutoRadius(-5794, 22161),
            new PlutoRadius(4601, 4032),
            new PlutoRadius(-1729, 234),
            new PlutoRadius(-415, 702),
            new PlutoRadius(239, 723),
            new PlutoRadius(67, -67),
            new PlutoRadius(1034, -451),
            new PlutoRadius(-129, 504),
            new PlutoRadius(480, -231),
            new PlutoRadius(2, -441),
            new PlutoRadius(-3359, 265),
            new PlutoRadius(7856, -7832),
            new PlutoRadius(36, 45763),
            new PlutoRadius(8663, 8547),
            new PlutoRadius(-809, -769),
            new PlutoRadius(263, -144),
            new PlutoRadius(-126, 32),
            new PlutoRadius(-35, -16),
            new PlutoRadius(-19, -4),
            new PlutoRadius(-15, 8),
            new PlutoRadius(-4, 12),
            new PlutoRadius(5, 6),
            new PlutoRadius(3, 1),
            new PlutoRadius(6, -2),
            new PlutoRadius(2, 2),
            new PlutoRadius(-2, -2),
            new PlutoRadius(14, 13),
            new PlutoRadius(-63, 13),
            new PlutoRadius(136, -236),
            new PlutoRadius(273, 1065),
            new PlutoRadius(251, 149),
            new PlutoRadius(-25, -9),
            new PlutoRadius(9, -2),
            new PlutoRadius(-8, 7),
            new PlutoRadius(2, -10),
            new PlutoRadius(19, 35),
            new PlutoRadius(10, 2)
    };

    static public class HelioCoordsFunc implements PosFunc {
        /**
         * Chap 37. Equ 37.1
         * params : Julian day, Longitude, Latitude, Radius
         * <p/>
         * Calculate Pluto heliocentric ecliptical coordinates for given julian day.
         * This function is accurate to within 0.07" in longitude, 0.02" in latitude
         * and 0.000006 AU in radius.
         * Note: This function is not valid outside the period of 1885-2099.
         * Longitude and Latitude are in radians, radius in AU.
         *
         * @param jd
         * @param eclipticPos The rect coords of ecliptic pos, as a result
         */
        public void compute(double jd, Tuple3d eclipticPos) {
            double sumLongitude = 0, sumLatitude = 0, sumRadius = 0;
            double J, S, P;
            double t, a, sinA, cosA;
            int i;
            double L, B, R;

            /* get julian centuries since J2000 */
            t = (jd - 2451545) / 36525;

            /* calculate mean longitudes for jupiter, saturn and pluto */
            J = 34.35 + 3034.9057 * t;

            S = 50.08 + 1222.1138 * t;

            P = 238.96 + 144.9600 * t;

            /* calc periodic terms in table 37.A */
            for (i = 0; i < argument.length; i++) {
                a = argument[i].J * J + argument[i].S * S + argument[i].P * P;
                double aInRadians = Math.toRadians(a);
                sinA = Math.sin(aInRadians);
                cosA = Math.cos(aInRadians);

                /* longitude */
                sumLongitude += longitude[i].A * sinA + longitude[i].B * cosA;

                /* latitude */
                sumLatitude += latitude[i].A * sinA + latitude[i].B * cosA;

                /* radius */
                sumRadius += radius[i].A * sinA + radius[i].B * cosA;
            }

            /* calc L, B, R */
            L = Math.toRadians(238.958116 + 144.96 * t + sumLongitude * 0.000001);
            B = Math.toRadians(-3.908239 + sumLatitude * 0.000001);
            R = 40.7241346 + sumRadius * 0.0000001;

            /* convert to rectangular coord */
            MiscStellPlanet.spheToRect(L, B, R, eclipticPos);
        }
    }

    public static PosFunc createHelioFunc() {
        return new Pluto.HelioCoordsFunc();
    }
}
