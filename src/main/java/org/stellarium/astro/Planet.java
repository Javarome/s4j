/*
* Stellarium
* Copyright (C) 2002 Fabien Chereau
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
package org.stellarium.astro;

import org.stellarium.*;
import org.stellarium.astro.planet.OsculatingFunc;
import org.stellarium.astro.planet.PosFunc;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.projector.Projector;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.StrictMath.*;
import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * A planet.
 *
 * @author <a href="mailto:javarome@javarome.net">Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 */
public class Planet extends StelObjectBase {
    public static final double J2000 = 2451545.0;

    public static final int ORBIT_SEGMENTS = 72;

    public static final String EARTH_NAME = "Earth";

    Planet(Planet parent, String _englishName, boolean _flagHalo, boolean _flagLighting, double _radius, double oblateness, Color someColor, double _albedo, String texMapName, String texHaloName, PosFunc someCoordFunc, OsculatingFunc osulatingFunc, boolean hidden, Logger parentLogger) throws StellariumException {
        super(parentLogger);
        this.englishName = _englishName;
        this.flagHalo = _flagHalo;
        this.flagLighting = _flagLighting;
        this.radius = _radius;
        this.oneMinusOblateness = 1.0 - oblateness;
        this.color = someColor;
        this.albedo = _albedo;
        this.sphereScale = 1.d;
        this.lastJD = J2000;
        this.deltaJD = NavigatorIfc.JD_SECOND;
        this.orbitCached = false;
        this.coordFunc = someCoordFunc;
        this.osculatingFunc = osulatingFunc;
        this.parent = parent;
        this.hidden = hidden;

        if (parent != null) {
            parent.satellites.add(this);
        }
        eclipticPos = new Point3d(0, 0, 0);
        rotLocalToParent = new Matrix4d();
        rotLocalToParent.setIdentity();
        matLocalToParent = new Matrix4d();
        matLocalToParent.setIdentity();
        if (pointerPlanet == null) {
            pointerPlanet = textureFactory.createTexture("pointeur4.png");
        }
        texMap = textureFactory.createTexture(texMapName, STexture.TEX_LOAD_TYPE_PNG_SOLID_REPEAT);
        if (flagHalo) {
            texHalo = textureFactory.createTexture(texHaloName);
        }

        // 60 day trails
        deltaTrail = 1;
        // small increment like 0.125 would allow observation of latitude related wobble of moon
        // if decide to show moon trail
        maxTrail = 60;
        lastTrailJD = 0;// for now
        trailOn = false;
        firstPoint = true;

        nameI18n = englishName;
    }

    /**
     * @param nav
     * @return the information String "ready to print" :)
     */
    public String getInfoString(NavigatorIfc nav) {
        StringBuffer oss = new StringBuffer(englishName);

        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        if (sphereScale != 1d) {
            oss.append(format.format(sphereScale));
        }
        oss.append('\n');

        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        oss.append("Magnitude : ").append(format.format(computeMagnitude(nav.getObserverHelioPos()))).append('\n');

        Point3d equPos = getEarthEquPos(nav);
        StelUtility.Coords coords = StelUtility.rectToSphe(equPos);

        oss.append("RA/DE: ").append(StelUtility.printAngleHms(coords.getRA()));
        oss.append("/").append(StelUtility.printAngleDms(coords.getDE())).append('\n');

        // calculate alt az
        Point3d localPos = nav.earthEquToLocal(equPos);
        coords = StelUtility.rectToSphe(localPos);
        coords.northToZero();
        oss.append("Az/Alt: ").append(StelUtility.printAngleDms(coords.getRA()));
        oss.append("/").append(StelUtility.printAngleDms(coords.getDE())).append('\n');

        format.setMinimumFractionDigits(8);
        format.setMaximumFractionDigits(8);
        oss.append("Distance : ").append(format.format(StelUtility.getLength(equPos))).append("AU").append('\n');

        return oss.toString();
    }

    /**
     * Get sky label (sky translation)
     *
     * @param nav
     * @return
     */
    String getSkyLabel(final NavigatorIfc nav) {
        StringBuffer oss = new StringBuffer(nameI18n);
        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        if (sphereScale != 1.d) {
            oss.append(" (x").append(format.format(sphereScale)).append(")");
        }
        return oss.toString();
    }

    /**
     * @return The information String "ready to print" :)
     */
    public String getShortInfoString(NavigatorIfc nav) {
        StringBuffer oss = new StringBuffer(nameI18n);
        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        if (sphereScale != 1.d) {
            oss.append(" (x").append(format.format(sphereScale)).append(")");
        }

        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        oss.append("  Magnitude : ").append(format.format(computeMagnitude(nav.getObserverHelioPos())));

        Point3d equPos = getEarthEquPos(nav);
        format.setMinimumFractionDigits(8);
        format.setMaximumFractionDigits(8);
        oss.append("  Distance : ").append(format.format(StelUtility.getLength(equPos))).append("AU");

        return oss.toString();
    }

    public double getCloseFOV(NavigatorIfc nav) {
        return atan(radius * sphereScale * 2.d / getLengthEarthEquPos(nav)) * 180 * 4 / PI;
    }

    private double getLengthEarthEquPos(NavigatorIfc nav) {
        return StelUtility.getLength(getEarthEquPos(nav));
    }

