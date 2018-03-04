/*
 * Stellarium
 * This file Copyright (C) 2004 Robert Spearman
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

// This is an ad hoc meteor model
// Could use a simple ablation physics model in the future

/*
NOTE: Here the radiant is always along the ecliptic at the apex of the Earth's way.
In reality, individual meteor streams have varying velocity vectors and therefore radiants
which are generally not at the apex of the Earth's way, such as the Perseids shower.
*/
package org.stellarium.astro;

// Improved realism and efficiency 2004-12

import org.stellarium.Navigator;
import org.stellarium.NavigatorIfc;
import org.stellarium.StelUtility;
import org.stellarium.ToneReproductor;
import org.stellarium.projector.DefaultProjector;
import static org.stellarium.ui.SglAccess.*;

import javax.media.opengl.GL;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

// TODO: This class is not OK need to be changed
class Meteor {
    Meteor(DefaultProjector proj, NavigatorIfc nav, ToneReproductor eye, double v) {
        //  velocity = 11+(double)rand()/((double)RAND_MAX+1)*v;  // abs range 11-72 km/s
        velocity = v;

        maxMag = 1;

        // determine meteor model view matrix (want z in dir of travel of earth, z=0 at center of earth)
        // meteor life is so short, no need to recalculate
        double equRotation;// rotation needed to align with path of earth
        Vector3d sunDir = nav.helioToEarthEqu(new Vector3d(0, 0, 0));

        Matrix4d tmat = new Matrix4d();
        tmat.rotX(Math.toRadians(-23.45));// ecliptical tilt
        // TODO: Check if translator needed here.
        // The vector transform is not taking the translator point of tmat. Equivalent to multiplyWithoutTransalation.
        tmat.transform(sunDir);// convert to ecliptical coordinates
        sunDir.normalize();
        equRotation = Math.acos(sunDir.dot(new Vector3d(1, 0, 0)));
        if (sunDir.y < 0) {
            equRotation = 2 * Math.PI - equRotation;
        }

        equRotation -= StelUtility.M_PI_2;

        mmat = new Matrix4d();
        mmat.rotX(Math.toRadians(23.45));
        mmat.rotZ(equRotation);
        mmat.rotY(StelUtility.M_PI_2);

        // select random trajectory using polar coordinates in XY plane, centered on observer
        xyDistance = Math.random() * VISIBLE_RADIUS;
        double angle = Math.random() * 2 * Math.PI;

        // find observer position in meteor coordinate system
        obs = nav.localToEarthEqu(new Point3d(0, 0, EARTH_RADIUS));
        Matrix4d mmatTranspose = new Matrix4d();
        mmatTranspose.transpose(mmat);
        mmatTranspose.transform(obs);

        // set meteor start x,y
        posInternal.x = posTrain.x = position.x = xyDistance * Math.cos(angle) + obs.x;
        posInternal.y = posTrain.y = position.y = xyDistance * Math.sin(angle) + obs.y;

        // determine life of meteor (start and end z value based on atmosphere burn altitudes)

        // D is distance from center of earth
        double D = Math.sqrt(position.x * position.x + position.y * position.y);

        if (D > EARTH_RADIUS + HIGH_ALTITUDE) {
            // won't be visible
            alive = false;
            return;
        }

        startH = Math.sqrt(Math.pow(EARTH_RADIUS + HIGH_ALTITUDE, 2) - D * D);

        // determine end of burn point, and nearest point to observer for distance mag calculation
        // mag should be max at nearest point still burning
        if (D > EARTH_RADIUS + LOW_ALTITUDE) {
            endH = -startH;// earth grazing
            minDist = xyDistance;
        } else {
            endH = Math.sqrt(Math.pow(EARTH_RADIUS + LOW_ALTITUDE, 2) - D * D);
            minDist = Math.sqrt(xyDistance * xyDistance + Math.pow(endH - obs.z, 2));
        }

        if (minDist > VISIBLE_RADIUS) {
            // on average, not visible (although if were zoomed ...)
            alive = false;
            return;
        }

        /* experiment
        // limit lifetime to 0.5-3.0 sec
        double tmp_h = start_h - velocity * (0.5 + (double)rand()/((double)RAND_MAX+1) * 2.5);
        if( tmp_h > end_h ) {
          end_h = tmp_h;
        }
        */

        posTrain.z = position.z = startH;

        //  printf("New meteor: %f %f s:%f e:%f v:%f\n", position[0], position[1], start_h, end_h, velocity);

        alive = true;
        train = false;

        // Determine drawing color given magnitude and eye
        // (won't be visible during daylight)

        // *** color varies somewhat based on velocity, plus atmosphere reddening

        // determine intensity
        double Mag1 = Math.random() * 6.75 - 3;
        double Mag2 = Math.random() * 6.75 - 3;
        double Mag = (Mag1 + Mag2) / 2.0f;

        mag = (5. + Mag) / 256.0;
        if (mag > 250) {
            mag = mag - 256;
        }

        double term1 = Math.exp(-0.92103f * (mag + 12.12331f)) * 108064.73f;

        double cmag = 1.f;
        double rmag;

        // Compute the equivalent star luminance for a 5 arc min circle and convert it
        // in function of the eye adaptation
        rmag = eye.adaptLuminance(term1);
        rmag = rmag / Math.pow(proj.getFieldOfView(), 0.85f) * 50.f;

        // if size of star is too small (blink) we put its size to 1.2 -. no more blink
        // And we compensate the difference of brighteness with cmag
        if (rmag < 1.2f) {
            cmag = rmag * rmag / 1.44f;
        }

        mag = cmag;// assumes white

        // most visible meteors are under about 180km distant
        // scale max mag down if outside this range
        double scale = 1;
        if (minDist != 0) {
            scale = 180 * 180 / (minDist * minDist);
        }
        if (scale < 1) {
            mag *= scale;
        }
    }


