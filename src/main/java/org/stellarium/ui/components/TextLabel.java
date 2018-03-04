/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:02:42 AM
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


/**
 * A text bloc
 */
public class TextLabel extends StelContainer {
    public TextLabel(String someLabel) {
        this(someLabel, null);
    }

    public TextLabel(String someLabel, SFontIfc someFont) {
        super();
        if (someFont != null) {
            painter.setFont(someFont);
        }
        setLabel(someLabel);
        adjustSize();
    }

    public TextLabel() {
        this(null);
    }

    public void setLabel(String _label) {
        if (_label == null)
            label = "";
        else
            label = _label;
        childs.clear();

        StelLabel tempLabel;
        String pch;

        int i = 0;
        int lineHeight = (int) painter.getFont().getLineHeight() + 1;

        BufferedReader is = new BufferedReader(new StringReader(label));
        try {
            while ((pch = is.readLine()) != null) {
                tempLabel = new StelLabel(pch);
                tempLabel.setPainter(painter);
                tempLabel.setPos(0, i * lineHeight);
                tempLabel.adjustSize();
                addComponent(tempLabel);
                ++i;
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read lines in label: " + label, e);
        }
    }

    public void adjustSize() {
        int maxX = 0;
        for (StellariumComponent iter : childs) {
            if (iter.getSizeX() > maxX) {
                maxX = iter.getSizeX();
            }
        }
        setSize(maxX, childs.size() * ((int) painter.getFont().getLineHeight() + 1));
    }

    public void setTextColor(SColor c) {
        for (StellariumComponent iter : childs) {
            iter.setTextColor(c);
        }
    }

    protected String label;
}
