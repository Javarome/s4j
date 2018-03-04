package org.stellarium;

import org.stellarium.astro.planet.PosFunc;
import org.stellarium.functional.UnaryFunction;
import org.stellarium.solve.DoubleSolve;

import javax.vecmath.Matrix4d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import static java.lang.Math.*;

/**
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 */
public class EllipticalOrbit extends Orbit {

    public EllipticalOrbit(double _pericenterDistance,
                           double _eccentricity,
                           double _inclination,
                           double _ascendingNode,
                           double _argOfPeriapsis,
                           double _meanAnomalyAtEpoch,
                           double _period,
                           double _epoch,
                           double parent_rot_obliquity,
                           double parent_rot_ascendingnode) {
        pericenterDistance = _pericenterDistance;
        eccentricity = _eccentricity;
        inclination = _inclination;
        ascendingNode = _ascendingNode;
        argOfPeriapsis = _argOfPeriapsis;
        meanAnomalyAtEpoch = _meanAnomalyAtEpoch;
        period = _period;
        epoch = _epoch;
        final double c_obl = cos(parent_rot_obliquity);
        final double s_obl = sin(parent_rot_obliquity);
        final double c_nod = cos(parent_rot_ascendingnode);
        final double s_nod = sin(parent_rot_ascendingnode);
        rotate_to_vsop87[0] = c_nod;
        rotate_to_vsop87[1] = -s_nod * c_obl;
        rotate_to_vsop87[2] = s_nod * s_obl;
        rotate_to_vsop87[3] = s_nod;
        rotate_to_vsop87[4] = c_nod * c_obl;
        rotate_to_vsop87[5] = -c_nod * s_obl;
        rotate_to_vsop87[6] = 0.0;
        rotate_to_vsop87[7] = s_obl;
        rotate_to_vsop87[8] = c_obl;
    }

    private double pericenterDistance;

    double eccentricity;

    double inclination;

    double ascendingNode;

    double argOfPeriapsis;

    double meanAnomalyAtEpoch;

    double period;

    double epoch;

    double[] rotate_to_vsop87 = new double[9];

    public PosFunc getEllipticalFunc() {
        return new EllipticalOrbitFunc();
    }

    /**
     * Standard iteration for solving Kepler's Equation
     */
    class SolveKeplerFunc1 implements UnaryFunction {

        double ecc;

        double M;

        SolveKeplerFunc1(double _ecc, double _M) {
            ecc = _ecc;
            M = _M;
        }

        public double operator(double x) {
            return M + ecc * sin(x);
        }
    }

    /**
     * Faster converging iteration for Kepler's Equation; more efficient
     * than above for orbits with eccentricities greater than 0.3.  This
     * is from Jean Meeus's _Astronomical Algorithms_ (2nd ed), p. 199
     */
    class SolveKeplerFunc2 implements UnaryFunction {
        double ecc;

        double M;

        SolveKeplerFunc2(double _ecc, double _M) {
            ecc = _ecc;
            M = _M;
        }

        public double operator(double x) {
            return x + (M + ecc * sin(x) - x) / (1 - ecc * cos(x));
        }
    }

    double sign(double x) {
        if (x < 0)
            return -1;
        else if (x > 0)
            return 1;
        else
            return 0;
    }

    class SolveKeplerLaguerreConway implements UnaryFunction {
        double ecc;

        double M;

        SolveKeplerLaguerreConway(double _ecc, double _M) {
            ecc = _ecc;
            M = _M;
        }

        public double operator(double x) {
            double s = ecc * sin(x);
            double c = ecc * cos(x);
            double f = x - s - M;
            double f1 = 1 - c;
            double f2 = s;
            x += -5 * f / (f1 + sign(f1) * sqrt(abs(16 * f1 * f1 - 20 * f * f2)));

            return x;
        }
    }

    class SolveKeplerLaguerreConwayHyp implements UnaryFunction {
        double ecc;

        double M;

        SolveKeplerLaguerreConwayHyp(double _ecc, double _M) {
            ecc = _ecc;
            M = _M;
        }

        public double operator(double x) {
            double s = ecc * sinh(x);
            double c = ecc * cosh(x);
            double f = s - x - M;
            double f1 = c - 1;
            double f2 = s;
            x += -5 * f / (f1 + sign(f1) * sqrt(abs(16 * f1 * f1 - 20 * f * f2)));
            return x;
        }
    }

