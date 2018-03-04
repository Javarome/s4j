package org.stellarium.landscape;

import junit.framework.TestCase;
import org.stellarium.ApplicationCallback;
import org.stellarium.Navigator;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.ui.FontFactory;

import java.util.logging.Logger;

/**
 * @author Jerome Beau
 * @version 1 dec. 2006 00:08:14
 */
public class LandscapeTest extends TestCase {
    protected ResourceLocatorUtil rlu = ResourceLocatorUtil.getInstance();

    protected void setUp() throws Exception {
        rlu.init(null, Logger.getAnonymousLogger());
    }

    public void testOldStyle() {
        ApplicationCallback applicationCallback = new ApplicationCallback() {
            public void recordCommand(String str) {
            }

            public void orderRepaint() {
            }

            public void startPointer(DefaultProjector projection, Navigator navigation) {
            }

            public void stopPointer() {
            }

            public void quit() {
            }

            public FontFactory getFontFactory() {
                return null;
            }
        };
        LandscapeOldStyle landscape = new LandscapeOldStyle(Logger.getAnonymousLogger());
        landscape.load(rlu.getDataFile("landscapes.ini"), "guereins");
    }
}
