/*
* This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau
* and is a Java version of the original Stellarium C++ version,
* (http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/constellation_mgr.cpp?rev=1.53&view=markup)
* which is Copyright (c) 2002 Fabien Chereau
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
package org.stellarium.landscape;

import org.stellarium.SkyBright;
import org.stellarium.Skylight;
import org.stellarium.StelUtility;
import org.stellarium.ToneReproductor;
import org.stellarium.astro.JulianDay;
import org.stellarium.projector.Projector;
import static org.stellarium.ui.SglAccess.*;
import org.stellarium.ui.fader.Fader;
import org.stellarium.ui.fader.ParabolicFader;

import static javax.media.opengl.GL.*;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.atan;
import java.util.Date;

/**
 * @author Jerome Beau, Fred Simon
 * @version 0.8.2
 */
public class StelAtmosphere {
    private int DEFAULT_RESOLUTION = 48;

    public StelAtmosphere() {
        worldAdaptationLuminance = 0;
        atmIntensity = 0;
        setResolution(DEFAULT_RESOLUTION);
        setFadeDuration(3.f);
    }

    public void setResolution(int someResolution) {
        if (someResolution != skyResolution) {
            skyResolution = someResolution;
            // Create the vector array used to store the sky color on the full field of view
            tabSky = new Vector3d[skyResolution + 1][skyResolution + 1];
            for (int k = 0; k < skyResolution + 1; k++) {
                tabSky[k] = new Vector3d[skyResolution + 1];
                for (int i = 0; i < tabSky[k].length; i++) {
                    tabSky[k][i] = new Vector3d();
                }
            }
        }
    }

    /**
     * Define whether to display atmosphere
     */
    public void setVisible(boolean b) {
        fader.set(b);
    }

    public double getIntensity() {
        return atmIntensity;
    }

    public void computeColor(double JD, Point3d sunPos, Point3d moonPos, float moonPhase, ToneReproductor eye, Projector prj) {
        computeColor(JD, sunPos, moonPos, moonPhase, eye, prj, 45, 200, 15, 40);
    }

    public void computeColor(double JD, Point3d sunPos, Point3d moonPos, float moonPhase, ToneReproductor eye, Projector prj,
                             double latitude, double altitude) {
        computeColor(JD, sunPos, moonPos, moonPhase, eye, prj, latitude, altitude, 15, 40);
    }

    /**
     * @param latitude Latitude, in degrees
     */
    public void computeColor(double JD, Point3d sunPos, Point3d moonPos, double moonPhase, ToneReproductor eye, Projector prj,
                             double latitude, double altitude, double temperature, double relativeHumidity) {
        double minMwLum = 0.13;

        // no need to calculate if not visible
        if (!fader.hasInterstate()) {
            atmIntensity = 0;
            worldAdaptationLuminance = 3.75f;
            milkywayAdaptationLuminance = minMwLum;// brighter than without atm, since no drawing addition of atm brightness
            return;
        } else {
            atmIntensity = fader.getInterstate();
        }

        //Vector3d obj;
        Skylight.SkylightStruct2 b2 = new Skylight.SkylightStruct2();

        Vector3d someSunPos = new Vector3d(sunPos);
        Vector3d someMoonPos = new Vector3d(moonPos);

        // these are for radii
        double sunAngularSize = atan(696000 / (someSunPos.length() * StelUtility.AU));
        double moonAngularSize = atan(1738 / (someMoonPos.length() * StelUtility.AU));

        double touchAngle = sunAngularSize + moonAngularSize;
        double darkAngle = moonAngularSize - sunAngularSize;

        someSunPos.normalize();
        someMoonPos.normalize();

        // determine luminance falloff during solar eclipses
        double separationAngle = acos(someSunPos.dot(someMoonPos));// angle between them

        //	printf("touch at %f\tnow at %f (%f)\n", touchAngle, separationAngle, separationAngle/touchAngle);

        // bright stars should be visible at total eclipse
        // TODO: correct for atmospheric diffusion
        // TODO: use better coverage function (non-linear)
        // because of above issues, this algorithm darkens more quickly than reality
        if (separationAngle < touchAngle) {
            double min;
            if (darkAngle < 0) {
                // annular eclipse
                double aSun = sunAngularSize * sunAngularSize;
                min = (aSun - (moonAngularSize * moonAngularSize)) / aSun;// minimum proportion of sun uncovered
                darkAngle *= -1;
            } else min = 0.004;// so bright stars show up at total eclipse

            if (separationAngle < darkAngle) {
                atmIntensity = (float) min;
            } else {
                atmIntensity *= min + (((1 - min) * (separationAngle - darkAngle)) / (touchAngle - darkAngle));
            }

            //		printf("atm int %f (min %f)\n", atm_intensity, min);
        }

        float sunPos_pos[] = new float[3];
        sunPos_pos[0] = (float) someSunPos.x;
        sunPos_pos[1] = (float) someSunPos.y;
        sunPos_pos[2] = (float) someSunPos.z;

        double moon_pos[] = new double[3];
        moon_pos[0] = someMoonPos.x;
        moon_pos[1] = someMoonPos.y;
        moon_pos[2] = someMoonPos.z;

        skyLight.setParamsV(sunPos_pos, 5.f);

        skyBright.setLoc(Math.toRadians(latitude), altitude, temperature, relativeHumidity);
        skyBright.setSunMoon(moon_pos[2], sunPos_pos[2]);

        // Calculate the date from the julian day.
        Date date = JulianDay.julianToDate(JD);

        skyBright.setDate(date.getYear(), date.getMonth(), moonPhase);

        double stepX = (double) prj.getViewportWidth() / skyResolution;
        double stepY = (double) prj.getViewportHeight() / skyResolution;
        double viewportLeft = (double) prj.getViewportPosX();
        double viewportBottom = (double) prj.getViewportPosY();

        Point3d point3d = new Point3d(1, 0, 0);

        // Variables used to compute the average sky luminance
        double sumLum = 0;
        int nbLum = 0;

        // Compute the sky color for every point above the ground
        for (int x = 0; x <= skyResolution; ++x) {
            for (int y = 0; y <= skyResolution; ++y) {
                prj.unprojectLocal(viewportLeft + x * stepX, viewportBottom + y * stepY, point3d);
                Vector3d tmp = new Vector3d(point3d);
                tmp.normalize();
                point3d.set(tmp);

                if (point3d.z <= 0) {
                    point3d.z = -point3d.z;
                    // The sky below the ground is the symetric of the one above :
                    // it looks nice and gives proper values for brightness estimation
                }

                b2.pos[0] = (float) point3d.x;
                b2.pos[1] = (float) point3d.y;
                b2.pos[2] = (float) point3d.z;

                // Use the Skylight model for the color
                skyLight.get_xyY_valuev(b2);

                // Use the skybright.cpp 's models for brightness which gives better results.
                b2.color[2] = (float) skyBright.getLuminance(moon_pos[0] * b2.pos[0] + moon_pos[1] * b2.pos[1] +
                        moon_pos[2] * b2.pos[2], sunPos_pos[0] * b2.pos[0] + sunPos_pos[1] * b2.pos[1] +
                        sunPos_pos[2] * b2.pos[2], b2.pos[2]);


                sumLum += b2.color[2];
                ++nbLum;
                eye.xyYToRGB(b2.color);
                tabSky[x][y].set(atmIntensity * b2.color[0], atmIntensity * b2.color[1], atmIntensity * b2.color[2]);
            }
        }

        worldAdaptationLuminance = 3.75f + 3.5 * (sumLum / nbLum) * atmIntensity;
        milkywayAdaptationLuminance = minMwLum * (1 - atmIntensity) + 30 * (sumLum / nbLum) * atmIntensity;

        sumLum = 0;
        nbLum = 0;
    }

