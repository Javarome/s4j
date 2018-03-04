package org.stellarium.functional;

/**
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @version $revision$
 */
public abstract interface UnaryFunction {
    /**
     * Perform the function.
     *
     * @param d The variable
     * @return The function result
     */
    double operator(double d);
}
