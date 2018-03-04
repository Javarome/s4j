package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import static java.lang.StrictMath.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * *********************************************************************
 * <p/>
 * LUNAR SOLUTION ELP2000-82B
 * by Chapront-Touze M., Chapront J.
 * ftp://ftp.imcce.fr/pub/ephem/moon/elp82b
 * <p/>
 * I (Johannes Gajdosik) have just taken the Fortran code and data
 * obtained from above and used it to create this piece of software.
 * <p/>
 * I can neigther allow nor forbid the usage of ELP2000-82B.
 * The copyright notice below covers not the works of
 * Chapront-Touze M. and Chapront J., but just my work,
 * that is the compilation and rearrangement of
 * the Fortran code and data obtained from above
 * into the software supplied in this file.
 * <p/>
 * <p/>
 * Copyright (c) 2005 Johannes Gajdosik
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * <p/>
 * My implementation of ELP2000-82B has the following modifications compared to
 * the original Fortran code:
 * 1) fundamentally rearrange the series into optimized instructions
 * for fast calculation of the results
 * 2) units are: julian day, AU
 * <p/>
 * **************************************************************
 */
public class Elp82b implements PosFunc {
    static
    final int[] elp82b_max_lambda_factor = {
            10,
            6,
            8,
            6,
            2,
            8,
            4,
            6,
            5,
            17,
            61,
            65,
            71,
            14,
            12,
            4,
            4,
    };

    static
    final double[] elp82b_constants = {
            0.000000000000000000e+00,
            0.000000000000000000e+00,
            3.850005584468033630e+05,
            -1.100000000000000039e-04,
            0.000000000000000000e+00,
            2.059999999999999765e-03,
            0.000000000000000000e+00,
            0.000000000000000000e+00,
            0.000000000000000000e+00,
    };

    static final List<Double> elp82b_coefficients = new ArrayList<Double>(37514 * 2);

    static final List<Short> elp82b_instructions = new ArrayList<Short>(125000);

