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

import org.stellarium.ui.render.SFontIfc;
import org.stellarium.vecmath.Rectangle4i;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

/**
 * @author Jerome Beau, Fred Simon
 * @version 0.8.2
 */
public interface Projector {
    /**
     * Get the Field of View in degree
     *
     * @return
     */
    double getFieldOfView();

    /**
     * Set 2D coordinate
     */
    void setOrthographicProjection();

    void resetPerspectiveProjection();

    boolean isGravityLabelsEnabled();

    void printGravity180(SFontIfc fontIfc, double x, double y, String ws, float xshift, float yshift);

    void printGravity180(SFontIfc font, double x, double y, String ws,
                         boolean speedOptimize, float xshift, float yshift);

    int getViewportPosX();

    int getViewportPosY();

    int getViewportWidth();

    int getViewportHeight();

    Rectangle4i getViewport();

    boolean projectJ2000Check(Point3d v, Point3d win);

    /**
     * @param v1
     * @param win1
     * @param v2
     * @param win2
     * @return If the projected is visible
     */
    boolean projectJ2000LineCheck(Point3d v1, Point3d win1, Point3d v2, Point3d win2);

    TYPE getType();

    /**
     * Set the maximum Field of View in degree
     *
     * @param max The new maximum field of view
     */
    void setMaxFov(double max);

    /**
     * Method by orientInside false by default
     */
    void sSphere(double radius, double oneMinusOblateness, int slices, int stacks, Matrix4d mat);

    /**
     * Reimplementation of gluSphere : glu is overrided for non standard projection
     *
     * @param radius
     * @param slices
     * @param stacks
     * @param mat
     * @param orientInside
     */
    void sSphere(double radius, double oneMinusOblateness,
                 int slices, int stacks,
                 Matrix4d mat, boolean orientInside);

    /**
     * @param v
     * @param win
     * @return If the display is inside the screen
     */
    boolean projectEarthEqu(Point3d v, Point3d win);

    /**
     * Same function with input vector v in local coordinate
     *
     * @return If the projected is visible
     */
    boolean projectLocal(Point3d v, Point3d win);

    ProjFunc getProjectEarthEquFunc();

    ProjFunc getProjectLocalFunc();

    ProjFunc getProjectJ2000Func();

    boolean needGlFrontFaceCW();

    void unprojectLocal(double x, double y, Point3d v);

    void sVertex3(double x, double y, double z, Matrix4d mat);

    // taking account of precession
    boolean projectJ2000(Point3d v, Point3d win);

    public enum TYPE {perspective, fisheye, cylinder, stereographic, spheric_mirror}

    public interface ProjFunc {
        boolean execute(Point3d v, Point3d win);
    }
}