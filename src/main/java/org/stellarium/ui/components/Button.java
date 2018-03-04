/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:59:56 PM
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
 * Simplest button with one press Callback
 */
public class Button extends CallbackComponent {
    public static final int BUTTON_HEIGHT = 25;

    public Button() {
        size.set(10, 10);
    }

    public void draw() {
        if (!visible) return;
        painter.drawSquareEdge(pos, size, painter.getBaseColor().mul((1.f + 0.4f * (isMouseOver ? 1 : 0))));
    }

    public void mouseClicked(MouseEvent e) {
        if (visible) {
            int bt = e.getButton();
            int x = e.getX();
            int y = e.getY();
            callbackOnClic(x, y, bt, S_GUI_VALUE.S_GUI_PRESSED);
            if (bt == MouseEvent.BUTTON1 && isIn(x, y)) {
                e.consume();
            }
        }
    }

    public void setHideBorder(boolean _b) {
        hideBorder = _b;
    }

    public void setHideBorderMouseOver(boolean _b) {
        hideBorderMouseOver = _b;
    }

    public void setHideTexture(boolean _b) {
        hideTexture = _b;
    }

    protected boolean hideBorder;

    protected boolean hideBorderMouseOver;

    protected boolean hideTexture;
}
