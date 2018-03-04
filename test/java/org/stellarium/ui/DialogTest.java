package org.stellarium.ui;

import junit.framework.TestCase;
import org.osgi.framework.BundleContext;
import org.stellarium.StelApp;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.ui.dialog.ConfigDialog;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Jérôme Beau
 * @version 12 nov. 2006 00:01:22
 */
public class DialogTest extends TestCase {

    public void testMap() throws Exception {
        JFrame jFrame = new JFrame("Config dialog test");
        List<String> args = Arrays.asList("--home=C:\\Projet\\stellarium4java\\stellarium4java\\resources");
        ResourceLocatorUtil.getInstance().init(args, Logger.getAnonymousLogger());
        StelApp mockApp = new StelApp(Logger.getAnonymousLogger()) {

            public void startMainLoop() {
            }

            public void stop(BundleContext bundleContext) throws Exception {
            }

            public void start(BundleContext bundleContext) throws Exception {
            }

            public FontFactory getFontFactory() {
                return null;
            }
        };
        ConfigDialog configDialog = new ConfigDialog(jFrame, mockApp, Logger.getAnonymousLogger()) {
            protected void buildUI() {
                JPanel locationTab = createLocationConfigTab();
                add(locationTab);
            }
        };
        configDialog.setVisible(true);
    }
}
