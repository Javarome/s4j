package org.stellarium;// orbit.cpp
//
// Copyright (C) 2001, Chris Laurel <claurel@shatters.net>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

import javax.vecmath.Vector3d;

/**
 * Orbit computation.
 *
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>
 * @version Java
 */

abstract class Orbit {
    public abstract Vector3d positionAtTime(double d);

    public double getPeriod() {
        return 0;
    }

    public double getBoundingRadius() {
        return 0;
    }

    public abstract void sample(double d1, double d2, int i, OrbitSampleProc p);
}