    /**
     * Draw the atmosphere using the precalc values stored in tab_sky
     */
    public void draw(Projector prj, int deltaTime) {
        if (fader.hasInterstate()) {
            // printf("Atm int: %f\n", atm_intensity);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_COLOR);

            double stepX = (double) prj.getViewportWidth() / skyResolution;
            double stepY = (double) prj.getViewportHeight() / skyResolution;
            double viewportLeft = (double) prj.getViewportPosX();
            double viewBottom = (double) prj.getViewportPosY();

            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            prj.setOrthographicProjection();// set 2D coordinate
            try {
                for (int y2 = 0; y2 < skyResolution; ++y2) {
                    glBegin(GL_QUAD_STRIP);
                    for (int x2 = 0; x2 < skyResolution + 1; ++x2) {
                        Vector3d tabSkyElement = tabSky[x2][y2];
                        glColor3d(tabSkyElement.x, tabSkyElement.y, tabSkyElement.z);
                        glVertex2i((int) (viewportLeft + x2 * stepX), (int) (viewBottom + y2 * stepY));
                        Vector3d tabSkyElementYMore = tabSky[x2][y2 + 1];
                        glColor3d(tabSkyElementYMore.x, tabSkyElementYMore.y, tabSkyElementYMore.z);
                        glVertex2i((int) (viewportLeft + x2 * stepX), (int) (viewBottom + (y2 + 1) * stepY));
                    }
                    glEnd();
                }
            } finally {
                prj.resetPerspectiveProjection();
            }
        }
    }

    public void update(long deltaTime) {
        fader.update(deltaTime);
    }

    /**
     * Set fade in/out duration in seconds
     */
    public void setFadeDuration(float duration) {
        fader.setDuration((int) (duration * 1000));
    }

    /**
     * Get fade in/out duration in seconds
     */
    public float getFadeDuration() {
        return fader.getDuration() / 1000;
    }

    /**
     * Get whether atmosphere is displayed
     */
    public boolean isVisible() {
        return fader.getState();
    }

    public float getFadeIntensity() {
        return fader.getInterstate();
    }// let's you know how far faded in or out the atm is (0-1)

    public double getWorldAdaptationLuminance() {
        return worldAdaptationLuminance;
    }

    double getMilkywayAdaptationLuminance() {
        return milkywayAdaptationLuminance;
    }

    private Skylight skyLight = new Skylight();

    private SkyBright skyBright = new SkyBright();

    private int skyResolution;

    /**
     * For Atmosphere calculation
     */
    private Vector3d[][] tabSky;

    /**
     * intern variable used to store the Horizon Y screen value
     */
    private int startY;

    double worldAdaptationLuminance;

    double milkywayAdaptationLuminance;

    private float atmIntensity;

    Fader fader = new ParabolicFader();

    public int getResolution() {
        return skyResolution;
    }
}
