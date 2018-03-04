package org.stellarium.astro.planet;

/**
 * Created by IntelliJ IDEA.
 * User: freds
 * Date: Oct 23, 2007
 * Time: 2:38:17 AM
 * To change this template use File | Settings | File Templates.
 */
public enum Body {
    MERCURY(6023600),
    VENUS(408523.5),
    EMB(328900.5),
    MARS(3098710),
    JUPITER(1047.355),
    SATURN(3498.5),
    URANUS(22869),
    NEPTUNE(19314);

    final double vsop87_mu;

    /* dirty caching in static variables */
    double[] vsop87_elem = new double[6];

    Body(double d) {
        this.vsop87_mu = (1.0 + 1.0 / d) * Vsop87.GAUSS_GRAV_CONST;
    }
}
