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

import org.stellarium.astro.JulianDay;

import javax.vecmath.*;
import java.awt.*;
import static java.lang.StrictMath.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_utility.h?view=markup">C++ header of this file</>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_utility.cpp?view=markup&pathrev=stellarium-0-8-2">C++ implementation of this file</>
 */
public class StelUtility {

    public static final double M_PI = PI;

    public static final double M_PI_2 = 1.5707963267948965580;

    public static final double M_PI_4 = M_PI_2 / 2;

    /**
     * Astronomical Unit
     */
    public static final double AU = 149597870.691;
    private static SimpleDateFormat commandDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:s");

    double hmsToRad(int h, int m, double s) {
        return (2 * h * PI) / 24. + (m * PI) / (12. * 60.) + (s * PI) / 43200.;
    }

    double dmsToRad(int d, int m, double s) {
        double t = (d * PI) / 180. + (m * PI) / 10800. + (s * PI) / 648000.;
        return abs(t) * d / abs(d);
    }

    static double hmsToRad(int h, double m) {
        return (2 * h * PI) / 24. + (m * PI) / (12. * 60.);
    }

    static double dmsToRad(int d, double m) {
        double t = (d * PI) / 180. + (m * PI) / 10800.;
        return abs(t) * d / abs(d);
    }

    public static void spheToRect(double lng, double lat, Tuple3d v) {
        final double cosLat = cos(lat);
        v.set(cos(lng) * cosLat, sin(lng) * cosLat, sin(lat));
    }

    public static void spheToRect(double lng, double lat, double r, Tuple3d v) {
        final double cosLat = cos(lat);
        v.set(cos(lng) * cosLat * r, sin(lng) * cosLat * r, sin(lat) * r);
    }

    //    static void spheToRect(double lng, double lat, Vector3f v) {
    //        final double cosLat = cos(lat);
    //        v.set((float) (Math.cos(lng) * cosLat), (float) (Math.sin(lng) * cosLat), (float) sin(lat));
    //    }

    /**
     * Convert String int ISO 8601-like format [+/-]YYYY-MM-DDThh:mm:ss (no timzone offset) to julian day
     * TODO: move to better location for reuse
     *
     * @param dateString The string date
     * @return The Julian date
     * @throws StellariumException If the date could not be successfuly parsed.
     */
    public static double stringToJDay(String dateString) throws StellariumException {
        dateString = dateString.replace('T', ' ');
        try {
            Date date = commandDateFormat.parse(dateString);
            int year = 1900 + date.getYear();
            int month = 1 + date.getMonth();
            int day = date.getDate();
            int hour = date.getHours();
            int minute = date.getMinutes();
            int second = date.getSeconds();

            // bounds checking (per STUI time object)
            if (year > 100000 || year < -100000 || month < 1 || month > 12 || day < 1 || day > 31 || hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                throw new StellariumException("Error parsing date.");
            }

            // code taken from STUI.cpp
            if (month <= 2) {
                year--;
                month += 12;
            }

            // Correct for the lost days in Oct 1582 when the Gregorian calendar replaced the Julian calendar.
            int b = -2;
            if (year > 1582 || (year == 1582 && (month > 10 || (month == 10 && day >= 15)))) {
                b = year / 400 - year / 100;
            }

            return floor(365.25 * year) + floor(30.6001 * (month + 1)) + b + 1720996.5 + day + hour / 24.0 + minute / 1440.0 + second / 86400.0;
        } catch (ParseException e) {
            throw new StellariumException(e);
        }
    }

    public static void normalize(Point3d point3d) {
        point3d.scale(1 / getLength(point3d));
    }

    /**
     * Encapsulate longitude/latitude coordinates that were returned by reference in the C++ version.
     * The DE is the declinaison matches a latitude
     * The RA is the Right ascention matches a longitude
     */
    public static class Coords {
        private double latitude;

        private double longitude;

        public Coords(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getDE() {
            return latitude;
        }

        public double getRA() {
            return longitude;
        }

        /**
         * Move Noth to zero, E is 90 degrees
         */
        public void northToZero() {
            longitude = 3 * PI - longitude;
            if (longitude > PI * 2)
                longitude -= PI * 2;
        }
    }

    public static Coords rectToSphe(final Tuple3d v) {
        double r = getLength(v);
        double lat = asin(v.z / r);
        double lng = atan2(v.y, v.x);
        return new Coords(lat, lng);
    }

    public static double getLength(Tuple3d v) {
        return sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
    }

    public static double getLengthSquared(Tuple3d v) {
        return (v.x * v.x + v.y * v.y + v.z * v.z);
    }

    //    static Coords rectToSphe(final Vector3f v) {
    //        double r = v.length();
    //        double lat = asin(v.z / r);
    //        double lng = atan2(v.y, v.x);
    //        return new Coords(lat, lng);
    //    }

