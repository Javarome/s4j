/*
* This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau
* and is a Java version of the original Stellarium C++ version,
* ()
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
package org.stellarium.projector;

import org.stellarium.StellariumException;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.vecmath.Rectangle4i;

import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import java.util.logging.Logger;

import static java.lang.StrictMath.*;
import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * Projector.
 * <p/>
 *
 * @author <a href="mailto:javarome@javarome.net">J&eacute;r&ocirc;me Beau</a>, Fred Simon
 * @version 0.8.2
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/projector.cpp?view=markup&pathrev=stellarium-0-8-2">projector.cpp</a>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/projector.h?view=log&pathrev=stellarium-0-8-2">projector.h</a>
 */
public class DefaultProjector implements Projector {
    private static Logger logger;
    private static Logger parentLogger;

    public enum PROJECTOR_MASK_TYPE {
        DISK("disk"),
        NONE("none");

        private final String desc;

        PROJECTOR_MASK_TYPE(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }

    public static String maskTypeToString(PROJECTOR_MASK_TYPE type) {
        return type.getDesc();
    }

    public static PROJECTOR_MASK_TYPE stringToMaskType(String s) {
        if (PROJECTOR_MASK_TYPE.DISK.getDesc().equals(s)) return PROJECTOR_MASK_TYPE.DISK;
        return PROJECTOR_MASK_TYPE.NONE;
    }

    public static DefaultProjector create(TYPE type,
                                          Rectangle4i viewport,
                                          double fov, Logger parentLogger) throws StellariumException {
        DefaultProjector.parentLogger = parentLogger;
        DefaultProjector rval = null;
        switch (type) {
            case perspective:
                rval = new DefaultProjector(viewport, fov);
                break;
            case fisheye:
                rval = new FisheyeProjector(viewport, fov);
                break;
            case cylinder:
                rval = new CylinderProjector(viewport, fov);
                break;
            case stereographic:
                rval = new StereographicProjector(viewport, fov);
                break;
            case spheric_mirror:
                rval = new SphericMirrorProjector(viewport, fov);
                break;
        }
        if (rval == null) {
            // just shutup the compiler, this point will never be reached
            throw new StellariumException("fatal: Projector::create(" + type + ") failed");
        }
        getLogger().fine("Projector class " + rval.getClass().getName() + " created");
        return rval;
    }

    protected DefaultProjector(Rectangle4i viewport) {
        this(viewport, 60);
    }

    protected DefaultProjector(Rectangle4i viewport, double fieldOfView) {
        maskType = PROJECTOR_MASK_TYPE.NONE;
        this.fieldOfView = 1.0;
        minFov = 0.0001;
        maxFov = 100;
        zNear = 0.1;
        zFar = 10000;
        vecViewport = viewport;
        autoZoom = false;
        gravityLabels = false;
        flipHorz = 1.0;
        flipVert = 1.0;
        setViewport(viewport);
        setFieldOfView(fieldOfView);
    }

    /**
     * Init the viewing matrix, setting the field of view, the clipping planes, and screen ratio
     * The function is a reimplementation of gluPerspective
     */
    protected void initProjectMatrix() {
        double f = 1 / tan(fieldOfView * PI / 360);
        double ratio = (double) getViewportHeight() / getViewportWidth();
        matProjection = new Matrix4d(
                flipHorz * f * ratio, 0, 0, 0,
                0, flipVert * f, 0, 0,
                0, 0, (zFar + zNear) / (zNear - zFar), (2 * zFar * zNear) / (zNear - zFar),
                0, 0, -1, 0);
        glMatrixMode(GL_PROJECTION);
        glLoadMatrixd(matProjection);
        glMatrixMode(GL_MODELVIEW);
    }

    public void setViewport(int x, int y, int w, int h) {
        vecViewport.x = x;
        vecViewport.y = y;
        vecViewport.w = w;
        vecViewport.z = h;
        center = new Point3d((double) vecViewport.x + (vecViewport.w / 2),
                (double) vecViewport.y + (vecViewport.z / 2),
                0.0);
        calculateViewScalingFactor();
        applyViewport();
        initProjectMatrix();
    }

    private void calculateViewScalingFactor() {
        viewScalingFactor = min(getViewportWidth(), getViewportHeight()) * 180.0d / (fieldOfView * PI);
    }

    /**
     * Set the Field of View in degree
     *
     * @param f The new field of view
     */
    public void setFieldOfView(double f) {
        fieldOfView = f;
        if (f > maxFov) fieldOfView = maxFov;
        if (f < minFov) fieldOfView = minFov;
        calculateViewScalingFactor();
        initProjectMatrix();
    }

    /**
     * Set the maximum Field of View in degree
     *
     * @param max The new maximum field of view
     */
    public void setMaxFov(double max) {
        if (fieldOfView > max) setFieldOfView(max);
        maxFov = max;
    }


