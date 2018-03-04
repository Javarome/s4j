package org.stellarium.ui;

import com.apple.eawt.event.GestureUtilities;
import com.apple.eawt.event.MagnificationEvent;
import com.apple.eawt.event.MagnificationListener;
import org.stellarium.StelApp;
import org.stellarium.StelCore;
import org.stellarium.StellariumException;
import org.stellarium.data.IniFileParser;

import javax.media.opengl.GLCanvas;
import javax.swing.*;
import java.util.logging.Logger;

/**
 * A Graphical UI that leverages Mac OS-specific features
 */
public class MacSwingUI extends SwingUI implements MagnificationListener {
    public MacSwingUI(StelCore someCore, StelApp someApp, Logger parentLogger) throws StellariumException {
        super(someCore, someApp, parentLogger);
    }

    @Override
    protected JPanel createDesktop(IniFileParser conf, GLCanvas someGlCanvas) {
        JPanel desktop = super.createDesktop(conf, someGlCanvas);
        GestureUtilities.addGestureListenerTo(desktop, this);
        return desktop;
    }

    public void magnify(MagnificationEvent e) {
        double magnification = e.getMagnification();
        double aimedFOV = core.getAimFov();
        double delta = -magnification * 30 * aimedFOV / 60.0;
        core.zoomTo(aimedFOV + delta, 0.1);
        e.consume();
    }
}