    double eccentricAnomaly(double M) {
        if (eccentricity == 0.0) {
            // Circular orbit
            return M;
        } else if (eccentricity < 0.2) {
            // Low eccentricity, so use the standard iteration technique
            DoubleSolve.DoublePair sol = DoubleSolve.solveIterationFixed(new SolveKeplerFunc1(eccentricity, M), M, 5);
            return sol.first;
        } else if (eccentricity < 0.9) {
            // Higher eccentricity elliptical orbit; use a more complex but
            // much faster converging iteration.
            DoubleSolve.DoublePair sol = DoubleSolve.solveIterationFixed(new SolveKeplerFunc2(eccentricity, M), M, 6);
            // Debugging
            // printf("ecc: %f, error: %f mas\n",
            //        eccentricity, radToDeg(sol.second) * 3600000);
            return sol.first;
        } else if (eccentricity < 1.0) {
            // Extremely stable Laguerre-Conway method for solving Kepler's
            // equation.  Only use this for high-eccentricity orbits, as it
            // requires more calcuation.
            double E = M + 0.85 * eccentricity * sign(Math.sin(M));
            DoubleSolve.DoublePair sol = DoubleSolve.solveIterationFixed(new SolveKeplerLaguerreConway(eccentricity, M), E, 8);
            return sol.first;
        } else if (eccentricity == 1.0) {
            // Nearly parabolic orbit; very common for comets
            // TODO: handle this
            return M;
        } else {
            // Laguerre-Conway method for hyperbolic (ecc > 1) orbits.
            double E = Math.log(2 * M / eccentricity + 1.85);
            DoubleSolve.DoublePair sol = DoubleSolve.solveIterationFixed(new SolveKeplerLaguerreConwayHyp(eccentricity, M), E, 30);
            return sol.first;
        }
    }

    Vector3d positionAtE(double E) {
        double x, z;

        if (eccentricity < 1) {
            double a = pericenterDistance / (1 - eccentricity);
            x = a * (cos(E) - eccentricity);
            z = a * sqrt(1 - eccentricity * eccentricity) * -sin(E);
        } else if (eccentricity > 1) {
            double a = pericenterDistance / (1 - eccentricity);
            x = -a * (eccentricity - cosh(E));
            z = -a * sqrt(eccentricity * eccentricity - 1) * -sinh(E);
        } else {
            // TODO: Handle parabolic orbits
            x = 0;
            z = 0;
        }

        Matrix4d r = new Matrix4d();
        Matrix4d ry = new Matrix4d();
        Matrix4d rx = new Matrix4d();
        ry.rotY(ascendingNode);
        rx.rotX(inclination);
        r.mul(ry, rx);
        ry.rotY(argOfPeriapsis);
        r.mul(ry);

        Vector3d result = new Vector3d(x, 0, z);
        r.transform(result);

        return result;
    }

    /**
     * @return The offset from the center
     */
    public Vector3d positionAtTime(double t) {
        t = t - epoch;
        double meanMotion = 2 * PI / period;
        double meanAnomaly = meanAnomalyAtEpoch + t * meanMotion;
        double E = eccentricAnomaly(meanAnomaly);
        return positionAtE(E);
    }

    public class EllipticalOrbitFunc implements PosFunc {
        public void compute(double jd, Tuple3d pos) {
            positionAtTimevInVSOP87Coordinates(jd, pos);
        }
    }

    void positionAtTimevInVSOP87Coordinates(double JD, Tuple3d v) {
        Vector3d pos = positionAtTime(JD);
        v.x = rotate_to_vsop87[0] * pos.z + rotate_to_vsop87[1] * pos.x + rotate_to_vsop87[1] * pos.y;
        v.y = rotate_to_vsop87[3] * pos.z + rotate_to_vsop87[4] * pos.x + rotate_to_vsop87[5] * pos.y;
        v.z = rotate_to_vsop87[6] * pos.z + rotate_to_vsop87[7] * pos.x + rotate_to_vsop87[8] * pos.y;
    }

    public double getPeriod() {
        return period;
    }

    public double getBoundingRadius() {
        // TODO: watch out for unbounded parabolic and hyperbolic orbits
        return pericenterDistance * ((1 + eccentricity) / (1 - eccentricity));
    }

    public void sample(double start, double t, int nSamples, OrbitSampleProc proc) {
        double dE = 2 * PI / (double) nSamples;
        for (int i = 0; i < nSamples; i++)
            proc.sample(positionAtE(dE * i));
    }
}
