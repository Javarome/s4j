/*
* Stellarium
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

import org.stellarium.astro.Planet;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import static org.stellarium.ui.SglAccess.glLoadMatrixd;

/**
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 */
public class Navigator implements NavigatorIfc {

    public static final Matrix4d matJ2000ToVsop87;

    public static final Matrix4d matVsop87ToJ2000;

    static {
        Matrix4d tmp = new Matrix4d();
        matJ2000ToVsop87 = new Matrix4d();
        matJ2000ToVsop87.rotX(Math.toRadians(-23.4392803055555555556));
        tmp.rotZ(Math.toRadians(0.0000275));
        matJ2000ToVsop87.mul(tmp);

        matVsop87ToJ2000 = new Matrix4d();
        matVsop87ToJ2000.transpose(matJ2000ToVsop87);
    }

    Navigator(Observator obs) throws StellariumException {
        if (obs == null) {
            throw new StellariumException("Can't create a Navigator without a valid Observator");
        }
        timeSpeed = JD_SECOND;
        position = obs;
        localVision = new Point3d(1, 0, 0);
        equVision = new Point3d(1, 0, 0);
        precEquVision = new Point3d(1, 0, 0);// not correct yet...
        viewingMode = Navigator.VIEWING_MODE_TYPE.HORIZON;// default
    }

    public Matrix4d getMatVsop87ToJ2000() {
        return matVsop87ToJ2000;
    }

    ////////////////////////////////////////////////////////////////////////////////
    void updateVisionVector(long deltaTime, StelObject selected) {
        if (autoMove) {
            double raAim, declinationAim, raStart, declinationStart, raNow, deNow;

            if (zoomingMode == ZoomingMode.ZOOMING && selected != null) {
                // if zooming in, object may be moving so be sure to zoom to latest position
                move.aim = new Vector3d(selected.getEarthEquPos(this));
                move.aim.normalize();
                move.aim.scale(2);
            }

            // Use a smooth function
            float smooth = 4.f;
            double c;

            if (zoomingMode == ZoomingMode.ZOOMING) {
                if (move.coef > .9) {
                    c = 1;
                } else {
                    c = 1 - Math.pow(1. - 1.11 * (move.coef), 3);
                }
            } else if (zoomingMode == ZoomingMode.UNZOOMING) {
                if (move.coef < 0.1) {
                    // keep in view at first as zoom out
                    c = 0;

                    /* could track as moves too, but would need to know if start was actually
                       a zoomed in view on the object or an extraneous zoom out command
                       if(move.local_pos) {
                       move.start=earth_equ_to_local(selected.getEarthEquPos(this));
                       } else {
                       move.start=selected.getEarthEquPos(this);
                       }
                       move.start.normalize();
                    */

                } else {
                    c = Math.pow(1.11 * (move.coef - .1), 3);
                }
            } else c = (Math.atan(smooth * 2. * move.coef - smooth) / (2 * Math.atan(smooth))) + 0.5;


            StelUtility.Coords coordsAim;
            StelUtility.Coords coordsStart;
            if (move.localPos) {
                coordsAim = StelUtility.rectToSphe(move.aim);
                coordsStart = StelUtility.rectToSphe(move.start);
            } else {
                coordsAim = StelUtility.rectToSphe(earthEquToLocal(new Point3d(move.aim)));
                coordsStart = StelUtility.rectToSphe(earthEquToLocal(new Point3d(move.start)));
            }
            raAim = coordsAim.getRA();
            declinationAim = coordsAim.getDE();
            raStart = coordsStart.getRA();
            declinationStart = coordsStart.getDE();

            // Trick to choose the good moving direction and never travel on a distance > PI
            if (raAim - raStart > Math.PI) {
                raAim -= 2. * Math.PI;
            } else if (raAim - raStart < -Math.PI) {
                raAim += 2. * Math.PI;
            }

            deNow = declinationAim * c + declinationStart * (1. - c);
            raNow = raAim * c + raStart * (1. - c);

            StelUtility.spheToRect(raNow, deNow, localVision);
            equVision = localToEarthEqu(localVision);

            move.coef += move.speed * deltaTime;
            if (move.coef >= 1) {
                autoMove = false;
                if (move.localPos) {
                    localVision = new Point3d(move.aim);
                    equVision = localToEarthEqu(localVision);
                } else {
                    equVision = new Point3d(move.aim);
                    localVision = earthEquToLocal(equVision);
                }
            }
        } else {
            if (tracking && selected != null) {// Equatorial vision vector locked on selected object
                equVision = selected.getEarthEquPos(this);
                // Recalc local vision vector
                localVision = earthEquToLocal(equVision);

            } else {
                if (lockEquPos) {// Equatorial vision vector locked
                    // Recalc local vision vector
                    localVision = earthEquToLocal(equVision);
                } else {// Local vision vector locked
                    // Recalc equatorial vision vector
                    equVision = localToEarthEqu(localVision);
                }
            }
        }

        matEarthEquToJ2000.transform(equVision, precEquVision);
    }