    /**
     * returns true if alive
     *
     * @param deltaTime
     * @return
     */
    boolean update(int deltaTime) {
        if (!alive) {
            return false;
        }

        if (position.z < endH) {
            // burning has stopped so magnitude fades out
            // assume linear fade out

            mag -= maxMag * (double) deltaTime / 500.0f;
            if (mag < 0) {
                alive = false;// no longer visible
            }
        }

        // *** would need time direction multiplier to allow reverse time replay
        position.z -= position.z - (velocity * deltaTime / 1000.0f);// TODO(JBE): set position

        // train doesn't extend beyond start of burn

        if (position.z + velocity * 0.5f > startH) {
            posTrain.z = startH;
        } else {
            posTrain.z -= velocity * (double) deltaTime / 1000.0f;
        }

        //printf("meteor position: %f delta_t %d\n", position[2], deltaTime);

        // determine visual magnitude based on distance to observer
        double dist = Math.sqrt(xyDistance * xyDistance + Math.pow(position.z - obs.z, 2));

        if (dist == 0) {
            dist = .01;// just to be cautious (meteor hits observer!)
        }

        distMultiplier = minDist * minDist / (dist * dist);

        return alive;
    }

    /**
     * returns true if visible
     *
     * @param proj
     * @param nav
     * @return
     */
    boolean draw(DefaultProjector proj, Navigator nav) {
        if (!alive) {
            return false;
        }

        Point3d start = new Point3d(), end = new Point3d();

        Point3d spos = position;
        Point3d epos = posTrain;

        // convert to equ
        mmat.transform(spos);
        mmat.transform(epos);

        // convert to local and correct for earth radius [since equ and local coordinates in stellarium use same 0 point!]
        spos = nav.earthEquToLocal(spos);
        epos = nav.earthEquToLocal(epos);
        spos.z -= EARTH_RADIUS;
        epos.z -= EARTH_RADIUS;

        boolean t1 = proj.projectLocalCheck(StelUtility.div(spos, 1216), start);// 1216 is to scale down under 1 for desktop version
        boolean t2 = proj.projectLocalCheck(StelUtility.div(epos, 1216), end);

        // don't draw if not visible (but may come into view)
        if (!t1 & !t2) {
            return true;
        }

        //  printf("[%f %f %f] (%d, %d) (%d, %d)\n", position[0], position[1], position[2], (int)start[0], (int)start[1], (int)end[0], (int)end[1]);

        glEnable(GL.GL_BLEND);
        glDisable(GL.GL_TEXTURE_2D);// much dimmer without this

        if (train) {
            // connect this point with last drawn point

            double tmag = mag * distMultiplier;

            // compute an intermediate point so can curve slightly along projection distortions
            Point3d intpos = new Point3d();
            Point3d posi = posInternal;
            posi.z = position.z + (posTrain.z - position.z) / 2;
            mmat.transform(posi);
            posi = nav.earthEquToLocal(posi);
            posi.z -= EARTH_RADIUS;
            proj.projectLocal(StelUtility.div(posi, 1216), intpos);

            // draw dark to light
            glBegin(GL.GL_LINE_STRIP);
            glColor3d(0, 0, 0);
            glVertex3d(end.x, end.y, 0);
            glColor3d(tmag / 2, tmag / 2, tmag / 2);
            glVertex3d(intpos.x, intpos.y, 0);
            glColor3d(tmag, tmag, tmag);
            glVertex3d(start.x, start.y, 0);
            glEnd();
        } else {
            glPointSize(1);
            glBegin(GL.GL_POINTS);
            glVertex3d(start.x, start.y, 0);
            glEnd();
        }

        /*
        // TEMP - show radiant
        Vector3d radiant = Vector3d(0,0,0.5f);
        radiant.transfo4d(mmat);
        if( projection.PROJECT_EARTH_EQU(radiant, start) ) {
          glColor3f(1,0,1);
          glBegin(GL_LINES);
          glVertex3f(start[0]-10,start[1],0);
          glVertex3f(start[0]+10,start[1],0);
          glEnd();

          glBegin(GL_LINES);
          glVertex3f(start[0],start[1]-10,0);
          glVertex3f(start[0],start[1]+10,0);
          glEnd();
        }
        */

        glEnable(GL.GL_TEXTURE_2D);

        train = true;

        return (true);
    }

