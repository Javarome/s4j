/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:42:56 PM
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

import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontIfc;


/**
 * Text label
 */
public class StelLabel extends StellariumComponent {

    public StelLabel(String someLabel, SFontIfc someFont) {
        super();
        setLabel(someLabel);
        if (someFont != null) {
            painter.setFont(someFont);
        }
//        adjustSize();
    }

    public StelLabel(String label) {
        this(label, null);
    }

    public StelLabel() {
        this("");
    }

    public void setLabel(String someLlabel) {
        label = someLlabel;
    }

    public void draw() {
        if (!visible) {
            return;
        }

        if (painter.getFont() != null) {
            painter.print(pos.i0, pos.i1 + size.i1, label);
        }
    }

    public void draw(float someIntensity) {
        if (!visible) {
            return;
        }

        if (painter.getFont() != null) {
            SColor textColor = new SColor(painter.getTextColor());
            textColor.scale(someIntensity);
            painter.print(pos.i0, pos.i1 + size.i1, label, textColor);
        }
    }

    public void adjustSize() {
        if (label == null) {
            label = "";
        }
        size.i0 = (int) Math.ceil(painter.getFont().getStrLen(label));
        size.i1 = (int) Math.ceil(painter.getFont().getLineHeight());
    }

    public String getLabel() {
        return label;
    }

    protected String label;
}
