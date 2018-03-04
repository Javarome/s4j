package org.stellarium;

/**
 * An error occuring when executing a Stellarium command.
 *
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @version $revision$
 */
public class StellariumCommandException extends StellariumException {

    public StellariumCommandException(String commandline, String string) {
        super("Could not execute command: \n\"" + commandline + "\"\n\n" + string);
    }
}