    public double getSatellitesFOV(NavigatorIfc nav) {
        double length = getLengthEarthEquPos(nav);
        // TODO: calculate from satellite orbits rather than hard code
        if ("Jupiter".equals(englishName)) {
            return atan(0.005 / length) * 180 * 4.0d / PI;
        }
        if ("Saturn".equals(englishName)) {
            return atan(0.005 / length) * 180 * 4.0d / PI;
        }
        if ("Mars".equals(englishName)) {
            return atan(0.0001 / length) * 180 * 4.0d / PI;
        }
        if ("Uranus".equals(englishName)) {
            return atan(0.002 / length) * 180 * 4.0d / PI;
        }
        return -1;
    }

    public double getParentSatellitesFOV(NavigatorIfc nav) {
        return parent != null && parent.parent != null ? parent.getSatellitesFOV(nav) : -1.0;
    }

    private static STexture pointerPlanet;

    protected void drawPointerTexture(Navigator nav, DefaultProjector prj, long localTime, Point3d screenPos) {
        glColor3f(1.0f, 0.3f, 0.3f);
        drawPointerTexture1(nav, prj, localTime, screenPos);
    }

    public STexture getPointer() {
        return pointerPlanet;
    }

    /**
     * Set the orbital elements
     *
     * @param _period
     * @param _offset
     * @param _epoch
     * @param _obliquity
     * @param _ascendingNode
     * @param _precessionRate
     * @param _sideralPeriod
     */
    void setRotationElements(double _period, double _offset, double _epoch, double _obliquity, double _ascendingNode, double _precessionRate, double _sideralPeriod) {
        re.period = _period;
        re.offset = _offset;
        re.epoch = _epoch;
        re.obliquity = _obliquity;
        re.ascendingNode = _ascendingNode;
        re.precessionRate = _precessionRate;
        re.siderealPeriod = _sideralPeriod;// used for drawing orbit lines

        deltaOrbitJD = re.siderealPeriod / ORBIT_SEGMENTS;
    }

    public double getRotAscendingnode() {
        return re.ascendingNode;
    }

    public double getRotObliquity() {
        return re.obliquity;
    }

    /**
     * Return the planet position in rectangular earth equatorial coordinate
     */
    public Point3d getEarthEquPos(NavigatorIfc nav) {
        Point3d v = getHeliocentricEclipticPos();
        return nav.helioToEarthPosEqu(v);// this is earth equatorial but centered
        // on observer's position (latitude, longitude)
        //return navigation.helio_to_earth_equ(&v); this is the real equatorial centered on earth center
    }

    public Point3d getObsJ2000Pos(NavigatorIfc nav) {
        Vector3d result = new Vector3d(getHeliocentricEclipticPos());
        Point3d obsHelioPos = nav.getObserverHelioPos();
        obsHelioPos.negate();
        result.add(obsHelioPos);
        nav.getMatVsop87ToJ2000().transform(result);
        return new Point3d(result);
    }


    // Compute the position in the parent Planet coordinate system
    // Actually call the provided function to compute the ecliptical position
    void computePositionWithoutOrbits(double date) {
        if (abs(lastJD - date) > deltaJD) {
            coordFunc.compute(date, eclipticPos);
            lastJD = date;
        }
    }

    /**
     * Compute the position in the parent planet coordinate system
     * Actually call the provided function to compute the ecliptical position
     *
     * @param date
     */
    void computePosition(double date) {
        if (deltaOrbitJD > 0 && (abs(lastOrbitJD - date) > deltaOrbitJD || !orbitCached)) {
            // calculate orbit first (for line drawing)
            double dateIncrement = re.siderealPeriod / ORBIT_SEGMENTS;
            double calcDate;
            //	  int deltaPoints = (int)(0.5 + (date - last_orbitJD)/dateIncrement);
            int deltaPoints;

            if (date > lastOrbitJD) {
                deltaPoints = (int) (0.5 + (date - lastOrbitJD) / dateIncrement);
            } else {
                deltaPoints = (int) (-0.5 + (date - lastOrbitJD) / dateIncrement);
            }
            double newDate = lastOrbitJD + deltaPoints * dateIncrement;

            //	  printf( "Updating orbit coordinates for %s (delta %f) (%d points)\n", name.c_str(), delta_orbitJD, deltaPoints);


            if (deltaPoints > 0 && deltaPoints < ORBIT_SEGMENTS && orbitCached) {

                for (int d = 0; d < ORBIT_SEGMENTS; d++) {
                    if (d + deltaPoints >= ORBIT_SEGMENTS) {
                        // calculate new points
                        calcDate = newDate + (d - ORBIT_SEGMENTS / 2) * dateIncrement;
                        // date increments between points will not be completely constant though

                        computeTransMatrix(calcDate);
                        if (osculatingFunc != null) {
                            osculatingFunc.compute(date, calcDate, eclipticPos);
                        } else {
                            coordFunc.compute(calcDate, eclipticPos);
                        }
                        orbit[d] = getHeliocentricEclipticPos();
                    } else {
                        orbit[d] = orbit[d + deltaPoints];
                    }
                }

                lastOrbitJD = newDate;

            } else if (deltaPoints < 0 && abs(deltaPoints) < ORBIT_SEGMENTS && orbitCached) {
                for (int d = ORBIT_SEGMENTS - 1; d >= 0; d--) {
                    if (d + deltaPoints < 0) {
                        // calculate new points
                        calcDate = newDate + (d - ORBIT_SEGMENTS / 2) * dateIncrement;

                        computeTransMatrix(calcDate);
                        if (osculatingFunc != null) {
                            osculatingFunc.compute(date, calcDate, eclipticPos);
                        } else {
                            coordFunc.compute(calcDate, eclipticPos);
                        }
                        orbit[d] = getHeliocentricEclipticPos();
                    } else {
                        orbit[d] = orbit[d + deltaPoints];
                    }
                }

                lastOrbitJD = newDate;

            } else if (deltaPoints != 0 || !orbitCached) {
                // update all points (less efficient)
                for (int d = 0; d < ORBIT_SEGMENTS; d++) {
                    calcDate = date + (d - ORBIT_SEGMENTS / 2) * dateIncrement;
                    computeTransMatrix(calcDate);
                    if (osculatingFunc != null) {
                        osculatingFunc.compute(date, calcDate, eclipticPos);
                    } else {
                        coordFunc.compute(calcDate, eclipticPos);
                    }
                    orbit[d] = getHeliocentricEclipticPos();
                }

                lastOrbitJD = date;
                if (osculatingFunc == null) {
                    orbitCached = true;
                }
            }

            // calculate actual planet position
            coordFunc.compute(date, eclipticPos);

            lastJD = date;

        } else if (abs(lastJD - date) > deltaJD) {
            // calculate actual planet position
            coordFunc.compute(date, eclipticPos);
            lastJD = date;
        }
    }

