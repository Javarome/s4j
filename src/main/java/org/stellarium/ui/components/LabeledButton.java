/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:01:07 AM
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

import org.stellarium.ui.SglAccess;
import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.vecmath.Vector2i;


/**
 * Button with text on it
 */
public class LabeledButton extends Button {
    public static final int LABEL_PAD = 10;

    public LabeledButton(String label) {
        this(label, null);
    }

    public LabeledButton(String someLabel, SFontIfc someFont) {
        this(someLabel, someFont, Justification.JUSTIFY_CENTER);
    }

    public LabeledButton(String someLabel, SFontIfc someFont, Justification j) {
        this(someLabel, someFont, j, false);
    }

    public LabeledButton(String someLabel, SFontIfc someFont,
                         Justification j, boolean brigth) {
        this.label = new StelLabel(someLabel, someFont);
        //setFont(someFont);
        setJustification(j);
        setBright(brigth);
        super.setSize(label.getSize().plus(new Vector2i(14, 12 + (int) (this.label.getFont().getDescent()))));
        justify();
    }

    void justify() {
        switch (justification) {
            case JUSTIFY_CENTER:
                label.setPos((size.i0 - label.getSizeX()) / 2, (size.i1 - label.getSizeY()) / 2 - (int) label.getFont().getDescent() + 1);
                break;
            case JUSTIFY_LEFT:
                label.setPos(0 + LABEL_PAD, (size.i1 - label.getSizeY()) / 2 - (int) label.getFont().getDescent() + 1);
                break;
            case JUSTIFY_RIGHT:
                label.setPos(size.i0 - label.getSizeX() - LABEL_PAD, (size.i1 - label.getSizeY()) / 2 - (int) label.getFont().getDescent() + 1);
                break;
        }
    }

    void adjustSize() {
        super.setSize(label.getSize().plus(new Vector2i(0, (int) label.getFont().getDescent())));
        justify();
    }

    public void setColorScheme(SColor baseColor, SColor textColor) {
        if (!guiColorSchemeMember) return;
        label.setColorScheme(baseColor, textColor);
        super.setColorScheme(baseColor, textColor);
    }

    public void draw() {
        if (visible) {
            super.draw();
            SglAccess.glPushMatrix();
            SglAccess.glTranslatef(pos.i0, pos.i1, 0.f);
            scissor.push(pos, size);
            label.setPos((size.minus(label.getSize())).div(2));
            label.draw();
            scissor.pop();
            SglAccess.glPopMatrix();
        }
    }

    void setActive(boolean state) {
        super.setActive(state);
        label.setActive(state);
    }

    public void setFont(SFontIfc someFont) {
        super.setFont(someFont);
        label.setFont(someFont);
    }

    public void setTextColor(SColor c) {
        super.setTextColor(c);
        label.setTextColor(c);
    }

    public void setPainter(Painter p) {
        super.setPainter(p);
        label.setPainter(p);
    }

    public void setJustification(Justification justification) {
        this.justification = justification;
    }

    public String getLabel() {
        return label.getLabel();
    }

    public void setLabel(String label) {
        this.label.setLabel(label);
    }

    public void setBright(boolean bright) {
        isBright = bright;
    }

    protected StelLabel label;

    Justification justification;

    boolean isBright;
}
