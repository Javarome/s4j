/*
 * This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau
 * and is a Java version of the original Stellarium C++ version,
 * (http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/sky_localizer.h?rev=1.4&view=markup
    http://cvs.sourceforge.net/viewcvs.py/stellarium/stellarium/src/sky_localizer.cpp?rev=1.7&view=markup)
 * which is Copyright (c) 2004 Robert Spearman
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
package org.stellarium;

import org.stellarium.data.IniFileParser;
import org.stellarium.data.ResourceLocatorUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class SkyLocalizer {

    private static SkyLocalizer instance;

    public static SkyLocalizer getInstance() {
        return instance;
    }

    SkyLocalizer() throws StellariumException {
        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        URL skyCulturesDir = ResourceLocatorUtil.getInstance().getSkyCulturesDir();
        //File[] entries = locatorUtil.listSkyCultures();
        try {
            URL[] entries = {new URL(skyCulturesDir.toExternalForm() + "/western")};
            for (URL entryp : entries) {
                URL tmpFic = null;
                final String folderName = entryp.toExternalForm().substring(entryp.toExternalForm().lastIndexOf("/") + 1);
                tmpFic = new URL(entryp.toExternalForm() + "/info.ini");
                //if (tmpFic.exists()) {
                IniFileParser conf = new IniFileParser(getClass(), tmpFic);
                final String description = conf.getStr(IniFileParser.INFO_SECTION, IniFileParser.NAME);
                dirToName.put(folderName, description);
                nameToDir.put(description, folderName);
                //}
            }
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }

        instance = this;
    }

    /**
     * Get the list of human readable culture names in english
     */
    public List<String> getSkyCultureList() {
        final ArrayList<String> result = new ArrayList<String>(nameToDir.keySet());
        Collections.sort(result);
        return result;
    }

    public String getFolderName(String skyCultureName) {
        return nameToDir.get(skyCultureName);
    }

    public String getSkyCultureName(String folderName) {
        return dirToName.get(folderName);
    }

    // TODO: Find the map between Locale and sky_cultures folder name
    private Map<String, String> nameToDir = new HashMap<String, String>();

    private Map<String, String> dirToName = new HashMap<String, String>();
}