    //    static Vector3f stringToVector3f(String s) {
    //        Object[] numbers = stringToNumbers(s);
    //        return new Vector3f(((Double) numbers[0]).floatValue(), ((Double) numbers[1]).floatValue(), ((Double) numbers[2]).floatValue());
    //    }

    /**
     * Obtains a Vector3d from a String with the form x,y,z
     */
    public static Vector3d stringToVector3d(String s) {
        return new Vector3d(stringToDoubles(s));
    }

    /**
     * Obtains a Point3d from a String with the form x,y,z
     */
    public static Point3d stringToPoint3d(String s) {
        return new Point3d(stringToDoubles(s));
    }

    /**
     * Obtains an array of Doubles from a String with the form x,y,z
     */
    static double[] stringToDoubles(String s) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(s.trim(), ",");
            double[] result = new double[4];
            for (int i = 0; i < result.length; i++) {
                if (tokenizer.hasMoreTokens())
                    result[i] = Double.parseDouble(tokenizer.nextToken());
                else
                    result[i] = 0;
            }
            return result;
        } catch (Exception e) {
            throw new StellariumException("Wrong Vector 3 double format for " + s, e);
        }
    }

    /**
     * Obtains a Color from a String with the form x,y,z
     */
    public static Color stringToColor(String s) {
        // TODO(JBE): Can't we use Color.decode(String) or Color.getColor(String) here?
        float red = 0;
        float green = 0;
        float blue = 0;
        float alpha = 1.0f;     // Opaque by default
        StringTokenizer tokenizer = new StringTokenizer(s.trim(), ",");
        if (tokenizer.hasMoreTokens()) {
            red = Math.min(Float.parseFloat(tokenizer.nextToken()), 1.0f);
            if (tokenizer.hasMoreTokens()) {
                green = Math.min(Float.parseFloat(tokenizer.nextToken()), 1.0f);
                if (tokenizer.hasMoreTokens()) {
                    blue = Math.min(Float.parseFloat(tokenizer.nextToken()), 1.0f);
                    if (tokenizer.hasMoreTokens()) {
                        alpha = Math.min(Float.parseFloat(tokenizer.nextToken()), 1.0f);
                    }
                }
            }
        }
        return new Color(red, green, blue, alpha);
    }

    static float[] stringToFloats(String s) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(s.trim(), ",");
            float[] result = new float[4];
            for (int i = 0; i < result.length; i++) {
                if (tokenizer.hasMoreTokens())
                    result[i] = Float.parseFloat(tokenizer.nextToken());
                else
                    result[i] = 0;
            }
            return result;
        } catch (Exception e) {
            throw new StellariumException("Wrong Vector 3 double format for " + s, e);
        }
    }

    //    static String vector3fToString(Vector3f v) {
    //        return v.x + "," + v.y + "," + v.z;
    //    }

    /**
     * @return A String from a Vector3d with the form x,y,z
     */
    public static String tuple3dToString(Tuple4f v) {
        return v.x + "," + v.y + "," + v.z;
    }

    /**
     * @return A String from a Vector3d with the form x,y,z
     */
    public static String tuple3fToString(Tuple3f v) {
        return v.x + "," + v.y + "," + v.z;
    }

    /**
     * @return A String from a Color with the form x,y,z
     */
    static String colorToString(Color v) {
        return v.getRed() + "," + v.getGreen() + "," + v.getBlue();
    }

    /**
     * Provide the luminance in cd/m^2 from the magnitude and the surface in arcmin^2
     */
    public static double magToLuminance(double mag, double surface) {
        return exp(-0.4f * 2.3025851f * (mag - (-2.5f * log10(surface)))) * 108064.73f;
    }

    /**
     * strips trailing whitespaces from buf.
     */
    static boolean iswhite(char c) {
        return (c) == ' ' || (c) == '\t';
    }

    /**
     * salta espacios en blanco
     */
    static String skipwhite(String s) {
        int i;
        for (i = 0; i < s.length() && iswhite(s.charAt(i)); i++) ;
        return s.substring(i);
    }

    enum Type {
        HOURS, DEGREES, LAT, LONG
    }

    public static double getDecAngle(String ptr) {
        if (ptr == null) {
            return 0;
        }
        boolean negative = false;
        String delim = " :.,;\u00BA�DdHhMm'\n\tNSEWnsew\"\u00B0";
        int dghh, minutes;
        double seconds = 0, pos;

        ptr = ptr.trim();
        ptr = skipwhite(ptr);

        /* The last letter has precedence over the sign */
        if (ptr.indexOf('S') >= 0 || ptr.indexOf('s') >= 0 || ptr.indexOf('W') >= 0 || ptr.indexOf('w') >= 0) {
            negative = true;
        }

        char firstChar = ptr.charAt(0);
        if ('+' == firstChar || '-' == firstChar) {
            negative = firstChar == '-' || negative;
            ptr = ptr.substring(1);
        }
        ptr = skipwhite(ptr);
        Type type;
        if ((ptr.indexOf('H') >= 0 && ptr.indexOf('H') < 3) || (ptr.indexOf('h') > 0 && ptr.indexOf('h') < 3)) {
            type = Type.HOURS;
        } else if (ptr.indexOf('S') >= 0 || ptr.indexOf('s') >= 0 || ptr.indexOf('N') >= 0 || ptr.indexOf('n') >= 0) {
            type = Type.LAT;
        } else {
            type = Type.DEGREES;/* unspecified, the caller must control it */
        }
        StringTokenizer strtok = new StringTokenizer(ptr, delim);
        if (strtok.hasMoreTokens()) {
            ptr = strtok.nextToken();
            dghh = java.lang.Integer.parseInt(ptr);
        } else {
            return 0;
        }

        if (strtok.hasMoreTokens()) {
            ptr = strtok.nextToken();
            minutes = java.lang.Integer.parseInt(ptr);
            if (minutes > 59) {
                return 0;
            }
        } else {
            return 0;
        }

        if (strtok.hasMoreTokens()) {
            ptr = strtok.nextToken(delim);
            ptr = ptr.replace(',', '.');
            seconds = Double.parseDouble(ptr);
            if (seconds > 60) {
                return 0;
            }
        }

        if (strtok.hasMoreTokens()) {
            ptr = strtok.nextToken(" \n\t");
            ptr = skipwhite(ptr);
            if ("S".equals(ptr) || "W".equals(ptr) || "s".equals(ptr) || "W".equals(ptr)) negative = true;
        }

        pos = dghh + minutes / 60.0 + seconds / 3600.0;
        if (type == Type.HOURS && pos > 24) {
            return 0;
        }
        if (type == Type.LAT && pos > 90) {
            return 0;
        } else if (pos > 180) {
            return 0;
        }
        if (negative) {
            pos = 0 - pos;
        }

        return pos;
    }

    /**
     * @param angle The angle to represent, in radians
     * @return
     */
    public static String printAngleDms(double angle) {
        return printAngleDms(angle, false, false);
    }

    /**
     * Print the passed angle with the format dd�mm'ss.ss(.ss)"
     *
     * @param angle    Angle in radian
     * @param decimals Define if 2 decimal must also be printed
     * @param useD     Define if letter "d" must be used instead of °
     * @return The corresponding string
     */
    public static String printAngleDms(double angle, boolean decimals, boolean useD) {
        StringBuffer buf = new StringBuffer();
        // char degsign = '°'; ???
        char degsign = '\u00B0';
        if (useD) {
            degsign = 'd';
        }

        angle *= 180. / M_PI;

        if (angle < 0) {
            angle *= -1;
            buf.append('-');
        } else {
            buf.append('+');
        }

        if (decimals) {
            int d = (int) (0.5 + angle * (60 * 60 * 100));
            int centi = d % 100;
            d /= 100;
            int s = d % 60;
            d /= 60;
            int m = d % 60;
            d /= 60;
            buf.append(d).append(degsign).append(m).append('\'').append(s).append(centi).append('\"');
        } else {
            int d = (int) (0.5 + angle * (60 * 60));
            int s = d % 60;
            d /= 60;
            int m = d % 60;
            d /= 60;
            buf.append(d).append(degsign).append(m).append('\'').append(s).append('\"');
        }
        return buf.toString();
    }

    public static String printAngleDmsStel(double location) {
        double deg = floor(location);
        double sec = 60.0 * (location - deg);
        if (sec <= 0.0) {
            sec *= -1;
        }
        double min = floor(sec);
        sec = 60.0 * (sec - min);
        StringBuffer buf = new StringBuffer();
        buf.append((int) deg).append("%+").append("\6").append((int) min).append("'%").append(sec).append("\"");
        return buf.toString();
    }

    /**
     * Obtains a human readable angle in the form: hhhmmmss.sss"
     */
    public static String printAngleHms(double angle) {
        return printAngleHms(angle, false);
    }

    public static String printAngleHms(double angle, boolean decimals) {
        StringBuffer buf = new StringBuffer();
        angle = angle % (2.0 * M_PI);
        if (angle < 0.0) {
            angle += 2.0 * M_PI; // range: [0..2.0*M_PI)
        }
        angle *= 12. / M_PI; // range: [0..24)
        if (decimals) {
            angle = 0.5 + angle * (60 * 60 * 100); // range:[0.5,24*60*60*100+0.5)
            if (angle >= (24 * 60 * 60 * 100)) {
                angle -= (24 * 60 * 60 * 100);
            }
            int h = (int) angle;
            int centi = h % 100;
            h /= 100;
            int s = h % 60;
            h /= 60;
            int m = h % 60;
            h /= 60;
            buf.append(h).append(" h ").append(m).append(" m ").append(s).append(" s ").append(centi);
        } else {
            angle = 0.5 + angle * (60 * 60); // range:[0.5,24*60*60+0.5)
            if (angle >= (24 * 60 * 60)) {
                angle -= (24 * 60 * 60);
            }
            int h = (int) angle;
            int s = h % 60;
            h /= 60;
            int m = h % 60;
            h /= 60;
            buf.append(h).append(" h ").append(m).append(" m ").append(s).append(" s ");
        }
        return buf.toString();
    }

    public static Point3d div(Point3d v, double d) {
        return new Point3d(v.x / d, v.y / d, v.z / d);
    }

    /**
     * Returns the dot product of 2 Tuple3d.
     *
     * @param v1 the other vector
     * @param v2 the other vector
     * @return the dot product of v1 and v2
     */
    public static double dot(Tuple3d v1, Tuple3d v2) {
        return (v1.x * v2.x) + (v1.y * v2.y) + (v1.z * v2.z);
    }

    public static double[] toArray(Tuple3d v) {
        double[] tmp = new double[3];
        tmp[0] = v.x;
        tmp[1] = v.y;
        tmp[2] = v.z;
        return tmp;
    }

    public static float[] toArray(Tuple3f v) {
        float[] tmp = new float[3];
        tmp[0] = v.x;
        tmp[1] = v.y;
        tmp[2] = v.z;
        return tmp;
    }

    public static float[] toArray(Tuple4f tuple4f) {
        float[] tmp = new float[4];
        tmp[0] = tuple4f.x;
        tmp[1] = tuple4f.y;
        tmp[2] = tuple4f.z;
        tmp[3] = tuple4f.w;
        return tmp;
    }

    /**
     * Use to remove a boring warning
     *
     * @param max The maximum number of characters to issue
     * @param fmt The time format pattern
     * @param tm  The date for format
     * @return The formatted text
     */
    public static String myStrFTime(int max, String fmt, Date tm) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(fmt);// TODO(JBE): Set format as constant attribute
        String formattedDate = dateFormat.format(tm);
        return formattedDate.substring(0, min(max, formattedDate.length()));
    }

    public static double getGMTShiftFromSystem(double jd) {
        return getGMTShiftFromSystem(jd, false);
    }

    /**
     * Return the time zone name taken from system locale
     *
     * @param jd Julian Day
     * @return
     */
    public static String getTimeZoneNameFromSystem(double jd) {
        // The timezone name depends on the day because of the summer time
        Date rawTime = JulianDay.julianToDate(jd);

        //Date timeinfo = localtime(rawTime);
        Date timeinfo = new Date(rawTime.getTime());
        return StelUtility.myStrFTime(254, "%Z", timeinfo);
    }

    /**
     * Return the number of hours to add to gmt time to get the local time in day jd
     * taking the parameters from system. This takes into account the daylight saving
     * time if there is. (positive for Est of GMT)
     * TODO : %z in strftime only works on GNU compiler
     * Fixed 31-05-2004 Now use the extern variables set by tzset()
     *
     * @param jd     Julian Day
     * @param _local
     */
    public static double getGMTShiftFromSystem(double jd, boolean _local) {
        // doesn't account for dst changes
        // TODO come up with correct and portable solution
        int timezone = (new Date()).getTimezoneOffset();
        return -(double) timezone / 3600;

        /*
        // correct, but not portable
        struct tm * timeinfo;

        if(!_local) {
          // jd is UTC
          struct tm rawtime;
          get_tm_from_julian(jd, &rawtime);
          time_t ltime = timegm(&rawtime);
          timeinfo = localtime(&ltime);
        } else {
          time_t rtime;
          rtime = get_time_t_from_julian(jd);
          timeinfo = localtime(&rtime);
        }

        static char heure[20];
        heure[0] = '\0';

        my_strftime(heure, 19, "%z", timeinfo);
        //	cout << heure << endl;

        //cout << timezone << endl;

        heure[5] = '\0';
        double min = 1.f/60.f * atoi(&heure[3]);
        heure[3] = '\0';
        return min + atoi(heure);
        */
    }

    /**
     * Return the time in ISO 8601 format that is : %Y-%m-%d %H:%M:%S
     *
     * @param jd The julian date
     */
    public static String getISO8601TimeUTC(double jd) {
        Date timeUTC = JulianDay.julianToDate(jd);
        return myStrFTime(254, "%Y-%m-%d %H:%M:%S", timeUTC);
    }

    /**
     * test for nullity and emptyness
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isZero(double d) {
        return abs(d) < 2 * Double.MIN_VALUE;
    }
}