/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:05:12 AM
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
package org.stellarium.ui.components;

import org.stellarium.StellariumException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;


/**
 * Widget used to set time zone. Initialized from a file of Type /usr/share/zoneinfo/zone.tab
 */
public class TimeZoneItem extends StelContainer {
    /**
     * Builds a new TimeZoneItem
     *
     * @param zonetabFile The file name
     * @throws org.stellarium.StellariumException
     *          If the file name is empty, or if the file could not be read
     */
    TimeZoneItem(String zonetabFile) throws StellariumException {
        if (zonetabFile == null || zonetabFile.length() <= 0) {
            throw new StellariumException("Can't create a TimeZoneItem from an empty file name");
        }
        continentsNames.setSize(100, 150);
        try {
            BufferedReader is = new BufferedReader(new FileReader(zonetabFile));
            try {
                String zoneline;
                int i;

                while ((zoneline = is.readLine()) != null) {
                    if (zoneline.charAt(0) == '#') continue;
                    String tzname = zoneline;
                    i = tzname.indexOf("/");
                    String s = tzname.substring(0, i);
                    String newitem = tzname.substring(i + 1, tzname.length());
                    StringList stringList = continents.get(s);
                    if (stringList == null) {
                        stringList = new StringList();
                        continents.put(s, stringList);
                        stringList.addItem(newitem);
                        stringList.setPos(105, 0);
                        stringList.setSize(150, 150);
                        stringList.setOnPressCallback(new StelCallback() {
                            public void execute() {
                                onCityClic();
                            }
                        });
                        continentsNames.addItem(s);
                    } else {
                        stringList.addItem(newitem);
                    }
                }
            } finally {
                is.close();
            }
            size.i0 = continentsNames.getSizeX() * 4;
            size.i1 = continentsNames.getSizeY();

            continentsNames.setOnPressCallback(new StelCallback() {
                public void execute() {
                    onContinentClic();
                }
            });

            addComponent(continentsNames);
            addComponent(continents.get(continentsNames.getValue()));
        } catch (IOException e) {
            throw new StellariumException("Could not create TimeZoneItem due to I/O error: ", e);
        }
    }

    public void draw() {
        if (!visible) return;
        //painter.drawSquareEdge(pos, size);
        draw();
    }

    String gettz() {
        StringList stringList = continents.get(continentsNames.getValue());
        if (stringList != null)
            return continentsNames.getValue() + "/" + stringList.getValue();
        else return continentsNames.getValue() + "/error";
    }

    void setTz(String tz) {
        int i = tz.indexOf("/");
        continentsNames.setValue(tz.substring(0, i));
        continents.get(continentsNames.getValue()).setValue(tz.substring(i + 1, tz.length()));
    }

    void onContinentClic() {
        removeAllComponents();
        addComponent(continentsNames);
        addComponent(continents.get(continentsNames.getValue()));
        if (onPressCallback != null) {
            onPressCallback.execute();
        }
    }

    void onCityClic() {
        if (onPressCallback != null) {
            onPressCallback.execute();
        }
    }

    protected StringList continentsNames;

    protected Map<String, StringList> continents;

    protected StringList currentEdit;

    protected StringList lb;
}
