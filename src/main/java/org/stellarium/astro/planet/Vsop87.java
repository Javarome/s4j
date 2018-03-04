/************************************************************************

 The PLANETARY SOLUTION VSOP87 by Bretagnon P. and Francou G. can be found at
 ftp://ftp.imcce.fr/pub/ephem/planets/vsop87

 I (Johannes Gajdosik) have just taken the data obtained from above
 (VSOP87.mer,...,VSOP87.nep) and rearranged it into this piece of software.

 I can neigther allow nor forbid the usage of VSOP87.
 The copyright notice below covers not the work of Bretagnon P. and Francou G.
 but just my work, that is the compilation of the VSOP87 data
 into the software supplied in this file.


 Copyright (c) 2006 Johannes Gajdosik

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

import org.stellarium.StellariumException;

import javax.vecmath.Tuple3d;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.abs;
import static org.stellarium.astro.planet.PlanetOrbitUtils.ellipticToRectangularA;

/**
 * Copyright (c) 2006 Johannes Gajdosik
 * This is my implementation of the VSOP87 planetary solution.
 * I tried to optimize for speed by rearranging the terms so that
 * for a given touple of (a[0],a[1],...,a[11]) the values
 * (cos,sin)(a[0]*lambda[0](T)+...a[11]*lambda[11](T))
 * have only to be calculated once.
 * Furthermore I used the addition formulas
 * (cos,sin)(x+y) = ...
 * so that for given T the functions cos and sin have only to be called 12 times.
 *
 * @author Johannes Gajdosik
 * @author <a href="mailto:rr0@rr0.org"/>Jerome Beau</a>
 * @author <a href="mailto:freds@jfrog.org"/>Frederic Simon</a>
 * @version 1.0
 * @since 0.8.2
 */
public class Vsop87 {
    /**
     * Original VSOP87 values come from IAU 1976 constants.
     * Thanks to Jean-Louis Simon for this information.
     */
    public static final double GAUSS_GRAV_CONST = 0.01720209895d * 0.01720209895d;

