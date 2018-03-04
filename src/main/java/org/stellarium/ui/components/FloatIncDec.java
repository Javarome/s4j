/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:04:56 AM
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


public class FloatIncDec extends StelContainer {
    public FloatIncDec(SFontIfc someFont, STexture texUp, STexture texDown, double someMin, double someMax, double initValue, double someInc) {
        value = (float) initValue;
        min = (float) someMin;
        max = (float) someMax;
        inc = (float) someInc;
        label = new StelLabel();
        if (someFont != null) {
            label.setFont(someFont);
        }
        label.setSize(30, 10);
        label.setPos(9, 2);
        btless = new TexturedButton(texUp);
        btless.setSize(8, 8);
        btless.setPos(0, 0);
        btless.setBaseColor(painter.getTextColor());
        btless.setOnPressCallback(new StelCallback() {
            public void execute() {
                incValue();
            }
        });
        btmore = new TexturedButton(texDown);
        btmore.setSize(8, 8);
        btmore.setPos(0, 7);
        btmore.setBaseColor(painter.getTextColor());
        btmore.setOnPressCallback(new StelCallback() {
            public void execute() {
                decValue();
            }
        });
        addComponent(btmore);
        addComponent(btless);
        addComponent(label);
        setSize(50, 20);
    }

    public void draw() {
        if (!visible) return;
        label.setLabel("" + value);
        draw();
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        value = v;
        if (value > max) value = max;
        if (value < min) value = min;
    }

    protected void incValue() {
        value += inc;
        if (value > max) value = max;
        if (onPressCallback != null) onPressCallback.execute();
    }

    void decValue() {
        value -= inc;
        if (value < min) value = min;
        if (onPressCallback != null) onPressCallback.execute();
    }

    float value, min, max, inc;

    TexturedButton btmore;

    TexturedButton btless;

    StelLabel label;
}
