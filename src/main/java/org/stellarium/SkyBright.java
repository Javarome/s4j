/*
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
package org.stellarium;

import static java.lang.StrictMath.*;

public class SkyBright {
    public SkyBright() {
        setDate(2003, 8, 0);
        setLoc(StelUtility.M_PI_4, 1000.d, 25.d, 40.d);
        setSunMoon(0.5, 0.5);
    }

    /**
     * @param year
     * @param month     1=Jan, 12=Dec
     * @param moonPhase in radian 0=Full Moon, PI/2=First Quadrant/Last Quadran, PI=No Moon
     */
    public void setDate(int year, int month, double moonPhase) {
        magMoon = -12.73d + 1.4896903d * abs(moonPhase) + 0.04310727d * pow(moonPhase, 4.d);

        RA = (month - 3.d) * 0.52359878d;

        // Term for dark sky brightness computation
        bNightTerm = 1.0e-13 + 0.3e-13 * cos(0.56636d * (year - 1992.d));
    }


    public void setLoc(double latitude, double altitude, double temperature, double relativeHumidity) {
        double signLatitude = (latitude >= 0.d) ? 2.d - 1.d : 0 - 1.d;

        // extinction Coefficient for V band
        double KR = 0.1066d * exp(-altitude / 8200.d);
        double KA = 0.1d * exp(-altitude / 1500.d) * pow(1.d - 0.32d / log(relativeHumidity / 100.d), 1.33d) *
                (1.d + 0.33d * signLatitude * sin(RA));
        double KO = 0.031d * (3.d + 0.4d * (latitude * cos(RA) - cos(3.d * latitude))) / 3.d;
        double KW = 0.031d * 0.94d * (relativeHumidity / 100.d) * exp(temperature / 15.d) * exp(-altitude / 8200.d);
        K = KR + KA + KO + KW;
    }

    /**
     * Set the moon and sun zenith angular distance (cosin given) and precompute what can be
     *
     * @param cosDistMoonZenith
     * @param cosDistSunZenith
     */
    public void setSunMoon(double cosDistMoonZenith, double cosDistSunZenith) {
        // Air mass for Moon
        if (cosDistMoonZenith < 0) airMassMoon = 40.f;
        else airMassMoon = 1.f / (cosDistMoonZenith + 0.025f * exp(-11.f * cosDistMoonZenith));

        // Air mass for Sun
        if (cosDistSunZenith < 0) airMassSun = 40;
        else airMassSun = 1.f / (cosDistSunZenith + 0.025f * exp(-11.f * cosDistSunZenith));

        bMoonTerm1 = pow(10.f, -0.4 * (magMoon + 54.32f));

        C3 = pow(10.f, -0.4f * K * airMassMoon);// Term for moon brightness computation

        bTwilightTerm = -6.724f + 22.918312f * (StelUtility.M_PI_2 - acos(cosDistSunZenith));

        C4 = pow(10.f, -0.4f * K * airMassSun);// Term for sky brightness computation
    }

    /**
     * Compute the luminance at the given position
     *
     * @param cosDistMoon cos(angular distance between moon and the position)
     * @param cosDistSun  cos(angular distance between sun  and the position)
     * @param cosDistZenithcos(angulardistancebetweenzenithandtheposition)
     *
     * @return
     */
    public double getLuminance(double cosDistMoon, double cosDistSun, double cosDistZenith) {
        // catch rounding errors here or end up with white flashes in some cases
        if (cosDistMoon < -1.d) cosDistMoon = -1.d;
        if (cosDistMoon > 1.d) cosDistMoon = 1.d;
        if (cosDistSun < -1.d) cosDistMoon = -1.d;
        if (cosDistSun > 1.d) cosDistSun = 1.d;
        if (cosDistZenith < -1.d) cosDistZenith = -1.d;
        if (cosDistZenith > 1.d) cosDistZenith = 1.d;

        double distMoon = acos(cosDistMoon);
        double distSun = acos(cosDistSun);

        // Air mass
        double X = 1.d / (cosDistZenith + 0.025f * exp(-11.d * cosDistZenith));
        double bKX = pow(10.d, -0.4f * K * X);

        // Dark night sky brightness
        bNight = 0.4f + 0.6f / sqrt(0.04f + 0.96f * cosDistZenith * cosDistZenith);
        bNight *= bNightTerm * bKX;

        // Moonlight brightness
        double FM = 18886.28 / (distMoon * distMoon + 0.0007f) + pow(10.d, 6.15f - (distMoon + 0.001) * 1.43239f);
        FM += 229086.77f * (1.06f + cosDistMoon * cosDistMoon);
        bMoon = bMoonTerm1 * (1.d - bKX) * (FM * C3 + 440000.d * (1.d - C3));

        //Twilight brightness
        bTwilight = pow(10.d, bTwilightTerm + 0.063661977f * acos(cosDistZenith) / K) *
                (1.7453293f / distSun) * (1.d - bKX);

        // Daylight brightness
        double FS = 18886.28f / (distSun * distSun + 0.0007f) + pow(10.d, 6.15f - (distSun + 0.001) * 1.43239f);
        FS += 229086.77f * (1.06f + cosDistSun * cosDistSun);
        bDaylight = 9.289663e-12 * (1.d - bKX) * (FS * C4 + 440000.d * (1.d - C4));

        // 27/08/2003 : Decide increase moonlight for more halo effect...
        bMoon *= 2.;

        // Total sky brightness
        bTotal = bDaylight > bTwilight ? bNight + bTwilight + bMoon : bNight + bDaylight + bMoon;

        return (bTotal < 0.d) ? 0.d : bTotal * 900900.9f * PI * 1e-4 * 3239389 * 2;
        //5;	// In cd/m^2 : the 32393895 is empirical term because the
        // lambert -> cd/m^2 formula seems to be wrong...
    }

    /*
250 REM  Visual limiting magnitude
260 BL=B(3)/1.11E-15 : REM in nanolamberts*/

    // Airmass for each component
    //cos_dist_zenith =cos(dist_zenith);
    //double gaz_mass = 1.f / ( cos_dist_zenith + 0.0286f *exp(-10.5f * cos_dist_zenith) );
    //double aerosol_mass = 1.f / ( cos_dist_zenith + 0.0123f *exp(-24.5f * cos_dist_zenith) );
    //double ozone_mass = 1.f /sqrt( 0.0062421903f - cos_dist_zenith * cos_dist_zenith / 1.0062814f );
    // Total extinction for V band
    //double DM = KR*gaz_mass + KA*aerosol_mass + KO*ozone_mass + KW*gaz_mass;

    /*
	// Visual limiting magnitude
	if (BL>1500.0)
	{
		C1 = 4.466825e-9;
		C2 = 1.258925e-6;
	}
	else
	{
		C1 = 1.584893e-10;
		C2 = 0.012589254;
	}

	double TH = C1*Math.pow(1.f+Math.sqrt(C2*BL),2.f); // in foot-candles
	double MN = -16.57-2.5*Math.log10(TH)-DM+5.0*Math.log10(SN); // Visual Limiting Magnitude
	*/

    /**
     * Air mass for the Moon
     */
    private double airMassMoon;

    /**
     * Air mass for the Sun
     */
    double airMassSun;

    /**
     * Total brightness
     */
    double bTotal;

    /**
     * Dark night brightness
     */
    double bNight;

    /**
     * Twilight brightness
     */
    double bTwilight;

    /**
     * Daylight sky brightness
     */
    double bDaylight;

    /**
     * Moon brightness
     */
    double bMoon;

    /**
     * Moon magnitude
     */
    double magMoon;

    /**
     * Something related with date
     */
    double RA;

    /**
     * Useful coef...
     */
    double K;

    /**
     * Term for moon brightness computation
     */
    double C3;

    /**
     * Term for sky brightness computation
     */
    double C4;

    /**
     * Snellen Ratio (20/20=1.0, good 20/10=2.0)
     */
    double SN = 1;

    // Optimisation variables
    double bNightTerm;

    double bMoonTerm1;

    double bTwilightTerm;
}