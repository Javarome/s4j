package org.stellarium;

import org.osgi.framework.BundleActivator;

/**
 * A component that can be plugged into S4J.
 *
 * @author @author <a href="mailto:rr0@rr0.org"/>J&eacute;r&ecirc;me Beau</a>
 * @version 2.0.0
 * @since 2.0.0
 */
public interface StelComponent extends BundleActivator {
    /**
     * Redraw the component.
     *
     * @param deltaTime The time that has elapsed
     */
    void draw(int deltaTime);
}
