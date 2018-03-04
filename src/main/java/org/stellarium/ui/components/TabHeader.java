/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:04:21 AM
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
import org.stellarium.vecmath.Vector2i;


/**
 * Everything to handle tabs
 */
public class TabHeader extends LabeledButton {
    public TabHeader(StellariumComponent c, String _label) {
        super(_label, null);
        assoc = c;
    }

    public void draw() {
        if (!visible) {
            return;
        }
        painter.drawSquareFill(pos, size, painter.getBaseColor().mul(0.7f + 0.3f * (active ? 1 : 0)));
        if (!active) {
            painter.drawSquareEdge(pos, size);
        } else {
            painter.drawLine(pos, new Vector2i(pos.i0, pos.i1 + size.i1));
            painter.drawLine(new Vector2i(pos.i0 + 1, pos.i1), new Vector2i(pos.i0 + size.i0 - 1, pos.i1));
            painter.drawLine(new Vector2i(pos.i0 + size.i0 - 1, pos.i1), pos.plus(size).minus(new Vector2i(1, 0)));
        }
        // Draw laeb
        SglAccess.glPushMatrix();
        SglAccess.glTranslatef(pos.i0, pos.i1, 0.f);
        scissor.push(pos, size);
        label.setPos((size.minus(label.getSize())).div(2).plus(new Vector2i(1, 0)));
        label.draw();
        scissor.pop();
        SglAccess.glPopMatrix();
    }

    void setActive(boolean s) {
        active = s;
        if (active) {
            assoc.setVisible(true);
        } else {
            assoc.setVisible(false);
        }
    }

    protected StellariumComponent assoc;
}