    /**
     * Compute the transformation matrix from the local planet coordinate to the parent planet coordinate
     *
     * @param jd
     */
    void computeTransMatrix(double jd) {
        axisRotation = getSiderealTime(jd);

        // Special case - heliocentric coordinates are on ecliptic,
        // not solar equator...
        Matrix4d tmp = new Matrix4d();
        if (parent != null) {
            rotLocalToParent = new Matrix4d();
            rotLocalToParent.rotZ(re.ascendingNode - re.precessionRate * (jd - re.epoch));
            tmp.rotX(re.obliquity);
            rotLocalToParent.mul(tmp);
        }

        tmp.setIdentity();
        tmp.setTranslation(new Vector3d(eclipticPos));
        matLocalToParent.mul(tmp, rotLocalToParent);
    }

    public Matrix4d getRotEquatorialToVsop87() {
        Matrix4d rval = new Matrix4d(rotLocalToParent);
        if (parent != null) {
            for (Planet p = parent; p.parent != null; p = p.parent) {
                rval.mul(p.rotLocalToParent, rval);
            }
        }
        return rval;
    }

    /**
     * @return A matrix which converts from heliocentric ecliptic coordinate to local geographic coordinate
     */
    /*
    Matrix4d getHelioToGeoMatrix() {
        Matrix4d mat = matLocalToParent;
        Matrix4d subMat = new Matrix4d();
        subMat.rotZ(axisRotation * PI / 180);
        mat.mul(subMat);

        // Iterate thru parents
        Planet p = parent;
        while (p != null && p.parent != null) {
            mat.mul(p.matLocalToParent);
            p = p.parent;
        }
        return mat;
    }
    */

    /**
     * Compute the z rotation to use from equatorial to geographic coordinates
     *
     * @param jd The Julian Day
     */
    public double getSiderealTime(double jd) {
        if (englishName.equals(EARTH_NAME)) {
            return SideralTime.getApparentSiderealTime(jd);
        }

        double t = jd - re.epoch;
        double rotations = t / re.period;
        double wholeRotations = floor(rotations);
        double remainder = rotations - wholeRotations;

        return remainder * 360 + re.offset;
    }

    /**
     * @return The planet position in the parent planet ecliptic coordinate
     */
    public Point3d getEclipticPos() {
        return eclipticPos;
    }

    /**
     * used only for earth shadow, lunar eclipse
     *
     * @return The heliocentric ecliptical position
     */
    public Point3d getHeliocentricEclipticPos() {
        Point3d pos = new Point3d(eclipticPos);
        Planet p = parent;
        while (p != null && p.parent != null) {
            //pos.transfo4d(p.matLocalToParent);
            pos.add(p.eclipticPos);
            p = p.parent;
            if (p.parent != null) {
                throw new StellariumException("a satellite has no satellites");
            }
        }
        return pos;
    }

    /**
     * @param obsHelioPos
     * @return the distance to the given position in heliocentric coordinate (in AU)
     */
    double computeDistance(Point3d obsHelioPos) {
        distance = obsHelioPos.distance(getHeliocentricEclipticPos());
        return distance;
    }

    /**
     * @param obsPos
     * @return the phase angle for an observer at pos obsPos in the heliocentric coordinate (dist in AU)
     */
    public double getPhase(Point3d obsPos) {
        final double sq = StelUtility.getLengthSquared(obsPos);
        final Point3d heliopos = getHeliocentricEclipticPos();
        final double Rq = StelUtility.getLengthSquared(heliopos);
        final double pq = obsPos.distanceSquared(heliopos);
        final double cos_chi = (pq + Rq - sq) / (2.0 * sqrt(pq * Rq));
        return (1.0 - acos(cos_chi) / PI) * cos_chi
                + sqrt(1.0 - cos_chi * cos_chi) / PI;
    }

