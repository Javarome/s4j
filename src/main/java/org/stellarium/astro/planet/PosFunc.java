package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;

/**
 * @author <a href="mailto:javarome@javarome.net">Jerome Beau</a>
 * @version 0.71
 */
public interface PosFunc {
    /**
     * @param jd
     * @param pos TODO: Fred this should be a point
     */
    void compute(double jd, Tuple3d pos);
}