    /**
     * Fill with black around the circle
     */
    public void drawViewportShape() {
        if (maskType != PROJECTOR_MASK_TYPE.DISK) return;

        glDisable(GL_BLEND);
        glColor3f(0.f, 0.f, 0.f);

        setOrthographicProjection();
        try {
            glTranslatef(getViewportPosX() + getViewportWidth() / 2, getViewportPosY() + getViewportHeight() / 2, 0.f);
            GLUquadric p = gluNewQuadric();
            gluDisk(p, min(getViewportWidth(), getViewportHeight()) / 2, getViewportWidth() + getViewportHeight(), 256, 1);// should always cover whole screen
            gluDeleteQuadric(p);
        } finally {
            resetPerspectiveProjection();
        }
    }

    public void setClippingPlanes(double znear, double zfar) {
        zNear = znear;
        zFar = zfar;
        initProjectMatrix();
    }

    public void changeFov(double deltaFov) {
        // if we are zooming in or out
        if (deltaFov != 0) {
            setFieldOfView(fieldOfView + deltaFov);
        }
    }

    /**
     * Set the standard modelview matrices used for projection
     *
     * @param _matEarthEquToEye
     * @param _matHelioToEye
     * @param _matLocalToEye
     * @param _matJ2000ToEye
     */
    public void setModelviewMatrices(Matrix4d _matEarthEquToEye,
                                     Matrix4d _matHelioToEye,
                                     Matrix4d _matLocalToEye,
                                     Matrix4d _matJ2000ToEye) {
        matEarthEquToEye = new Matrix4d(_matEarthEquToEye);
        matJ2000ToEye = new Matrix4d(_matJ2000ToEye);
        matHelioToEye = new Matrix4d(_matHelioToEye);
        matLocalToEye = new Matrix4d(_matLocalToEye);

        invMatEarthEquToEye = new Matrix4d();
        invMatJ2000ToEye = new Matrix4d();
        invMatHelioToEye = new Matrix4d();
        invMatLocalToEye = new Matrix4d();

        invMatEarthEquToEye.mul(matProjection, matEarthEquToEye);
        invMatEarthEquToEye.invert();
        invMatJ2000ToEye.mul(matProjection, matJ2000ToEye);
        invMatJ2000ToEye.invert();
        invMatHelioToEye.mul(matProjection, matHelioToEye);
        invMatHelioToEye.invert();
        invMatLocalToEye.mul(matProjection, matLocalToEye);
        invMatLocalToEye.invert();

        projectEarthEquFunc = new ProjectEarthEqu();
    }

    /**
     * Update AutoZoom if activated
     *
     * @param deltaTime
     */
    public void updateAutoZoom(long deltaTime) {
        if (autoZoom) {
            // Use a smooth function
            double c;
            if (zoomMove.start > zoomMove.aim) {
                // slow down as approach final view
                c = 1 - (1 - zoomMove.coef) * (1 - zoomMove.coef) * (1 - zoomMove.coef);
            } else {
                // speed up as leave zoom target
                c = zoomMove.coef * zoomMove.coef * zoomMove.coef;
            }

            setFieldOfView(zoomMove.start + (zoomMove.aim - zoomMove.start) * c);
            zoomMove.coef += zoomMove.speed * deltaTime;
            if (zoomMove.coef >= 1.) {
                autoZoom = false;
                setFieldOfView(zoomMove.aim);
            }
        }
        /*
        if (flag_auto_zoom)
        {
            // Use a smooth function
            double smooth = 3.f;
            double c = atan(smooth * 2.*zoom_move.coef-smooth)/Math.atan(smooth)/2+0.5;
            setFov(zoom_move.start + (zoom_move.aim - zoom_move.start) * c);
            zoom_move.coef+=zoom_move.speed*deltaTime;
            if (zoom_move.coef>=1.)
            {
                flag_auto_zoom = 0;
                setFov(zoom_move.aim);
            }
        }*/
    }

    /**
     * Zoom to the given field of view in degree,
     * using the default duration of 1
     *
     * @param aimFov aimed field of view in degree
     */
    void zoomTo(double aimFov) {
        zoomTo(aimFov, 1.);
    }

    /**
     * Zoom to the given field of view in degree
     *
     * @param aimFov       aimed field of view, in degrees.
     * @param moveDuration Duration of zoom, in seconds.
     */
    public void zoomTo(double aimFov, double moveDuration) {
        zoomMove.aim = (float) aimFov;
        zoomMove.start = (float) fieldOfView;
        zoomMove.speed = (float) (1 / (moveDuration * 1000));
        zoomMove.coef = 0;
        autoZoom = true;
    }

    /**
     * Set the drawing mode in 2D for drawing inside the viewport only.
     * Use resetPerspectiveProjection() to reset previous projection mode
     */
    public void setOrthographicProjection() {
        glMatrixMode(GL_PROJECTION);// projection matrix mode
        glPushMatrix();// store previous matrix
        glLoadIdentity();
        gluOrtho2D(vecViewport.x, vecViewport.x + vecViewport.getWidth(),
                vecViewport.y, vecViewport.y + vecViewport.getHeight());// set a 2D orthographic projection
        glMatrixMode(GL_MODELVIEW);// modelview matrix mode
        glPushMatrix();
        glLoadIdentity();
    }

