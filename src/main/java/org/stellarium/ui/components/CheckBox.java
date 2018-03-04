/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:00:27 AM
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

import java.awt.event.MouseEvent;


/**
 * Button with a cross on it
 */
public class CheckBox extends Button {
    public CheckBox(boolean state) {
        isChecked = state;
    }

    public void draw() {
        if (!visible) return;
        if (isChecked) painter.drawCross(pos, size);
        super.draw();
    }

    public void mouseClicked(MouseEvent e) {
        if (visible) {
            int x = e.getX();
            int y = e.getY();
            int bt = e.getButton();
            if (bt == MouseEvent.BUTTON1 && isIn(x, y)) {
                isChecked = !isChecked;
            }
            super.mouseClicked(e);
        }
    }

    public boolean getState() {
        return isChecked;
    }

    public void setState(boolean s) {
        isChecked = s;
    }

    protected boolean isChecked;
}