    ////////////////////////////////////////////////////////////////////////////////
    void setLocalVision(Point3d pos) {
        localVision = new Point3d(pos);
        equVision = localToEarthEqu(localVision);
        matEarthEquToJ2000.transform(equVision, precEquVision);
    }

    ////////////////////////////////////////////////////////////////////////////////
    void updateMove(double deltaAz, double deltaAlt) {
        double azVision, altVision;

        StelUtility.Coords coords;
        if (viewingMode == Navigator.VIEWING_MODE_TYPE.EQUATOR) {
            coords = StelUtility.rectToSphe(equVision);
        } else {
            coords = StelUtility.rectToSphe(localVision);
        }
        azVision = coords.getLongitude();
        altVision = coords.getLatitude();

        // if we are moving in the Azimuthal angle (left/right)
        if (deltaAz != 0) {
            azVision -= deltaAz;
        }
        if (deltaAlt != 0) {
            double modifiedAltVision = altVision + deltaAlt;
            if (modifiedAltVision <= StelUtility.M_PI_2 && modifiedAltVision >= -StelUtility.M_PI_2) {
                altVision += deltaAlt;
            } else if (modifiedAltVision > StelUtility.M_PI_2) {
                altVision = StelUtility.M_PI_2 - 0.000001;// Prevent bug
            } else if (modifiedAltVision < -StelUtility.M_PI_2) {
                altVision = -StelUtility.M_PI_2 + 0.000001;// Prevent bug
            }
        }

        // recalc all the position variables
        if (deltaAz != 0 || deltaAlt != 0) {
            if (viewingMode == Navigator.VIEWING_MODE_TYPE.EQUATOR) {
                StelUtility.spheToRect(azVision, altVision, equVision);
                localVision = earthEquToLocal(equVision);
            } else {
                StelUtility.spheToRect(azVision, altVision, localVision);
                // Calc the equatorial coordinate of the direction of vision wich was in Altazimuthal coordinate
                equVision = localToEarthEqu(localVision);
                matEarthEquToJ2000.transform(equVision, precEquVision);
            }
        }

        // Update the final modelview matrices
        updateModelViewMat();
    }

    /**
     * Increment time
     *
     * @param deltaTime
     */
    void updateTime(long deltaTime) {
        julianDay += timeSpeed * ((double) deltaTime) / 1000.0;

        // Fix time limits to -100000 to +100000 to prevent bugs
        if (julianDay > 38245309.499988) {
            julianDay = 38245309.499988;
        }
        if (julianDay < -34803211.500012) {
            julianDay = -34803211.500012;
        }
    }

    // The non optimized (more clear version is available on the CVS : before date 25/07/2003)
    public void updateTransformMatrices() {
        matLocalToEarthEqu = position.getRotLocalToEquatorial(julianDay);
        matEarthEquToLocal.transpose(matLocalToEarthEqu);

        matEarthEquToJ2000.mul(matVsop87ToJ2000, position.getRotEquatorialToVsop87());
        matJ2000ToEarthEqu.transpose(matEarthEquToJ2000);

        matHelioToEarthEqu.mul(matJ2000ToEarthEqu, matVsop87ToJ2000);
        Matrix4d negPosCenterTrans = new Matrix4d();
        Vector3d tr = new Vector3d(position.getCenterVsop87Pos());
        tr.scale(-1);
        negPosCenterTrans.set(tr);
        matHelioToEarthEqu.mul(negPosCenterTrans);

        // These two next have to take into account the position of the observer on the earth
        Matrix4d tmp = new Matrix4d();
        tmp.mul(matJ2000ToVsop87, matEarthEquToJ2000);
        tmp.mul(matLocalToEarthEqu);

        tr = new Vector3d(position.getCenterVsop87Pos());
        Matrix4d translation = new Matrix4d();
        translation.set(tr);
        matLocalToHelio.mul(translation, tmp);
        translation.set(new Vector3d(0, 0, position.getDistanceFromCenter()));
        matLocalToHelio.mul(translation);

        translation.set(new Vector3d(0, 0, -position.getDistanceFromCenter()));
        tmp.transpose();
        matHelioToLocal.mul(translation, tmp);
        matHelioToLocal.mul(negPosCenterTrans);
    }

