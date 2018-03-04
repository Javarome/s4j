package org.stellarium.landscape;

import org.stellarium.Navigator;
import org.stellarium.StellariumException;
import org.stellarium.ToneReproductor;
import org.stellarium.data.IniFileParser;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.ui.render.STexture;

import javax.media.opengl.GL;
import java.net.URL;
import java.util.logging.Logger;

import static org.stellarium.ui.SglAccess.*;

// TODO: this class does not exists in Stelarium tag 0.7.1. Should it be removed?
class LandscapeSpherical extends Landscape {

    /**
     * Default constructor with radius of 1
     */
    LandscapeSpherical(Logger parentLogger) {
        this(1, parentLogger);
    }

    LandscapeSpherical(float _radius, Logger parentLogger) {
        super(_radius, parentLogger);
    }

    protected void load(URL landscapeFile, String sectionName) throws StellariumException {
        IniFileParser pd = new IniFileParser(getClass(), landscapeFile);// The landscape data ini file parser

        String type;
        type = pd.getStr(sectionName, IniFileParser.TYPE);
        name = pd.getStr(sectionName, IniFileParser.NAME);
        if (!"spherical".equals(type) || "".equals(name)) {
            throw new StellariumException("No valid landscape definition found for " + sectionName + ". No landscape in use.");
        } else {
            validLandscape = true;
        }

        create(name, false, pd.getStr(sectionName, "maptex"));
    }

    void create(String name, boolean fullPath, String maptex) throws StellariumException {
        validLandscape = true;
        this.name = name;
        this.mapTex = textureFactory.createTexture(fullPath, maptex, STexture.TEX_LOAD_TYPE_PNG_ALPHA, false);
    }

    public void draw(ToneReproductor eye, DefaultProjector prj, Navigator nav) {
        if (!validLandscape) {
            return;
        }
        if (landFader.getInterstate() == 0) {
            return;
        }

        // Normal transparency mode
        glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        glColor4f(skyBrightness, skyBrightness, skyBrightness, landFader.getInterstate());

        glEnable(GL.GL_CULL_FACE);
        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);
        glBindTexture(GL.GL_TEXTURE_2D, mapTex.getID());

        // TODO: verify that this works correctly for custom projections
        // seam is at East
        // TODO: This Method exists in 0.8.1 only
        //prj.sSphere(radius, 1.0, 40, 20, nav.getLocalToEyeMat(), true);

        glDisable(GL.GL_CULL_FACE);
    }

    private STexture mapTex;
}