    /**
     * Reset the previous projection mode after a call to setOrthographicProjection()
     */
    public void resetPerspectiveProjection() {
        glMatrixMode(GL_PROJECTION);// Restore previous matrix
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    /**
     * Method by orientInside false by default
     */
    public void sSphere(double radius, double oneMinusOblateness, int slices, int stacks, final Matrix4d mat) {
        sSphere(radius, oneMinusOblateness, slices, stacks, mat, false);
    }

    /**
     * Reimplementation of gluSphere : glu is overrided for non standard projection
     *
     * @param radius
     * @param slices
     * @param stacks
     * @param mat
     * @param orientInside
     */
    public void sSphere(double radius, double oneMinusOblateness,
                        int slices, int stacks,
                        final Matrix4d mat, boolean orientInside) {
        glPushMatrix();
        glLoadMatrixd(mat);

        if (oneMinusOblateness == 1.0) {// gluSphere seems to have hardware acceleration
            GLUquadric p = gluNewQuadric();
            gluQuadricTexture(p, true);
            if (orientInside) {
                gluQuadricOrientation(p, GLU.GLU_INSIDE);
            }
            gluSphere(p, radius, slices, stacks);
            gluDeleteQuadric(p);
        } else {
            //GLfloat rho, theta;
            double x, y, z;
            double s, t, ds, dt;
            int i, j;
            double nsign;

            if (orientInside) nsign = -1.0;
            else nsign = 1.0;

            final double drho = PI / stacks;

            // in Sun C/C++ on Solaris 8 VLAs are not allowed, so let's use new double[]
            double[] cosSinRho = new double[2 * (stacks + 1)];

            int cosSinRhoIdx = 0;
            for (i = 0; i <= stacks; i++) {
                double rho = i * drho;
                cosSinRho[cosSinRhoIdx] = cos(rho);
                cosSinRhoIdx++;
                cosSinRho[cosSinRhoIdx] = sin(rho);
                cosSinRhoIdx++;
            }

            final double dtheta = 2.0 * PI / slices;
            double[] cosSinTheta = new double[2 * (slices + 1)];
            int cosSinThetaIdx = 0;
            for (i = 0; i <= slices; i++) {
                double theta = (i == slices) ? 0.0 : i * dtheta;
                cosSinTheta[cosSinThetaIdx] = cos(theta);
                cosSinThetaIdx++;
                cosSinTheta[cosSinThetaIdx] = sin(theta);
                cosSinThetaIdx++;
            }
            // texturing: s goes from 0.0/0.25/0.5/0.75/1.0 at +y/+x/-y/-x/+y axis
            // t goes from -1.0/+1.0 at z = -radius/+radius (linear along longitudes)
            // cannot use triangle fan on texturing (s coord. at top/bottom tip varies)
            ds = 1.0 / slices;
            dt = 1.0 / stacks;
            t = 0.0;// because loop now runs from 0

            // draw intermediate stacks as quad strips
            for (i = 0, cosSinRhoIdx = 0; i < stacks;
                 i++, cosSinRhoIdx += 2) {
                glBegin(GL_QUAD_STRIP);
                s = 0.0;
                for (j = 0, cosSinThetaIdx = 0; j <= slices;
                     j++, cosSinThetaIdx += 2) {
                    x = -cosSinTheta[cosSinThetaIdx + 1] * cosSinRho[cosSinRhoIdx + 1];
                    y = cosSinTheta[cosSinThetaIdx] * cosSinRho[cosSinRhoIdx + 1];
                    z = nsign * cosSinRho[cosSinRhoIdx];
                    glNormal3d(x * oneMinusOblateness * nsign,
                            y * oneMinusOblateness * nsign,
                            z * nsign);
                    glTexCoord2d(s, t);
                    sVertex3(x * radius,
                            y * radius,
                            oneMinusOblateness * z * radius, mat);
                    x = -cosSinTheta[cosSinThetaIdx + 1] * cosSinRho[cosSinRhoIdx + 3];
                    y = cosSinTheta[cosSinThetaIdx] * cosSinRho[cosSinRhoIdx + 3];
                    z = nsign * cosSinRho[cosSinRhoIdx + 2];
                    glNormal3d(x * oneMinusOblateness * nsign,
                            y * oneMinusOblateness * nsign,
                            z * nsign);
                    glTexCoord2d(s, t + dt);
                    s += ds;
                    sVertex3(x * radius,
                            y * radius,
                            oneMinusOblateness * z * radius, mat);
                }
                glEnd();
                t += dt;
            }
        }

        glPopMatrix();
    }

    /**
     * Draw a half sphere
     */
    void sHalfSphere(double radius, int slices, int stacks, final Matrix4d mat, boolean orientInside) {
        glPushMatrix();
        glLoadMatrixd(mat);

        float rho, drho, theta, dtheta;
        float x, y, z;
        float s, t, ds, dt;
        int i, j, imin, imax;
        float nsign;

        if (orientInside) nsign = -1.0f;
        else nsign = 1.0f;

        drho = (float) (Math.PI / (double) stacks);
        dtheta = (float) (2.0 * PI / (double) slices);

        // texturing: s goes from 0.0/0.25/0.5/0.75/1.0 at +y/+x/-y/-x/+y axis
        // t goes from -1.0/+1.0 at z = -radius/+radius (linear along longitudes)
        // cannot use triangle fan on texturing (s coord. at top/bottom tip varies)
        ds = 1.0f / slices;
        dt = 1.0f / stacks;
        t = 0f;// because loop now runs from 0
        imin = 0;
        imax = stacks;

        // draw intermediate stacks as quad strips
        for (i = imin; i < imax / 2; i++) {
            rho = i * drho;
            glBegin(GL_QUAD_STRIP);
            s = 0.0f;
            for (j = 0; j <= slices; j++) {
                theta = (j == slices) ? 0.0f : j * dtheta;
                x = (float) (-sin(theta) * sin(rho));
                y = (float) (cos(theta) * sin(rho));
                z = (float) (nsign * cos(rho));
                glNormal3f(x * nsign, y * nsign, z * nsign);
                glTexCoord2f(s, t);
                sVertex3(x * radius, y * radius, z * radius, mat);
                x = (float) (-sin(theta) * sin(rho + drho));
                y = (float) (cos(theta) * sin(rho + drho));
                z = (float) (nsign * cos(rho + drho));
                glNormal3f(x * nsign, y * nsign, z * nsign);
                glTexCoord2f(s, t + dt);
                s += ds;
                sVertex3(x * radius, y * radius, z * radius, mat);
            }
            glEnd();
            t += dt;
        }
        glPopMatrix();
    }

    /**
     * Draw a disk with a special texturing mode having texture center at center
     */
    public void sDisk(float radius, int slices, int stacks,
                      final Matrix4d mat, boolean orientInside) {
        glPushMatrix();
        glLoadMatrixd(mat);

        double r, dr, theta, dtheta;
        float x, y;
        int j;
        float nsign;

        if (orientInside) {
            nsign = -1.0f;
        } else nsign = 1.0f;

        dr = radius / (double) stacks;
        dtheta = 2.0 * PI / (double) slices;
        if (slices < 0) slices = -slices;

        float diameter = 2 * radius;
        // draw intermediate stacks as quad strips
        for (r = 0; r < radius; r += dr) {
            glBegin(GL_TRIANGLE_STRIP);
            for (j = 0; j <= slices; j++) {
                theta = (j == slices) ? 0.0 : j * dtheta;
                x = (float) (r * cos(theta));
                y = (float) (r * sin(theta));
                glNormal3f(0, 0, nsign);
                glTexCoord2f(0.5f + (x / diameter), 0.5f - (y / diameter));
                sVertex3(x, y, 0, mat);
                x = (float) ((r + dr) * cos(theta));
                y = (float) ((r + dr) * sin(theta));
                glNormal3f(0, 0, nsign);
                glTexCoord2f(0.5f + (x / diameter), 0.5f - (y / diameter));
                sVertex3(x, y, 0, mat);
            }
            glEnd();
        }
        glPopMatrix();
    }

    /**
     * Draw a ring with a radial texturing
     */
    public final void sRing(double rMin, double rMax,
                            int slices, int stacks,
                            final Matrix4d mat, boolean orientInside) {
        glPushMatrix();
        glLoadMatrixd(mat);

        double theta;
        double x, y;
        int j;

        final double nsign = (orientInside) ? -1.0 : 1.0;

        final double dr = (rMax - rMin) / stacks;
        final double dtheta = 2.0 * PI / slices;
        if (slices < 0) slices = -slices;

        //in Sun C/C++ on Solaris 8 VLAs are not allowed, so let's use new double[]
        double[] cos_sin_theta = new double[2 * (slices + 1)];

        int cos_sin_theta_p = 0;

        for (j = 0; j <= slices; j++) {
            theta = (j == slices) ? 0.0 : j * dtheta;
            cos_sin_theta[cos_sin_theta_p] = cos(theta);
            cos_sin_theta_p++;
            cos_sin_theta[cos_sin_theta_p] = sin(theta);
            cos_sin_theta_p++;
        }

        // draw intermediate stacks as quad strips
        for (double r = rMin; r < rMax; r += dr) {
            final double tex_r0 = (r - rMin) / (rMax - rMin);
            final double tex_r1 = (r + dr - rMin) / (rMax - rMin);
            glBegin(GL_QUAD_STRIP/*GL_TRIANGLE_STRIP*/);
            for (j = 0, cos_sin_theta_p = 0;
                 j <= slices;
                 j++, cos_sin_theta_p += 2) {
                //theta = (j == slices) ? 0.0 : j * dtheta;
                x = r * cos_sin_theta[cos_sin_theta_p];
                y = r * cos_sin_theta[cos_sin_theta_p + 1];
                glNormal3d(0, 0, nsign);
                glTexCoord2d(tex_r0, 0.5);
                sVertex3(x, y, 0, mat);
                x = (r + dr) * cos_sin_theta[cos_sin_theta_p];
                y = (r + dr) * cos_sin_theta[cos_sin_theta_p + 1];
                glNormal3d(0, 0, nsign);
                glTexCoord2d(tex_r1, 0.5);
                sVertex3(x, y, 0, mat);
            }
            glEnd();
        }
        glPopMatrix();
    }

    void sSphereMapTexCoordFast(double rho_div_fov,
                                double costheta, double sintheta) {
        if (rho_div_fov > 0.5) rho_div_fov = 0.5;
        glTexCoord2d(0.5 + rho_div_fov * costheta,
                0.5 - rho_div_fov * sintheta);
    }

    /**
     * Draw a fisheye texture in a sphere
     */
    public final void sSphereMap(double radius, int slices, int stacks,
                                 final Matrix4d mat, float textureFOV, boolean orientInside) {
        glPushMatrix();
        glLoadMatrixd(mat);

        double rho, x, y, z;
        int i, j;
        final double nsign = orientInside ? -1 : 1;

        final double drho = PI / stacks;

        // in Sun C/C++ on Solaris 8 VLAs are not allowed, so let's use new double[]
        double[] cos_sin_rho = new double[2 * (stacks + 1)];
        int cos_sin_rho_p = 0;
        for (i = 0; i <= stacks; i++) {
            rho = i * drho;
            cos_sin_rho[cos_sin_rho_p] = cos(rho);
            cos_sin_rho_p++;
            cos_sin_rho[cos_sin_rho_p] = sin(rho);
            cos_sin_rho_p++;
        }

        final double dtheta = (2.0 * PI) / slices;
        double[] cos_sin_theta = new double[2 * (slices + 1)];
        int cos_sin_theta_p = 0;
        for (i = 0; i <= slices; i++) {
            final double theta = (i == slices) ? 0.0 : i * dtheta;
            cos_sin_theta[cos_sin_theta_p] = cos(theta);
            cos_sin_theta_p++;
            cos_sin_theta[cos_sin_theta_p] = sin(theta);
            cos_sin_theta_p++;
        }

        // texturing: s goes from 0.0/0.25/0.5/0.75/1.0 at +y/+x/-y/-x/+y axis
        // t goes from -1.0/+1.0 at z = -radius/+radius (linear along longitudes)
        // cannot use triangle fan on texturing (s coord. at top/bottom tip varies)

        // draw intermediate stacks as quad strips
        if (!orientInside)// nsign==1
        {
            for (i = 0, cos_sin_rho_p = 0, rho = 0.0;
                 i < stacks; ++i, cos_sin_rho_p += 2, rho += drho) {
                glBegin(GL_QUAD_STRIP);
                for (j = 0, cos_sin_theta_p = 0;
                     j <= slices; ++j, cos_sin_theta_p += 2) {
                    x = -cos_sin_theta[cos_sin_theta_p + 1] * cos_sin_rho[cos_sin_rho_p + 1];
                    y = cos_sin_theta[cos_sin_theta_p] * cos_sin_rho[cos_sin_rho_p + 1];
                    z = cos_sin_rho[cos_sin_rho_p];
                    glNormal3d(x * nsign, y * nsign, z * nsign);
                    sSphereMapTexCoordFast(rho / textureFOV,
                            cos_sin_theta[cos_sin_theta_p],
                            cos_sin_theta[cos_sin_theta_p + 1]);
                    sVertex3(x * radius, y * radius, z * radius, mat);

                    x = -cos_sin_theta[cos_sin_theta_p + 1] * cos_sin_rho[cos_sin_rho_p + 3];
                    y = cos_sin_theta[cos_sin_theta_p] * cos_sin_rho[cos_sin_rho_p + 3];
                    z = cos_sin_rho[cos_sin_rho_p + 2];
                    glNormal3d(x * nsign, y * nsign, z * nsign);
                    sSphereMapTexCoordFast((rho + drho) / textureFOV,
                            cos_sin_theta[cos_sin_theta_p],
                            cos_sin_theta[cos_sin_theta_p + 1]);
                    sVertex3(x * radius, y * radius, z * radius, mat);
                }
                glEnd();
            }
        } else {
            for (i = 0, cos_sin_rho_p = 0, rho = 0.0;
                 i < stacks; ++i, cos_sin_rho_p += 2, rho += drho) {
                glBegin(GL_QUAD_STRIP);
                for (j = 0, cos_sin_theta_p = 0;
                     j <= slices; ++j, cos_sin_theta_p += 2) {
                    x = -cos_sin_theta[cos_sin_theta_p + 1] * cos_sin_rho[cos_sin_rho_p + 3];
                    y = cos_sin_theta[cos_sin_theta_p] * cos_sin_rho[cos_sin_rho_p + 3];
                    z = cos_sin_rho[cos_sin_rho_p + 2];
                    glNormal3d(x * nsign, y * nsign, z * nsign);
                    sSphereMapTexCoordFast((rho + drho) / textureFOV,
                            cos_sin_theta[cos_sin_theta_p],
                            -cos_sin_theta[cos_sin_theta_p + 1]);
                    sVertex3(x * radius, y * radius, z * radius, mat);

                    x = -cos_sin_theta[cos_sin_theta_p + 1] * cos_sin_rho[cos_sin_rho_p + 1];
                    y = cos_sin_theta[cos_sin_theta_p] * cos_sin_rho[cos_sin_rho_p + 1];
                    z = cos_sin_rho[cos_sin_rho_p];
                    glNormal3d(x * nsign, y * nsign, z * nsign);
                    sSphereMapTexCoordFast(rho / textureFOV,
                            cos_sin_theta[cos_sin_theta_p],
                            -cos_sin_theta[cos_sin_theta_p + 1]);
                    sVertex3(x * radius, y * radius, z * radius, mat);
                }
                glEnd();
            }
        }

        glPopMatrix();
    }

    /**
     * Reimplementation of gluCylinder : glu is overrided for non standard projection
     */
    public void sCylinder(double radius, double height, int slices, int stacks, final Matrix4d mat, boolean orientInside) {
        glPushMatrix();
        glLoadMatrixd(mat);
        GLUquadric p = gluNewQuadric();
        gluQuadricTexture(p, true);
        if (orientInside) {
            glCullFace(GL_FRONT);
        }
        gluCylinder(p, radius, radius, height, slices, stacks);
        gluDeleteQuadric(p);
        glPopMatrix();
        if (orientInside) {
            glCullFace(GL_BACK);
        }
    }

    /**
     * Method with speed optimization by default
     *
     * @param font
     * @param x
     * @param y
     * @param ws
     * @param xshift
     * @param yshift
     */
    public void printGravity180(final SFontIfc font, double x, double y, String ws, float xshift, float yshift) {
        printGravity180(font, x, y, ws, true, xshift, yshift);
    }

    public void printGravity180(final SFontIfc font, double x, double y, String ws,
                                boolean speedOptimize, float xshift, float yshift) {
        // TODO: Check why printing char by char?
        font.print((int) x, (int) y, ws);
        // TODO: Check why this was static when it is used in this method only... ???
        /*
        double dx, dy, d, theta, psi;

        dx = x - (vecViewport.x + vecViewport.w / 2);
        dy = y - (vecViewport.y + vecViewport.z / 2);
        d = (float) sqrt(dx * dx + dy * dy);

        // If the text is too far away to be visible in the screen return
        if (d > max(vecViewport.getHeight(), vecViewport.getWidth()) * 2) return;


        theta = (float) (Math.PI + atan2(dx, dy - 1));
        psi = (float) (Math.atan2((double) font.getStrLen(ws) / ws.length(), d + 1) * 180. / PI);

        if (psi > 5) psi = 5;
        setOrthographicProjection();
        glTranslatef((float) x, (float) y, 0);
        glRotatef((float) (theta * 180 / PI), 0, 0, -1);
        glTranslatef(xshift, -yshift, 0);
        glScalef(1, -1, 1);

        glEnable(GL.GL_BLEND);
        glEnable(GL.GL_TEXTURE_2D);
        glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);// Normal transparency mode
        for (int i = 0; i < ws.length(); ++i) {

            char c = ws.charAt(i);
            if (c >= 16 && c <= 18) {
                // handle hilight colors (TUI)

                // Note: this is hard coded - not generalized

                if (c == 17) glColor3f(0.5f, 1f, 0.5f);// normal
                if (c == 18) glColor3f(1f, 1f, 1f);// hilight

            } else {

                if (!speedOptimize) {
                    font.printCharOutlined(c);
                } else {
                    font.printChar(c);
                }

                // with typeface need to manually advance
                // TODO, absolute rotation would be better than relative
                // TODO: would look better with kerning information...
                glTranslatef(font.getStrLen(ws.substring(i, 1)) * 1.05f, 0, 0);

                if (!speedOptimize) {
                    psi = atan2(font.getStrLen(ws.substring(i, 1)) * 1.05, d) * 180. / PI;
                    if (psi > 5) psi = 5;
                }

                // keep text horizontal if gravity labels off
                if (gravityLabels) glRotatef((float) psi, 0f, 0f, -1f);
            }
        }
        resetPerspectiveProjection();
        */
    }

    // All following methods are from the projector.h defined or inlined functions
    // Public part of the projector.h

    public TYPE getType() {
        return TYPE.perspective;
    }

    public PROJECTOR_MASK_TYPE getMaskType() {
        return maskType;
    }

    public void setMaskType(PROJECTOR_MASK_TYPE maskType) {
        this.maskType = maskType;
    }

    void setViewport(Rectangle4i viewport) {
        setViewport(viewport.x, viewport.y, viewport.w, viewport.z);
    }

    public void setViewportPosX(int x) {
        setViewport(x, vecViewport.y, vecViewport.w, vecViewport.z);
    }

    public void setViewportPosY(int y) {
        setViewport(vecViewport.x, y, vecViewport.w, vecViewport.z);
    }

    public void setViewportHeight(int width, int height) {
        setViewport(vecViewport.x, vecViewport.y, width, height);
    }

    public int getViewportPosX() {
        return vecViewport.x;
    }

    public int getViewportPosY() {
        return vecViewport.y;
    }

    public int getViewportWidth() {
        return vecViewport.w;
    }

    public int getViewportHeight() {
        return vecViewport.z;
    }

    public final Rectangle4i getViewport() {
        return vecViewport;
    }

    /**
     * Set the current openGL viewport to projector's viewport
     */
    public void applyViewport() {
        glViewport(vecViewport.x, vecViewport.y, vecViewport.w, vecViewport.z);
    }

    public boolean getFlipHorz() {
        return flipHorz < 0.0;
    }

    public boolean getFlipVert() {
        return flipVert < 0.0;
    }

    public void setFlipHorz(boolean flip) {
        flipHorz = flip ? -1.0 : 1.0;
        initProjectMatrix();
    }

    public void setFlipVert(boolean flip) {
        flipVert = flip ? -1.0 : 1.0;
        initProjectMatrix();
    }

    public boolean needGlFrontFaceCW() {
        return flipHorz * flipVert < 0.0;
    }

    /**
     * Get the Field of View in degree
     *
     * @return
     */
    public double getFieldOfView() {
        return fieldOfView;
    }

    public final double getRadPerPixel() {
        return viewScalingFactor;
    }

    /**
     * Get the maximum Field of View in degree
     *
     * @return
     */
    public double getMaxFov() {
        return maxFov;
    }

    /**
     * If is currently zooming, return the target FOV, otherwise return current FOV
     *
     * @return
     */
    public final double getAimFov() {
        return (autoZoom ? zoomMove.aim : fieldOfView);
    }

    public double[] getClippingPlanes() {
        double[] result = new double[2];
        result[0] = zNear;
        result[1] = zFar;
        return result;
    }

    /**
     * @param pos
     * @return true if the 2D pos is inside the viewport
     */
    public final boolean checkInViewport(Point3d pos) {
        return (pos.y > vecViewport.y && pos.y < (vecViewport.y + vecViewport.getHeight()) &&
                pos.x > vecViewport.x && pos.x < (vecViewport.x + vecViewport.getWidth()));
    }

    /**
     * Return in vector "win" the projection on the screen of point v in earth equatorial coordinate
     * according to the current modelview and projection matrices (reimplementation of gluProject)
     *
     * @return true if the z screen coordinate is < 1, ie if it isn't behind the observer
     *         except for the _check version which return true if the projected point is inside the screen
     */
    public final boolean projectEarthEqu(final Point3d v, Point3d win) {
        return projectCustom(v, win, matEarthEquToEye);
    }

    public final boolean projectEarthEquCheck(final Point3d v, Point3d win) {
        return projectCustomCheck(v, win, matEarthEquToEye);
    }

    public final boolean projectEarthEquLineCheck(final Point3d v1, Point3d win1, final Point3d v2, Point3d win2) {
        return projectCustomLineCheck(v1, win1, v2, win2, matEarthEquToEye);
    }

    /**
     * transformation from screen 2D point x,y to object.
     *
     * @param x The 2D point absciss
     * @param y The 2D point ordonnee
     * @param v The 3D coordinates
     */
    public void unprojectEarthEqu(double x, double y, Point3d v) {
        unproject(x, y, invMatEarthEquToEye, v);
    }

    public final void unprojectJ2000(double x, double y, Point3d v) {
        unproject(x, y, invMatJ2000ToEye, v);
    }

    // taking account of precession
    public final boolean projectJ2000(final Point3d v, Point3d win) {
        return projectCustom(v, win, matJ2000ToEye);
    }

    public final boolean projectJ2000Check(final Point3d v, Point3d win) {
        return projectCustomCheck(v, win, matJ2000ToEye);
    }

    /**
     * @param v1
     * @param win1
     * @param v2
     * @param win2
     * @return If the projected is visible
     */
    public final boolean projectJ2000LineCheck(final Point3d v1, Point3d win1, final Point3d v2, Point3d win2) {
        return projectCustomLineCheck(v1, win1, v2, win2, matJ2000ToEye);
    }

    // Same function with input vector v in heliocentric coordinate
    public final boolean projectHelioCheck(final Point3d v, Point3d win) {
        return projectCustomCheck(v, win, matHelioToEye);
    }

    public final boolean projectHelio(final Point3d v, Tuple3d win) {
        return projectCustom(v, win, matHelioToEye);
    }

    public final boolean projectHelioLineCheck(final Point3d v1, Point3d win1, final Point3d v2, Point3d win2) {
        return projectCustomLineCheck(v1, win1, v2, win2, matHelioToEye);
    }

    public final void unprojectHelio(double x, double y, Point3d v) {
        unproject(x, y, invMatHelioToEye, v);
    }

    /**
     * Same function with input vector v in local coordinate
     *
     * @return If the projected is visible
     */
    public final boolean projectLocal(final Point3d v, Point3d win) {
        return projectCustom(v, win, matLocalToEye);
    }

    public final boolean projectLocalCheck(final Point3d v, Point3d win) {
        return projectCustomCheck(v, win, matLocalToEye);
    }

    public final void unprojectLocal(double x, double y, Point3d v) {
        unproject(x, y, invMatLocalToEye, v);
    }

    /**
     * @param v
     * @param win
     * @param mat
     * @return If the projected is visible
     */
    public boolean projectCustom(final Point3d v, Tuple3d win, final Matrix4d mat) {
        gluProject(v, mat, matProjection, vecViewport, win);
        return win.z < 1;
    }

    public boolean projectCustomCheck(Point3d v, Point3d win, Matrix4d mat) {
        return projectCustom(v, win, mat) && checkInViewport(win);
    }

    /**
     * Project two points and make sure both are in front of viewer and that at least one is on screen
     *
     * @return If the projected is visible
     */
    public boolean projectCustomLineCheck(final Point3d v1, Point3d win1,
                                          final Point3d v2, Point3d win2, final Matrix4d mat) {
        return projectCustom(v1, win1, mat) && projectCustom(v2, win2, mat) &&
                (checkInViewport(win1) || checkInViewport(win2));
    }

    public void sVertex3(double x, double y, double z, final Matrix4d mat) {
        glVertex3d(x, y, z);
    }

    public void setFlagGravityLabels(boolean gravityLabels) {
        this.gravityLabels = gravityLabels;
    }

    public boolean isGravityLabelsEnabled() {
        return gravityLabels;
    }

    public void setLightPos(Point3d sunPos) {
        lightPos = new Point3d(sunPos);
    }

    public Point3d getLightPos() {
        return lightPos;
    }

    /**
     * Data for auto mov
     */
    protected class AutoZoom {
        /**
         * Current FOV
         */
        float start;

        /**
         * Target FOV
         */
        float aim;

        float speed;

        float coef;
    }

    /**
     * The current projector mask
     */
    PROJECTOR_MASK_TYPE maskType;

    /**
     * Field of view in degree
     */
    double fieldOfView;

    /**
     * Minimum field of view in degree
     */
    double minFov;

    /**
     * Maximum field of view in degree
     */
    double maxFov;

    /**
     * Near and far clipping planes
     */
    double zNear, zFar;

    /**
     * Viewport parameters
     */
    Rectangle4i vecViewport;

    /**
     * Projection matrix
     */
    Matrix4d matProjection = new Matrix4d(new double[]{
            1., 0., 0., 0.,
            0., 1., 0., 0.,
            0., 0., -1, 0.,
            0., 0., 0., 1.
    });

    /**
     * Viewport center in screen pixel
     */
    Point3d center;

    Point3d lightPos;

    double viewScalingFactor;// ??

    double flipHorz, flipVert;

    /**
     * Modelview Matrix for earth equatorial projection
     */
    Matrix4d matEarthEquToEye;

    /**
     * for precessed equ coords
     */
    Matrix4d matJ2000ToEye;

    /**
     * Modelview Matrix for earth equatorial projection
     */
    Matrix4d matHelioToEye;

    /**
     * Modelview Matrix for earth equatorial projection
     */
    Matrix4d matLocalToEye;

    /**
     * Inverse of mat_projection*mat_earth_equ_to_eye
     */
    Matrix4d invMatEarthEquToEye;

    /**
     * Inverse of mat_projection*mat_j2000_to_eye
     */
    Matrix4d invMatJ2000ToEye;

    /**
     * Inverse of mat_projection*mat_helio_to_eye
     */
    Matrix4d invMatHelioToEye;

    /**
     * Inverse of mat_projection*mat_local_to_eye
     */
    Matrix4d invMatLocalToEye = new Matrix4d();

    /**
     * transformation from screen 2D point x,y to object.
     *
     * @param x The 2D point absciss
     * @param y The 2D point ordonnee
     * @param m The already inverted full tranformation matrix
     * @param v The 3D coordinates
     */
    void unproject(double x, double y, Matrix4d m, Point3d v) {
        v.set((x - vecViewport.x) * 2. / vecViewport.w - 1.0,
                (y - vecViewport.y) * 2. / vecViewport.z - 1.0,
                1.0);
        //v.transfo4d(m);
        m.transform(v);
    }

    // Automove
    /**
     * Current auto movement
     */
    AutoZoom zoomMove = new AutoZoom();

    /**
     * Define if autozoom is on or off
     */
    boolean autoZoom;

    /**
     * should label text align with the horizon?
     */
    boolean gravityLabels;

    //From here it is the replacement from the callback methods for projection implemented by stel objects
    // Replaces C++ function pointer

    ProjFunc projectEarthEquFunc = new ProjectEarthEqu();

    ProjFunc projectLocalFunc = new ProjectLocal();

    ProjFunc projectJ2000Func = new ProjectJ2000();

    /**
     * Local projection strategy
     */
    class ProjectLocal implements ProjFunc {
        public boolean execute(Point3d v, Point3d win) {
            return projectLocal(v, win);
        }
    }

    /**
     * Actual class pointing to the function used as callback
     */
    class ProjectEarthEqu implements ProjFunc {
        public boolean execute(Point3d v, Point3d win) {
            return projectEarthEqu(v, win);
        }
    }

    class ProjectJ2000 implements ProjFunc {
        public boolean execute(Point3d v, Point3d win) {
            return projectJ2000(v, win);
        }
    }

    public ProjFunc getProjectEarthEquFunc() {
        return projectEarthEquFunc;
    }

    public ProjFunc getProjectLocalFunc() {
        return projectLocalFunc;
    }

    public ProjFunc getProjectJ2000Func() {
        return projectJ2000Func;
    }

    private static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(DefaultProjector.class.getName());
            if (parentLogger != null) {
                logger.setParent(parentLogger);
            }
        }
        return logger;
    }
}