    public static final int[] vsop87_max_lambda_factor = new int[]{
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

    /**
     * Radians
     */
    public static final double[] lambda_0 = new double[]{
            4.40260884240,
            3.17614669689,
            1.75347045953,
            6.20347611291,
            0.59954649739,
            0.87401675650,
            5.48129387159,
            5.31188628676,
            5.19846674103,
            1.62790523337,
            2.35555589827,
            3.81034454697
    };

    /**
     * Radians per thousand Julian years
     */
    public static final double[] frequency = new double[]{
            26087.9031415742,
            10213.2855462110,
            6283.0758499914,
            3340.6124266998,
            529.6909650946,
            213.2990954380,
            74.7815985673,
            38.1330356378,
            77713.7714681205,
            84334.6615813083,
            83286.9142695536,
            83997.0911355954
    };

    public final List<Double> vsop87_constants = new ArrayList<Double>(253);

    public final List<Double> vsop87_coefficients = new ArrayList<Double>(61126 * 2);

    public final List<Short> vsop87_instructions = new ArrayList<Short>(75000);

    public final List<Short> vsop87_index_translation_table = new ArrayList<Short>(8 * 6 * 6);

    /**
     * 10 days:
     */
    static final int ELEM_VALIDITY = 10;

    static final double VAL_FACTOR = 365250.0d / ELEM_VALIDITY;

    /**
     * dirty caching in static variables
     */
    static double vsop87_t0 = -1e99;
    protected final Logger logger;

    /*
    Can't find posfunc phobos_special for Phobos
Can't find posfunc deimos_special for Deimos
Can't find posfunc mimas_special for Mimas
Can't find posfunc enceladus_special for Enceladus
Can't find posfunc tethys_special for Tethys
Can't find posfunc dione_special for Dione
Can't find posfunc rhea_special for Rhea
Can't find posfunc titan_special for Titan
Can't find posfunc hyperion_special for Hyperion
Can't find posfunc iapetus_special for Iapetus
Can't find posfunc miranda_special for Miranda
Can't find posfunc ariel_special for Ariel
Can't find posfunc umbriel_special for Umbriel
Can't find posfunc titania_special for Titania
Can't find posfunc oberon_special for Oberon

Can't find posfunc deimos_special for Deimos
Can't find posfunc mimas_special for Mimas
Can't find posfunc enceladus_special for Enceladus
Can't find posfunc tethys_special for Tethys
Can't find posfunc dione_special for Dione
Can't find posfunc rhea_special for Rhea
Can't find posfunc titan_special for Titan
Can't find posfunc hyperion_special for Hyperion
Can't find posfunc iapetus_special for Iapetus
Can't find posfunc miranda_special for Miranda
Can't find posfunc ariel_special for Ariel
Can't find posfunc umbriel_special for Umbriel
Can't find posfunc titania_special for Titania
Can't find posfunc oberon_special for Oberon


...no position data for Baxendell's unphotographable nebula 7088
...no position data for Pelican nebula I5067 I5067, 70
...no position data for η Car nebula 3372 ( 222 names loaded)

     */
    public Vsop87(InputStreamReader inputStreamReader, Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        try {
            LineNumberReader reader = new LineNumberReader(inputStreamReader);
            try {
                // Initializing arrays from Vsop87.data file
                readConstants(reader);
                readCoefficients(reader);
                readInstructions(reader);
                readTranslationTable(reader);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new StellariumException("Error while reading \"" + inputStreamReader + "\"", e);
        }
    }

    private void readTranslationTable(LineNumberReader reader) throws IOException {
        String line = reader.readLine();
        if (!line.startsWith("vsop87_index_translation_table")) {
            throw new StellariumException(reader + " should contain vsop87_index_translation_table at line " + reader.getLineNumber() + " but contains \"" + line + "\"");
        }
        logger.fine("Reading translation table at line " + reader.getLineNumber());
        while (true) {
            line = reader.readLine();
            if (line.contains("}")) {
                break;
            } else {
                vsop87_index_translation_table.add(Short.parseShort(line.replace(',', ' ').trim()));
            }
        }
    }

    private void readInstructions(LineNumberReader reader) throws IOException {
        String line = reader.readLine();
        if (!line.startsWith("vsop87_instructions")) {
            throw new StellariumException(reader + " should contain vsop87_instructions at line " + reader.getLineNumber());
        }
        while (true) {
            line = reader.readLine();
            if (line.contains("}")) {
                break;
            } else {
                StringTokenizer st = new StringTokenizer(line, " \t,");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (token.startsWith("0x"))
                        vsop87_instructions.add(Short.parseShort(token.substring(2), 16));
                    else
                        vsop87_instructions.add(Short.parseShort(token));
                }
            }
        }
    }

    private void readCoefficients(LineNumberReader reader) throws IOException {
        String line = reader.readLine();
        if (!line.startsWith("vsop87_coefficients")) {
            throw new StellariumException(reader + " should contain vsop87_coefficients at line " + reader.getLineNumber());
        }
        while (true) {
            line = reader.readLine();
            if (line.contains("}")) {
                break;
            } else {
                StringTokenizer st = new StringTokenizer(line, " \t,");
                vsop87_coefficients.add(Double.parseDouble(st.nextToken()));
                vsop87_coefficients.add(Double.parseDouble(st.nextToken()));
            }
        }
    }

    private void readConstants(LineNumberReader reader) throws IOException {
        String line = reader.readLine();
        if (!line.startsWith("vsop87_constants")) {
            throw new StellariumException(reader + " should starts with vsop87_constants at line 0");
        }
        while (true) {
            line = reader.readLine();
            if (line.contains("}")) {
                break;
            } else {
                vsop87_constants.add(Double.parseDouble(line.replace(',', ' ').trim()));
            }
        }
    }

    /**
     * Return the rectangular coordinates of the given planet
     * and the given julian date jd expressed in dynamical time (TAI+32.184s).
     * The origin of the xyz-coordinates is the center of the sun.
     * The reference frame is "dynamical equinox and ecliptic J2000",
     * which is the reference frame in VSOP87 and VSOP87A.
     *
     * @param body
     * @param xyz
     */
    public void getVsop87Coor(double jd, Body body, Tuple3d xyz) {
        double t = recalculateIfNeccesary(jd);
        ellipticToRectangularA(body.vsop87_mu, body.vsop87_elem, 365250.0 * (t - vsop87_t0), xyz);
    }

    /**
     * The oculating orbit of epoch jd0, evatuated at jd, is returned.
     *
     * @param jd0
     * @param jd
     * @param body
     */
    public void getVsop87OsculatingCoor(double jd0, double jd, Body body, Tuple3d xyz) {
        double t0 = recalculateIfNeccesary(jd0);
        double t = (jd - 2451545.0) / 365250.0;
        ellipticToRectangularA(body.vsop87_mu, body.vsop87_elem,
                365250.0 * (t - vsop87_t0), xyz);
    }

    private synchronized double recalculateIfNeccesary(double jd) {
        double t = (jd - 2451545.0) / 365250.0;
        if (abs(t - vsop87_t0) > 0.5 / VAL_FACTOR) {
            vsop87_t0 = t;
            calcVsop87Elem(vsop87_t0);
        }
        return t;
    }

    private void calcVsop87Elem(double t) {
        double[] lambda = new double[12];
        for (int i = 0; i < 12; i++) {
            lambda[i] = lambda_0[i] + frequency[i] * t;
        }
        List<Double> cosSinLambda = PlanetOrbitUtils.prepareLambdaArray(12, vsop87_max_lambda_factor, lambda);

        double[] accu = new double[vsop87_constants.size()];
        for (int i = 0; i < accu.length; i++) {
            accu[i] = 0.0;
        }
        double[] stack = new double[12 * 2];
        PlanetOrbitUtils.accumulateTerms(vsop87_instructions, vsop87_coefficients, cosSinLambda,
                accu, stack, false);

        for (Body body : Body.values()) {
            for (int i = 0; i < body.vsop87_elem.length; i++) {
                body.vsop87_elem[i] = 0.0d;
            }
        }

        /* terms of order t^alpha: */
        int fullPosInElem = 0;
        double usePolynomials = (6.1 - abs(t)) / 0.1;
        if (usePolynomials > 0) {
            if (usePolynomials > 1.0) {
                usePolynomials = 1.0;
            }
            for (Body body : Body.values()) {
                for (int i = 0; i < body.vsop87_elem.length; i++) {
                    double result = 0.0d;
                    int alpha;
                    for (alpha = 5; alpha > 0; alpha--) {
                        int j = vsop87_index_translation_table.get(fullPosInElem * 6 + alpha);
                        if (j >= 0) {
                            result += accu[j] + vsop87_constants.get(j);
                            result *= t;
                        }
                    }
                    body.vsop87_elem[i] = result * usePolynomials;
                    fullPosInElem++;
                }
            }
        }

        // terms of order t^0:
        fullPosInElem = 0;
        for (Body body : Body.values()) {
            for (int i = 0; i < body.vsop87_elem.length; i++) {
                int j = vsop87_index_translation_table.get(fullPosInElem * 6);
                body.vsop87_elem[i] += accu[j] + vsop87_constants.get(j);
                fullPosInElem++;
            }
        }

        // longitudes:
        fullPosInElem = 0;
        for (Body body : Body.values()) {
            body.vsop87_elem[1] = (body.vsop87_elem[1] + t * frequency[fullPosInElem]) % (2 * PI);
            if (body.vsop87_elem[1] < 0.0) {
                body.vsop87_elem[1] += 2 * PI;
            }
            fullPosInElem++;
        }

        /*
          for (i=0;i<8;i++) {
            printf("%f %d %15.10f %15.10f %15.10f %15.10f %15.10f %15.10f\n",
                   2451545.0+t*365250.0,i,
                   elem[i*6+0],elem[i*6+1],elem[i*6+2],
                   elem[i*6+3],elem[i*6+3],elem[i*6+4]);
          }
        */
    }

}