/*
 * User: freds
 * Date: Nov 11, 2006
 * Time: 10:39:28 PM
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

import junit.framework.TestCase;

import java.util.*;


public class DateTimeTest extends TestCase {
    public void testTimeZoneProperty() throws Exception {
        Properties properties = System.getProperties();
        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            System.out.println(" " + entry.getKey() + "=" + entry.getValue());
        }

        Calendar defCalendar = Calendar.getInstance();
        TimeZone timeZone = defCalendar.getTimeZone();
        System.out.println(" Default Time Zone:" + timeZone);

        String[] timeZones = TimeZone.getAvailableIDs();
        for (int i = 0; i < timeZones.length; i++) {
            String zone = timeZones[i];
            System.out.println(" Time Zone " + i + " = " + zone);
        }

        String londonId = "Europe/London";
        TimeZone.setDefault(TimeZone.getTimeZone(londonId));

        defCalendar = Calendar.getInstance();
        timeZone = defCalendar.getTimeZone();
        System.out.println(" Default Time Zone:" + timeZone);

        String mitId = "MIT";
        TimeZone.setDefault(TimeZone.getTimeZone(mitId));

        defCalendar = Calendar.getInstance();
        timeZone = defCalendar.getTimeZone();
        System.out.println(" Default Time Zone:" + timeZone);
    }
}
