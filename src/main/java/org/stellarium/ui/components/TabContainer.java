/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:04:31 AM
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
import org.stellarium.vecmath.Vector2i;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


public class TabContainer extends StelContainer {

    public TabContainer(SFontIfc someFont) {
        if (someFont != null) {
            painter.setFont(someFont);
        }
    }

    public TabContainer() {
        this(null);
    }

    public void addTab(StellariumComponent c, String name) {
        StelContainer tempInside = new StelContainer();
        tempInside.reshape(pos.i0, pos.i1 + headerHeight, size.i0, size.i1 - headerHeight);
        tempInside.addComponent(c);

        TabHeader tempHead = new TabHeader(tempInside, name);
        tempHead.setPainter(painter);
        tempHead.reshape(getHeadersSize(), 0, tempHead.getSizeX() + 4, headerHeight);
        headers.add(0, tempHead);

        addComponent(tempInside);
        addComponent(tempHead);
        select(tempHead);
    }

    public void draw() {
        if (!visible) return;
        painter.drawSquareFill(pos, size, painter.getBaseColor().div(3));
        painter.drawLine(new Vector2i(pos.i0 + getHeadersSize(), pos.i1 + headerHeight - 1),
                new Vector2i(pos.i0 + size.i0, pos.i1 + headerHeight - 1));
        super.draw();
    }

    int getHeadersSize() {
        int s = 0;
        for (TabHeader iter : headers) {
            s += iter.getSizeX();
        }
        return s;
    }

    public void mouseClicked(MouseEvent e) {
        if (visible) {
            for (TabHeader iter : headers) {
                int x = e.getX();
                int y = e.getY();
                MouseEvent newEvent = new MouseEvent((java.awt.Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), x - pos.i0, y - pos.i1, e.getClickCount(), e.isPopupTrigger(), e.getButton());
                iter.mouseClicked(newEvent);
                if (newEvent.isConsumed()) {
                    select(iter);
                    return;
                }
            }
            super.mouseClicked(e);
        }
    }

    void select(TabHeader t) {
        for (StellariumComponent iter : childs) {
            iter.setVisible(false);
        }
        for (TabHeader iter2 : headers) {
            iter2.setVisible(true);
            if (iter2 == t) iter2.setActive(true);
            else iter2.setActive(false);
        }
    }

    protected List<TabHeader> headers = new ArrayList<TabHeader>();

    protected int headerHeight;
}