    double computeMagnitude(Point3d obsPos) {
        final double sq = StelUtility.getLengthSquared(obsPos);
        if (parent == null) {
            // sun
            return -26.73d + (2.5d * log10(sq));
        }
        final Point3d heliopos = getHeliocentricEclipticPos();
        final double Rq = StelUtility.getLengthSquared(heliopos);
        final double pq = obsPos.distanceSquared(heliopos);
        final double cos_chi = (pq + Rq - sq) / (2.0 * sqrt(pq * Rq));
        final double phase = (1.0 - acos(cos_chi) / PI) * cos_chi
                + sqrt(1.0 - cos_chi * cos_chi) / PI;

        final double F = (2.0 * albedo * radius * radius * phase) / (3.0 * pq * Rq);
        final double rval = -26.73f - (2.5f * log10(F));
        //cout << "Planet(" << getEnglishName()
        //     << ")::compute_magnitude(" << obs_pos << "): "
        //        "phase: " << phase
        //     << ",F: " << F
        //     << ",rval: " << rval
        //     << endl;
        return rval;
    }

    double computeMagnitude(NavigatorIfc nav) {
        return computeMagnitude(nav.getObserverHelioPos());
    }

    void setBigHalo(String halotexfile) throws StellariumException {
        texBigHalo = textureFactory.createTexture(halotexfile, STexture.TEX_LOAD_TYPE_PNG_SOLID);
    }

    /**
     * Return the radius of a circle containing the object on screen
     */
    public double getOnScreenSize(Projector prj, NavigatorIfc nav) {
        double rad = radius;
        if (rings != null) {
            rad = rings.getSize();
        }
        return (int) (atan(rad * sphereScale * 2.d / getLengthEarthEquPos(nav)) * (180d / PI) * (prj.getViewportHeight() / prj.getFieldOfView()));
    }

    /**
     * Draw the planet and all the related infos : name, circle etc..
     *
     * @param prj
     * @param nav
     * @param eye
     * @param flagPoint
     * @param stencil
     */
    void draw(DefaultProjector prj, NavigatorIfc nav, ToneReproductor eye, boolean flagPoint, boolean stencil) {
        if (hidden) {
            return;
        }

        Matrix4d mat = new Matrix4d(matLocalToParent);
        Matrix4d tmp = new Matrix4d();
        Planet p = parent;
        while (p != null && p.parent != null) {
            tmp.setIdentity();
            tmp.setTranslation(new Vector3d(p.eclipticPos));
            tmp.mul(mat);
            mat.mul(tmp, p.rotLocalToParent);
            p = p.parent;
        }

        // This removed totally the planet shaking bug!!!
        mat.mul(nav.getHelioToEyeMat(), mat);

        if (this == nav.getHomePlanet()) {
            if (rings != null) {
                rings.draw(prj, mat, 1000.0);
            }
            return;
        }

        // Compute the 2D position and check if in the screen
        double screenSz = getOnScreenSize(prj, nav);
        double viewportLeft = prj.getViewportPosX();
        double viewportBottom = prj.getViewportPosY();
        if (prj.projectCustom(new Point3d(0, 0, 0), screenPos, mat) &&
                screenPos.y > viewportBottom - screenSz && screenPos.y < viewportBottom + prj.getViewportHeight() + screenSz &&
                screenPos.x > viewportLeft - screenSz && screenPos.x < viewportLeft + prj.getViewportWidth() + screenSz) {
            // Draw the name, and the circle if it's not too close from the body it's turning around
            // this prevents name overlaping (ie for jupiter satellites)
            double angDist = 300.d * atan(StelUtility.getLength(getEclipticPos()) / getLengthEarthEquPos(nav)) / prj.getFieldOfView();
            if (angDist == 0) {
                angDist = 1;// if ang_dist == 0, the planet is sun..
            }

            // by putting here, only draw orbit if planet is visible for clarity
            drawOrbit(nav, prj);

            drawTrail(nav, prj);

            if (angDist > 0.25) {
                if (angDist > 1) {
                    angDist = 1;
                }
                //glColor4f(0.5f*ang_dist,0.5f*ang_dist,0.7f*ang_dist,1.f*ang_dist);
                drawHints(nav, prj);
            }
            if (rings != null && screenSz > 1) {
                double dist = getLengthEarthEquPos(nav);
                double[] clippingPlanes = prj.getClippingPlanes();// Copy clipping planes
                double n = clippingPlanes[0];
                double f = clippingPlanes[1];
                // If zNear is too big, then Saturn and the rings are clipped
                // in perspective projection
                // when near the edge of the screen (home_planet=Hyperion).
                // If zNear is too small, the depth test does not work properly
                // when seen from great distance.
                double zNear = dist - (rings.getSize() * 2);
                if (zNear < 0.001) zNear = 0.0000001;
                else if (zNear < 0.05) zNear *= 0.1;
                else if (zNear < 0.5) zNear *= 0.5;
                prj.setClippingPlanes(zNear, dist + (rings.getSize() * 10));
                glClear(GL_DEPTH_BUFFER_BIT);
                glEnable(GL_DEPTH_TEST);
                drawSphere(prj, mat, screenSz);
                rings.draw(prj, mat, screenSz);
                glDisable(GL_DEPTH_TEST);
                prj.setClippingPlanes(n, f);// Release old clipping planes
            } else {
                if (stencil) glEnable(GL_STENCIL_TEST);
                drawSphere(prj, mat, screenSz);
                if (stencil) glDisable(GL_STENCIL_TEST);
            }
            if (texHalo != null) {
                if (flagPoint) {
                    drawPointHalo(nav, prj, eye);
                } else {
                    drawHalo(nav, prj, eye);
                }
            }
            if (texBigHalo != null) {
                drawBigHalo(nav, prj, eye);
            }
        }
        previousScreenPos.set(screenPos);
    }

