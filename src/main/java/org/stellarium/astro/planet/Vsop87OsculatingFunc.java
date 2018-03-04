package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;

/**
 *
 */
public class Vsop87OsculatingFunc implements OsculatingFunc {
    private final Body body;
    private Vsop87 vsop87;

    public Vsop87OsculatingFunc(Body body, Vsop87 vsop87) {
        this.body = body;
        this.vsop87 = vsop87;
    }

    public void compute(double jd0, double jd, Tuple3d xyz/*[3]*/) {
        vsop87.getVsop87OsculatingCoor(jd0, jd, body, xyz);
    }
}
