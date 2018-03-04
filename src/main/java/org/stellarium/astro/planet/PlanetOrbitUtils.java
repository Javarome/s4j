/************************************************************************

 Copyright (c) 2005 Johannes Gajdosik

 Permission is hereby granted, free of charge, to any person obtaining a
 copy of this software and associated documentation files (the "Software"),
 to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense,
 and/or sell copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.

 ****************************************************************/
package org.stellarium.astro.planet;

import javax.vecmath.Tuple3d;
import static java.lang.StrictMath.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The code in this file is heavily inspired by the TASS17 and GUST86 theories
 * found on
 * ftp://ftp.imcce.fr/pub/ephem/satel
 * <p/>
 * I (Johannes Gajdosik) have just taken the Fortran code and data
 * obtained from above and rearranged it into this piece of software.
 * <p/>
 * I can neigther allow nor forbid the above theories.
 * The copyright notice below covers just my work,
 * that is the compilation of the data obtained from above
 * into the software supplied in this file.
 *
 * @author Johannes Gajdosik
 * @author <a href="mailto:freds@jfrog.org"/>Frederic Simon</a>
 * @version Java (0.8.2)
 */
public class PlanetOrbitUtils {

    private static void
    ellipticToRectangular(double mu, double a, double n,
                          double[] elem, double dt, Tuple3d xyz) {
        double L = (elem[1] + n * dt) % (2.0d * PI);
        /* solve Keplers equation
            x = L - elem[2]*sin(x) + elem[3]*cos(x)
          not by trivially iterating
            x_0 = L
            x_{j+1} = L - elem[2]*sin(x_j) + elem[3]*cos(x_j)
          but instead by Newton approximation:
            0 = f(x) = x - L - elem[2]*sin(x) + elem[3]*cos(x)
            f'(x) = 1 - elem[2]*cos(x) - elem[3]*sin(x)
            x_0 = L or whatever, perhaps first step of trivial iteration
            x_{j+1} = x_j - f(x_j)/f'(x_j)
        */
        double Le = L - elem[2] * sin(L) + elem[3] * cos(L);
        for (; ;) {
            double cLe = cos(Le);
            double sLe = sin(Le);
            /* for excenticity < 1 we have denominator > 0 */
            double dLe = (L - Le + elem[2] * sLe - elem[3] * cLe)
                    / (1.0 - elem[2] * cLe - elem[3] * sLe);
            Le += dLe;
            if (abs(dLe) <= 1e-14) break;/* L1: <1e-12 */
        }

        double cLe = cos(Le);
        double sLe = sin(Le);

        double dlf = -elem[2] * sLe + elem[3] * cLe;
        double phi = sqrt(1.0 - elem[2] * elem[2] - elem[3] * elem[3]);
        double psi = 1.0 / (1.0 + phi);

        double x1 = a * (cLe - elem[2] - psi * dlf * elem[3]);
        double y1 = a * (sLe - elem[3] + psi * dlf * elem[2]);

        double elem_4q = elem[4] * elem[4];
        double elem_5q = elem[5] * elem[5];
        double dwho = 2.0 * sqrt(1.0 - elem_4q - elem_5q);
        double rtp = 1.0 - elem_5q - elem_5q;
        double rtq = 1.0 - elem_4q - elem_4q;
        double rdg = 2.0 * elem[5] * elem[4];

        xyz.x = x1 * rtp + y1 * rdg;
        xyz.y = x1 * rdg + y1 * rtq;
        xyz.z = (-x1 * elem[5] + y1 * elem[4]) * dwho;

        /*
           double rsam1 = -elem[2]*cLe - elem[3]*sLe;
           double h = a*n / (1.0 + rsam1);
           double vx1 = h * (-sLe - psi*rsam1*elem[3]);
           double vy1 = h * ( cLe + psi*rsam1*elem[2]);

          xyz[3] = vx1 * rtp + vy1 * rdg;
          xyz[4] = vx1 * rdg + vy1 * rtq;
          xyz[5] = (-vx1 * elem[5] + vy1 * elem[4]) * dwho;
        */
    }

    /**
     * Given the orbital elements at some time t0 calculate the
     * rectangular coordinates at time (t0+dt).
     * <p/>
     * mu = G*(m1+m2) .. gravitational constant of the two body problem
     * a .. semi major axis
     * n = mean motion = 2*M_PI/(orbit period)
     * <p/>
     * elem[0] .. unused (eigther a or n)
     * elem[1] .. L
     * elem[2] .. K=e*cos(Omega+omega)
     * elem[3] .. H=e*sin(Omega+omega)
     * elem[4] .. Q=sin(i/2)*cos(Omega)
     * elem[5] .. P=sin(i/2)*sin(Omega)
     * <p/>
     * Omega = longitude of ascending node
     * omega = argument of pericenter
     * L = mean longitude = Omega + omega + M
     * M = mean anomaly
     * i = inclination
     * e = excentricity
     * <p/>
     * Units are suspected to be: Julian days, AU, rad
     *
     * @param mu
     * @param elem
     * @param dt
     * @param xyz
     */
    public static void ellipticToRectangularN(double mu, double[] elem, double dt,
                                              Tuple3d xyz) {
        double n = elem[0];
        double a = cbrt(mu / (n * n));
        ellipticToRectangular(mu, a, n, elem, dt, xyz);
    }

