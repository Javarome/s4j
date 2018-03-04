/*
 * This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau
 * and is a Java version of the original Stellarium C++ version,
 * Author and Copyright of this file and of the stellarium telescope feature:
 * Johannes Gajdosik, 2006
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
package org.stellarium.telescope;

import org.stellarium.NavigatorIfc;
import org.stellarium.StelObject;
import org.stellarium.StelObjectBase;
import org.stellarium.StelUtility;
import org.stellarium.ui.render.STexture;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * @author Jerome Beau, Fred Simon
 * @version 0.8.2
 */
public abstract class Telescope extends StelObjectBase {
    static class PrintRADec {
        int rightAscensionInt;

        int declinationInt;

        public String toString() {
            StringBuffer o = new StringBuffer();
            int h = rightAscensionInt;
            int d = (int) Math.floor(0.5 + declinationInt * (360 * 3600 * 1000 / 4294967296.0));
            char dec_sign;
            if (d >= 0) {
                if (d > 90 * 3600 * 1000) {
                    d = 180 * 3600 * 1000 - d;
                    h += 0x80000000;
                }
                dec_sign = '+';
            } else {
                if (d < -90 * 3600 * 1000) {
                    d = -180 * 3600 * 1000 - d;
                    h += 0x80000000;
                }
                d = -d;
                dec_sign = '-';
            }
            h = (int) Math.floor(0.5 + h * (24 * 3600 * 10000 / 4294967296.0));
            final int ra_ms = h % 10000;
            h /= 10000;
            final int ra_s = h % 60;
            h /= 60;
            final int ra_m = h % 60;
            h /= 60;
            h %= 24;
            final int dec_ms = d % 1000;
            d /= 1000;
            final int dec_s = d % 60;
            d /= 60;
            final int dec_m = d % 60;
            d /= 60;
            o.append("ra = ")
                    // TODO: FRED concept of filler/size/number migrate to NumberFormat
                    .append(' ').append(2).append(h).append('h')
                    .append('0').append(2).append(ra_m).append('m')
                    .append('0').append(2).append(ra_s).append('.')
                    .append('0').append(4).append(ra_ms)
                    .append(" dec = ")
                    .append(((d < 10) ? " " : "")).append(dec_sign).append(d).append('d')
                    .append('0').append(2).append(dec_m).append('m')
                    .append('0').append(2).append(dec_s).append('.')
                    .append('0').append(3).append(dec_ms)
                    .append(' ');
            return o.toString();
        }
    }

    static class TelescopeDummy extends Telescope {

        public TelescopeDummy(String name, String params, Logger parentLogger) {
            super(name, parentLogger);
            desiredPos.x = XYZ.x = 1.0;
            desiredPos.y = XYZ.y = 0.0;
            desiredPos.z = XYZ.z = 0.0;
        }

        public boolean isConnected() {
            return true;
        }

        public boolean hasKnownPosition() {
            return true;
        }

        public STexture getPointer() {
            return null;
        }

        public Point3d getObsJ2000Pos(NavigatorIfc nav) {
            return XYZ;
        }

        public void handleSelectFds() {
        }

        public void prepareSelectFds() {
            XYZ.scale(31.0);
            XYZ.add(desiredPos);
            double lq = StelUtility.getLengthSquared(XYZ);
            if (lq > 0.0) XYZ.scale(1.0 / Math.sqrt(lq));
            else XYZ.set(desiredPos);
        }

        public void telescopeGoto(Point3d j2000Pos) {
            desiredPos = new Vector3d(j2000Pos);
            desiredPos.normalize();
        }

        Point3d XYZ;// j2000 position

        Vector3d desiredPos;
    }

    // TODO: FRED I'm here

    public static Telescope create(String url) {
        return null;
    }

    public void close() {
        // Nothing by default
    }

    public String getEnglishName() {
        return name;
    }

    public String getNameI18n() {
        return nameI18n;
    }

    public String getInfoString(NavigatorIfc nav) {
        return "";
    }

    public String getShortInfoString(NavigatorIfc nav) {
        return "";
    }

    public TYPE getType() {
        return StelObject.TYPE.TELESCOPE;
    }

    public Point3d getEarthEquPos(NavigatorIfc nav) {
        return nav.j2000ToEarthEqu(getObsJ2000Pos(nav));
    }

    public void telescopeGoto(Point3d j2000Pos) {

    }

    public abstract boolean isConnected();

    public abstract boolean hasKnownPosition();

    // all TCP (and all possible other style) communication shall be done in
    // these functions:
    public abstract void prepareSelectFds();

    public abstract void handleSelectFds();

    protected Telescope(String name, Logger parentLogger) {
        super(parentLogger);
    }

    private ByteBuffer read_fds;

    private ByteBuffer write_fds;

    protected String nameI18n;

    protected String name;

    private boolean isInitialized() {
        return true;
    }

    public float getMag(NavigatorIfc nav) {
        return -10.f;
    }

}