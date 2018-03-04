package org.stellarium.ui;

import junit.framework.TestCase;
import org.stellarium.StelApp;

import java.util.logging.Logger;

/**
 * @author Jerome Beau
 * @version 2.0
 * @since 1.0
 */
public class VideoTest extends TestCase {

    public void testInit() throws Exception {
        StelApp app = new StelApp(Logger.getAnonymousLogger());
        //app.initSDL(1024, 768, 0, false);
    }
}
