/*
 * Stellarium
 * This file Copyright (C) 2005 Robert Spearman
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

import javax.vecmath.Vector3d;
import java.util.Map;

class SettingMgr {

    void set(String name, String value) {
        configString.put(name, value);

        // attempt to parse into double and vector as well
        String vstr = value + configDouble.get(name);

        char tmp = 0;
        Vector3d vector3d = configVector.get(name);
        String tstr = value + vector3d.x + tmp + vector3d.y + tmp + vector3d.z;
    }

    void set(String name, double value) {
        configDouble.put(name, value);

        String oss = "" + value;
        configString.put(name, oss);
    }

    void set(String name, Vector3d value) {
        String oss = value.x + "," + value.y + "," + value.z;
        configString.put(name, oss);
        configVector.put(name, value);
    }

    void print_values() {
        for (Map.Entry iter : configString.entrySet()) {
            System.out.println(iter.getKey() + " : " + iter.getValue());
        }
    }

    /**
     * return as runnable commands for use with scripts
     *
     * @param out
     * @return
     */
    String createCommands(String out) {
        // TODO: more precision on doubles as strings

        for (Map.Entry iter : configString.entrySet()) {
            out += "set " + iter.getKey() + " " + iter.getValue();
        }
        return out;
    }

    String get_string(String name) {
        return configString.get(name);
    }

    double get_double(String name) {
        return configDouble.get(name);
    }

    Vector3d get_vector(String name) {
        return configVector.get(name);
    }

    boolean is_true(String name) {
        return configDouble.get(name) != null;
    }

    private Map<String, String> configString;
    private Map<String, Double> configDouble;
    private Map<String, Vector3d> configVector;

}