    static {
        try {
            // Initializing arrays from Elp82b.data file
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(Elp82b.class.getResourceAsStream("Elp82b.data")));
            String line = reader.readLine();
            if (!line.startsWith("elp82b_coefficients")) {
                throw new IOException("File Elp82b.data should contain elp82b_coefficients at line " + reader.getLineNumber());
            }
            while (line != null) {
                line = reader.readLine();
                if (line.contains("}")) {
                    break;
                } else {
                    StringTokenizer st = new StringTokenizer(line, " \t,");
                    elp82b_coefficients.add(Double.parseDouble(st.nextToken()));
                    elp82b_coefficients.add(Double.parseDouble(st.nextToken()));
                }
            }
            line = reader.readLine();
            if (!line.startsWith("elp82b_instructions")) {
                throw new IOException("File Elp82b.data should contain elp82b_instructions at line " + reader.getLineNumber());
            }
            while (line != null) {
                line = reader.readLine();
                if (line.contains("}")) {
                    break;
                } else {
                    StringTokenizer st = new StringTokenizer(line, " \t,");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if (token.startsWith("0x"))
                            elp82b_instructions.add(Short.parseShort(token.substring(2), 16));
                        else if (token.contains("+16*"))
                            elp82b_instructions.add((short) (Short.parseShort(token.substring(0, 1)) + 16 * Short.parseShort(token.substring(5))));
                        else
                            elp82b_instructions.add(Short.parseShort(token));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Delaunay's arguments */
    static
    final double[] del = {
            (1732559343.73604 - 129597742.2758) * (PI / (180 * 3600)),
            (129597742.2758 - 1161.2283) * (PI / (180 * 3600)),
            (1732559343.73604 - 14643420.2632) * (PI / (180 * 3600)),
            (1732559343.73604 - -6967919.3622) * (PI / (180 * 3600)),
            (-5.8883 - -0.0202) * (PI / (180 * 3600)),
            (-0.0202 - 0.5327) * (PI / (180 * 3600)),
            (-5.8883 - -38.2776) * (PI / (180 * 3600)),
            (-5.8883 - 6.3622) * (PI / (180 * 3600)),
            (0.006604 - 9e-6) * (PI / (180 * 3600)),
            (9e-6 - -1.38e-4) * (PI / (180 * 3600)),
            (0.006604 - -0.045047) * (PI / (180 * 3600)),
            (0.006604 - 0.007625) * (PI / (180 * 3600)),
            (-3.169e-5 - 1.5e-7) * (PI / (180 * 3600)),
            (1.5e-7 - 0.0) * (PI / (180 * 3600)),
            (-3.169e-5 - 2.1301e-4) * (PI / (180 * 3600)),
            (-3.169e-5 - -3.586e-5) * (PI / (180 * 3600))
    };

    /* Precession */
    static final double[] zeta = {
            (1732559343.73604 + 5029.0966) * (PI / (180 * 3600))//w[3]+preces
    };

    /* Planetary arguments */
    static final double[] p = {
            538101628.68898 * (PI / (180 * 3600)),
            210664136.43355 * (PI / (180 * 3600)),
            129597742.2758 * (PI / (180 * 3600)),
            68905077.59284 * (PI / (180 * 3600)),
            10925660.42861 * (PI / (180 * 3600)),
            4399609.65932 * (PI / (180 * 3600)),
            1542481.19393 * (PI / (180 * 3600)),
            786550.32074 * (PI / (180 * 3600))
    };

    /* Polynom for incrementing r1 */
    static final double[] w = {
            218 * 3600 + 18 * 60 + 59.95571,
            1732559343.73604,
            -5.8883,
            0.006604,
            -3.169e-5,
    };


    static final double a0_div_ath_times_au =
            384747.9806448954 / (384747.9806743165 * 149597870.691);

    /* Polynoms for transformation matrix */
    static final double p1 = 1.0180391e-5;

    static final double p2 = 4.7020439e-7;

    static final double p3 = -5.417367e-10;

    static final double p4 = -2.507948e-12;

    static final double p5 = 4.63486e-15;

    static final double q1 = -1.13469002e-4;

    static final double q2 = 1.2372674e-7;

    static final double q3 = 1.265417e-9;

    static final double q4 = -1.371808e-12;

    static final double q5 = -3.20334e-15;

    public void compute(double jd, Tuple3d pos) {
        getElp82bCoor(jd, pos);
    }

    public void getElp82bCoor(double jd, Tuple3d xyz) {
        final double t = (jd - 2451545.0) / 36525.0;
        double[] lambda = new double[17];
        int i, k;
        for (i = 0; i < 4; i++) {
            lambda[i] = 0.0;
            for (k = 3; k >= 0; k--) {
                lambda[i] += del[k * 4 + i];
                lambda[i] *= t;
            }
            lambda[5 + i] = del[i] * t;
        }
        lambda[4] = zeta[0] * t;
        for (i = 0; i < 8; i++) {
            lambda[9 + i] = p[i] * t;
        }

        List<Double> cos_sin_lambda = PlanetOrbitUtils.prepareLambdaArray(17, elp82b_max_lambda_factor, lambda);
        double[] accu = new double[elp82b_constants.length];
        System.arraycopy(elp82b_constants, 0, accu, 0, elp82b_constants.length);
        double[] stack = new double[17 * 2];
        PlanetOrbitUtils.accumulateTerms(elp82b_instructions, elp82b_coefficients, cos_sin_lambda,
                accu, stack, true);

        final double r1 = (accu[0] + w[0]
                + t * (accu[3] + w[1]
                + t * (accu[6] + w[2]
                + t * (w[3]
                + t * w[4])))) * (PI / (180 * 3600));
        final double r2 = (accu[1] + t * (accu[4] + t * accu[7])) * (PI / (180 * 3600));
        final double r3 = (accu[2] + t * (accu[5] + t * accu[8])) * a0_div_ath_times_au;

        final double rh = r3 * cos(r2);
        final double x3 = r3 * sin(r2);
        final double x1 = rh * cos(r1);
        final double x2 = rh * sin(r1);

        double pw = t * (p1 + t * (p2 + t * (p3 + t * (p4 + t * p5))));
        double qw = t * (q1 + t * (q2 + t * (q3 + t * (q4 + t * q5))));
        final double pwq = pw * pw;
        final double qwq = qw * qw;
        final double pwqw = 2.0 * pw * qw;
        final double pw2 = 1.0 - 2.0 * pwq;
        final double qw2 = 1.0 - 2.0 * qwq;
        final double ra = 2.0 * sqrt(1.0 - pwq - qwq);
        pw *= ra;
        qw *= ra;

        // VSOP87 coordinates:
        xyz.x = pw2 * x1 + pwqw * x2 + pw * x3;
        xyz.y = pwqw * x1 + qw2 * x2 - qw * x3;
        xyz.z = -pw * x1 + qw * x2 + (pw2 + qw2 - 1.0) * x3;

        //printf("Moon: %f  %22.15f %22.15f %22.15f\n",
        //       jd,xyz[0],xyz[1],xyz[2]);
    }
}