    /**
     * Draw the circle and name of the planet
     *
     * @param nav
     * @param prj
     */
    void drawHints(NavigatorIfc nav, DefaultProjector prj) {
        if (!hintFader.hasInterstate())
            return;

        prj.setOrthographicProjection();// 2D coordinate

        glEnable(GL_BLEND);
        glDisable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);

        double tmp = 10.d + getOnScreenSize(prj, nav) / (2.d * sphereScale);// Shift for nameI18n printing

        float interstate = hintFader.getInterstate();
        glColor4f(labelColor.getRed(), labelColor.getGreen(), labelColor.getBlue(), interstate);
        if (prj.isGravityLabelsEnabled()) {
            prj.printGravity180(planetNameFontIfc, (float) screenPos.x, (float) screenPos.y, getSkyLabel(nav), true,
                    (float) tmp, (float) tmp);
        } else {
            planetNameFontIfc.print((int) Math.floor(screenPos.x + tmp), (int) Math.floor(screenPos.y + tmp), getSkyLabel(nav), true);
        }

        // hint disapears smoothly on close view
        tmp -= 10.f;
        if (tmp < 1) tmp = 1;
        glColor4f(labelColor.getRed(), labelColor.getGreen(), labelColor.getBlue(), interstate / (float) tmp);

