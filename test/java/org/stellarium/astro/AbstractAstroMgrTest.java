/*
 * User: freds
 * Date: Nov 9, 2006
 * Time: 12:13:29 AM
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
package org.stellarium.astro;

import org.stellarium.StellariumException;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.ui.FontFactory;
import org.stellarium.ui.render.SFontFactory;
import org.stellarium.ui.render.SFontIfc;

import java.awt.*;
import java.util.logging.Logger;


public abstract class AbstractAstroMgrTest extends AbstractStellariumTest {
    protected HipStarMgr hipStarMgr;
    protected ResourceLocatorUtil rlu = ResourceLocatorUtil.getInstance();
    private FontFactory fontFactory;

    protected void setUp() throws Exception {
        super.setUp();
        rlu.init(getCommandLineArgs(), Logger.getAnonymousLogger());
        fontFactory = new SFontFactory(new Dimension(1024, 768));
    }

    protected void createAndLoadHipStar() throws StellariumException {
        hipStarMgr = new HipStarMgr(Logger.getAnonymousLogger());
        //LoadingBar bar = new LoadingBar(null,0.3f,"starfont","Coucou",200,200,"extra",22f,10f,10f);
        SFontIfc hipStarFont = fontFactory.create(11, "starfont");
        hipStarMgr.init(hipStarFont,
                rlu.getDataFile("hipparcos.fab"),
                rlu.getSkyCultureFile("western", "star_names.fab"),
                rlu.getDataFile("name.fab"));
    }
}
