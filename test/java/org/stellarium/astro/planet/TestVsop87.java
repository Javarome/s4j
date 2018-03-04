package org.stellarium.astro.planet;

import junit.framework.TestCase;
import org.stellarium.astro.JulianDay;

import javax.vecmath.Point3d;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 *
 */
public class TestVsop87 extends TestCase {
    static int vsop87_max_lambda_factor[] = {
            16,
            27,
            27,
            32,
            22,
            26,
            24,
            23,
            2,
            1,
            2,
            1
    };
    private Vsop87 vsop87;

    public TestVsop87() {
    }

    public TestVsop87(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        String filename = "Vsop87.data";
        InputStream resourceAsStream = Vsop87.class.getResourceAsStream(filename);
        InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
        vsop87 = new Vsop87(inputStreamReader, Logger.getAnonymousLogger());
    }

    public void testLambdaFreqSum() {
        for (int max_factor : vsop87_max_lambda_factor) {
            for (int m = 2; m <= max_factor; m++) {
                //cslp += 4;
                /* addition theorem:
                   cos(m*l) = cos(m0*l+m1*l) = cos(m0*l)*cos(m1*l)-sin(m0*l)*sin(m1*l)
                   sin(m*l) = sin(m0*l+m1*l) = cos(m0*l)*sin(m1*l)+sin(m0*l)*cos(m1*l)
                */
                int m0 = ((((m + 0) >> 1) - 1) << 2);
                int m1 = ((((m + 1) >> 1) - 1) << 2);
                System.out.println("m0=" + m0 + " m1=" + m1);
            }
        }
    }

    public void testGetVsop87Coor() {
        Point3d result = new Point3d();
        vsop87.getVsop87Coor(JulianDay.getJulianFromSys(), Body.MARS, result);
        System.out.println("Result MARS today " + result);
    }
}
