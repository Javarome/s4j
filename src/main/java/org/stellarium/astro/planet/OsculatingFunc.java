package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;

/**
 * @author Fred Simon
 * @version 0.8.2
 */
public interface OsculatingFunc {
    void compute(double jd0, double jd, Tuple3d xyz/*[3]*/);
}
