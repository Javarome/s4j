/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:05:06 AM
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

import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;


/**
 * Widget used to set time and date
 */
public class TimeItem extends StelContainer {
    public TimeItem(SFontIfc someFont, STexture texUp, STexture texDown) {
        this(someFont, texUp, texDown, 2451545.0);
    }

    /**
     * Creates a new Time Item.
     *
     * @param someFont Item's font.
     * @param texUp    Texture when up
     * @param texDown  Texture when up
     * @param someJD   The Julian Day
     */
    TimeItem(SFontIfc someFont, STexture texUp, STexture texDown, double someJD) {
        if (someFont != null) {
            setFont(someFont);
        }

        y = new IntIncDec(getFont(), texUp, texDown, -9999, 99999, 1930, 1);
        m = new IntIncDec(getFont(), texUp, texDown, 1, 12, 12, 1);
        d = new IntIncDec(getFont(), texUp, texDown, 1, 31, 11, 1);
        h = new IntIncDec(getFont(), texUp, texDown, 0, 23, 16, 1);
        mn = new IntIncDec(getFont(), texUp, texDown, 0, 59, 35, 1);
        s = new IntIncDec(getFont(), texUp, texDown, 0, 59, 23, 1);

        y.setOnPressCallback(new StelCallback() {
            public void execute() {
                onTimeChange();
            }
        });
        m.setOnPressCallback(new StelCallback() {
            public void execute() {
                onTimeChange();
            }
        });
        d.setOnPressCallback(new StelCallback() {
            public void execute() {
                onTimeChange();
            }
        });
        h.setOnPressCallback(new StelCallback() {
            public void execute() {
                onTimeChange();
            }
        });
        mn.setOnPressCallback(new StelCallback() {
            public void execute() {
                onTimeChange();
            }
        });
        s.setOnPressCallback(new StelCallback() {
            public void execute() {
                onTimeChange();
            }
        });

        StelLabel l1 = new StelLabel("Year");
        l1.setPos(5, 5);
        y.setPos(50, 5);
        y.setSize(50, 32);

        StelLabel l2 = new StelLabel("Month");
        l2.setPos(5, 25);
        m.setPos(50, 25);
        m.setSize(50, 32);

        StelLabel l3 = new StelLabel("Day");
        l3.setPos(5, 45);
        d.setPos(50, 45);
        d.setSize(50, 32);

        StelLabel l4 = new StelLabel("Hour");
        l4.setPos(130, 5);
        h.setPos(190, 5);
        h.setSize(50, 32);

        StelLabel l5 = new StelLabel("Minutes");
        l5.setPos(130, 25);
        mn.setPos(190, 25);
        mn.setSize(50, 32);

        StelLabel l6 = new StelLabel("Seconds");
        l6.setPos(130, 45);
        s.setPos(190, 45);
        s.setSize(50, 32);

        setSize(230, 65);

        addComponent(y);
        addComponent(m);
        addComponent(d);
        addComponent(h);
        addComponent(mn);
        addComponent(s);

        addComponent(l1);
        addComponent(l2);
        addComponent(l3);
        addComponent(l4);
        addComponent(l5);
        addComponent(l6);

        setJDay(someJD);
    }

    public double getJDay() {
        int iy, im, id, ih, imn, is;

        iy = (int) y.getValue();
        im = (int) m.getValue();
        id = (int) d.getValue();
        ih = (int) h.getValue();
        imn = (int) mn.getValue();
        is = (int) s.getValue();

        if (im <= 2) {
            iy -= 1;
            im += 12;
        }

        // Correct for the lost days in Oct 1582 when the Gregorian calendar
        // replaced the Julian calendar.
        int B = -2;
        if (iy > 1582 || (iy == 1582 && (im > 10 || (im == 10 && id >= 15)))) {
            B = iy / 400 - iy / 100;
        }

        return Math.floor(365.25 * iy) + Math.floor(30.6001 * (im + 1)) + B + 1720996.5 + id + ih / 24.0 + imn / 1440.0 + (double) is / 86400.0;
    }

    /**
     * for use with commands - no special characters, just the local date
     *
     * @return
     */
    public String getDateString() {
        StringBuffer os = new StringBuffer();
        os.append(y.getValue()).append(":")
                .append(m.getValue()).append(":")
                .append(d.getValue()).append("T")
                .append(h.getValue()).append(":")
                .append(mn.getValue()).append(":")
                .append(s.getValue());
        return os.toString();
    }

    public void setJDay(double JD) {
        int iy, im, id, ih, imn, is;

        int a = (int) (JD + 0.5);
        double c;
        if (a < 2299161) {
            c = a + 1524;
        } else {
            double b = (int) ((a - 1867216.25) / 36524.25);
            c = a + b - (int) (b / 4) + 1525;
        }

        int dd = (int) ((c - 122.1) / 365.25);
        int e = (int) (365.25 * dd);
        int f = (int) ((c - e) / 30.6001);

        double dday = c - e - (int) (30.6001 * f) + ((JD + 0.5) - (int) (JD + 0.5));

        im = f - 1 - 12 * (f / 14);
        iy = dd - 4715 - (int) ((7.0 + im) / 10.0);
        id = (int) dday;

        double dhour = (dday - id) * 24;
        ih = (int) dhour;

        double dminute = (dhour - ih) * 60;
        imn = (int) dminute;

        is = (int) Math.round((dminute - imn) * 60);

        y.setValue(iy);
        m.setValue(im);
        d.setValue(id);
        h.setValue(ih);
        mn.setValue(imn);
        s.setValue(Math.round(is));
    }

    public void draw() {
        if (!visible) return;
        painter.drawSquareEdge(pos, size);
        painter.drawSquareFill(pos, size);
        draw();
    }

    void onTimeChange() {
        if (onChangeTimeCallback != null) onChangeTimeCallback.execute();
    }

    public void setOnChangeTimeCallback(StelCallback c) {
        onChangeTimeCallback = c;
    }

    protected IntIncDec d, m, y, h, mn, s;

    protected StelCallback onChangeTimeCallback;
}