    /**
     * Update the modelview matrices
     */
    void updateModelViewMat() {
        Vector3d f;

        if (viewingMode == Navigator.VIEWING_MODE_TYPE.EQUATOR) {
            // view will use equatorial coordinates, so that north is always up
            f = new Vector3d(equVision);
        } else {
            // view will correct for horizon (always down)
            f = new Vector3d(localVision);
        }

        f.normalize();
        Point3d pyx = new Point3d(f.y, -f.x, 0);

        if (viewingMode == Navigator.VIEWING_MODE_TYPE.EQUATOR) {
            // convert everything back to local coord
            f = new Vector3d(localVision);
            f.normalize();
            pyx = earthEquToLocal(pyx);
        }
        Vector3d s = new Vector3d(pyx);

        Vector3d u = new Vector3d();
        u.cross(s, f);
        s.normalize();
        u.normalize();

        double[] newValues = {
                s.x, s.y, s.z, 0,
                u.x, u.y, u.z, 0,
                -f.x, -f.y, -f.z, 0,
                0, 0, 0, 1
                /*
                s.x, u.x, -f.x, 0,
                s.y, u.y, -f.y, 0,
                s.z, u.z, -f.z, 0,
                0, 0, 0, 1
                */
        };
        matLocalToEye.set(newValues);

        matEarthEquToEye.mul(matLocalToEye, matEarthEquToLocal);
        matHelioToEye.mul(matLocalToEye, matHelioToLocal);
        matJ2000ToEye.mul(matEarthEquToEye, matJ2000ToEarthEqu);
    }

    /**
     * @return the observer heliocentric position
     */
    public Point3d getObserverHelioPos() {
        Point3d v = new Point3d(0, 0, 0);
        matLocalToHelio.transform(v);
        return v;
    }

    /**
     * Move to the given equatorial position
     *
     * @param aim
     * @param moveDuration
     */
    public void moveTo(Point3d aim, float moveDuration) {
        moveTo(aim, moveDuration, false, ZoomingMode.NOT_ZOOMING);
    }

    /**
     * Move to the given equatorial position
     *
     * @param aim
     * @param moveDuration
     * @param localPos
     * @param zooming
     */
    void moveTo(Point3d aim, float moveDuration, boolean localPos, ZoomingMode zooming) {
        zoomingMode = zooming;
        move.aim = new Vector3d(aim);
        move.aim.normalize();
        move.aim.scale(2);
        move.start = localPos ? new Vector3d(localVision) : new Vector3d(equVision);
        move.start.normalize();
        move.speed = 1 / (moveDuration * 1000);
        move.coef = 0;
        move.localPos = localPos;
        autoMove = true;
    }

    /**
     * Set Type of viewing mode (align with horizon or equatorial coordinates)
     *
     * @param viewMode
     */
    public void setViewingMode(VIEWING_MODE_TYPE viewMode) {
        viewingMode = viewMode;

        // TODO: include some nice smoothing function trigger here to rotate between
        // the two modes
    }

    public void switchViewingMode() {
        if (viewingMode == VIEWING_MODE_TYPE.HORIZON) {
            setViewingMode(VIEWING_MODE_TYPE.EQUATOR);
        } else {
            setViewingMode(VIEWING_MODE_TYPE.HORIZON);
        }
    }

    /**
     * Time controls
     *
     * @param someJulianDay Julian Day
     */
    public void setJDay(double someJulianDay) {
        julianDay = someJulianDay;
    }

    public double getJulianDay() {
        return julianDay;
    }

    public void setTimeSpeed(double ts) {
        timeSpeed = ts;
    }

    public double getTimeSpeed() {
        return timeSpeed;
    }

    // Flags controls
    public void setTracking(boolean v) {
        tracking = v;
    }

    public boolean getTracking() {
        return tracking;
    }

    public void setLockEquPos(boolean v) {
        lockEquPos = v;
    }

    public boolean getLockEquPos() {
        return lockEquPos;
    }

    /**
     * @return The vision direction
     */
    Point3d getEquVision() {
        return equVision;
    }

    public Point3d getPrecEquVision() {
        return precEquVision;
    }

    Point3d getLocalVision() {
        return localVision;
    }

    public Planet getHomePlanet() {
        return position.getHomePlanet();
    }

    void switchToEarthEquatorial() {
        glLoadMatrixd(matEarthEquToEye);
    }

    /**
     * Place openGL in heliocentric ecliptical coordinates
     */
    public void switchToHeliocentric() {
        glLoadMatrixd(matHelioToEye);
    }

    /**
     * Place openGL in local viewer coordinates (Usually somewhere on earth viewing in a specific direction)
     */
    public void switchToLocal() {
        glLoadMatrixd(matLocalToEye);
    }

    /**
     * Transform point from local coordinate to equatorial
     */
    public Point3d localToEarthEqu(Point3d v) {
        Point3d result = new Point3d();
        matLocalToEarthEqu.transform(v, result);
        return result;
    }

    /**
     * Transform vector from equatorial coordinate to local
     */
    public Point3d earthEquToLocal(Point3d v) {
        Point3d result = new Point3d();
        matEarthEquToLocal.transform(v, result);
        return result;
    }

