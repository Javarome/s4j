/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:56:49 PM
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

import org.stellarium.StellariumException;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.vecmath.Vector2i;

import java.awt.event.MouseEvent;


public abstract class CallbackComponent extends StellariumComponent {
    protected CallbackComponent() {
        super();
    }

    public boolean callbackOnClic(int x, int y, int bt, S_GUI_VALUE state) throws StellariumException {
        if (!visible) {
            return false;
        }
        if (state == S_GUI_VALUE.S_GUI_PRESSED && bt == MouseEvent.BUTTON1 && isIn(x, y)) {
            if (onPressCallback != null) {
                onPressCallback.execute();
            }
        }
        return false;
    }

    public boolean callbackOnMove(int x, int y) throws StellariumException {
        if (!visible) {
            return false;
        }
        if (isIn(x, y)) {
            if (onMouseInOutCallback != null && !isMouseOver) {
                isMouseOver = true;
                onMouseInOutCallback.execute();
            }
            isMouseOver = true;
        } else {
            if (isMouseOver) {
                if (onMouseInOutCallback != null && isMouseOver) {
                    isMouseOver = false;
                    onMouseInOutCallback.execute();
                }
            }
            isMouseOver = false;
        }
        return false;
    }

    int getPosx() {
        return pos.i0;
    }

    int getPosy() {
        return pos.i1;
    }

    public int getSizeX() {
        return size.i0;
    }

    public int getSizeY() {
        return size.i1;
    }

    public void setSizeX(int v) {
        size.i0 = v;
    }

    void setSizey(int v) {
        size.i1 = v;
    }

    void setPosx(int v) {
        pos.i0 = v;
    }

    void setPosy(int v) {
        pos.i1 = v;
    }

    Vector2i getPos() {
        return pos;
    }

    public Vector2i getSize() {
        return size;
    }

    public void setPos(Vector2i _pos) {
        pos = _pos;
    }

    public void setSize(Vector2i _size) {
        size = _size;
    }

    public void setPos(int x, int y) {
        pos.i0 = x;
        pos.i1 = y;
    }

    public void setSize(int w, int h) {
        size.i0 = w;
        size.i1 = h;
    }

    public void setVisible(boolean _visible) {
        visible = _visible;
    }

    public boolean getVisible() {
        return visible;
    }

    void setActive(boolean isActive) {
        active = isActive;
    }

    boolean getActive() {
        return active;
    }

    void setFocus(boolean isFocus) {
        focus = isFocus;
    }

    boolean getFocus() {
        return focus;
    }

    public void setTexture(STexture tex) {
        painter.setTexture(tex);
    }

    void setFont(SFontIfc f) {
        painter.setFont(f);
    }

    public void mouseClicked(MouseEvent e) {
        callbackOnClic(e.getX(), e.getY(), e.getButton(), S_GUI_VALUE.S_GUI_PRESSED);
    }

    public void mouseMoved(MouseEvent e) {
        callbackOnMove(e.getX(), e.getY());
    }

    public void setOnMouseInOutCallback(StelCallback c) {
        onMouseInOutCallback = c;
    }

    public void setOnPressCallback(StelCallback c) {
        onPressCallback = c;
    }

    public boolean getIsMouseOver() {
        return isMouseOver;
    }

    protected StelCallback onPressCallback;

    protected StelCallback onMouseInOutCallback;

    protected boolean isMouseOver;
}
