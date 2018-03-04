/*
 * Copyright (C) 2006 Frederic Simon
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
package org.stellarium.data;

import java.io.IOException;


public class HipData implements Comparable<HipData> {
    int hp;

    double ra;

    double de;

    int mag;

    double magnitude;

    byte type;

    double distance;

    public HipData() {
    }

    public HipData(int hp, double ra, double de, double mag, int type, double distance) {
        this.hp = hp;
        this.ra = ra;
        this.de = de;
        this.mag = -1;
        this.magnitude = mag;
        this.type = (byte) type;
        this.distance = distance;
    }

    public void fillData(int hp, ReverseDataInputStream dis) throws IOException {
        this.hp = hp;
        this.ra = dis.readFloat();
        this.de = dis.readFloat();
        this.mag = dis.readUnsignedShort();
        magnitude = (float) ((5. + mag) / 256);
        if (magnitude > 250) magnitude -= 256;
        this.type = dis.readByte();
        this.distance = dis.readFloat();
    }

    public int getHp() {
        return hp;
    }

    public double getRa() {
        return ra;
    }

    public double getDe() {
        return de;
    }

    public int getShortMag() {
        return mag;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public byte getType() {
        return type;
    }

    public double getDistance() {
        return distance;
    }

    public int compareTo(HipData o) {
        if (o.mag < mag)
            return -1;
        if (mag > o.mag)
            return 1;
        return hp - o.hp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HipData hipData = (HipData) o;

        if (Double.compare(hipData.de, de) != 0) return false;
        if (Double.compare(hipData.distance, distance) != 0) return false;
        if (hp != hipData.hp) return false;
        if (hipData.mag != mag) return false;
        if (Double.compare(hipData.ra, ra) != 0) return false;
        if (type != hipData.type) return false;

        return true;
    }

    public int hashCode() {
        return hp;
    }

}
