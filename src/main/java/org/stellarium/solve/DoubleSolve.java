//
// Copyright (C) 2001, Chris Laurel <claurel@shatters.net>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

package org.stellarium.solve;

import org.stellarium.functional.UnaryFunction;

/**
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @version Java
 */
public class DoubleSolve {

    public interface Pair {
    }

    public static class DoublePair implements Pair {
        public double first;
        public double second;

        public DoublePair(double d1, double d2) {
            this.first = d1;
            this.second = d2;
        }
    }

    /**
     * DoubleSolve a function using the bisection method.  Returns a pair
     * with the solution as the first element and the error as the second.
     */
    public static DoublePair solve_bisection(UnaryFunction f, double lower, double upper, double err, int maxIter) {
        double x = 0.0;

        for (int i = 0; i < maxIter; i++) {
            x = (lower + upper) * 0.5;
            if (upper - lower < 2 * err)
                break;

            double y = f.operator(x);
            if (y < 0)
                lower = x;
            else
                upper = x;
        }

        return new DoublePair(x, (upper - lower) / 2);
    }

    /**
     * DoubleSolve using iteration; terminate when error is below err or the maximum
     * number of iterations is reached.
     */
    public static DoublePair solve_iteration(UnaryFunction f, double x0, double err, int maxIter) {
        double x = 0;
        double x2 = x0;

        for (int i = 0; i < maxIter; i++) {
            x = x2;
            x2 = f.operator(x);
            if (Math.abs(x2 - x) < err)
                return new DoublePair(x2, x2 - x);
        }

        return new DoublePair(x2, x2 - x);
    }

    /**
     * DoubleSolve using iteration method and a fixed number of steps.
     */
    public static DoublePair solveIterationFixed(UnaryFunction f, double x0, int maxIter) {
        double x = 0;
        double x2 = x0;

        for (int i = 0; i < maxIter; i++) {
            x = x2;
            x2 = f.operator(x);
        }

        return new DoublePair(x2, x2 - x);
    }
}
