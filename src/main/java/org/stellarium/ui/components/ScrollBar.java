/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:02:05 AM
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

import java.awt.event.MouseEvent;


public class ScrollBar extends CallbackComponent {
    public ScrollBar(boolean _vertical, int _totalElements, int _elementsForBar) {
        vertical = _vertical;
        elements = _totalElements;
        elementsForBar = _elementsForBar;
    }

    public void setOnChangeCallback(StelCallback c) {
        onChangeCallback = c;
    }

    public void draw() {
        if (!visible) {
            return;
        }

        painter.drawSquareEdge(pos, size);
        SglAccess.glPushMatrix();
        SglAccess.glTranslatef(pos.i0, pos.i1, 0.f);
        scissor.push(pos, size);

        if (!sized) {
            adjustSize();
        }
        scrollBt.draw();
        scissor.pop();
        SglAccess.glPopMatrix();

        String oss = String.valueOf(value);
        painter.print(pos.i0 + 2, pos.i1 + 2, oss);
    }

    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            dragging = false;
        }
    }

    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (visible && isIn(x, y)) {
            if (scrollBt.isIn(x - pos.i0, y - pos.i1)) {
                oldPos.set(x - pos.i0, y - pos.i1);
                oldValue = value;
                dragging = true;
            } else {
                int n;

                if (vertical)
                    n = y - pos.i1;
                else
                    n = x - pos.i0;

                if (n < scrollOffset) setValue(value - 1);
                else if (n > scrollOffset + scrollSize) setValue(value + 1);

            }
            e.consume();
        }
    }

    public void mouseMoved(MouseEvent e) {
        float delta, v;

        if (!visible) return;

        int x = e.getX();
        int y = e.getY();
        MouseEvent scrollBtMouseEvent = new MouseEvent((java.awt.Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), x - pos.i0, y - pos.i1, e.getClickCount(), e.isPopupTrigger());
        scrollBt.mouseMoved(scrollBtMouseEvent);

        if (!isIn(x, y)) {
            dragging = false;
            return;
        }

        if (!dragging) {
            return;
        }

        if (vertical) {
            delta = ((float) (y - pos.i1) - oldPos.i1) / (size.i1 - scrollSize);
        } else {
            delta = ((float) (x - pos.i0) - oldPos.i0) / (size.i0 - scrollSize);
        }

        v = (float) oldValue + delta * (elements - elementsForBar);

        if (v < 0) {
            v = 0;
        } else if (v > elements - elementsForBar) {
            v = elements - elementsForBar;
        }

        setValue((int) v);
        if (onChangeCallback != null) {
            onChangeCallback.execute();
        }
    }

    void setTotalElements(int _elements) {
        elements = _elements;
        sized = false;
    }

    void setElementsForBar(int _elementsForBar) {
        if (_elementsForBar > elements)
            elementsForBar = elements;
        else
            elementsForBar = _elementsForBar;
        sized = false;
    }

    int getValue() {
        return value;
    }

    private StelCallback onChangeCallback;

    private void adjustSize() {
        int s;

        if (vertical) {
            s = getSizeY();
            setSize(SGUI.SCROLL_SIZE, s);
        } else {
            s = getSizeX();
            setSize(s, SGUI.SCROLL_SIZE);
        }

        scrollSize = (int) (((float) elementsForBar / elements) * s) - 2;
        scrollOffset = (int) (((float) firstElement / elements) * s) + 1;

        if (value >= elements - elementsForBar && s - scrollSize - scrollOffset > 1) {
            scrollOffset = s - scrollSize - 1;
        }

        if (vertical) {
            scrollBt.setSize(getSizeX() - 2, scrollSize);
            scrollBt.setPos(1, scrollOffset);
        } else {
            scrollBt.setSize(scrollSize, getSizeY() - 2);
            scrollBt.setPos(scrollOffset, 1);
        }
        sized = true;
    }

    void setValue(int _value) {
        value = _value;
        firstElement = value;
        if (onChangeCallback != null) {
            onChangeCallback.execute();
        }
        sized = false;
    }

    private Button scrollBt;

    private boolean vertical = true;

    private int scrollOffset, scrollSize;

    private int elements = 1, elementsForBar = 1;

    private boolean dragging;

    private int value;

    private int firstElement;

    private boolean sized;

    private Vector2i oldPos;

    private int oldValue;
}