    Point3d earthEquToJ2000(Point3d v) {
        Point3d result = new Point3d();
        matEarthEquToJ2000.transform(v, result);
        return result;
    }

    public Point3d j2000ToEarthEqu(Point3d v) {
        Point3d result = new Point3d();
        matJ2000ToEarthEqu.transform(v, result);
        return result;
    }

    /**
     * Transform vector from heliocentric coordinate to local
     *
     * @param v the helio pos
     * @return
     */
    public void helioToLocal(Point3d v) {
        matHelioToLocal.transform(v);
    }

    /**
     * Transform vector from heliocentric coordinate to earth equatorial
     */
    public Vector3d helioToEarthEqu(Vector3d v) {
        Vector3d result = new Vector3d();
        matHelioToEarthEqu.transform(v, result);
        return result;
    }

    /**
     * Transform vector from heliocentric coordinate to false equatorial : equatorial
     * coordinate but centered on the observer position (usefull for objects close to earth)
     */
    public Point3d helioToEarthPosEqu(Point3d v) {
        Matrix4d tempMatrix = new Matrix4d();
        tempMatrix.mul(matLocalToEarthEqu, matHelioToLocal);
        Point3d result = new Point3d();
        tempMatrix.transform(v, result);
        return result;
    }

    /**
     * Return the modelview matrix for some coordinate systems
     */
    public Matrix4d getHelioToEyeMat() {
        return matHelioToEye;
    }

    public Matrix4d getEarthEquToEyeMat() {
        return matEarthEquToEye;
    }

    public Matrix4d getLocalToEyeMat() {
        return matLocalToEye;
    }

    public Matrix4d getJ2000ToEyeMat() {
        return matJ2000ToEye;
    }

    public VIEWING_MODE_TYPE getViewingMode() {
        return viewingMode;
    }

    /**
     * Struct used to store data for auto mov
     */
    private class AutoMove {
        Vector3d start;

        Vector3d aim;

        double speed;

        double coef;

        /**
         * Define if the position are in equatorial or altazimutal
         */
        boolean localPos;
    }

    // Matrices used for every coordinate transfo
    /**
     * Transform from Heliocentric to Observator local coordinate
     */
    private Matrix4d matHelioToLocal = new Matrix4d();

    /**
     * Transform from Observator local coordinate to Heliocentric
     */
    private Matrix4d matLocalToHelio = new Matrix4d();

    /**
     * Transform from Observator local coordinate to Earth Equatorial
     */
    private Matrix4d matLocalToEarthEqu = new Matrix4d();

    /**
     * Transform from Observator local coordinate to Earth Equatorial
     */
    private Matrix4d matEarthEquToLocal = new Matrix4d();

    /**
     * Transform from Heliocentric to earth equatorial coordinate
     */
    private Matrix4d matHelioToEarthEqu = new Matrix4d();

    private Matrix4d matEarthEquToJ2000 = new Matrix4d();

    private Matrix4d matJ2000ToEarthEqu = new Matrix4d();

    /**
     * Modelview matrix for observer local drawing
     */
    private Matrix4d matLocalToEye = new Matrix4d();

    /**
     * Modelview matrix for geocentric equatorial drawing
     */
    private Matrix4d matEarthEquToEye = new Matrix4d();

    /**
     * precessed version
     */
    private Matrix4d matJ2000ToEye = new Matrix4d();

    /**
     * Modelview matrix for heliocentric equatorial drawing
     */
    private Matrix4d matHelioToEye = new Matrix4d();

    // Vision variables
    /**
     * Viewing direction in local and equatorial coordinates
     */
    private Point3d localVision = new Point3d();

    private Point3d equVision = new Point3d();

    private Point3d precEquVision = new Point3d();

    /**
     * Define if the selected object is followed
     */
    private boolean tracking;

    /**
     * Define if the equatorial position is locked
     */
    private boolean lockEquPos;

    // Automove
    /**
     * Current auto movement
     */
    private AutoMove move = new AutoMove();

    /**
     * Define if automove is on or off
     */
    private boolean autoMove;

    /**
     * @see #zoomingMode
     */
    public static enum ZoomingMode {
        NOT_ZOOMING, ZOOMING, UNZOOMING
    }

    /**
     * @see ZoomingMode
     */
    private ZoomingMode zoomingMode;

    // Time variable
    /**
     * Positive : forward, Negative : Backward, 1 = 1sec/sec
     */
    private double timeSpeed;

    /**
     * Curent time in Julian day
     */
    private double julianDay;

    /**
     * Position variables
     */
    private Observator position;

    /**
     * defines if view corrects for horizon, or uses equatorial coordinates
     */
    private VIEWING_MODE_TYPE viewingMode;
}