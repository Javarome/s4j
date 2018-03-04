/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:17:06 AM
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
package org.stellarium.ui;

import java.util.ArrayList;
import java.util.List;


public class CityMgr {
    public static final int CITIES_PROXIMITY = 10;

    public CityMgr() {
        this(CITIES_PROXIMITY);
    }

    public CityMgr(double proximity) {
        this.proximity = proximity;
    }

    public void addCity(String _name, String _state, String _country,
                        double _longitude, double _latitude, float _zone, int _showatzoom) {
        addCity(_name, _state, _country, _longitude, _latitude, _zone, _showatzoom, 0);
    }

    public void addCity(String _name, String _state, String _country,
                        double _longitude, double _latitude, float _zone, int _showatzoom, int _altitude) {
        City city = new City(_name, _state, _country, _latitude, _longitude, _zone, _showatzoom, _altitude);
        cities.add(city);
    }

    public int getNearest(double _longitude, double _latitude) {
        double dist, closest = 10000000.;
        int index = -1;
        int i = 0;

        if (cities.isEmpty()) return -1;

        for (City city : cities) {
            double latDiff = _latitude - city.getLatitude();
            double longDiff = _longitude - city.getLongitude();
            dist = (latDiff * latDiff) + (longDiff * longDiff);
            if (index == -1) {
                closest = dist;
                index = i;
            } else if (dist < closest) {
                closest = dist;
                index = i;
            }
            i++;
        }
        if (closest < proximity)
            return index;
        else
            return -1;
    }

    public void setProximity(double someProximity) {
        if (someProximity < 0) {
            proximity = CITIES_PROXIMITY;
        } else {
            proximity = someProximity;
        }
    }

    public City getCity(int _index) {
        if (_index < 0 || _index >= cities.size()) {
            return null;
        } else {
            return cities.get(_index);
        }
    }

    public int size() {
        return cities.size();
    }

    private List<City> cities = new ArrayList<City>();

    private double proximity;
}
