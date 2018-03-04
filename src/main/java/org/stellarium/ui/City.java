/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:13:14 AM
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


public class City {
    public City(String name, String state, String country, double latitude, double longitude, float zone, int showatzoom, int altitude) {
        this.name = name;
        this.state = state;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zone = zone;
        this.showatzoom = showatzoom;
        this.altitude = altitude;
    }

    public void addCity(String _name, String _state, String _country,
                        double _longitude, double _latitude, float zone, int _showatzoom, int _altitude) {

    }

    public String getName() {
        return name;
    }

    public String getNameI18() {
        return getName();
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getZone() {
        return zone;
    }

    public int getShowatzoom() {
        return showatzoom;
    }

    public int getAltitude() {
        return altitude;
    }

    private String name;

    private String state;

    private String country;

    private double latitude;

    private double longitude;

    private float zone;

    private int showatzoom;

    private int altitude;

}
