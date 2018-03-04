package org.stellarium;

/**
 * An exception that denotes a Stellarium for Java error.
 *
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @version $revision$
 */
public class StellariumException extends RuntimeException {
    public StellariumException() {
    }

    public StellariumException(String string) {
        super(string);
    }

    public StellariumException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public StellariumException(Throwable throwable) {
        super(throwable);
    }
}
