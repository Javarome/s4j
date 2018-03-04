/*
 * Stellarium
 * Copyright (C) 2002 Fabien Chereau
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

import org.stellarium.astro.Planet;
import org.stellarium.astro.SolarSystem;
import org.stellarium.data.IniFileParser;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author <a href="mailto:rr0@rr0.org"/>Jérôme Beau</a>, Fred Simon
 * @version Java 1.6
 */
public class Observator {
    protected final Logger logger;

    Observator(SolarSystem someSolarSystem, Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }

        solarSystem = someSolarSystem;
    }

    Point3d getCenterVsop87Pos() {
        return planet.getHeliocentricEclipticPos();
    }

    double getDistanceFromCenter() {
        return planet.getRadius() + (altitude / (1000 * StelUtility.AU));
    }

    Matrix4d getRotLocalToEquatorial(double jd) {
        double lat = latitude;
        // TODO: Figure out how to keep continuity in sky as reach poles
        // otherwise sky jumps in rotation when reach poles in equatorial mode
        // This is a kludge
        if (lat > 89.5) {
            lat = 89.5;
        } else if (lat < -89.5) {
            lat = -89.5;
        }
        Matrix4d result = new Matrix4d();
        result.rotZ(Math.toRadians(planet.getSiderealTime(jd) + longitude));
        Matrix4d tmp = new Matrix4d();
        tmp.rotY(Math.toRadians(90 - lat));
        result.mul(tmp);
        return result;
    }

    public Matrix4d getRotEquatorialToVsop87() {
        return planet.getRotEquatorialToVsop87();
    }

    boolean setHomePlanet(String englishName) {
        Planet p = solarSystem.searchByEnglishName(englishName);
        if (p != null) {
            planet = p;
            return true;
        }
        return false;
    }

    public void load(IniFileParser conf, String section) {
        name = conf.getStr(section, IniFileParser.NAME);
        name = name.replace('_', ' ');

        if (!setHomePlanet(conf.getStr(section, "home_planet", "Earth"))) {
            planet = solarSystem.getEarth();
        }

        logger.config("Loading location: \"" + name + "\", ");

        latitude = StelUtility.getDecAngle(conf.getStr(section, IniFileParser.LATITUDE));
        longitude = StelUtility.getDecAngle(conf.getStr(section, IniFileParser.LONGITUDE));
        altitude = conf.getInt(section, "altitude");
        setLandscapeName(conf.getStr(section, "landscape_name", "sea"));

        logger.config("Location landscape is: \"" + landscapeName + "\"");

        /*
                String tzstr = conf.getStr(section, "time_zone");
                if ("system_default".equals(tzstr)) {
                    timeZoneMode = StelApp.TZ_FORMAT.SYSTEM_DEFAULT;
                    // Set the program global intern timezones variables from the system locale
                    tzset();
                } else {
                    if ("gmt+x".equals(tzstr)) // TODO : handle GMT+X timezones form
                    {
                        timeZoneMode = StelApp.TZ_FORMAT.GMT_SHIFT;
                        // GMT_shift = x;
                    } else {
                        // We have a custom time zone name
                        timeZoneMode = StelApp.TZ_FORMAT.CUSTOM;
                        setCustomTzName(tzstr);
                    }
                }

                timeFormat = stringToSTimeFormat(conf.getStr(section, "time_display_format"));
                dateFormat = stringToSDateFormat(conf.getStr(section, "date_display_format"));
        */
    }

    public void save(IniFileParser conf, String section) throws StellariumException {
        setConf(conf, section);

        /*
                if (timeZoneMode == StelApp.TZ_FORMAT.CUSTOM) {
                    conf.setStr(section + ":time_zone", customTzName);
                }
                if (timeZoneMode == StelApp.TZ_FORMAT.SYSTEM_DEFAULT) {
                    conf.setStr(section + ":time_zone", "system_default");
                }
                if (timeZoneMode == StelApp.TZ_FORMAT.GMT_SHIFT) {
                    conf.setStr(section + ":time_zone", "gmt+x");
                }

                conf.setStr(section + ":time_display_format", getTimeFormatStr());
                conf.setStr(section + ":date_display_format", getDateFormatStr());
        */

        conf.flush();
    }

    /**
     * change settings but don't write to files
     */
    public void setConf(IniFileParser conf, String section) {
        Preferences sect = conf.getSection(section);
        sect.put(IniFileParser.NAME, name);
        sect.put("home_planet", planet.getEnglishName());
        sect.put(IniFileParser.LATITUDE, StelUtility.printAngleDms(Math.toRadians(latitude), true, true));
        sect.put(IniFileParser.LONGITUDE, StelUtility.printAngleDms(Math.toRadians(longitude), true, true));
        sect.putInt("altitude", altitude);
        sect.put("landscape_name", landscapeName);

        // TODO: clear out old timezone settings from this section
        // if still in loaded conf?  Potential for confusion.
    }

    /**
     * Move gradually to a new observation location
     *
     * @param latitude
     * @param longitude
     * @param altitude
     * @param duration
     */
    public void moveTo(double latitude, double longitude, double altitude, int duration, String _name) {
        flagMoveTo = true;

        startLatitude = this.latitude;
        endLatitude = latitude;

        startLongitude = this.longitude;
        endLongitude = longitude;

        startAltitude = altitude;
        endAltitude = altitude;

        moveToCoef = 1.0f / duration;
        moveToMult = 0;

        name = _name;
        //  printf("coef = %f\n", move_to_coef);
    }

    public String getName() {
        return name;
    }

    public String getHomePlanetEnglishName() {
        return planet != null ? planet.getEnglishName() : "";
    }

    public String getHomePlanetNameI18n() {
        return planet != null ? planet.getNameI18n() : "";
    }

    /**
     * for moving observator position gradually
     *
     * @param deltaTime
     */
    // TODO need to work on direction of motion...
    void update(long deltaTime) {
        if (flagMoveTo) {
            moveToMult += moveToCoef * deltaTime;

            if (moveToMult >= 1) {
                moveToMult = 1;
                flagMoveTo = false;
            }

            latitude = startLatitude - moveToMult * (startLatitude - endLatitude);
            longitude = startLongitude - moveToMult * (startLongitude - endLongitude);
            altitude = (int) (startAltitude - moveToMult * (startAltitude - endAltitude));
        }
    }

    public Planet getHomePlanet() {
        return planet;
    }

    public void setLatitude(double l) {
        latitude = l;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude(double l) {
        longitude = l;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setAltitude(int a) {
        altitude = a;
    }

    public int getAltitude() {
        return altitude;
    }

    void setLandscapeName(String s) {
        landscapeName = s.toLowerCase();
    }

    public String getLandscapeName() {
        return landscapeName;
    }

    private final SolarSystem solarSystem;

    /**
     * Position name
     */
    private String name = "Anonymous_Location";

    private Planet planet;

    /**
     * Longitude in degree
     */
    double longitude;

    /**
     * Latitude in degree
     */
    double latitude;

    /**
     * Altitude in meter
     */
    int altitude;

    String landscapeName;

    /**
     * for changing position
     */
    private boolean flagMoveTo;

    private double startLatitude;

    private double endLatitude;

    private double startLongitude;

    private double endLongitude;

    private double startAltitude;

    private double endAltitude;

    private double moveToCoef;

    private double moveToMult;
}