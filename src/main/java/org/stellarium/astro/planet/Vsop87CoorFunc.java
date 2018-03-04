package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;

/**
 * Function to compute ecliptic position from a given date.
 *
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @version $revision$
 */
public class Vsop87CoorFunc implements PosFunc {
    private final Body body;
    private final Vsop87 vsop87;

    public Vsop87CoorFunc(Body body, Vsop87 vsop87) {
        this.body = body;
        this.vsop87 = vsop87;
    }

    /**
     * Chapter 31 Pg 206-207 Equ 31.1 31.2 , 31.3 using VSOP 87
     * Calculate earth rectangular heliocentric ecliptical coordinates
     * for given julian day. Values are in UA.
     *
     * @param jd          Julian day
     * @param eclipticPos The rect coords of ecliptic pos, as a result
     */
    public void compute(double jd, Tuple3d eclipticPos) {
        vsop87.getVsop87Coor(jd, body, eclipticPos);
    }
}
