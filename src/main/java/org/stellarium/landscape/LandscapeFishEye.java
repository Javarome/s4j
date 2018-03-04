/*
 * Stellarium
 * Copyright (C) 2003 Fabien Chereau
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.stellarium.landscape;

import org.stellarium.Navigator;
import org.stellarium.StellariumException;
import org.stellarium.ToneReproductor;
import org.stellarium.data.IniFileParser;
import org.stellarium.projector.DefaultProjector;
import org.stellarium.ui.render.STexture;

import java.net.URL;
import java.util.logging.Logger;

import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

class LandscapeFishEye extends Landscape {

    /**
     * Default constructor with radius of 1
     */
    LandscapeFishEye(Logger parentLogger) {
        super(1, parentLogger);
    }

    LandscapeFishEye(float _radius, Logger parentLogger) {
        super(_radius, parentLogger);
        mapTex = null;
    }

    protected void load(URL landscapeFile, String section_name) throws StellariumException {
        IniFileParser pd = loadCommon(landscapeFile, section_name);

        String type;
        type = pd.getStr(section_name, IniFileParser.TYPE);
        validLandscape = "fisheye".equals(type);
        if (!validLandscape) {
            logger.severe("No valid landscape definition found for " + section_name + ". No landscape in use.");
            throw new StellariumException("Landscape type mismatch for landscape " + section_name +
                    ", expected fisheye, found " + type + ".  No landscape in use.");
        }

        create(name, false, pd.getStr(section_name, "maptex"), pd.getDouble(section_name, "texturefov", 360));
    }

    // create a fisheye landscape from basic parameters (no ini file needed)
    void create(String name, boolean fullpath, String mapTex, double textureFieldOfView) throws StellariumException {
        //	cout << _name << " " << _fullpath << " " << _maptex << " " << _texturefov << "\n";
        validLandscape = true;// assume ok...
        this.name = name;
        this.mapTex = textureFactory.createTexture(fullpath, mapTex, STexture.TEX_LOAD_TYPE_PNG_ALPHA, false);
        this.texFOV = (float) Math.toRadians(textureFieldOfView);
    }

    public void draw(ToneReproductor eye, DefaultProjector prj, Navigator nav) {
        if (!validLandscape) {
            return;
        }
        if (!landFader.hasInterstate()) {
            return;
        }

        // Normal transparency mode
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glColor3f(skyBrightness, skyBrightness, skyBrightness);

        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mapTex.getID());
        prj.sSphereMap(radius, 40, 20, nav.getLocalToEyeMat(), texFOV, true);

        glDisable(GL_CULL_FACE);
    }

    private STexture mapTex;

    private float texFOV;
}