    boolean isAlive() {
        return alive;
    }

    static final double EARTH_RADIUS = 6369;

    static final double HIGH_ALTITUDE = 115;

    static final double LOW_ALTITUDE = 70;

    static final double VISIBLE_RADIUS = 457.8;


    /**
     * Tranformation matrix to align radiant with earth direction of travel
     */
    private Matrix4d mmat;

    /**
     * Observer position in meteor coord. system
     */
    private Point3d obs;

    /**
     * Equatorial coordinate position
     */
    private Point3d position = new Point3d();

    /**
     * Middle of train
     */
    private Point3d posInternal = new Point3d();

    /**
     * End of train
     */
    private Point3d posTrain = new Point3d();

    /**
     * Point or train visible?
     */
    private boolean train;

    /**
     * Start height above center of earth
     */
    private double startH;

    /**
     * End height
     */
    private double endH;

    /**
     * km/s
     */
    private double velocity;

    /**
     * Is it still visible?
     */
    private boolean alive;

    /**
     * Apparent magnitude at head, 0-1
     */
    private double mag;

    /**
     * 0-1
     */
    private double maxMag;

    /**
     * absolute magnitude
     */
    private double absMag;

    /**
     * visual magnitude at observer
     */
    private double visMag;

    /**
     * distance in XY plane (orthogonal to meteor path) from observer to meteor
     */
    private double xyDistance;

    /**
     * initial distance from observer
     */
    private double initDist;

    /**
     * nearest point to observer along path
     */
    private double minDist;

    /**
     * scale magnitude due to changes in distance
     */
    private double distMultiplier;
}
