/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:58:07 PM
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

import org.stellarium.vecmath.Vector2i;

import java.awt.event.MouseEvent;
import java.util.LinkedList;


/**
 * ClicList
 */
public class StringList extends CallbackComponent {
    public StringList() {
        elemsSize = 0;
        items.add("");
        current = items.getLast();
        itemSize = (int) painter.getFont().getLineHeight() + 1;
        size.i0 = 100;
        size.i1 = 100;
    }

    public void draw() {
        if (!visible) return;
        painter.drawSquareEdge(pos, size);

        int y = 0;
        int x = pos.i0;

        for (String item : items) {
            if (item.equals(current)) {
                painter.print(x + 3, pos.i1 + y + 2, item, painter.getTextColor().mul(2));
            } else {
                painter.print(x + 2, pos.i1 + y + 2, item);
            }
            painter.drawLine(new Vector2i(x, pos.i1 + y), new Vector2i(x + size.i0, pos.i1 + (int) y));
            y += itemSize;
            if (elemsSize > size.i1 && y + 2 * itemSize > size.i1) {
                painter.drawLine(new Vector2i(x, pos.i1 + y), new Vector2i(x + size.i0, pos.i1 + (int) y));
                painter.drawLine(new Vector2i(x + 5, pos.i1 + y + 5), new Vector2i(x - 5 + size.i0, pos.i1 + (int) y + 5));
                painter.drawLine(new Vector2i(x + 5, pos.i1 + y + 5), new Vector2i(x + size.i0 / 2, pos.i1 + size.i1 - 5));
                painter.drawLine(new Vector2i(x + size.i0 / 2, pos.i1 + size.i1 - 5), new Vector2i(x - 5 + size.i0, pos.i1 + (int) y + 5));
                return;
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (visible && isIn(x, y)) {
            int poss = (y - pos.i1) / itemSize;
            if (poss < 0 || poss >= items.size()) {
                e.consume();
                return;
            }
            current = items.get(poss);
            if (onPressCallback != null) onPressCallback.execute();
            e.consume();
        }
    }

    public void addItem(String newitem) {
        items.add(newitem);
        current = items.getFirst();
        elemsSize += itemSize;
    }

    public String getValue() {
        if (current == null || current.equals(items.getLast())) {
            return "";
        } else {
            return current;
        }
    }

    public boolean setValue(String s) {
        for (String iter : items) {
            if (s.equals(iter)) {
                current = iter;
                return true;
            }
        }
        return false;
    }

    public void adjustSize() {
        setSizey(elemsSize);
    }

    private int elemsSize;

    private int itemSize;

    private LinkedList<String> items = new LinkedList<String>();

    private String current;
}