        // Draw the 2D small circle
        glCircle(screenPos, 8);
        prj.resetPerspectiveProjection();// Restore the other coordinate
    }

    void drawSphere(DefaultProjector prj, Matrix4d mat, double screenSz) {
        // Adapt the number of facets according with the size of the sphere for optimization
        int nbFacet = (int) (screenSz * 40 / 50);// 40 facets for 1024 pixels diameter on screen
        if (nbFacet < 10) {
            nbFacet = 10;
        }
        if (nbFacet > 40) {
            nbFacet = 40;
        }
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        if (flagLighting) {
            glEnable(GL_LIGHTING);
        } else {
            glDisable(GL_LIGHTING);
            glColor3fv(color.getComponents(null), 0);
        }
        glBindTexture(GL_TEXTURE_2D, texMap.getID());

        // Rotate and add an extra quarter rotation so that the planet texture map
        // fits to the observers position. No idea why this is necessary,
        // perhaps some openGl strangeness, or confusing sin/cos.
        Matrix4d rotMat = new Matrix4d();
        rotMat.rotZ((PI / 180d) * (axisRotation + 90.));
        rotMat.mul(mat, rotMat);
        prj.sSphere(radius * sphereScale, oneMinusOblateness, nbFacet, nbFacet, rotMat);

        glDisable(GL_CULL_FACE);
        glDisable(GL_LIGHTING);
    }

    /**
     * Draw the small star-like 2D halo
     *
     * @param nav
     * @param prj
     * @param eye
     */
    protected void drawHalo(NavigatorIfc nav, DefaultProjector prj, ToneReproductor eye) {
        double rmag = eye.adaptLuminance(
                exp(-0.92103d * (computeMagnitude(nav.getObserverHelioPos()) + 12.12331d))
                        * 108064.73d);
        rmag = rmag / (50.d * pow(prj.getFieldOfView(), 0.85d));

        double cmag = 1;

        // if size of star is too small (blink) we put its size to 1.2 --> no more blink
        // And we compensate the difference of brighteness with cmag
        if (rmag < 1.2) {
            if (rmag < 0.3) {
                return;
            }
            cmag = rmag * rmag / 1.44f;
            rmag = 1.2f;
        } else {
            if (rmag > 5) {
                rmag = (float) (5.f + sqrt(rmag - 5) / 6);
                if (rmag > 9) {
                    rmag = 9;
                }
            }
        }

        // Global scaling
        rmag *= objectScale;

        glBlendFunc(GL_ONE, GL_ONE);
        double screenR = getOnScreenSize(prj, nav);
        cmag *= 0.5 * rmag / screenR;
        if (cmag > 1) {
            cmag = 1;
        }

        if (rmag < screenR) {
            cmag *= rmag / screenR;
            rmag = screenR;
        }

        prj.setOrthographicProjection();// 2D coordinate

        glBindTexture(GL_TEXTURE_2D, texHalo.getID());
        glEnable(GL_BLEND);
        glDisable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glColor3d(color.getRed() * cmag, color.getGreen() * cmag, color.getBlue() * cmag);
        glTranslated(screenPos.x, screenPos.y, 0);
        texHalo.displayTexture(-rmag, -rmag, 2 * rmag, 2 * rmag);

        prj.resetPerspectiveProjection();// Restore the other coordinate
    }

    /**
     * Draw the small star-like point
     *
     * @param nav
     * @param prj
     * @param eye
     */
    protected void drawPointHalo(NavigatorIfc nav, DefaultProjector prj, ToneReproductor eye) {
        double rmag = eye.adaptLuminance(
                exp(-0.92103d * (computeMagnitude(nav.getObserverHelioPos()) + 12.12331d))
                        * 108064.73d);
        rmag = rmag / (10d * pow(prj.getFieldOfView(), 0.85d));

        double cmag = 1.d;

        // if size of star is too small (blink) we put its size to 1.2 --> no more blink
        // And we compensate the difference of brighteness with cmag
        if (rmag < 0.3d) {
            return;
        }
        cmag = rmag * rmag / (1.4d * 1.4d);
        rmag = 1.4d;

        // Global scaling
        //rmag*=star_scale;

        glBlendFunc(GL_ONE, GL_ONE);
        double screenR = getOnScreenSize(prj, nav);
        cmag *= rmag / screenR;
        if (cmag > 1.f) cmag = 1.f;

        if (rmag < screenR) {
            cmag *= rmag / screenR;
            rmag = screenR;
        }
        prj.setOrthographicProjection();// 2D coordinate

        glBindTexture(GL_TEXTURE_2D, texHalo.getID());
        glEnable(GL_BLEND);
        glDisable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glColor3d(color.getRed() * cmag, color.getGreen() * cmag, color.getBlue() * cmag);
        glTranslated(screenPos.x, screenPos.y, 0);
        texHalo.displayTexture(-rmag, -rmag, 2 * rmag, 2 * rmag);

        prj.resetPerspectiveProjection();// Restore the other coordinate
    }

    protected void drawBigHalo(NavigatorIfc nav, DefaultProjector prj, ToneReproductor eye) {
        glBlendFunc(GL_ONE, GL_ONE);
        double screen_r = getOnScreenSize(prj, nav);

        double rmag = bigHaloSize / 2d;

        double cmag = rmag / screen_r;
        if (cmag > 1.d) cmag = 1.d;

        if (rmag < screen_r * 2d) {
            cmag *= rmag / (screen_r * 2d);
            rmag = screen_r * 2d;
        }

        prj.setOrthographicProjection();// 2D coordinate

        glBindTexture(GL_TEXTURE_2D, texBigHalo.getID());
        glEnable(GL_BLEND);
        glDisable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glColor3d(color.getRed() * cmag, color.getGreen() * cmag, color.getBlue() * cmag);
        glTranslated(screenPos.x, screenPos.y, 0);
        texBigHalo.displayTexture(-rmag, -rmag, 2 * rmag, 2 * rmag);

        prj.resetPerspectiveProjection();// Restore the other coordinate
    }

    /**
     * draw orbital path of planet
     *
     * @param nav
     * @param prj
     */
    public void drawOrbit(NavigatorIfc nav, DefaultProjector prj) {
        if (!orbitFader.hasInterstate())
            return;

        Vector3d onScreen = new Vector3d();

        if (re.siderealPeriod == 0) {
            return;
        }

        prj.setOrthographicProjection();// 2D coordinate

        // Normal transparency mode
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glEnable(GL_BLEND);
        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);

        glColor4f(orbitColor.getRed(), orbitColor.getGreen(), orbitColor.getBlue(), orbitFader.getInterstate());

        boolean on = false;
        int d;
        for (int n = 0; n <= ORBIT_SEGMENTS; n++) {
            if (n == ORBIT_SEGMENTS) {
                d = 0;// connect loop
            } else {
                d = n;
            }

            // special case - use current planet position as center vertex so that draws
            // on it's orbit all the time (since segmented rather than smooth curve)
            if (n == ORBIT_SEGMENTS / 2) {
                if (prj.projectHelio(getHeliocentricEclipticPos(), onScreen)) {
                    if (!on) {
                        glBegin(GL_LINE_STRIP);
                    }
                    glVertex3d(onScreen.x, onScreen.y, 0);
                    on = true;
                } else if (on) {
                    glEnd();
                    on = false;
                }
            } else {
                if (prj.projectHelio(orbit[d], onScreen)) {
                    if (!on) glBegin(GL_LINE_STRIP);
                    glVertex3d(onScreen.x, onScreen.y, 0);
                    on = true;
                } else if (on) {
                    glEnd();
                    on = false;
                }
            }
        }

        if (on) glEnd();

        prj.resetPerspectiveProjection();// Restore the other coordinate

        glDisable(GL_BLEND);
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
    }

    /**
     * draw trail of planet as seen from earth
     *
     * @param nav
     * @param prj
     */
    void drawTrail(NavigatorIfc nav, DefaultProjector prj) {
        if (trail.isEmpty()) {
            return;
        }

        Point3d onScreen1 = new Point3d();
        Point3d onScreen2 = new Point3d();

        //  if(!re.sidereal_period) return;   // limits to planets

        prj.setOrthographicProjection();// 2D coordinate

        glEnable(GL_BLEND);
        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);

        glColor3fv(trailColor.getColorComponents(null), 0);

        int iter;
        int nextiter;

        // TODO: Find out why the list is navigated from last to first ?
        if (trail.size() > 1) {
            nextiter = trail.size() - 1;
            nextiter--;

            for (iter = nextiter; iter != 0; iter--) {
                nextiter--;

                if (prj.projectEarthEquLineCheck(trail.get(iter).point, onScreen1, trail.get(nextiter).point, onScreen2)) {
                    glBegin(GL_LINE_STRIP);
                    glVertex3d(onScreen1.x, onScreen1.y, 0);
                    glVertex3d(onScreen2.x, onScreen2.y, 0);
                    glEnd();
                }
            }
        }

        // draw final segment to finish at current planet position
        if (!firstPoint && prj.projectEarthEquLineCheck((trail.getFirst()).point, onScreen1, getEarthEquPos(nav), onScreen2)) {
            glBegin(GL_LINE_STRIP);
            glVertex3d(onScreen1.x, onScreen1.y, 0);
            glVertex3d(onScreen2.x, onScreen2.y, 0);
            glEnd();
        }

        prj.resetPerspectiveProjection();// Restore the other coordinate

        glDisable(GL_BLEND);
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
    }

    /**
     * update trail points as needed
     *
     * @param nav
     */
    void updateTrail(NavigatorIfc nav) {
        if (!trailOn) {
            return;
        }
        double date = nav.getJulianDay();

        boolean dt;
        double v1 = abs((date - lastTrailJD) / deltaTrail);
        if (firstPoint || (dt = v1 > maxTrail)) {
            dt = true;
            // clear old trail
            trail.clear();
            firstPoint = false;
        }

        // Note that when jump by a week or day at a time, loose detail on trails
        // particularly for moon (if decide to show moon trail)

        // add only one point at a time, using current position only
        if (dt) {
            lastTrailJD = date;
            TrailPoint tp = new TrailPoint();
            Point3d v = getHeliocentricEclipticPos();
            //      trail.add(0, nav->helio_to_earth_equ(v) );  // centered on earth
            tp.point = nav.helioToEarthPosEqu(v);
            tp.date = date;
            trail.addFirst(tp);

            //      if( trail.size() > (unsigned int)maxTrail ) {
            if (trail.size() > maxTrail) {
                trail.removeLast();
            }
        }

        // because sampling depends on speed and frame rate, need to clear out
        // points if trail gets longer than desired

        int index = 0;
        for (TrailPoint iter : trail) {
            if (abs(iter.date - date) / deltaTrail > maxTrail) {
                // TODO: Find a way in java to cut a linked list ?
                trail = new LinkedList<TrailPoint>(trail.subList(0, index));
                break;
            }
            index++;
        }
    }

    /**
     * start/stop accumulating new trail data (clear old data)
     */
    void startTrail(boolean b) {
        if (b) {
            firstPoint = true;
            //  printf("trail for %s: %f\n", name.c_str(), re.sidereal_period);
            // only interested in trails for planets
            if (re.siderealPeriod > 0) {
                trailOn = true;
            }
        } else {
            trailOn = false;
        }
    }

    public void update(long delta_time) {
        //        hintFader.update(delta_time);
        //        orbitFader.update(delta_time);
        //        trailFader.update(delta_time);
    }

    void setFlagHints(boolean b) {
        hintFader.set(b);
    }

    boolean getFlagHints() {
        return hintFader.getState();
    }

    void setFlagOrbits(boolean b) {
        orbitFader.set(b);
    }

    boolean getFlagOrbits() {
        return orbitFader.getState();
    }

    public void setFlagTrail(boolean b) {
        if (b == trailFader.getState())
            return;
        trailFader.set(b);
        startTrail(b);
    }

    boolean getFlagTrail() {
        return trailFader.getState();
    }

    static void setflagShow(boolean b) {
        flagShow.set(b);
    }

    static boolean getflagShow() {
        return flagShow.getState();
    }

    public void translateName(Translator trans) {
        nameI18n = trans.translate(englishName);
    }

    class TrailPoint {
        Point3d point;

        double date;
    }

    /**
     * Class used to store orbital elements
     */
    class RotationElements {
        public RotationElements() {
            period = 1.;
            offset = 0.;
            epoch = J2000;
            obliquity = 0.;
            ascendingNode = 0.;
            precessionRate = 0.;
        }

        /**
         * rotation period
         */
        double period;

        /**
         * rotation at epoch
         */
        double offset;

        double epoch;

        /**
         * tilt of rotation axis w.r.t. ecliptic
         */
        double obliquity;

        /**
         * long. of ascending node of equator on the ecliptic
         */
        double ascendingNode;

        /**
         * rate of precession of rotation axis in rads/day
         */
        double precessionRate;

        /**
         * sidereal period (planet year in earth days)
         */
        double siderealPeriod;
    }

    /**
     * Class to manage rings for planets like saturn
     */
    public static class Ring {
        protected final STextureFactory textureFactory;
        protected final Logger logger;

        public double getSize() {
            return radiusMax;
        }

        private final double radiusMin;

        private final double radiusMax;

        private STexture tex;

        Ring(double radius_min, double radius_max, String _texname, Logger parentLogger) throws StellariumException {
            logger = Logger.getLogger(getClass().getName());
            if (parentLogger != null) {
                logger.setParent(parentLogger);
            }
            textureFactory = new STextureFactory(logger);
            radiusMin = radius_min;
            radiusMax = radius_max;
            tex = textureFactory.createTexture(_texname, STexture.TEX_LOAD_TYPE_PNG_ALPHA);
        }

        public void draw(DefaultProjector prj, Matrix4d mat, double screen_sz) {
            screen_sz -= 50;
            screen_sz /= 250.0;
            if (screen_sz < 0.0) {
                screen_sz = 0.0;
            } else if (screen_sz > 1.0) {
                screen_sz = 1.0;
            }
            final int slices = 128 + (int) ((256 - 128) * screen_sz);
            final int stacks = 8 + (int) ((32 - 8) * screen_sz);

            // Normal transparency mode
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            //glRotatef(axis_rotation + 180.,0.,0.,1.);
            glColor3f(1.0f, 0.88f, 0.82f);// For saturn only..
            glEnable(GL_TEXTURE_2D);
            glDisable(GL_CULL_FACE);
            glEnable(GL_BLEND);

            glBindTexture(GL_TEXTURE_2D, tex.getID());

            // TODO: radial texture would look much better

            // solve the ring wraparound by culling:
            // decide if we are above or below the ring plane
            final double h = mat.m02 * mat.m03
                    + mat.m12 * mat.m13
                    + mat.m22 * mat.m23;
            prj.sRing(radiusMin, radiusMax, (h < 0.0) ? slices : -slices, stacks, mat, false);
            glDisable(GL_CULL_FACE);

            /* Old way
            glPushMatrix();
            double[] doubles = Projector.matrix4dToDoubles(mat);
            glLoadMatrixd(doubles, 0);
            double r = radiusMax;
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            prj.sVertex3(-r, -r, 0., mat);// Bottom left
            glTexCoord2f(1, 0);
            prj.sVertex3(r, -r, 0, mat);// Bottom right
            glTexCoord2f(1, 1);
            prj.sVertex3(r, r, 0, mat);// Top right
            glTexCoord2f(0, 1);
            prj.sVertex3(-r, r, 0, mat);// Top left
            glEnd();
            glPopMatrix();
            */
        }
    }

    public float getMag(NavigatorIfc nav) {
        return (float) computeMagnitude(nav);
    }

    //	void setLabelColor(const Vector3f& v) {label_color = v;}

    // Compute the distance to the given position in heliocentric coordinate (in AU)

    public double getDistance() {
        return distance;
    }

    public StelObject.TYPE getType() {
        return StelObject.TYPE.PLANET;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getNameI18n() {
        return nameI18n;
    }

    void setNameI18n(String cn) {
        nameI18n = cn;
    }

    void setRings(Planet.Ring r) {
        rings = r;
    }

    public void setSphereScale(double s) {
        sphereScale = s;
    }

    public double getSphereScale() {
        return sphereScale;
    }

    Planet getParent() {
        return parent;
    }

    void setHaloSize(double s) {
        bigHaloSize = s;
    }

    static void setFont(SFontIfc f) {
        planetNameFontIfc = f;
    }

    static void setScale(double s) {
        objectScale = s;
    }

    static double getScale() {
        return objectScale;
    }

    static void setLabelColor(Color lc) {
        labelColor = lc;
    }

    public static Color getLabelColor() {
        return labelColor;
    }

    static void setOrbitColor(Color oc) {
        orbitColor = oc;
    }

    public static Color getOrbitColor() {
        return orbitColor;
    }

    public static Color getTrailColor() {
        return trailColor;
    }

    static void setTrailColor(Color _color) {
        trailColor = _color;
    }

    public double getRadius() {
        return radius;
    }

    public STexture getMapTexture() {
        return texMap;
    }

    /**
     * english planet name
     */
    String englishName;

    /**
     * International translated name
     */
    String nameI18n;

    /**
     * Set wether a little "star like" halo will be drawn
     */
    boolean flagHalo;

    /**
     * Set wether light computation has to be proceed
     */
    boolean flagLighting;

    /**
     * Rotation param
     */
    RotationElements re = new RotationElements();

    /**
     * Planet radius in UA
     */
    double radius;

    /**
     * (polar radius)/(equatorial radius)
     */
    double oneMinusOblateness;

    /**
     * store heliocentric coordinates for drawing the orbit
     */
    Point3d orbit[] = new Point3d[ORBIT_SEGMENTS];

    /**
     * Position in UA in the rectangular ecliptic coordinate system
     * centered on the parent planet
     */
    Point3d eclipticPos;

    /**
     * Used to store temporarily the 2D position on screen
     */
    Point3d screenPos = new Point3d();

    /**
     * The position of this planet in the previous frame.
     */
    Point3d previousScreenPos = new Point3d();

    Color color;

    /**
     * Planet albedo
     */
    double albedo;

    Matrix4d rotLocalToParent;

    /**
     * Transfo matrix from local ecliptique to parent ecliptic
     */
    Matrix4d matLocalToParent;

    /**
     * Rotation angle of the planet on it's axis
     */
    double axisRotation;

    /**
     * Planet map texture
     */
    STexture texMap;

    /**
     * Little halo texture
     */
    STexture texHalo;

    /**
     * Big halo texture
     */
    STexture texBigHalo;

    /**
     * Halo size on screen
     */
    double bigHaloSize;

    /**
     * Planet rings
     */
    Ring rings;

    /**
     * Temporary variable used to store the distance to a given point
     * it is used for sorting while drawing
     */
    double distance;

    /**
     * Artificial scaling for better viewing
     */
    double sphereScale;

    double lastJD;

    double lastOrbitJD;

    double deltaJD;

    double deltaOrbitJD;

    /**
     * whether orbit calculations are cached for drawing orbit yet
     */
    boolean orbitCached;

    /**
     * The Callback for the calculation of the equatorial rect heliocentric position at time JD.
     */
    PosFunc coordFunc;

    OsculatingFunc osculatingFunc;

    /**
     * Planet parent i.e. sun for earth
     */
    Planet parent;

    /**
     * satellites of the planet
     */
    List<Planet> satellites = new ArrayList<Planet>();

    static SFontIfc planetNameFontIfc = null;

    static double objectScale = 1.;

    static Color labelColor = new Color(.4f, .4f, .8f);

    static Color orbitColor = new Color(1f, .6f, 1f);

    static Color trailColor = new Color(1f, .7f, .7f);

    LinkedList<TrailPoint> trail = new LinkedList<TrailPoint>();

    /**
     * accumulate trail data if true
     */
    boolean trailOn;

    double deltaTrail;

    int maxTrail;

    double lastTrailJD;

    /**
     * if need to take first point of trail still
     */
    boolean firstPoint;

    static final LinearFader flagShow = new LinearFader();

    LinearFader hintFader = new LinearFader();

    LinearFader orbitFader = new LinearFader();

    LinearFader trailFader = new LinearFader();

    /**
     * useful for fake planets used as observation positions - not drawn or labeled
     */
    boolean hidden;
}