    public static void ellipticToRectangularA(double mu, double[] elem, double dt,
                                              Tuple3d xyz) {
        double a = elem[0];
        double n = sqrt(mu / (a * a * a));
        ellipticToRectangular(mu, a, n, elem, dt, xyz);
    }

    static List<Double> prepareLambdaArray(int nr_of_lambdas,
                                           int max_lambda_factor[],
                                           double lambda[]) {
        List<Double> result = new ArrayList<Double>(203 * 4);
        /* initialize result:
           (cos,sin)(1*lambda[0]),(cos,sin)(-1*lambda[0]),(cos,sin)(2*lambda[0]),...
           (cos,sin)(1*lambda[1]),(cos,sin)(-1*lambda[1]),(cos,sin)(2*lambda[1]),...
        */
        int cos_sin_lambdap = 0;
        int i;
        for (i = 0; i < nr_of_lambdas; i++) {
            int max_factor = max_lambda_factor[i];
            double cosLambda = cos(lambda[i]);
            double sinLambda = sin(lambda[i]);
            result.add(cosLambda);
            result.add(sinLambda);
            result.add(cosLambda);
            result.add(-sinLambda);
            int m;
            double cosm0;
            double cosm1;
            double sinm0;
            double sinm1;
            for (m = 2; m <= max_factor; m++) {
                //cslp += 4;
                /* addition theorem:
                   cos(m*l) = cos(m0*l+m1*l) = cos(m0*l)*cos(m1*l)-sin(m0*l)*sin(m1*l)
                   sin(m*l) = sin(m0*l+m1*l) = cos(m0*l)*sin(m1*l)+sin(m0*l)*cos(m1*l)
                */
                int m0 = ((((m + 0) >> 1) - 1) << 2);
                int m1 = ((((m + 1) >> 1) - 1) << 2);
                cosm0 = result.get(cos_sin_lambdap + m0);
                cosm1 = result.get(cos_sin_lambdap + m1);
                sinm0 = result.get(cos_sin_lambdap + m0 + 1);
                sinm1 = result.get(cos_sin_lambdap + m1 + 1);
                cosLambda = cosm0 * cosm1 - sinm0 * sinm1;
                sinLambda = cosm0 * sinm1 + sinm0 * cosm1;
                result.add(cosLambda);
                result.add(sinLambda);
                result.add(cosLambda);
                result.add(-sinLambda);
            }
            cos_sin_lambdap += (max_factor << 2);
        }

        return result;
    }

    static class IndexHolder {
        public int spIdx = 0;
        public int lambda_index = 0;
        public int term_count = 0;
    }

    static void accumulateTerms(List<Short> instructions,
                                List<Double> coefficients,
                                List<Double> cos_sin_lambda,
                                double accu[],
                                double[] sp, boolean isElp82b) {
        /* Accumulates the series given in instructions/coefficients.
           The argument of the series is cos_sin_lambda which has
           been initialized using prepareLambdaArray().
           accu is the output accumulator.
           sp must point to a memory area holding 2*nr_of_lambdas double.
           area must be supplied by the caller, it will be destroyed during the
           calculation.
        */
        Iterator<Short> instructionsIterator = instructions.iterator();
        Iterator<Double> coefficientsIterator = coefficients.iterator();
        IndexHolder idx = new IndexHolder();
        sp[0] = 1.0;
        sp[1] = 0.0;
        if (isElp82b) {
            for (; ;) {
                idx.term_count = instructionsIterator.next();
                if (idx.term_count < 0xFE) {
                    idx.lambda_index = ((idx.term_count & 15) << 8) | (instructionsIterator.next());
                    idx.term_count >>= 4;
                    calculateNewArg(cos_sin_lambda, accu, sp,
                            instructionsIterator, coefficientsIterator,
                            idx);
                } else {
                    if (idx.term_count == 0xFF) break;
                    /* pop argument from the stack */
                    idx.spIdx -= 2;
                }
            }
        } else {
            for (; ;) {
                idx.lambda_index = instructionsIterator.next();
                if (idx.lambda_index < 0xFE) {
                    idx.lambda_index = (idx.lambda_index << 8) | (instructionsIterator.next());
                    idx.term_count = instructionsIterator.next();
                    calculateNewArg(cos_sin_lambda, accu, sp,
                            instructionsIterator, coefficientsIterator,
                            idx);
                } else {
                    if (idx.lambda_index == 0xFF) break;
                    /* pop argument from the stack */
                    idx.spIdx -= 2;
                }
            }
        }
    }

    private static void calculateNewArg(List<Double> cos_sin_lambda,
                                        double[] accu,
                                        double[] sp,
                                        Iterator<Short> instructionsIterator,
                                        Iterator<Double> coefficientsIterator,
                                        IndexHolder idx) {
        /* calculate new argument and push it on the stack */
        double cosLambda = cos_sin_lambda.get(idx.lambda_index << 1);
        double sinLambda = cos_sin_lambda.get((idx.lambda_index << 1) + 1);
        sp[idx.spIdx + 2] = cosLambda * sp[idx.spIdx] - sinLambda * sp[idx.spIdx + 1];
        sp[idx.spIdx + 3] = cosLambda * sp[idx.spIdx + 1] + sinLambda * sp[idx.spIdx];
        idx.spIdx += 2;
        while (--idx.term_count >= 0) {
            Double coef1 = coefficientsIterator.next();
            Double coef2 = coefficientsIterator.next();
            accu[instructionsIterator.next()] += (coef1 * sp[idx.spIdx]
                    + coef2 * sp[idx.spIdx + 1]);
        }
    }
}
